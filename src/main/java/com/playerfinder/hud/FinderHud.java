package com.playerfinder.hud;

import com.playerfinder.PlayerFinder;
import com.playerfinder.config.FinderConfig;
import com.playerfinder.config.FinderGroup;
import com.playerfinder.config.FinderMember;
import com.playerfinder.core.GroupTree;
import com.playerfinder.core.OnlineService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Draws the compact "who's online" panel. Lists each top-level group that has at least one member
 *  online (or all members, if {@code hudShowOffline}), in the group's colour. Read-only HUD. */
public final class FinderHud {
    private FinderHud() {}

    private static final int MAX_LINES = 26;
    private static final int LINE_H = 10;
    private static final int PAD = 3;

    private record Line(Component text, int rgb) {}

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.options != null && mc.options.hideGui) return;

        FinderConfig cfg = PlayerFinder.config();
        if (!cfg.hudEnabled || cfg.root == null || cfg.root.groups.isEmpty()) return;

        OnlineService.Snapshot online = OnlineService.current();
        List<Line> lines = new ArrayList<>();

        for (FinderGroup top : cfg.root.groups) {
            int groupRgb = GroupTree.effectiveColorRgb(cfg.root, top);
            List<FinderMember> all = GroupTree.collectMembers(top, true);
            List<FinderMember> onlineMembers = new ArrayList<>();
            for (FinderMember m : all) {
                if (online.isOnline(m)) onlineMembers.add(m);
            }
            if (onlineMembers.isEmpty() && !cfg.hudShowOffline) continue;

            lines.add(new Line(
                    Component.literal(top.name + " (" + onlineMembers.size() + "/" + all.size() + ")"),
                    groupRgb));
            for (FinderMember m : onlineMembers) {
                lines.add(new Line(Component.literal("  " + safeName(m)), groupRgb));
                if (lines.size() >= MAX_LINES) break;
            }
            if (cfg.hudShowOffline) {
                for (FinderMember m : all) {
                    if (online.isOnline(m)) continue;
                    lines.add(new Line(Component.literal("  " + safeName(m)), 0x555555));
                    if (lines.size() >= MAX_LINES) break;
                }
            }
            if (lines.size() >= MAX_LINES) break;
        }

        if (lines.isEmpty()) return;

        Font font = mc.font;
        int contentW = 0;
        for (Line l : lines) contentW = Math.max(contentW, font.width(l.text()));
        int contentH = lines.size() * LINE_H;

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int x, y;
        switch (cfg.hudAnchor == null ? "top_left" : cfg.hudAnchor) {
            case "top_right" -> { x = screenW - cfg.hudOffsetX - contentW; y = cfg.hudOffsetY; }
            case "bottom_left" -> { x = cfg.hudOffsetX; y = screenH - cfg.hudOffsetY - contentH; }
            case "bottom_right" -> { x = screenW - cfg.hudOffsetX - contentW; y = screenH - cfg.hudOffsetY - contentH; }
            default -> { x = cfg.hudOffsetX; y = cfg.hudOffsetY; }
        }

        graphics.fill(x - PAD, y - PAD, x + contentW + PAD, y + contentH + PAD - 1, 0x90000000);

        int yy = y;
        for (Line l : lines) {
            graphics.drawString(font, l.text(), x, yy, 0xFF000000 | l.rgb());
            yy += LINE_H;
        }
    }

    private static String safeName(FinderMember m) {
        return m.name != null ? m.name : (m.uuid != null ? m.uuid : "?");
    }
}
