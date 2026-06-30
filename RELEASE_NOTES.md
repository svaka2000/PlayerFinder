## PlayerFinder v1.0.0

Find the players you actually want to play with. Organize people into named **groups** with nested **sub-groups**, then instantly see who's online, highlight their nametags, or hide everyone else.

### Features
- **Groups & sub-groups** — arbitrarily nested (`pvp.sword.t2`), each with its own colour.
- **Who's online** — `/pf online [group]` shows which members are on the current server (reads the tab list), plus an optional on-screen HUD panel.
- **Highlight** — recolours group members' nametags so they stand out in a lobby.
- **Hide everyone but them** — `/pf solo <group>` to isolate your targets; `/pf unsolo` to undo.
- **MCTiers import** — `/pf import mctiers <gamemode> [tier]` pulls a whole tier list (e.g. every T2 sword player) into a group. Gamemodes: sword, axe, mace, pot, nethop, smp, uhc, vanilla.
- Four optional keybinds (Options → Controls → PlayerFinder) and a Mod Menu settings screen.

### Downloads
Pick the jar for your Minecraft version (Fabric + Fabric API required):

| Minecraft | Jar |
|---|---|
| 1.21.4 | `playerfinder-1.0.0+1.21.4.jar` |
| 1.21.10 | `playerfinder-1.0.0+1.21.10.jar` |
| 1.21.11 | `playerfinder-1.0.0+1.21.11.jar` |

### Not a cheat
Client-side, render/info only. It reads the in-game tab list and the public MCTiers API and **sends no packets** — the server can't tell it's installed. Highlight only recolours nametags of players you can already see (no X-ray/ESP, no glow-through-walls, no tracers). "Hide others" only stops your own client from drawing other players. It changes what you see, not the game.

See the [README](https://github.com/svaka2000/PlayerFinder#readme) for the full command list.
