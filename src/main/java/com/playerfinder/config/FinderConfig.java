package com.playerfinder.config;

/**
 * Everything PlayerFinder persists to {@code config/playerfinder.json}: the group tree plus a handful
 * of display toggles. Render-only data — no server interaction is configured here.
 */
public class FinderConfig {
    public int configVersion = 1;

    /** Hidden container; {@code root.groups} are the top-level groups. */
    public FinderGroup root = new FinderGroup("root");

    /** Master switch for nametag highlighting. */
    public boolean globalHighlight = true;

    /** Show the on-screen "who's online" panel. */
    public boolean hudEnabled = true;

    /** HUD anchor: {@code top_left}, {@code top_right}, {@code bottom_left}, {@code bottom_right}. */
    public String hudAnchor = "top_left";

    public int hudOffsetX = 4;
    public int hudOffsetY = 4;

    /** Also list offline members in the HUD (greyed out). */
    public boolean hudShowOffline = false;

    /** Last group path used with {@code /pf solo}; lets the solo keybind re-toggle it. */
    public String lastSoloPath = null;
}
