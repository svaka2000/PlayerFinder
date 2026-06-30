package com.playerfinder.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** 1.21.11 keybind builder: the category is a {@link KeyMapping.Category} object — our own registered
 *  "PlayerFinder" category (label from lang key {@code key.category.playerfinder.general}) so the binds
 *  get their own clearly-named section in Options → Controls. Falls back to MISC if registration fails. */
public final class KeybindCompat {
    private KeybindCompat() {}

    private static final KeyMapping.Category CATEGORY = registerCategory();

    private static KeyMapping.Category registerCategory() {
        try {
            return KeyMapping.Category.register(Identifier.fromNamespaceAndPath("playerfinder", "general"));
        } catch (Throwable t) {
            return KeyMapping.Category.MISC;
        }
    }

    public static KeyMapping create(String translationKey, int glfwKey) {
        return new KeyMapping(translationKey, InputConstants.Type.KEYSYM, glfwKey, CATEGORY);
    }
}
