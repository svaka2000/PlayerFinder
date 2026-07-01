package com.playerfinder.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.playerfinder.config.FinderMember;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal Minecraft <b>Server List Ping</b> (status) client — the exact request the multiplayer screen
 * makes to show a server's player count. It opens a TCP connection, performs the status handshake, and
 * reads the JSON status, which may include a {@code players.sample} (online names + UUIDs).
 *
 * <p>This never logs in, joins, or sends a single gameplay packet — it's an out-of-band status query to
 * servers the user has explicitly listed. (Render/info-only, anticheat-safe.)
 *
 * <p>Limitation: the status sample is capped (~12 random names on vanilla) and some networks disable or
 * fake it. We ping a few times and union the samples to surface more of a busy server's list, but a
 * specific player on a large server that hides the sample can't be confirmed this way.
 */
public final class ServerPinger {
    private ServerPinger() {}

    private static final ExecutorService POOL = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "PlayerFinder-pinger");
        t.setDaemon(true);
        return t;
    });

    /** Any recent protocol number; status responses are version-agnostic. */
    private static final int PROTOCOL = 767;

    public static final class Result {
        public boolean reachable;
        public int online = -1;
        public int max = -1;
        public final List<FinderMember> sample = new ArrayList<>();
        /** Reachable with players online, but the server returned no usable sample names. */
        public boolean sampleHidden;
        /** How many SLP passes we ended up sending. */
        public int pings;
        /** True if the full player list came from the Query protocol (no 12-name cap). */
        public boolean viaQuery;
        /** Non-null on failure (timeout / unknown host / refused). */
        public String error;
    }

    /** Scan a server off the game thread: one SLP ping, then (optionally) a full-list Query, then
     *  adaptive SLP passes that union the random ~12-name samples until the list converges — which is
     *  how we get past the per-ping 12-name cap on servers that expose a real, rotating sample. */
    public static CompletableFuture<Result> ping(String host, int port, int timeoutMs,
                                                 int minPings, int maxPings, int stableRounds,
                                                 int delayMs, boolean useQuery) {
        return CompletableFuture.supplyAsync(
                () -> pingBlocking(host, port, timeoutMs, minPings, maxPings, stableRounds, delayMs, useQuery),
                POOL);
    }

    private static Result pingBlocking(String host, int port, int timeoutMs, int minPings, int maxPings,
                                       int stableRounds, int delayMs, boolean useQuery) {
        Result r = new Result();
        minPings = Math.max(1, minPings);
        maxPings = Math.max(minPings, maxPings);

        // Resolve SRV once (most public servers publish host+port this way); fall back to host:port.
        String connectHost = host;
        int connectPort = port;
        SrvResolver.Endpoint srv = SrvResolver.resolve(host);
        if (srv != null) {
            connectHost = srv.host();
            connectPort = srv.port();
        }

        Map<String, FinderMember> union = new LinkedHashMap<>();

        // 1) First SLP ping — reachability + online/max + an initial sample.
        try {
            Result one = pingOnce(connectHost, connectPort, host, port, timeoutMs);
            r.reachable = true;
            r.online = one.online;
            r.max = one.max;
            mergeSample(union, one.sample);
            r.pings = 1;
        } catch (Exception e) {
            r.error = friendly(e);
        }

        // 2) Query protocol (full list in one shot) if the server allows it.
        if (useQuery && r.reachable) {
            List<String> queryNames = QueryClient.fullStatPlayers(connectHost, connectPort, timeoutMs);
            if (!queryNames.isEmpty()) {
                for (String nm : queryNames) mergeSample(union, List.of(new FinderMember(nm, null)));
                r.viaQuery = true;
            }
        }

        // 3) Adaptive SLP passes: keep unioning random samples until the list converges. Skip if the
        //    query already gave us the whole list, or the server isn't reachable.
        boolean haveAll = r.viaQuery && r.online >= 0 && union.size() >= r.online;
        if (r.reachable && !haveAll) {
            int stable = 0;
            while (r.pings < maxPings) {
                if (r.online >= 0 && union.size() >= r.online) break;          // got everyone
                if (r.pings >= minPings && stable >= stableRounds) break;       // sample converged
                if (delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                int before = union.size();
                try {
                    Result one = pingOnce(connectHost, connectPort, host, port, timeoutMs);
                    if (one.online >= 0) r.online = one.online;
                    if (one.max >= 0) r.max = one.max;
                    mergeSample(union, one.sample);
                } catch (Exception ignored) {
                    // transient mid-scan failure — treat as a no-growth pass
                }
                r.pings++;
                if (union.size() > before) stable = 0;
                else stable++;
            }
        }

        r.sample.addAll(union.values());
        r.sampleHidden = r.reachable && r.online > 0 && r.sample.isEmpty();
        return r;
    }

    private static void mergeSample(Map<String, FinderMember> union, List<FinderMember> add) {
        for (FinderMember m : add) {
            String key = m.uuid != null ? m.uuid.toLowerCase(Locale.ROOT)
                    : "name:" + (m.name == null ? "" : m.name.toLowerCase(Locale.ROOT));
            union.putIfAbsent(key, m);
        }
    }

    private static Result pingOnce(String connectHost, int connectPort, String handshakeHost,
                                   int handshakePort, int timeoutMs) throws IOException {
        Result r = new Result();
        try (Socket sock = new Socket()) {
            sock.connect(new InetSocketAddress(connectHost, connectPort), timeoutMs);
            sock.setSoTimeout(timeoutMs);
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));
            DataInputStream in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));

            // Handshake: next state = 1 (status)
            ByteArrayOutputStream hs = new ByteArrayOutputStream();
            DataOutputStream h = new DataOutputStream(hs);
            writeVarInt(h, 0x00);
            writeVarInt(h, PROTOCOL);
            writeString(h, handshakeHost);
            h.writeShort(handshakePort & 0xFFFF);
            writeVarInt(h, 1);
            writePacket(out, hs.toByteArray());

            // Status request (empty body)
            ByteArrayOutputStream sr = new ByteArrayOutputStream();
            writeVarInt(new DataOutputStream(sr), 0x00);
            writePacket(out, sr.toByteArray());
            out.flush();

            // Status response
            readVarInt(in);                 // packet length (ignored)
            int id = readVarInt(in);        // packet id
            if (id != 0x00) throw new IOException("unexpected status packet id " + id);
            int jsonLen = readVarInt(in);
            if (jsonLen < 0 || jsonLen > (1 << 22)) throw new IOException("bad status length " + jsonLen);
            byte[] buf = new byte[jsonLen];
            in.readFully(buf);
            parse(new String(buf, StandardCharsets.UTF_8), r);
            r.reachable = true;
            return r;
        }
    }

    private static void parse(String json, Result r) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return;
            JsonObject o = root.getAsJsonObject();
            if (o.has("players") && o.get("players").isJsonObject()) {
                JsonObject p = o.getAsJsonObject("players");
                if (p.has("online") && !p.get("online").isJsonNull()) r.online = p.get("online").getAsInt();
                if (p.has("max") && !p.get("max").isJsonNull()) r.max = p.get("max").getAsInt();
                if (p.has("sample") && p.get("sample").isJsonArray()) {
                    for (JsonElement el : p.getAsJsonArray("sample")) {
                        if (!el.isJsonObject()) continue;
                        JsonObject s = el.getAsJsonObject();
                        String name = s.has("name") && !s.get("name").isJsonNull() ? s.get("name").getAsString() : null;
                        String id = s.has("id") && !s.get("id").isJsonNull() ? s.get("id").getAsString() : null;
                        if (name != null) name = name.replaceAll("§.", "").trim(); // strip colour codes
                        // Drop decorative MOTD-in-sample entries (real players never carry the nil UUID).
                        if (id != null && id.replace("-", "").matches("0{32}")) continue;
                        if ((name == null || name.isEmpty()) && id == null) continue;
                        r.sample.add(new FinderMember(name, id));
                    }
                }
            }
        } catch (Exception ignored) {
            // malformed status JSON — leave whatever we parsed
        }
    }

    private static String friendly(Exception e) {
        if (e instanceof java.net.SocketTimeoutException) return "timeout";
        if (e instanceof java.net.UnknownHostException) return "unknown host";
        if (e instanceof java.net.ConnectException) return "offline / refused";
        String m = e.getMessage();
        return (m != null && !m.isBlank()) ? m : e.getClass().getSimpleName();
    }

    // --- protocol helpers ---
    private static void writePacket(DataOutputStream out, byte[] data) throws IOException {
        writeVarInt(out, data.length);
        out.write(data);
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, b.length);
        out.write(b);
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0, result = 0;
        byte read;
        do {
            read = in.readByte();
            result |= (read & 0x7F) << (7 * numRead);
            if (++numRead > 5) throw new IOException("VarInt too big");
        } while ((read & 0x80) != 0);
        return result;
    }
}
