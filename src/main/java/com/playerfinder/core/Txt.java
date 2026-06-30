package com.playerfinder.core;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/** Small text helper: build a literal component tinted with an arbitrary 0xRRGGBB colour, using the
 *  version-stable {@link TextColor#fromRgb(int)} / {@link Style} path. */
public final class Txt {
    private Txt() {}

    public static MutableComponent colored(String text, int rgb) {
        return Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF)));
    }
}
