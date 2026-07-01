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

    /** Minimum ping passes per server (a server returns a random ~12-name sample each ping, so unioning
     *  passes surfaces more of a busy server's list — this is how we get past the 12-name cap). */
    public int scanPasses = 3;

    /** Hard cap on adaptive ping passes per server. */
    public int scanMaxPings = 40;

    /** Stop pinging a server once this many consecutive passes reveal no new names (the sample has
     *  converged). Prevents wasting pings on servers that hide/fix their sample. */
    public int scanStableRounds = 5;

    /** Small delay between passes (ms) to rotate the random sample and defeat brief status caches. */
    public int scanPingDelayMs = 150;

    /** Also try the GameSpy Query protocol (UDP). If the server has {@code enable-query=true} this
     *  returns its FULL online player list in one shot (no 12-name cap). Most servers disable it. */
    public boolean scanUseQuery = true;

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
