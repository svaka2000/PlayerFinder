package com.playerfinder.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** 1.21.4 keybind builder: the category is a translation-key {@code String} — our own "PlayerFinder"
 *  section (lang key {@code key.categories.playerfinder}) so the binds are easy to find in Options →
 *  Controls. */
public final class KeybindCompat {
    private KeybindCompat() {}

    public static KeyMapping create(String translationKey, int glfwKey) {
        return new KeyMapping(translationKey, InputConstants.Type.KEYSYM, glfwKey, "key.categories.playerfinder");
    }
}
