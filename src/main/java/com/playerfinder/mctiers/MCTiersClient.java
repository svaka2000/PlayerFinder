package com.playerfinder.mctiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.playerfinder.config.FinderMember;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * Tiny client for the public MCTiers v2 API (https://mctiers.com/docs/v2). We only read the public
 * gamemode-rankings endpoint to bulk-import usernames into a group; nothing is written and no game
 * state is touched.
 *
 * <p>{@code GET /api/v2/mode/{gamemode}?count=N} returns an object keyed by tier number, each value an
 * array of {@code {uuid, name, region, pos}} (pos 0 = high sub-tier, 1 = low sub-tier).
 */
public final class MCTiersClient {
    private MCTiersClient() {}

    private static final String BASE = "https://mctiers.com/api/v2";

    /** Valid gamemode keys (from {@code /api/v2/mode/list}); also drives command suggestions. */
    public static final Set<String> GAMEMODES =
            Set.of("sword", "axe", "mace", "pot", "nethop", "smp", "uhc", "vanilla");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static boolean isValidGamemode(String gm) {
        return gm != null && GAMEMODES.contains(gm.toLowerCase(Locale.ROOT));
    }

    /** Fetch a gamemode's rankings, returning tier number -> players (sorted by tier ascending). */
    public static CompletableFuture<Map<Integer, List<FinderMember>>> fetchGamemode(String gamemode, int count) {
        String gm = gamemode.toLowerCase(Locale.ROOT);
        URI uri = URI.create(BASE + "/mode/" + gm + "?count=" + Math.max(1, count));
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "PlayerFinder/1.0 (+https://github.com/svaka2000/PlayerFinder)")
                .header("Accept", "application/json")
                .GET()
                .build();

        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException("MCTiers returned HTTP " + resp.statusCode());
                    }
                    return parse(resp.body());
                });
    }

    private static Map<Integer, List<FinderMember>> parse(String body) {
        Map<Integer, List<FinderMember>> byTier = new TreeMap<>();
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonObject()) return byTier;
        JsonObject obj = root.getAsJsonObject();
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            int tier;
            try {
                tier = Integer.parseInt(e.getKey());
            } catch (NumberFormatException ex) {
                continue; // ignore non-tier keys defensively
            }
            if (!e.getValue().isJsonArray()) continue;
            JsonArray arr = e.getValue().getAsJsonArray();
            List<FinderMember> players = new java.util.ArrayList<>();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject p = el.getAsJsonObject();
                String name = p.has("name") && !p.get("name").isJsonNull() ? p.get("name").getAsString() : null;
                String uuid = p.has("uuid") && !p.get("uuid").isJsonNull() ? p.get("uuid").getAsString() : null;
                if (name == null && uuid == null) continue;
                players.add(new FinderMember(name, uuid));
            }
            byTier.put(tier, players);
        }
        return byTier;
    }
}
