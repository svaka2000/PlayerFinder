# PlayerFinder

A client-side Fabric mod for finding the players you actually want to play with.

Organize players into named **groups** (with nested **sub-groups**), then instantly:

- see **who's online** on the current server, from any group or sub-group,
- **highlight** group members' nametags in the group's colour, or
- **hide everyone but** a chosen group ("solo"), so your target stands out in a crowded lobby.

You can also **import whole MCTiers tier lists** — e.g. every T2 sword player — straight into a group in one command.

> Built for people who hate fighting randoms. Add the people you like to fight once, and find them again whenever they're on.

---

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) (0.16+) and [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version.
2. Drop the matching `playerfinder-1.0.0+<mc>.jar` from the [latest release](https://github.com/svaka2000/PlayerFinder/releases) into your `mods/` folder.
3. (Optional) [Mod Menu](https://modrinth.com/mod/modmenu) adds a settings button.

**Supported:** Minecraft **1.21.4**, **1.21.10**, **1.21.11** (Fabric). Client-side only — the server does not need the mod and never sees it.

---

## Quick start

```
/pf group create rivals             # make a group
/pf add rivals Notch                # add a player to it
/pf add rivals jeb_
/pf online rivals                   # who from "rivals" is on this server right now?
/pf solo rivals                     # hide everyone except rivals
/pf unsolo                          # show everyone again
```

Import a tier list and find those players:

```
/pf import mctiers sword 2          # every T2 sword player -> group "mctiers.sword" / sub-group "T2"
/pf online mctiers.sword            # who from sword tiers is online
/pf solo mctiers.sword.T2           # hide everyone except T2 sword players
```

Paths are **dot-separated**: `pvp.sword.t2`. Groups can nest as deep as you like.

---

## Commands

| Command | What it does |
|---|---|
| `/pf help` | Full command list in chat |
| `/pf group create <path>` | Create a group or sub-group (creates parents as needed) |
| `/pf group list` | Print the whole tree with member counts |
| `/pf group color <path> <color>` | Set the group's highlight colour — a name (`red`, `aqua`, `gold`, `light_purple`) or 6-digit hex (`33ccff`) |
| `/pf group highlight <path> on\|off` | Turn highlighting on/off for one group |
| `/pf group remove <path>` | Delete a group (and its sub-groups) |
| `/pf group move <path> <parent>` | Re-parent a group (`parent` = `root` for top level) |
| `/pf add <path> <player>` | Add a player to a group (suggests online names) |
| `/pf remove <path> <player>` | Remove a player |
| `/pf online [path]` | Who from the group (recursively) is online on this server |
| `/pf solo <path>` | Hide everyone except this group |
| `/pf unsolo` | Show everyone again |
| `/pf highlight on\|off` | Master highlight toggle |
| `/pf hud on\|off` | The on-screen "who's online" panel |
| `/pf import mctiers <gamemode> [tier] [count]` | Import an MCTiers list into `mctiers.<gamemode>` |

`/playerfinder` is an alias for `/pf`.

### Keybinds (Options → Controls → PlayerFinder)

Unbound by default — bind whichever you want:

- **Toggle highlight** — flip the master highlight
- **Toggle hide-others (solo)** — solo the last group you used / clear solo
- **Toggle online HUD** — show/hide the panel
- **Print who's online** — dump all groups' online members to chat

---

## MCTiers import

`/pf import mctiers <gamemode> [tier] [count]` reads the public **[MCTiers v2 API](https://mctiers.com/docs/v2)** and bulk-adds players to a group.

- **Gamemodes:** `sword` `axe` `mace` `pot` `nethop` `smp` `uhc` `vanilla`
- With **no tier**, every tier becomes a sub-group: `mctiers.sword` → `T1`, `T2`, `T3`, `T4`, `T5`.
- With a **tier** (1–5), only that tier is imported.
- `count` (default 50, max 200) is how many players to pull *per tier*.

Examples:

```
/pf import mctiers sword            # all sword tiers, as sub-groups T1..T5
/pf import mctiers mace 1           # only T1 mace players
/pf import mctiers pot 2 100        # up to 100 T2 pot players
```

Players are stored with their UUID, so they still match if they change their name.

---

## The on-screen HUD

When enabled, a compact panel lists each group that has someone online, in the group's colour:

```
rivals (2/4)
  Notch
  jeb_
mctiers.sword (1/120)
  strafikk
```

Move it with `hudAnchor` (`top_left` / `top_right` / `bottom_left` / `bottom_right`) and the offsets in the config, or via Mod Menu.

---

## Is this allowed? (anti-cheat)

PlayerFinder is **render-only and information-only**. It is not a hacked client and gives no combat advantage:

- It **reads** only data your client already has — the in-game **tab list** (who's online) — and the **public MCTiers API**. It **never sends a packet to the server**, so the server can't even tell it's installed.
- **Highlight** only recolours the nametag of a player — the same name you would already see. It is **not X-ray / ESP**: it does **not** draw players through walls, add boxes/tracers, or reveal anyone you couldn't already see. It's the same idea as a "friends" highlight in mainstream clients.
- **Hide-others (solo)** only stops *your own client* from drawing other players. It **removes** information from your view rather than adding any, and never touches positions, hitboxes, reach, or your attacks.

It changes what *you* see on *your* screen. It does not change the game.

---

## Config

Saved to `config/playerfinder.json` (the group tree + display toggles). Safe to edit by hand or sync between instances.

---

## Credits

By **LBmods**. MCTiers data © the MCTiers team — see <https://mctiers.com>.
Licensed MIT.
