package com.playerfinder.compat;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

/** 1.21.4: authlib's {@link GameProfile} is the classic class — accessors are {@code getId()} /
 *  {@code getName()}. */
public final class ProfileCompat {
    private ProfileCompat() {}

    public static UUID id(GameProfile p) {
        return p == null ? null : p.getId();
    }

    public static String name(GameProfile p) {
        return p == null ? null : p.getName();
    }
}
