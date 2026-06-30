## PlayerFinder v1.1.0

Adds **cross-server search**: find which of your grouped players are online on *other* servers, not just the one you're on.

### New
- **`/pf scan [group]`** — checks a customizable list of servers (plus your current one) and reports which of your players are online and where.
- **`/pf server add <name> <address>`**, **`/pf server list`**, **`/pf server remove <name>`** — manage the scan list. Addresses accept `host` or `host:port`, and **SRV records** are resolved (so `play.example.net` just works).
- A new **Scan servers** keybind (Options → Controls → PlayerFinder).
- Config: `scanTimeoutMs`, `scanPasses` (multi-ping sample union, default 3), `scanIncludeCurrent`.

### How it works & its limit
Scanning uses the **Server List Ping** — the same status request your multiplayer screen makes for the player count. It never connects/logs in and sends no gameplay packets. Servers only publish a small **sample** of online names (~12 on vanilla); PlayerFinder pings a few times and merges them to catch more. **Big networks (Hypixel, etc.) hide or fake the sample** — there you'll see the online count but the line says *"player list hidden."* On smaller / practice / vanilla-style servers that expose the sample, specific players are detected reliably. (Wire protocol + SRV verified against real servers.)

### Everything from v1.0.0
Nested player groups, who's-online on the current server, nametag highlight, hide-everyone-but-them (solo), MCTiers tier-list import, on-screen HUD, Mod Menu screen.

### Downloads
Fabric + Fabric API required. Pick your version:

| Minecraft | Jar |
|---|---|
| 1.21.4 | `playerfinder-1.1.0+1.21.4.jar` |
| 1.21.10 | `playerfinder-1.1.0+1.21.10.jar` |
| 1.21.11 | `playerfinder-1.1.0+1.21.11.jar` |

### Not a cheat
Client-side, render/info only — reads the tab list, the public MCTiers API, and server status pings; sends no gameplay packets. No X-ray/ESP. See the [README](https://github.com/svaka2000/PlayerFinder#readme).
