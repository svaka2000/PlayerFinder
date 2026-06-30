package com.playerfinder;

import com.mojang.blaze3d.platform.InputConstants;
import com.playerfinder.command.FinderCommands;
import com.playerfinder.compat.KeybindCompat;
import com.playerfinder.config.ConfigManager;
import com.playerfinder.config.FinderConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * PlayerFinder client entrypoint: loads the group config, registers the {@code /pf} commands and four
 * (unbound-by-default) keybinds, and ticks them. Everything PlayerFinder does is client-side and
 * render/UI only.
 */
public class PlayerFinderClient implements ClientModInitializer {
    private static ConfigManager configRef;

    public static ConfigManager config() {
        return configRef;
    }

    @Override
    public void onInitializeClient() {
        ConfigManager cm = new ConfigManager();
        cm.load();
        configRef = cm;
        PlayerFinder.init(cm);

        // Client-side commands: /pf and /playerfinder.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                FinderCommands.register(dispatcher));

        // Keybinds (unbound by default — set them in Options -> Controls -> PlayerFinder, or use commands).
        final KeyMapping kHighlight = register("key.playerfinder.toggle_highlight");
        final KeyMapping kSolo = register("key.playerfinder.toggle_solo");
        final KeyMapping kHud = register("key.playerfinder.toggle_hud");
        final KeyMapping kOnline = register("key.playerfinder.who_online");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (kHighlight != null) while (kHighlight.consumeClick()) toggleHighlight(client);
            if (kHud != null) while (kHud.consumeClick()) toggleHud(client);
            if (kSolo != null) while (kSolo.consumeClick()) toggleSolo(client);
            if (kOnline != null) while (kOnline.consumeClick()) FinderCommands.printOnline(client, null);
        });

        PlayerFinder.LOGGER.info("[PlayerFinder] ready — {} top-level group(s)", cm.get().root.groups.size());
    }

    private static KeyMapping register(String translationKey) {
        try {
            return KeyBindingHelper.registerKeyBinding(
                    KeybindCompat.create(translationKey, InputConstants.UNKNOWN.getValue()));
        } catch (Throwable t) {
            PlayerFinder.LOGGER.warn("[PlayerFinder] keybind {} failed to register", translationKey, t);
            return null;
        }
    }

    private static void actionbar(Minecraft client, Component msg) {
        if (client.player != null) client.player.displayClientMessage(msg, true);
    }

    private static Component onOff(boolean on) {
        return Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static void toggleHighlight(Minecraft client) {
        FinderConfig cfg = PlayerFinder.config();
        cfg.globalHighlight = !cfg.globalHighlight;
        PlayerFinder.save();
        PlayerFinder.rebuild();
        actionbar(client, Component.literal("PlayerFinder highlight: ").append(onOff(cfg.globalHighlight)));
    }

    private static void toggleHud(Minecraft client) {
        FinderConfig cfg = PlayerFinder.config();
        cfg.hudEnabled = !cfg.hudEnabled;
        PlayerFinder.save();
        actionbar(client, Component.literal("PlayerFinder HUD: ").append(onOff(cfg.hudEnabled)));
    }

    private static void toggleSolo(Minecraft client) {
        FinderConfig cfg = PlayerFinder.config();
        if (PlayerFinder.isSoloActive()) {
            PlayerFinder.clearSolo();
            actionbar(client, Component.literal("Solo off — everyone visible").withStyle(ChatFormatting.GRAY));
        } else if (cfg.lastSoloPath != null && !cfg.lastSoloPath.isBlank()) {
            int n = PlayerFinder.setSolo(cfg.lastSoloPath);
            if (n >= 0) {
                actionbar(client, Component.literal("Solo: " + cfg.lastSoloPath + " (" + n + ")")
                        .withStyle(ChatFormatting.GOLD));
            } else {
                actionbar(client, Component.literal("That solo group is gone — set one with /pf solo <group>")
                        .withStyle(ChatFormatting.RED));
            }
        } else {
            actionbar(client, Component.literal("Pick a solo group first: /pf solo <group>")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}
