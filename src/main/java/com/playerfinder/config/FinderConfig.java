package com.playerfinder.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything PlayerFinder persists to {@code config/playerfinder.json}: the group tree, a handful of
 * display toggles, and the list of servers to scan. Render/info-only data — no game interaction.
 */
public class FinderConfig {
    public int configVersion = 2;

    /** Hidden container; {@code root.groups} are the top-level groups. */
    public FinderGroup root = new FinderGroup("root");

    /** Servers to query with {@code /pf scan} (Server List Ping — no connecting/joining). */
    public List<FinderServer> servers = new ArrayList<>();

    /** Per-server connect/read timeout for scanning, in milliseconds. */
    public int scanTimeoutMs = 3000;

    /** How many ping passes per server. Vanilla returns a random ~12-name sample each ping, so unioning
     *  a few passes surfaces more of a busy server's player list. */
    public int scanPasses = 3;

    /** Include the server you're currently on (read from the full tab list) in scan results. */
    public boolean scanIncludeCurrent = true;

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
