package com.playerfinder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.playerfinder.PlayerFinder;
import com.playerfinder.config.FinderConfig;
import com.playerfinder.config.FinderGroup;
import com.playerfinder.config.FinderMember;
import com.playerfinder.config.FinderServer;
import com.playerfinder.core.ColorUtil;
import com.playerfinder.core.GroupTree;
import com.playerfinder.core.OnlineService;
import com.playerfinder.core.Txt;
import com.playerfinder.mctiers.MCTiersClient;
import com.playerfinder.net.ServerPinger;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** The {@code /pf} (alias {@code /playerfinder}) client-command tree. All commands are client-side only —
 *  nothing is sent to the server. */
public final class FinderCommands {
    private FinderCommands() {}

    private static FinderConfig cfg() {
        return PlayerFinder.config();
    }

    private static void commit() {
        PlayerFinder.save();
        PlayerFinder.rebuild();
    }

    private static final SuggestionProvider<FabricClientCommandSource> PATHS =
            (c, b) -> SharedSuggestionProvider.suggest(GroupTree.allPaths(cfg().root), b);

    private static final SuggestionProvider<FabricClientCommandSource> PATHS_AND_ROOT =
            (c, b) -> {
                java.util.List<String> opts = new java.util.ArrayList<>();
                opts.add("root");
                opts.addAll(GroupTree.allPaths(cfg().root));
                return SharedSuggestionProvider.suggest(opts, b);
            };

    private static final SuggestionProvider<FabricClientCommandSource> ONLINE_NAMES =
            (c, b) -> SharedSuggestionProvider.suggest(OnlineService.onlineNames(), b);

    private static final SuggestionProvider<FabricClientCommandSource> GAMEMODES =
            (c, b) -> SharedSuggestionProvider.suggest(MCTiersClient.GAMEMODES, b);

    private static final SuggestionProvider<FabricClientCommandSource> SERVER_NAMES =
            (c, b) -> SharedSuggestionProvider.suggest(cfg().servers.stream().map(s -> s.name).toList(), b);

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralCommandNode<FabricClientCommandSource> root = dispatcher.register(
            literal("pf")
                .executes(FinderCommands::help)
                .then(literal("help").executes(FinderCommands::help))
                .then(literal("group")
                    .then(literal("list").executes(FinderCommands::groupList))
                    .then(literal("create")
                        .then(argument("path", StringArgumentType.string())
                            .executes(FinderCommands::groupCreate)))
                    .then(literal("remove")
                        .then(argument("path", StringArgumentType.string()).suggests(PATHS)
                            .executes(FinderCommands::groupRemove)))
                    .then(literal("color")
                        .then(argument("path", StringArgumentType.string()).suggests(PATHS)
                            .then(argument("color", StringArgumentType.string())
                                .executes(FinderCommands::groupColor))))
                    .then(literal("highlight")
                        .then(argument("path", StringArgumentType.string()).suggests(PATHS)
                            .then(literal("on").executes(c -> groupHighlight(c, true)))
                            .then(literal("off").executes(c -> groupHighlight(c, false)))))
                    .then(literal("move")
                        .then(argument("path", StringArgumentType.string()).suggests(PATHS)
                            .then(argument("parent", StringArgumentType.string()).suggests(PATHS_AND_ROOT)
                                .executes(FinderCommands::groupMove)))))
                .then(literal("add")
                    .then(argument("path", StringArgumentType.string()).suggests(PATHS)
                        .then(argument("player", StringArgumentType.word()).suggests(ONLINE_NAMES)
                            .executes(FinderCommands::addPlayer))))
                .then(literal("remove")
                    .then(argument("path", StringArgumentType.string()).suggests(PATHS)
                        .then(argument("player", StringArgumentType.word())
                            .executes(FinderCommands::removePlayer))))
                .then(literal("online")
                    .executes(c -> online(c, null))
                    .then(argument("path", StringArgumentType.string()).suggests(PATHS)
                        .executes(c -> online(c, StringArgumentType.getString(c, "path")))))
                .then(literal("solo")
                    .then(argument("path", StringArgumentType.string()).suggests(PATHS)
                        .executes(FinderCommands::solo)))
                .then(literal("unsolo").executes(FinderCommands::unsolo))
                .then(literal("highlight")
                    .then(literal("on").executes(c -> globalHighlight(c, true)))
                    .then(literal("off").executes(c -> globalHighlight(c, false))))
                .then(literal("hud")
                    .then(literal("on").executes(c -> hud(c, true)))
                    .then(literal("off").executes(c -> hud(c, false))))
                .then(literal("server")
                    .then(literal("list").executes(FinderCommands::serverList))
                    .then(literal("add")
                        .then(argument("name", StringArgumentType.word())
                            .then(argument("address", StringArgumentType.string())
                                .executes(FinderCommands::serverAdd))))
                    .then(literal("remove")
                        .then(argument("name", StringArgumentType.word()).suggests(SERVER_NAMES)
                            .executes(FinderCommands::serverRemove))))
                .then(literal("scan")
                    .executes(c -> scan(c, null))
                    .then(argument("path", StringArgumentType.string()).suggests(PATHS)
                        .executes(c -> scan(c, StringArgumentType.getString(c, "path")))))
                .then(literal("import")
                    .then(literal("mctiers")
                        .then(argument("gamemode", StringArgumentType.word()).suggests(GAMEMODES)
                            .executes(c -> importMctiers(c, -1, 50))
                            .then(argument("tier", IntegerArgumentType.integer(1, 5))
                                .executes(c -> importMctiers(c, IntegerArgumentType.getInteger(c, "tier"), 50))
                                .then(argument("count", IntegerArgumentType.integer(1, 200))
                                    .executes(c -> importMctiers(c,
                                            IntegerArgumentType.getInteger(c, "tier"),
                                            IntegerArgumentType.getInteger(c, "count")))))))));

