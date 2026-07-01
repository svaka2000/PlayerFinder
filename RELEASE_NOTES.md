## PlayerFinder v1.2.0

Gets **past the ~12-name Server List Ping cap** on cross-server scans — two ways.

### New in scanning
- **Adaptive multi-ping** — vanilla/Paper servers return a *different random* ~12-name sample each ping, so `/pf scan` now keeps pinging and **merges the samples until the list converges** (or it has everyone), then stops. On servers that expose a real rotating sample this enumerates far past 12 — up to the **full** online list. (Verified: a 30-player rotating server → all 30; a 200-player → ~196.)
- **Query protocol** — if a server has `enable-query=true`, PlayerFinder pulls its **entire** player list in one UDP request (no cap at all). Checked automatically, used when available.
- Scan lines now show how the list was obtained, e.g. `73/500 online · saw 96 names over 22 pings` or `· full list via query`.
- New config knobs: `scanPasses` (min pings), `scanMaxPings`, `scanStableRounds`, `scanPingDelayMs`, `scanUseQuery`.

### The one hard limit (unchanged, and unfixable client-side)
Some big networks (Hypixel, etc.) send an **empty or fake** sample **and** disable query — they never transmit their player list at all. There you'll see the online **count** but *"player list hidden — can't check names here."* No client mod can invent data a server refuses to send. On smaller / practice / vanilla-style servers, `/pf scan` now sees the whole list.

### Everything from before
Nested player groups, who's-online on the current server, nametag highlight, hide-everyone-but-them (solo), MCTiers tier-list import, cross-server search, on-screen HUD, Mod Menu screen.

### Downloads
Fabric + Fabric API required.

| Minecraft | Jar |
|---|---|
| 1.21.4 | `playerfinder-1.2.0+1.21.4.jar` |
| 1.21.10 | `playerfinder-1.2.0+1.21.10.jar` |
| 1.21.11 | `playerfinder-1.2.0+1.21.11.jar` |

### Not a cheat
Client-side, read-only status/query protocols (the same ones server-list pingers and trackers use) — no login/join, no gameplay packets, no X-ray/ESP. See the [README](https://github.com/svaka2000/PlayerFinder#readme).
