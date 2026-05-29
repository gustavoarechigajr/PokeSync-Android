# PokeSync Frontend (`src/pokesync/`)

The PokeSync UI: a clean, **mobile-first, Pokémon HOME-inspired** interface that runs in a WebView
inside the MAUI Android app, talking to the **embedded PKVault backend** over its REST API.

This module is the new presentation layer. The legacy upstream PKVault React UI still exists elsewhere
in `src/` but is **unmounted** (kept for reference until parity; delete later). `src/main.tsx` mounts
`<PokeSyncApp/>` inside the existing providers (`DataProvider`, `SplashMain`) which load settings +
static data.

## Why this structure

- **Reuse the backend + data layer, replace the looks.** PKVault's value is its backend (PKHeX
  parsing, storage, dex). We keep its REST API + the generated SDK and build new UI on top.
- **Self-contained.** Everything PokeSync-specific lives here, so future work has one home.

## Directory map

```
pokesync/
├── app/PokeSyncApp.tsx      Root: app bar + current screen. Imports design-system/global.
├── design-system/
│   ├── tokens.ts            THE source of truth: teal palette, spacing, radius, shadow, fonts,
│   │                        typeColor(). Never hardcode colors/spacing — use these.
│   ├── global.ts            @font-face (self-hosted Baloo 2 + Nunito) + resets + app background.
│   └── components/          Primitives: Panel, Pill, IconButton, TypeBadge, Slot, StatHexagon.
├── data/use-storage-data.ts Thin react-query hooks over the SDK (useBanks/useBoxesById/useSaves/
│                            useVaultPkms/useSavePkms). Reuses src/data/hooks/use-pkm-*-index.
├── features/
│   ├── storage/             Dual-pane box/vault screen (StorageScreen, BoxPane, BoxGrid, pkm-sprite).
│   └── summary/SummaryPanel Enriched Pokémon detail (shown on the pane opposite the selection).
└── shared/
    ├── ball-names.ts        Ball id → name (PKHeX Ball enum).
    └── navigation/          Controller + keyboard + touch input → NavIntents + focus cursor.
```

## Design system

- **Aesthetic:** Pokémon HOME teal/mint (see `tokens.palette`). Carried from the PokeSync-Android
  Kotlin app + the user's mockups in `/Images/PokeSync Design/`.
- **Fonts:** Baloo 2 (display) + Nunito (body), self-hosted via `@fontsource` (the app is **offline** —
  no CDN fonts).
- **Styling:** `@emotion/css` (`css`/`cx`), matching the rest of the repo.
- **Type colors:** `typeColor(name)` in tokens; canonical palette mirrors `src/ui/theme.ts`.

## Data layer

- The backend exposes a REST API; Orval generates the SDK in `src/data/sdk/` (fetchers + query keys +
  some react-query hooks). **To regenerate the SDK** (after backend API changes) you need Swagger,
  which is OFF by default. Run the backend on desktop with it enabled, then run Orval:
  ```
  # 1. backend with Swagger (desktop):
  cd PokeSync/PKVault.Backend && dotnet run -c Debug -p:EnableSwagger=true   # serves :5000/swagger
  # 2. regenerate (needs Node 22):
  cd PokeSync/frontend && VITE_OPENAPI_PATH=http://localhost:5000/swagger/v1/swagger.json \
      ./node_modules/.bin/tsx generate-sdk.ts
  ```
- New screens should consume `data/use-storage-data.ts` (or add a sibling hook there) rather than
  calling the SDK directly, so data access stays in one place.
- **Gotcha:** an imported save's boxes come from `SaveInfosDTO.boxCount`/`boxSlotCount` + the
  Pokémon's `boxId` — NOT from `bank.view.saves` (empty for imported saves).

## Input & navigation (controller + keyboard + touch)

`shared/navigation/`:
- `intents.ts` — the `NavIntent` vocabulary every input maps to.
- `use-input-intents.ts` — combines **keyboard** (Arrows/Enter/Escape/[ ]/Tab; Android often delivers
  D-pad/buttons as keys) and the **Gamepad API** (D-pad + left stick with hold-repeat; A/B/L/R/L2/R2)
  into NavIntents.
- Touch is handled by components directly (`onClick`).

**Default mapping:** D-pad/stick = move cursor · A/Enter = select · B/Esc = back · L/R = prev/next box ·
L2/R2/Tab = switch pane · Y = overview (reserved).

**To make a new screen controller-navigable:** keep a `focus` index in state, call `useInputIntents`
with a handler that updates focus on directions and acts on confirm/back/etc., and pass `focused` to
the focusable cells (e.g. `Slot`). See `features/storage/StorageScreen.tsx` for the reference pattern.

## Build, embed, deploy (must use Node 22)

```
export PATH="$HOME/.nvm/versions/node/v22.22.1/bin:$PATH"   # Node 18 is too old for the toolchain
cd PokeSync/frontend && npm run build                        # → dist/
rm -rf ../PKVault.Backend/wwwroot && cp -r dist ../PKVault.Backend/wwwroot
cd ../../PKHEX-Android/PKHeX.Android
dotnet build -t:Run -f net10.0-android -c Debug -p:AdbTarget="-s <device-serial>"
```
The backend embeds `wwwroot` (ManifestEmbeddedFileProvider) and serves it; the frontend resolves its
API base from `window.location.origin` (same-origin → no CORS).

## Roadmap (next slices)

1. **Transfer**: drag / move Pokémon save⇄vault (the core PokeSync action).
2. **Box overview** toggle (grid of boxes), party box, search/filter.
3. **Pokédex**, backups, settings screens.
4. Remappable controls; portrait/responsive layout; editing.
5. Polish: per-ball sprites, animations, wallpapers.
