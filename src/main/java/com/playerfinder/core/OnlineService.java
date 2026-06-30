package com.playerfinder.core;

import com.playerfinder.compat.ProfileCompat;
import com.playerfinder.config.FinderMember;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Reads who is currently on the server straight from the client's player list (the tab list). This is
 *  data the client already has; PlayerFinder never sends a packet to ask. */
public final class OnlineService {
    private OnlineService() {}

    /** Immutable snapshot of the current online players, indexed for fast membership checks. */
    public static final class Snapshot {
        public final Set<UUID> uuids;
        public final Set<String> namesLower;

        Snapshot(Set<UUID> uuids, Set<String> namesLower) {
            this.uuids = uuids;
            this.namesLower = namesLower;
        }

        public boolean isOnline(FinderMember m) {
            UUID u = m.parsedUuid();
            if (u != null && uuids.contains(u)) return true;
            return m.name != null && namesLower.contains(m.name.toLowerCase(Locale.ROOT));
        }
    }

    public static Snapshot current() {
        Set<UUID> uuids = new HashSet<>();
        Set<String> names = new HashSet<>();
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            for (PlayerInfo info : conn.getOnlinePlayers()) {
                if (info.getProfile() == null) continue;
                UUID id = ProfileCompat.id(info.getProfile());
                if (id != null) uuids.add(id);
                String n = ProfileCompat.name(info.getProfile());
                if (n != null) names.add(n.toLowerCase(Locale.ROOT));
            }
        }
        return new Snapshot(uuids, names);
    }

    /** Actual-case names of everyone currently online (for command suggestions). */
    public static List<String> onlineNames() {
        List<String> out = new ArrayList<>();
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            for (PlayerInfo info : conn.getOnlinePlayers()) {
                String n = ProfileCompat.name(info.getProfile());
                if (n != null) out.add(n);
            }
        }
        return out;
    }

    /** Resolve a currently-online player's UUID by name (case-insensitive), or {@code null}. */
    public static UUID lookupUuid(String name) {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn == null || name == null) return null;
        for (PlayerInfo info : conn.getOnlinePlayers()) {
            if (info.getProfile() == null) continue;
            String n = ProfileCompat.name(info.getProfile());
            if (n != null && n.equalsIgnoreCase(name)) return ProfileCompat.id(info.getProfile());
        }
        return null;
    }
}

