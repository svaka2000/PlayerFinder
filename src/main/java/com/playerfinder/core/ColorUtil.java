package com.playerfinder.core;

import net.minecraft.ChatFormatting;

import java.util.Locale;

/** Resolves a group's colour string (named colour or {@code #rrggbb}) into a 0xRRGGBB int. */
public final class ColorUtil {
    private ColorUtil() {}

    /** A pleasant rotation of distinct colours assigned to new top-level groups. */
    private static final int[] PALETTE = {
            0xFF5555, // red
            0x55FF55, // green
            0x55FFFF, // aqua
            0xFFFF55, // yellow
            0xFF55FF, // magenta
            0xFFAA00, // gold
            0x55AAFF, // light blue
            0xFF8888, // salmon
    };

    public static int paletteColor(int index) {
        return PALETTE[Math.floorMod(index, PALETTE.length)];
    }

    /** Parse a colour string to 0xRRGGBB, or return {@code fallback} if blank/unparseable. */
    public static int parseRgb(String s, int fallback) {
        if (s == null) return fallback;
        s = s.trim();
        if (s.isEmpty()) return fallback;

        String hex = null;
        if (s.startsWith("#")) {
            hex = s.substring(1);
        } else if (s.length() == 6 && s.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            hex = s;
        }
        if (hex != null) {
            try {
                return (int) (Long.parseLong(hex, 16) & 0xFFFFFF);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        ChatFormatting cf = ChatFormatting.getByName(s.toLowerCase(Locale.ROOT));
        if (cf != null && cf.isColor() && cf.getColor() != null) {
            return cf.getColor();
        }
        return fallback;
    }

    /** True if the string is a valid named or hex colour. (A successful parse is always >= 0.) */
    public static boolean isValid(String s) {
        return parseRgb(s, -1) >= 0;
    }
}
