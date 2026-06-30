# PlayerFinder — handoff / maintainer notes

Client-side Fabric mod, multi-version (1.21.4 / 1.21.10 / 1.21.11), Mojmap. Same studio workflow as FluidKB/KillFX.

## Build & release

```bash
./build-all.sh                 # builds all 3 versions into dist/
./build-all.sh 1.21.11         # build a subset
```

Each jar is `dist/playerfinder-1.0.0+<mc>.jar`. Per-version build logs in `/tmp/playerfinder-<mc>.log`.

Release (neutral author — never the real identity in commits):

```bash
GIT_AUTHOR_NAME=playerfinder GIT_AUTHOR_EMAIL=noreply@users.noreply.github.com \
GIT_COMMITTER_NAME=playerfinder GIT_COMMITTER_EMAIL=noreply@users.noreply.github.com \
git commit -m "..."

gh release create v1.0.0 --repo svaka2000/PlayerFinder \
  --notes-file RELEASE_NOTES.md dist/*.jar
```

Branch: `master`. Repo: private `github.com/svaka2000/PlayerFinder`.

## Architecture

Almost everything is in `src/main` because it targets stable, version-agnostic APIs. Only two things diverge across versions and live in per-version source sets (`src/v1_21_4|v1_21_10|v1_21_11`, selected by `graphicsVariant` in `build.gradle`):

1. **`KeybindCompat`** — keybind category: 1.21.4 = category String; 1.21.10 = `KeyMapping.Category.register(ResourceLocation)`; 1.21.11 = same but `Identifier`.
2. **`ProfileCompat`** — authlib `GameProfile`: 1.21.4 = classic class (`getId()`/`getName()`); 1.21.10 & 1.21.11 = **record** (`id()`/`name()`). All `GameProfile` access in shared code goes through this.

Core:
- `PlayerFinder` — static runtime: builds the highlight lookup + solo set; hot-path lookups `highlightRgbFor` / `isHiddenBySolo` (read volatile snapshots, rebuilt only on config/solo change).
- `config/` — `FinderConfig` (group tree + toggles) ↔ `config/playerfinder.json` via Gson.
- `core/GroupTree` — dot-separated path ops, recursive member collection, colour inheritance.
- `core/OnlineService` — reads the tab list (`ClientPacketListener.getOnlinePlayers()`).
- `mctiers/MCTiersClient` — async `java.net.http` GET to `mctiers.com/api/v2/mode/{gamemode}` (object keyed by tier → players).
- `command/FinderCommands` — Brigadier `/pf` tree (fabric client-command API v2).
- `hud/FinderHud` — the online panel.
- `gui/FinderScreen` + `compat/ModMenuIntegration` — settings screen.

Mixins (all `src/main`, stable targets):
- `PlayerNameMixin` → `Player.getDisplayName` RETURN → recolour (highlight).
- `EntityCullMixin` → `EntityRenderer.shouldRender` HEAD → cancel for non-solo players (hide-others).
- `GuiHudMixin` → `Gui.render(GuiGraphics, DeltaTracker)` TAIL → draw HUD.

## Anti-cheat contract (keep this line)

Render/info only. Reads only the tab list + public MCTiers API; sends **no** packets. Highlight only recolours nametags of players already visible (NOT through-walls ESP — no glow/boxes/tracers). Solo only suppresses local rendering of other players (removes info, never touches positions/hitboxes/reach). This is the legit cousin of the previously-declined injectable cheat client — do **not** add wallhack glow, tracers, reach, or anything that sends/forges packets.

## Status

- ✅ Compiles for 1.21.4 / 1.21.10 / 1.21.11; mixins resolve; jars well-formed (entrypoints, mixins, icon, lang packaged).
- ⚠️ Not yet run in a live client — the in-world highlight (does `getDisplayName` recolour reach the floating nametag on every version?) and the HUD layout want one play-test + tuning. MCTiers endpoint verified live against the real v2 API.

## Possible next steps (if Weilin asks)

- Extend the build matrix to the full 1.21 line (like KillFX) — the stable targets make this cheap; mainly add `KeybindCompat`/`ProfileCompat` buckets + `build-all.sh` rows.
- Per-tier colours on MCTiers import; sort HUD by online count; a tiny in-game group editor screen.