        // alias /playerfinder -> /pf
        dispatcher.register(literal("playerfinder").redirect(root));
    }

    // ---- feedback helpers -------------------------------------------------

    private static void info(CommandContext<FabricClientCommandSource> c, Component msg) {
        c.getSource().sendFeedback(Component.literal("[PF] ").withStyle(ChatFormatting.AQUA).append(msg));
    }

    private static void err(CommandContext<FabricClientCommandSource> c, String msg) {
        c.getSource().sendError(Component.literal("[PF] " + msg));
    }

    // ---- handlers ---------------------------------------------------------

    private static int help(CommandContext<FabricClientCommandSource> c) {
        FabricClientCommandSource s = c.getSource();
        s.sendFeedback(Component.literal("PlayerFinder").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        String[] lines = {
            "Paths are dot-separated, e.g. pvp.sword.t2",
            "/pf group create <path>           - make a group / sub-group",
            "/pf group list                    - show the whole tree",
            "/pf group color <path> <color>    - name (red, aqua, gold) or hex (33ccff)",
            "/pf group highlight <path> on|off - per-group nametag highlight",
            "/pf group remove <path>           - delete a group",
            "/pf group move <path> <parent>    - re-parent a group (parent = 'root' for top level)",
            "/pf add <path> <player>           - add a player to a group",
            "/pf remove <path> <player>        - remove a player",
            "/pf online [path]                 - who from the group is on THIS server",
            "/pf solo <path>                   - hide everyone except this group",
            "/pf unsolo                        - show everyone again",
            "/pf highlight on|off              - master highlight toggle",
            "/pf hud on|off                    - the on-screen online panel",
            "Cross-server search:",
            "/pf server add <name> <address>   - add a server to scan (host or host:port)",
            "/pf server list / remove <name>   - manage the scan list",
            "/pf scan [path]                   - search ALL listed servers for who's online",
            "/pf import mctiers <gamemode> [tier] [count] - import an MCTiers list",
            "   gamemodes: sword axe mace pot nethop smp uhc vanilla",
        };
        for (String l : lines) s.sendFeedback(Component.literal(l).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int groupList(CommandContext<FabricClientCommandSource> c) {
        FinderConfig cfg = cfg();
        if (cfg.root.groups.isEmpty()) {
            info(c, Component.literal("No groups yet. Try /pf group create friends").withStyle(ChatFormatting.GRAY));
            return 1;
        }
        c.getSource().sendFeedback(Component.literal("Groups:").withStyle(ChatFormatting.AQUA));
        for (FinderGroup g : cfg.root.groups) printGroup(c, g, 0);
        return 1;
    }

    private static void printGroup(CommandContext<FabricClientCommandSource> c, FinderGroup g, int depth) {
        int rgb = GroupTree.effectiveColorRgb(cfg().root, g);
        String indent = "  ".repeat(depth);
        int total = GroupTree.collectMembers(g, true).size();
        String hl = g.highlight ? "" : " (highlight off)";
        c.getSource().sendFeedback(
            Component.literal(indent + "- ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Txt.colored(g.name, rgb))
                .append(Component.literal("  [" + g.members.size() + " direct, " + total + " total]" + hl)
                        .withStyle(ChatFormatting.DARK_GRAY)));
        for (FinderMember m : g.members) {
            c.getSource().sendFeedback(Component.literal(indent + "    • " + m.name)
                    .withStyle(ChatFormatting.GRAY));
        }
        for (FinderGroup sub : g.groups) printGroup(c, sub, depth + 1);
    }

    private static int groupCreate(CommandContext<FabricClientCommandSource> c) {
        String path = StringArgumentType.getString(c, "path");
        FinderGroup g = GroupTree.createPath(cfg().root, path);
        commit();
        info(c, Component.literal("Created group ").append(Component.literal(GroupTree.pathOf(cfg().root, g))
                .withStyle(ChatFormatting.WHITE)));
        return 1;
    }

    private static int groupRemove(CommandContext<FabricClientCommandSource> c) {
        String path = StringArgumentType.getString(c, "path");
        FinderGroup removed = GroupTree.remove(cfg().root, path);
        if (removed == null) {
            err(c, "No such group: " + path);
            return 0;
        }
        commit();
        info(c, Component.literal("Removed group " + path).withStyle(ChatFormatting.WHITE));
        return 1;
    }

    private static int groupColor(CommandContext<FabricClientCommandSource> c) {
        String path = StringArgumentType.getString(c, "path");
        String color = StringArgumentType.getString(c, "color");
        FinderGroup g = GroupTree.resolve(cfg().root, path);
        if (g == null) {
            err(c, "No such group: " + path);
            return 0;
        }
        if (!ColorUtil.isValid(color)) {
            err(c, "Bad colour '" + color + "'. Use a name (red, aqua, gold, light_purple) or 6-digit hex (33ccff).");
            return 0;
        }
        g.color = color;
        commit();
        info(c, Component.literal("Set ").append(Txt.colored(g.name, ColorUtil.parseRgb(color, 0xFFFFFF)))
                .append(Component.literal(" colour to " + color).withStyle(ChatFormatting.GRAY)));
        return 1;
    }

    private static int groupHighlight(CommandContext<FabricClientCommandSource> c, boolean on) {
        String path = StringArgumentType.getString(c, "path");
        FinderGroup g = GroupTree.resolve(cfg().root, path);
        if (g == null) {
            err(c, "No such group: " + path);
            return 0;
        }
        g.highlight = on;
        commit();
        info(c, Component.literal("Highlight for " + path + ": ")
                .append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)));
        return 1;
    }

    private static int groupMove(CommandContext<FabricClientCommandSource> c) {
        String path = StringArgumentType.getString(c, "path");
        String parentPath = StringArgumentType.getString(c, "parent");
        FinderGroup g = GroupTree.resolve(cfg().root, path);
        if (g == null) {
            err(c, "No such group: " + path);
            return 0;
        }
        FinderGroup parent;
        if (parentPath.equalsIgnoreCase("root") || parentPath.isBlank() || parentPath.equals(".")) {
            parent = cfg().root;
        } else {
            parent = GroupTree.resolve(cfg().root, parentPath);
        }
        if (parent == null) {
            err(c, "No such parent group: " + parentPath);
            return 0;
        }
        if (!GroupTree.move(cfg().root, g, parent)) {
            err(c, "Can't move there (a name clash, or moving into itself/a sub-group).");
            return 0;
        }
        commit();
        info(c, Component.literal("Moved " + path + " -> " + GroupTree.pathOf(cfg().root, g)).withStyle(ChatFormatting.WHITE));
        return 1;
    }

    private static int addPlayer(CommandContext<FabricClientCommandSource> c) {
        String path = StringArgumentType.getString(c, "path");
        String player = StringArgumentType.getString(c, "player");
        FinderGroup g = GroupTree.resolve(cfg().root, path);
        if (g == null) {
            err(c, "No such group: " + path + " (create it with /pf group create " + path + ")");
            return 0;
        }
        if (g.memberByName(player) != null) {
            err(c, player + " is already in " + path);
            return 0;
        }
        UUID uuid = OnlineService.lookupUuid(player);   // resolved now if they're online, else matched by name later
        g.members.add(new FinderMember(player, uuid != null ? uuid.toString() : null));
        commit();
        info(c, Component.literal("Added ").append(Component.literal(player).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" to " + path + (uuid != null ? "" : " (will match by name)")).withStyle(ChatFormatting.GRAY)));
        return 1;
    }

    private static int removePlayer(CommandContext<FabricClientCommandSource> c) {
        String path = StringArgumentType.getString(c, "path");
        String player = StringArgumentType.getString(c, "player");
        FinderGroup g = GroupTree.resolve(cfg().root, path);
        if (g == null) {
            err(c, "No such group: " + path);
            return 0;
        }
        FinderMember m = g.memberByName(player);
        if (m == null) {
            err(c, player + " is not in " + path);
            return 0;
        }
        g.members.remove(m);
        commit();
        info(c, Component.literal("Removed " + player + " from " + path).withStyle(ChatFormatting.WHITE));
        return 1;
    }

    private static int online(CommandContext<FabricClientCommandSource> c, String path) {
        printOnline(c.getSource().getClient(), path);
        return 1;
    }

    /** Shared "who's online" report, posted to chat. Used by the command and the keybind. */
    public static void printOnline(Minecraft client, String path) {
        FinderGroup scope = (path == null) ? cfg().root : GroupTree.resolve(cfg().root, path);
        if (scope == null) {
            postErr(client, "No such group: " + path);
            return;
        }
        if (client.getConnection() == null) {
            postErr(client, "Not connected to a server.");
            return;
        }
        OnlineService.Snapshot online = OnlineService.current();
        List<FinderMember> all = GroupTree.collectMembers(scope, true);
        if (all.isEmpty()) {
            postInfo(client, Component.literal("[PF] No players in " + (path == null ? "any group" : path) + ".")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        int onlineCount = 0;
        MutableLine line = new MutableLine();
        for (FinderMember m : all) {
            if (online.isOnline(m)) {
                onlineCount++;
                int rgb = PlayerFinder.highlightRgbFor(m.parsedUuid(), m.name);
                line.append(Txt.colored(m.name == null ? "?" : m.name, rgb >= 0 ? rgb : 0x55FF55));
            }
        }
        String header = (path == null ? "All groups" : path) + ": " + onlineCount + "/" + all.size() + " online";
        postInfo(client, Component.literal("[PF] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(header).withStyle(ChatFormatting.WHITE)));
        if (onlineCount > 0) postInfo(client, line.build());
    }

    private static int solo(CommandContext<FabricClientCommandSource> c) {
        String path = StringArgumentType.getString(c, "path");
        int n = PlayerFinder.setSolo(path);
        if (n < 0) {
            err(c, "No such group: " + path);
            return 0;
        }
        info(c, Component.literal("Solo on: hiding everyone except ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(path).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + n + " players). /pf unsolo to undo.").withStyle(ChatFormatting.GRAY)));
        return 1;
    }

    private static int unsolo(CommandContext<FabricClientCommandSource> c) {
        PlayerFinder.clearSolo();
        info(c, Component.literal("Solo off — everyone is visible again.").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int globalHighlight(CommandContext<FabricClientCommandSource> c, boolean on) {
        cfg().globalHighlight = on;
        commit();
        info(c, Component.literal("Highlight (global): ")
                .append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)));
        return 1;
    }

    private static int hud(CommandContext<FabricClientCommandSource> c, boolean on) {
        cfg().hudEnabled = on;
        PlayerFinder.save();
        info(c, Component.literal("Online HUD: ")
                .append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)));
        return 1;
    }

    // ---- servers + cross-server scan -------------------------------------

    private static int serverAdd(CommandContext<FabricClientCommandSource> c) {
        String name = StringArgumentType.getString(c, "name");
        String address = StringArgumentType.getString(c, "address");
        String host = address;
        int port = 25565;
        int idx = address.lastIndexOf(':');
        if (idx > 0 && idx < address.length() - 1) {
            try {
                port = Integer.parseInt(address.substring(idx + 1));
                host = address.substring(0, idx);
            } catch (NumberFormatException ignored) {
                // not a port suffix — treat the whole thing as the host
            }
        }
        host = host.trim();
        if (host.isEmpty()) {
            err(c, "Bad address. Use host or host:port, e.g. play.example.net or 1.2.3.4:25566");
            return 0;
        }
        for (FinderServer s : cfg().servers) {
            if (s.name != null && s.name.equalsIgnoreCase(name)) {
                err(c, "A server named '" + name + "' already exists.");
                return 0;
            }
        }
        cfg().servers.add(new FinderServer(name, host, port));
        PlayerFinder.save();
        info(c, Component.literal("Added server ").append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + host + ":" + port + "). Run /pf scan to check it.").withStyle(ChatFormatting.GRAY)));
        return 1;
    }

    private static int serverRemove(CommandContext<FabricClientCommandSource> c) {
        String name = StringArgumentType.getString(c, "name");
        boolean removed = cfg().servers.removeIf(s -> s.name != null && s.name.equalsIgnoreCase(name));
        if (!removed) {
            err(c, "No server named '" + name + "'.");
            return 0;
        }
        PlayerFinder.save();
        info(c, Component.literal("Removed server " + name).withStyle(ChatFormatting.WHITE));
        return 1;
    }

    private static int serverList(CommandContext<FabricClientCommandSource> c) {
        List<FinderServer> servers = cfg().servers;
        if (servers.isEmpty()) {
            info(c, Component.literal("No servers yet. Add one: /pf server add <name> <address>").withStyle(ChatFormatting.GRAY));
            return 1;
        }
        c.getSource().sendFeedback(Component.literal("Servers (scanned by /pf scan):").withStyle(ChatFormatting.AQUA));
        for (FinderServer s : servers) {
            c.getSource().sendFeedback(Component.literal("  - ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(s.name).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  " + s.host + ":" + s.port).withStyle(ChatFormatting.GRAY)));
        }
        return 1;
    }

    private static int scan(CommandContext<FabricClientCommandSource> c, String path) {
        runScan(c.getSource().getClient(), path);
        return 1;
    }

    /** Cross-server scan, posting all output to chat. Used by the command and the scan keybind. */
    public static void runScan(Minecraft client, String path) {
        FinderConfig cfg = cfg();
        FinderGroup scope = (path == null) ? cfg.root : GroupTree.resolve(cfg.root, path);
        if (scope == null) {
            postErr(client, "No such group: " + path);
            return;
        }
        final List<FinderMember> members = GroupTree.collectMembers(scope, true);
        if (members.isEmpty()) {
            postInfo(client, Component.literal("[PF] No players in " + (path == null ? "any group" : path)
                    + " to search for.").withStyle(ChatFormatting.GRAY));
            return;
        }

        boolean haveCurrent = cfg.scanIncludeCurrent && client.getConnection() != null;
        final List<FinderServer> servers = new ArrayList<>(cfg.servers);
        if (servers.isEmpty() && !haveCurrent) {
            postErr(client, "No servers to scan. Add one: /pf server add <name> <address>");
            return;
        }

        postInfo(client, Component.literal("[PF] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("Scanning " + servers.size() + " server" + (servers.size() == 1 ? "" : "s")
                        + " for " + (path == null ? "all groups" : path) + " (" + members.size() + " players)…")
                        .withStyle(ChatFormatting.GRAY)));

        final List<CompletableFuture<ServerPinger.Result>> futures = new ArrayList<>();
        for (FinderServer s : servers) {
            futures.add(ServerPinger.ping(s.host, s.port, cfg.scanTimeoutMs,
                    cfg.scanPasses, cfg.scanMaxPings, cfg.scanStableRounds, cfg.scanPingDelayMs, cfg.scanUseQuery));
        }

        final boolean includeCurrent = haveCurrent;
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, e) -> client.execute(() -> {
                    java.util.Set<String> foundKeys = new java.util.HashSet<>();

                    if (includeCurrent) {
                        OnlineService.Snapshot cur = OnlineService.current();
                        List<FinderMember> matched = new ArrayList<>();
                        for (FinderMember m : members) if (cur.isOnline(m)) matched.add(m);
                        postServerLine(client, "Current server (you)", "tab list", matched, false, null, foundKeys);
                    }

                    for (int i = 0; i < servers.size(); i++) {
                        FinderServer s = servers.get(i);
                        ServerPinger.Result res;
                        try {
                            res = futures.get(i).join();
                        } catch (Exception ex) {
                            res = null;
                        }
                        List<FinderMember> matched = new ArrayList<>();
                        if (res != null && res.reachable) {
                            OnlineService.Snapshot snap = OnlineService.snapshotOf(res.sample);
                            for (FinderMember m : members) if (snap.isOnline(m)) matched.add(m);
                        }
                        String label = s.name + " (" + s.host + (s.port != 25565 ? ":" + s.port : "") + ")";
                        String status;
                        if (res == null || !res.reachable) {
                            status = (res != null && res.error != null) ? res.error : "unreachable";
                        } else {
                            String base = (res.online >= 0)
                                    ? res.online + (res.max >= 0 ? "/" + res.max : "") + " online" : "online";
                            String cov;
                            if (res.viaQuery) {
                                cov = " · full list via query";
                            } else if (!res.sample.isEmpty()) {
                                cov = " · saw " + res.sample.size() + " name" + (res.sample.size() == 1 ? "" : "s")
                                        + " over " + res.pings + " pings";
                            } else {
                                cov = "";
                            }
                            status = base + cov;
                        }
                        boolean hidden = res != null && res.sampleHidden;
                        postServerLine(client, label, status, matched, hidden,
                                (res == null ? null : res.error), foundKeys);
                    }

                    postInfo(client, Component.literal("[PF] ").withStyle(ChatFormatting.AQUA)
                            .append(Component.literal("Found " + foundKeys.size() + " of " + members.size()
                                    + " online across all scanned servers.").withStyle(ChatFormatting.WHITE)));
                }));
    }

    private static void postServerLine(Minecraft client, String label, String status, List<FinderMember> matched,
                                       boolean sampleHidden, String error, java.util.Set<String> foundKeys) {
        MutableComponent line = Component.literal("  ▸ ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(label).withStyle(ChatFormatting.WHITE));
        if (status != null) line.append(Component.literal("  " + status).withStyle(ChatFormatting.DARK_GRAY));
        postInfo(client, line);

        if (!matched.isEmpty()) {
            MutableLine names = new MutableLine();
            for (FinderMember m : matched) {
                foundKeys.add(memberKey(m));
                int rgb = PlayerFinder.highlightRgbFor(m.parsedUuid(), m.name);
                names.append(Txt.colored(m.name == null ? "?" : m.name, rgb >= 0 ? rgb : 0x55FF55));
            }
            postInfo(client, names.build());
        } else if (sampleHidden) {
            postInfo(client, Component.literal("      (player list hidden — can't check names here)")
                    .withStyle(ChatFormatting.GRAY));
        } else if (error != null) {
            postInfo(client, Component.literal("      (" + error + ")").withStyle(ChatFormatting.GRAY));
        } else {
            postInfo(client, Component.literal("      — none from your groups").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String memberKey(FinderMember m) {
        UUID u = m.parsedUuid();
        if (u != null) return FinderMember.uuidKey(u);
        return FinderMember.nameKey(m.name == null ? "" : m.name);
    }

    private static int importMctiers(CommandContext<FabricClientCommandSource> c, int tier, int count) {
        String gamemode = StringArgumentType.getString(c, "gamemode").toLowerCase(Locale.ROOT);
        if (!MCTiersClient.isValidGamemode(gamemode)) {
            err(c, "Unknown gamemode '" + gamemode + "'. One of: " + String.join(", ", MCTiersClient.GAMEMODES));
            return 0;
        }
        Minecraft client = c.getSource().getClient();
        info(c, Component.literal("Fetching MCTiers " + gamemode + (tier > 0 ? " T" + tier : "") + "…").withStyle(ChatFormatting.GRAY));

        MCTiersClient.fetchGamemode(gamemode, count).whenComplete((byTier, exData) -> client.execute(() -> {
            if (exData != null || byTier == null) {
                postErr(client, "MCTiers import failed: " + (exData != null ? rootMessage(exData) : "no data"));
                return;
            }
            String gmTitle = Character.toUpperCase(gamemode.charAt(0)) + gamemode.substring(1);
            FinderGroup base = GroupTree.createPath(cfg().root, "mctiers." + gamemode);
            if (base.color == null) base.color = "aqua";

            int added = 0, tiersTouched = 0;
            for (Map.Entry<Integer, List<FinderMember>> e : byTier.entrySet()) {
                int t = e.getKey();
                if (tier > 0 && t != tier) continue;
                FinderGroup tierGroup = base.childByName("T" + t);
                if (tierGroup == null) {
                    tierGroup = new FinderGroup("T" + t);
                    base.groups.add(tierGroup);
                }
                tiersTouched++;
                for (FinderMember src : e.getValue()) {
                    if (src.name != null && tierGroup.memberByName(src.name) != null) continue;
                    tierGroup.members.add(new FinderMember(src.name, src.uuid));
                    added++;
                }
            }
            commit();

            if (added == 0) {
                postInfo(client, Component.literal("MCTiers " + gmTitle
                        + (tier > 0 ? " T" + tier : "") + ": nothing new to add.").withStyle(ChatFormatting.GRAY));
            } else {
                postInfo(client, Component.literal("[PF] ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal("Imported " + added + " players from MCTiers " + gmTitle
                                + " into mctiers." + gamemode + " (" + tiersTouched + " tier"
                                + (tiersTouched == 1 ? "" : "s") + ").").withStyle(ChatFormatting.WHITE)));
                postInfo(client, Component.literal("Try: /pf online mctiers." + gamemode
                        + "   or   /pf solo mctiers." + gamemode).withStyle(ChatFormatting.GRAY));
            }
        }));
        return 1;
    }

    // ---- async-safe chat (called from the HTTP completion, marshalled to the client thread) ----

    private static void postInfo(Minecraft client, Component msg) {
        if (client.player != null) client.player.displayClientMessage(msg, false);
        else PlayerFinder.LOGGER.info("[PlayerFinder] {}", msg.getString());
    }

    private static void postErr(Minecraft client, String msg) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("[PF] " + msg).withStyle(ChatFormatting.RED), false);
        } else {
            PlayerFinder.LOGGER.warn("[PlayerFinder] {}", msg);
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() != null ? cur.getMessage() : cur.toString();
    }

    /** Builds a single chat line of online names separated by grey commas. */
    private static final class MutableLine {
        private net.minecraft.network.chat.MutableComponent acc = null;

        void append(Component name) {
            if (acc == null) {
                acc = Component.literal("  ").append(name);
            } else {
                acc.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY)).append(name);
            }
        }

        Component build() {
            return acc == null ? Component.empty() : acc;
        }
    }
}
