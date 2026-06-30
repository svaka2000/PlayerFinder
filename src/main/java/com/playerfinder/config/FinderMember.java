package com.playerfinder.config;

import java.util.Locale;
import java.util.UUID;

/**
 * One player inside a {@link FinderGroup}. We always keep the (last-known) name, and the UUID when we
 * know it (MCTiers imports and tab-list lookups provide one). Matching against the live server is done
 * by UUID when available and otherwise by name, so a manually-added name still works even before we've
 * ever seen that player online.
 */
public class FinderMember {
    public String name;
    /** Dashed UUID string, or {@code null} if unknown. */
    public String uuid;

    public FinderMember() {}

    public FinderMember(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    /** Parsed UUID, or {@code null} if absent/invalid. */
    public UUID parsedUuid() {
        if (uuid == null || uuid.isBlank()) return null;
        try {
            return UUID.fromString(uuid.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String uuidKey(UUID u) {
        return u.toString().toLowerCase(Locale.ROOT);
    }

    public static String nameKey(String n) {
        return "name:" + n.toLowerCase(Locale.ROOT);
    }

    /** True if this member identifies the same player as the given live identity. */
    public boolean matches(UUID liveUuid, String liveName) {
        UUID mine = parsedUuid();
        if (mine != null && liveUuid != null && mine.equals(liveUuid)) return true;
        return name != null && liveName != null && name.equalsIgnoreCase(liveName);
    }
}
