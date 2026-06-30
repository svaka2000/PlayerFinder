package com.playerfinder.core;

import com.playerfinder.config.FinderGroup;
import com.playerfinder.config.FinderMember;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure tree operations over the {@link FinderGroup} hierarchy: path resolution, create/remove/move,
 *  recursive member collection and colour inheritance. No Minecraft dependencies. */
public final class GroupTree {
    private GroupTree() {}

    /** Group paths are dot-separated ("pvp.sword.t2"); we also accept "/" for convenience. The canonical
     *  form produced by {@link #pathOf} uses dots, because Brigadier's string argument stops at "/". */
    public static List<String> segments(String path) {
        List<String> out = new ArrayList<>();
        if (path == null) return out;
        for (String s : path.split("[./]")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** Resolve a slash path to a group under {@code root}, or {@code null} if any segment is missing.
     *  An empty path returns {@code root} itself. */
    public static FinderGroup resolve(FinderGroup root, String path) {
        FinderGroup cur = root;
        for (String seg : segments(path)) {
            cur = cur.childByName(seg);
            if (cur == null) return null;
        }
        return cur;
    }

    /** Resolve, creating any missing groups along the way. Returns the deepest group. */
    public static FinderGroup createPath(FinderGroup root, String path) {
        FinderGroup cur = root;
        for (String seg : segments(path)) {
            FinderGroup next = cur.childByName(seg);
            if (next == null) {
                next = new FinderGroup(seg);
                cur.groups.add(next);
            }
            cur = next;
        }
        return cur;
    }

    public static FinderGroup parentOf(FinderGroup root, FinderGroup target) {
        if (root.groups != null) {
            for (FinderGroup child : root.groups) {
                if (child == target) return root;
                FinderGroup deeper = parentOf(child, target);
                if (deeper != null) return deeper;
            }
        }
        return null;
    }

    /** Slash path of a group relative to root (root itself → ""). */
    public static String pathOf(FinderGroup root, FinderGroup target) {
        if (target == root) return "";
        List<String> parts = new ArrayList<>();
        FinderGroup cur = target;
        while (cur != null && cur != root) {
            parts.add(0, cur.name);
            cur = parentOf(root, cur);
        }
        return String.join(".", parts);
    }

    /** Remove the group at {@code path}. Returns the removed group, or {@code null} if not found. */
    public static FinderGroup remove(FinderGroup root, String path) {
        FinderGroup target = resolve(root, path);
        if (target == null || target == root) return null;
        FinderGroup parent = parentOf(root, target);
        if (parent == null) return null;
        parent.groups.remove(target);
        return target;
    }

    /** Move {@code group} under {@code newParent}. Refuses to move a group into itself or a descendant. */
    public static boolean move(FinderGroup root, FinderGroup group, FinderGroup newParent) {
        if (group == root || newParent == null) return false;
        if (group == newParent || isDescendant(group, newParent)) return false;
        FinderGroup parent = parentOf(root, group);
        if (parent == null) return false;
        if (newParent.childByName(group.name) != null) return false; // name clash
        parent.groups.remove(group);
        newParent.groups.add(group);
        return true;
    }

    private static boolean isDescendant(FinderGroup ancestor, FinderGroup maybe) {
        for (FinderGroup child : ancestor.groups) {
            if (child == maybe || isDescendant(child, maybe)) return true;
        }
        return false;
    }

    /** Collect members of a group, optionally recursing into sub-groups; de-duplicated by identity key. */
    public static List<FinderMember> collectMembers(FinderGroup g, boolean recursive) {
        Map<String, FinderMember> seen = new LinkedHashMap<>();
        collect(g, recursive, seen);
        return new ArrayList<>(seen.values());
    }

    private static void collect(FinderGroup g, boolean recursive, Map<String, FinderMember> out) {
        for (FinderMember m : g.members) {
            String key = (m.uuid != null && !m.uuid.isBlank())
                    ? m.uuid.toLowerCase(java.util.Locale.ROOT)
                    : "name:" + (m.name == null ? "" : m.name.toLowerCase(java.util.Locale.ROOT));
            out.putIfAbsent(key, m);
        }
        if (recursive) {
            for (FinderGroup sub : g.groups) collect(sub, true, out);
        }
    }

    /** Resolve a group's effective highlight colour: nearest ancestor (incl. self) with an explicit
     *  colour, else a stable palette colour by top-level index. */
    public static int effectiveColorRgb(FinderGroup root, FinderGroup group) {
        FinderGroup cur = group;
        while (cur != null && cur != root) {
            if (cur.color != null && !cur.color.isBlank()) {
                return ColorUtil.parseRgb(cur.color, 0xFFFFFF);
            }
            cur = parentOf(root, cur);
        }
        // No explicit colour anywhere up the chain: pick by the top-level ancestor's position.
        FinderGroup top = group;
        FinderGroup p = parentOf(root, top);
        while (p != null && p != root) {
            top = p;
            p = parentOf(root, top);
        }
        int idx = root.groups.indexOf(top);
        return ColorUtil.paletteColor(Math.max(idx, 0));
    }

    /** All group paths under root (depth-first), for command suggestions and listing. */
    public static List<String> allPaths(FinderGroup root) {
        List<String> out = new ArrayList<>();
        for (FinderGroup g : root.groups) addPaths(root, g, out);
        return out;
    }

    private static void addPaths(FinderGroup root, FinderGroup g, List<String> out) {
        out.add(pathOf(root, g));
        for (FinderGroup sub : g.groups) addPaths(root, sub, out);
    }
}
