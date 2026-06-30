package com.playerfinder.gui;

import com.playerfinder.PlayerFinder;
import com.playerfinder.config.FinderConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * PlayerFinder settings: the global toggles (highlight, HUD, HUD anchor, solo). Groups themselves are
 * managed with the {@code /pf} commands — this screen links to {@code /pf help}. Built from plain vanilla
 * widgets so one screen serves every supported version.
 */
public class FinderScreen extends Screen {
    private static final List<String> ANCHORS = List.of("top_left", "top_right", "bottom_left", "bottom_right");

    private static final String VERSION = FabricLoader.getInstance()
            .getModContainer(PlayerFinder.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("?");

    private final Screen parent;
    private final FinderConfig cfg;
    private final Runnable onSave;

    public FinderScreen(Screen parent, FinderConfig cfg, Runnable onSave) {
        super(Component.literal("PlayerFinder"));
        this.parent = parent;
        this.cfg = cfg;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int cx = this.width / 2, w = 240, x = cx - w / 2;
        int y = this.height / 2 - 58;

        addRenderableWidget(Button.builder(highlightLabel(), b -> {
            cfg.globalHighlight = !cfg.globalHighlight;
            saveAndRebuild();
            b.setMessage(highlightLabel());
        }).bounds(x, y, w, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(hudLabel(), b -> {
            cfg.hudEnabled = !cfg.hudEnabled;
            save();
            b.setMessage(hudLabel());
        }).bounds(x, y, w, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(anchorLabel(), b -> {
            int i = (Math.max(0, ANCHORS.indexOf(cfg.hudAnchor)) + 1) % ANCHORS.size();
            cfg.hudAnchor = ANCHORS.get(i);
            save();
            b.setMessage(anchorLabel());
        }).bounds(x, y, w / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(offlineLabel(), b -> {
            cfg.hudShowOffline = !cfg.hudShowOffline;
            save();
            b.setMessage(offlineLabel());
        }).bounds(x + w / 2 + 2, y, w / 2 - 2, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(soloLabel(), b -> {
            if (PlayerFinder.isSoloActive()) {
                PlayerFinder.clearSolo();
            } else if (cfg.lastSoloPath != null && !cfg.lastSoloPath.isBlank()) {
                PlayerFinder.setSolo(cfg.lastSoloPath);
            }
            b.setMessage(soloLabel());
        }).bounds(x, y, w, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y + 6, w, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x90000000);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 92, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.literal("Manage groups with /pf  —  type /pf help"),
                this.width / 2, this.height / 2 - 78, 0xFFAAAAAA);
        g.drawCenteredString(this.font, Component.literal("v" + VERSION),
                this.width / 2, this.height / 2 - 67, 0xFF6FCF6F);
    }

    @Override
    public void onClose() {
        save();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    private void save() {
        if (onSave != null) onSave.run();
    }

    private void saveAndRebuild() {
        save();
        PlayerFinder.rebuild();
    }

    private Component onOff(String prefix, boolean on) {
        return Component.literal(prefix + ": ")
                .append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private Component highlightLabel() { return onOff("Highlight", cfg.globalHighlight); }
    private Component hudLabel() { return onOff("Online HUD", cfg.hudEnabled); }
    private Component offlineLabel() { return onOff("Show offline", cfg.hudShowOffline); }

    private Component anchorLabel() {
        String a = cfg.hudAnchor == null ? "top_left" : cfg.hudAnchor;
        return Component.literal("HUD: ").append(Component.literal(a).withStyle(ChatFormatting.AQUA));
    }

    private Component soloLabel() {
        if (PlayerFinder.isSoloActive()) {
            return Component.literal("Solo: ")
                    .append(Component.literal("ON (" + (cfg.lastSoloPath == null ? "" : cfg.lastSoloPath) + ") — click to clear")
                            .withStyle(ChatFormatting.GOLD));
        }
        String last = (cfg.lastSoloPath == null || cfg.lastSoloPath.isBlank()) ? "use /pf solo <group>" : cfg.lastSoloPath;
        return Component.literal("Solo: ").append(Component.literal("off (" + last + ")").withStyle(ChatFormatting.GRAY));
    }
}
