package com.playerfinder;

import com.playerfinder.config.ConfigManager;
import com.playerfinder.config.FinderConfig;
import com.playerfinder.config.FinderGroup;
import com.playerfinder.config.FinderMember;
import com.playerfinder.core.GroupTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared, thread-safe runtime state + lookups for PlayerFinder. The render mixins call the hot-path
 * lookups ({@link #highlightRgbFor} / {@link #isHiddenBySolo}) every frame, so those read pre-computed
 * snapshots that are only rebuilt when the config or solo selection changes.
 *
 * <p>Everything here is render/UI only: it reads the config and decides nametag colours and which
 * other players to draw. It never moves an entity, touches a hitbox, or sends a packet.
 */
public final class PlayerFinder {
    public static final String MOD_ID = "playerfinder";
    public static final Logger LOGGER = LoggerFactory.getLogger("PlayerFinder");

    private static ConfigManager configManager;

    /** member key -> highlight 0xRRGGBB. Empty when highlighting is off / nothing to highlight. */
    private static volatile Map<String, Integer> highlightByKey = Map.of();

    /** Member keys of the active solo group (recursive), or {@code null} when solo is inactive. */
    private static volatile Set<String> soloKeys = null;

    private PlayerFinder() {}

    public static void init(ConfigManager cm) {
        configManager = cm;
        rebuild();
    }

    public static ConfigManager configManager() {
        return configManager;
    }

    public static FinderConfig config() {
        return configManager.get();
    }

    public static void save() {
        configManager.save();
    }

    // ---- snapshot rebuild -------------------------------------------------

    /** Recompute the highlight lookup from the current config. Call after any config change. */
    public static void rebuild() {
        FinderConfig cfg = config();
        Map<String, Integer> hl = new HashMap<>();
        if (cfg.globalHighlight && cfg.root != null) {
            walkHighlight(cfg.root, hl);
        }
        highlightByKey = hl;
    }

    private static void walkHighlight(FinderGroup group, Map<String, Integer> out) {
        for (FinderGroup g : group.groups) {
            if (g.highlight) {
                int rgb = GroupTree.effectiveColorRgb(config().root, g);
                for (FinderMember m : g.members) {
                    UUID u = m.parsedUuid();
                    if (u != null) out.put(FinderMember.uuidKey(u), rgb);
                    if (m.name != null) out.put(FinderMember.nameKey(m.name), rgb);
                }
            }
            walkHighlight(g, out);
        }
    }

    // ---- solo (hide everyone but the chosen group) ------------------------

    public static boolean isSoloActive() {
        return soloKeys != null;
    }

    /** Activate solo for the group at {@code path}. Returns the member count, or -1 if the path is unknown. */
    public static int setSolo(String path) {
        FinderGroup g = GroupTree.resolve(config().root, path);
        if (g == null) return -1;
        Set<String> keys = new HashSet<>();
        for (FinderMember m : GroupTree.collectMembers(g, true)) {
            UUID u = m.parsedUuid();
            if (u != null) keys.add(FinderMember.uuidKey(u));
            if (m.name != null) keys.add(FinderMember.nameKey(m.name));
        }
        soloKeys = keys;
        config().lastSoloPath = GroupTree.pathOf(config().root, g);
        save();
        return keys.size();
    }

    public static void clearSolo() {
        soloKeys = null;
    }

    // ---- hot-path lookups (called from render mixins) ---------------------

    /** Highlight colour 0xRRGGBB for this player, or -1 if not highlighted. */
    public static int highlightRgbFor(UUID uuid, String name) {
        Map<String, Integer> map = highlightByKey;
        if (map.isEmpty()) return -1;
        if (uuid != null) {
            Integer c = map.get(FinderMember.uuidKey(uuid));
            if (c != null) return c;
        }
        if (name != null) {
            Integer c = map.get(FinderMember.nameKey(name));
            if (c != null) return c;
        }
        return -1;
    }

    /** True if solo is active and this player is NOT in the solo group (so it should not be drawn). */
    public static boolean isHiddenBySolo(UUID uuid, String name) {
        Set<String> keys = soloKeys;
        if (keys == null) return false;
        if (uuid != null && keys.contains(FinderMember.uuidKey(uuid))) return false;
        if (name != null && keys.contains(FinderMember.nameKey(name))) return false;
        return true;
    }

    static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
