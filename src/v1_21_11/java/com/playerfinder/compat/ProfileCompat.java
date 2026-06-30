package com.playerfinder.compat;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

/** 1.21.11: authlib's {@link GameProfile} is a record — accessors are {@code id()} / {@code name()}. */
public final class ProfileCompat {
    private ProfileCompat() {}

    public static UUID id(GameProfile p) {
        return p == null ? null : p.id();
    }

    public static String name(GameProfile p) {
        return p == null ? null : p.name();
    }
}
