package com.playerfinder.net;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft <b>Query</b> (GameSpy GS4/UT3) client over UDP. If a server sets {@code enable-query=true}
 * in server.properties, its full-stat query returns the <b>entire</b> online player list in one
 * response — bypassing the ~12-name cap of the Server List Ping sample.
 *
 * <p>Most servers leave query disabled, in which case there's simply no UDP response and we fall back
 * to SLP. This is a standard, documented server status protocol (the same one server trackers use) —
 * read-only, no login, no gameplay packets.
 */
public final class QueryClient {
    private QueryClient() {}

    private static final int SESSION_ID = 0x00000001 & 0x0F0F0F0F;

    /** Full player list via query, or an empty list if query is disabled/unreachable. */
    public static List<String> fullStatPlayers(String host, int port, int timeoutMs) {
        List<String> players = new ArrayList<>();
        try (DatagramSocket sock = new DatagramSocket()) {
            sock.setSoTimeout(Math.max(500, timeoutMs));
            InetSocketAddress addr = new InetSocketAddress(host, port);

            // 1) Handshake: FE FD 09 <sessionId>
            byte[] hs = new byte[]{
                    (byte) 0xFE, (byte) 0xFD, 0x09,
                    (byte) (SESSION_ID >> 24), (byte) (SESSION_ID >> 16), (byte) (SESSION_ID >> 8), (byte) SESSION_ID
            };
            sock.send(new DatagramPacket(hs, hs.length, addr));

            byte[] rbuf = new byte[8192];
            DatagramPacket resp = new DatagramPacket(rbuf, rbuf.length);
            sock.receive(resp);
            // Response: type(1) + sessionId(4) + token as null-terminated ASCII int
            int token = parseTokenChallenge(rbuf, resp.getLength());

            // 2) Full stat: FE FD 00 <sessionId> <token int32 BE> 00 00 00 00
            ByteArrayOutputStream req = new ByteArrayOutputStream();
            req.write(0xFE); req.write(0xFD); req.write(0x00);
            req.write(SESSION_ID >> 24); req.write(SESSION_ID >> 16); req.write(SESSION_ID >> 8); req.write(SESSION_ID);
            req.write(token >> 24); req.write(token >> 16); req.write(token >> 8); req.write(token);
            req.write(0x00); req.write(0x00); req.write(0x00); req.write(0x00); // padding => request full stat
            byte[] rb = req.toByteArray();
            sock.send(new DatagramPacket(rb, rb.length, addr));

            byte[] fbuf = new byte[65535];
            DatagramPacket fresp = new DatagramPacket(fbuf, fbuf.length);
            sock.receive(fresp);
            parsePlayers(fbuf, fresp.getLength(), players);
        } catch (Exception ignored) {
            // query disabled / firewalled / timeout — caller falls back to SLP
        }
        return players;
    }

    private static int parseTokenChallenge(byte[] buf, int len) {
        // skip type(1) + sessionId(4); the rest is a null-terminated ASCII signed int
        int i = 5;
        StringBuilder sb = new StringBuilder();
        while (i < len && buf[i] != 0) sb.append((char) buf[i++]);
        return Integer.parseInt(sb.toString().trim());
    }

    /** Extract player names from a full-stat response by locating the {@code player_} section. */
    static void parsePlayers(byte[] buf, int len, List<String> out) {
        byte[] marker = "player_".getBytes(StandardCharsets.US_ASCII);
        int idx = indexOf(buf, len, marker, 0);
        if (idx < 0) return;
        int p = idx + marker.length;
        // After "player_" there are two nulls before the list begins.
        while (p < len && buf[p] == 0) p++;
        // Read null-terminated names until an empty string (double-null) terminates the section.
        while (p < len) {
            int start = p;
            while (p < len && buf[p] != 0) p++;
            int nameLen = p - start;
            if (nameLen == 0) break; // empty string => end of players
            out.add(new String(buf, start, nameLen, StandardCharsets.UTF_8));
            p++; // skip the null terminator
        }
    }

    private static int indexOf(byte[] haystack, int len, byte[] needle, int from) {
        outer:
        for (int i = Math.max(0, from); i <= len - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
