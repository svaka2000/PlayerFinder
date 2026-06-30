package com.playerfinder.config;

import java.util.ArrayList;
import java.util.List;

/**
 * A named group of players that can itself contain sub-groups, forming an arbitrarily deep tree
 * ("pvp" → "sword" → "t2"). The config holds a single hidden root group whose children are the
 * top-level groups the player sees.
 */
public class FinderGroup {
    public String name;

    /** Display colour: a named colour ("red", "aqua", "gold", …) or a hex string ("#33ccff").
     *  {@code null} = inherit from the nearest coloured ancestor (or a default). */
    public String color;

    /** Whether members of this group get their nametags highlighted (subject to the global toggle). */
    public boolean highlight = true;

    public List<FinderMember> members = new ArrayList<>();
    public List<FinderGroup> groups = new ArrayList<>();

    public FinderGroup() {}

    public FinderGroup(String name) {
        this.name = name;
    }

    public FinderGroup childByName(String childName) {
        for (FinderGroup g : groups) {
            if (g.name != null && g.name.equalsIgnoreCase(childName)) return g;
        }
        return null;
    }

    public FinderMember memberByName(String memberName) {
        for (FinderMember m : members) {
            if (m.name != null && m.name.equalsIgnoreCase(memberName)) return m;
        }
        return null;
    }
}
