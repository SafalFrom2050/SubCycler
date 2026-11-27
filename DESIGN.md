# SubCycler Design System: Botanical Emerald Monochromatic Material 3 Expressive

> **Document Version**: 4.0.0  
> **Target Theme**: True Edge-to-Edge Adaptive Light & Dark 70% Neutral / 30% Botanical Emerald Material 3 Expressive ("Botanical Kinetic Architecture II") with Topological `graphics-shapes` Morphing, Role-Split Spring Physics, Cycle Vessel Cards, Time-Machine Wave Scrubbing & Variable Editorial Typography  
> **Intended Audience**: Human Engineers & Future AI Coding Agents

---

## 1. Design Philosophy & Aesthetic Identity

SubCycler uses a refined **70% Architectural Neutral / 30% Botanical Emerald Material 3 Expressive** brand identity. Clean neutral alabaster/white (Light) and refined obsidian-graphite (Dark) surfaces keep the UI calm and never overly green, while signature **Botanical Emerald (`#0C6B4E` / `#7CE6C1`)** and the bundled sculptural **fluid silk waves** masked backdrop anchor the Hero bento cards, active controls, and primary CTAs.

### Core Principles:
1. **70/30 Neutral-to-Emerald Restraint**: Neutral surfaces (`#F5F6F5` / `#FFFFFF` Light; `#101213` / `#1B1F21` Dark) carry the layout cleanly; Botanical Emerald concentrates on Hero duotone backdrops, active pills, the `Saved /yr` scalloped sticker, and the detached scalloped `+` button.
2. **Editorial Masked Photo Backdrop (`ExpressiveBackdrop.kt`)**: Bundles a lightweight (~52 KB) offline monochrome sculptural fluid silk waves artwork (`R.drawable.bg_expressive_silk_waves`) rendered quietly behind the canvas (`ExpressiveScreenBackdrop`) and vibrantly beneath a translucent emerald gradient mask inside Hero Bento cards (`ExpressiveHeroCardBackdrop`).
3. **Adaptive Light & Dark Themes (`isSystemInDarkTheme()`) — Distinct Brand Identity**: Follows system Light/Dark mode with adaptive status/navigation bars without adopting Android dynamic wallpaper colors.
4. **M3 Expressive Split Floating Bottom Toolbar (`SubTrackNavHost.kt`)**: A crisp neutral floating pill dock where the active tab expands smoothly with spring physics to reveal **Icon + Label** inside a tonal emerald pill while inactive tabs collapse to clean icons, paired side-by-side with a **detached 12-petal scalloped `+` button** on `Subscriptions` so cards are never overlapped.
5. **Balanced Badge Shades & Tactile Spring Physics**: Service badges mix Warm Sand and Cool Slate neutrals with Fresh Mint, Cool Jade, and Bold Emerald accents, powered by `Spring.DampingRatioMediumBouncy` reactive icons.

---

## 2. Signature Brand Token System (`Color.kt` & `Theme.kt`)

### Light Mode (70% Crisp Neutral Alabaster & White · 30% Botanical Emerald)
| Role | Hex | Usage |
| :--- | :--- | :--- |
| `background` / `surface` | `#F5F6F5` | Calm neutral alabaster canvas |
| `surfaceContainerLow` | `#FAFBFA` | Modal sheets, dialogs |
| `surfaceContainer` | `#FFFFFF` | Pure crisp white bento cards |
| `BrandLightHeroContainer` | `#CDEFE0` | Masked duotone Home & Savings Hero cards |
| `surfaceContainerHigh` | `#ECEFED` | Neutral stone-mist inputs, chips & nested pills |
| `primary` / `primaryContainer` | `#0C6B4E` / `#D2F2E3` | Botanical Emerald CTAs, active nav pill, detached scalloped `+` button |
| `secondary` / `secondaryContainer` | `#196B64` / `#D6F0ED` | Cool Jade complementary shade |
| `tertiary` / `tertiaryContainer` | `#4C5D6B` / `#E3E9EE` | Calm Cool Slate paused/break states |

### Dark Mode (70% Refined Obsidian-Graphite · 30% Botanical Mint)
| Role | Hex | Usage |
| :--- | :--- | :--- |
| `background` / `surface` | `#101213` | Clean architectural obsidian-graphite canvas |
| `surfaceContainerLow` | `#16191B` | Modal sheets, dialogs |
| `surfaceContainer` | `#1B1F21` | Velvet charcoal-graphite bento cards |
| `BrandDarkHeroContainer` | `#133A2E` | Masked duotone Home & Savings Hero cards |
| `surfaceContainerHigh` | `#24292C` | Neutral elevated floating dock, inputs & nested pills |
| `primary` / `primaryContainer` | `#7CE6C1` / `#164636` | Luminous soft mint CTAs, active nav pill & detached `+` button |
| `secondary` / `secondaryContainer` | `#84DBD4` / `#164341` | Cool Seafoam Jade shade |
| `tertiary` / `tertiaryContainer` | `#B3C3CF` / `#28333B` | Calm Cool Slate paused/break states |

---

## 3. Material 3 Expressive Variable Typography (`Type.kt` & `ExpressiveTypography.kt`)

SubCycler bundles two 100% offline **SIL Open Font License (OFL)** variable fonts inside `core/ui/src/main/res/font/` (preserving zero `INTERNET` permission):

- **`bricolage_grotesque_variable.ttf`** (`opsz` 12–96, `wdth` 75–100, `wght` 200–800):
  - `DisplayHeroFontFamily` (`opsz = 64–84sp`, `wdth = 90–94%`): Sculptural, slightly condensed high-contrast display voice for hero spend numerals, screen headlines, and `"Sub"` in the brand wordmark.
  - `DisplayFontFamily` (`opsz = 24–48sp`, `wdth = 94–98%`): Warm editorial display voice for dialog/sheet headers, Bento service card names, `"Cycler"` in the wordmark, and organic monogram badges (`NF`, `PV`, `AI`, `D+`, `SP`).
- **`plus_jakarta_sans_variable.ttf`** (`wght` 200–800):
  - `BodyFontFamily`: Crisp modern geometric sans across all body copy, pills, form inputs, tabs, and wide-tracked uppercase editorial eyebrows.

### Creative Typography Primitives (`ExpressiveTypography.kt`):
- **`ExpressiveMoneyText`**: Locale-safe split-scale currency renderer (works across `$12.16`, `¥6,469`, `CA$16.49`, `12,99 €`) pairing an elevated softer currency symbol, oversized tabular `DisplayHeroFontFamily` integer digits (`tnum`), and baseline-anchored smaller fractional cents.
- **`ExpressiveBrandWordmark`**: Two-tone, weight-contrasted `"Sub"` (Black `DisplayHeroFontFamily` in `onBackground`) + `"Cycler"` (SemiBold `DisplayFontFamily` in signature Botanical Emerald `primary`).
- **`ExpressiveEyebrow`**: Wide-tracked (`1.4sp`) `11sp` ExtraBold uppercase section eyebrows (`EFFECTIVE MONTHLY`, `PROJECTED 5-YEAR SAVINGS`, `DATE INSPECTOR`) creating editorial rhythm against tightly tracked display headlines.

---

## 4. Core Component Guidelines

### 1. Expressive Hero Bento Card & Scroll-Collapsible Efficiency HUD (`FinancialEfficiencyHud.kt`)
- Masked silk-waves duotone hero card (`RoundedCornerShape(28.dp)`, `20dp` padding) with an off-corner scroll-parallax `Clover8` watermark.
- Left column: `ExpressiveEyebrow("EFFECTIVE MONTHLY")`, `46sp` split-scale `ExpressiveMoneyText` total, `"Down from $X/mo · -32%"` reconciliation subtitle, and an active/paused summary pill.
- Right side: Touch-reactive tilted (`-6°`) `90dp` `ExpressiveCookie12Shape` scalloped savings sticker (`SAVE $X/yr`) in `boldPrimary()` tonal fill that navigates directly to the Savings tab.
- **Scroll-Collapsible Efficiency Capsule (`CompactEfficiencyCapsule`)**: As the Dashboard scrolls past the Hero card, a pinned stadium capsule springs in beneath the status bar (`ExpressiveSpatialFast`) pairing tabular effective monthly spend, live annual savings pill, and a morphing Settings cookie trigger over a 6-stop `BlendMode.DstIn` `Modifier.progressiveEdgeFade(...)`.

### 2. Dual-Layer "Cycle Vessel" Bento Cards (`SubscriptionGridItem.kt` & `NextActionBanner.kt`)
- **Layout**: 2-column responsive bento grid (`chunked(2)` in `LazyColumn`, equal row height via `IntrinsicSize.Min`, `12dp` spacing) wrapped in `ExpressiveRemovalCollapse` (two-beat implode + neighbor collision rebound) and `ExpressiveStaggeredArrival`.
- **Styling**: `RoundedCornerShape(24.dp)` in `surfaceContainer` with an ambient 70/30 horizontal tonal vessel fill (and 45° diagonal hatch during blackout rest) plus subtle `1dp` `outlineVariant` hairline strokes in Light mode.
- **Content**: Top row pairs a `46dp` borderless lifecycle-morphing `ExpressiveServiceBadge` (driven by `androidx.graphics:graphics-shapes` `RoundedPolygon` + cubic-Bézier `Morph` across `Incomplete`, `Active`, `UrgentDue`, `Coasting`, `BlackoutRest`, and `UnbankedReady` states) with curated `ExpressivePalettes.initialsForService()` monograms in `DisplayFontFamily` and a full-pill status badge (or 1-tap `+¥X · Bank` scalloped stamp). Bottom block displays the full readable service name in `DisplayFontFamily` (up to 2 lines), tabular price (`tnum`) with cadence, and a slim cycle progress track.
- **Dual-Layer Curtain `NextActionBanner`**: Renders resting and filled text passes clipped synchronously at the live cycle progress seam (`clipRect`) for guaranteed high contrast across the boundary.

### 3. Interactive Detail Bottom Sheet: Cycle Time-Machine & 1+(2×2) Command Deck (`SubscriptionDetailBottomSheet.kt`)
- `ModalBottomSheet` (`32dp` top corners, `surfaceContainerLow`) obeying strict 70/30 Neutral-to-Emerald restraint:
  - **Monumental Header**: `54dp` lifecycle-morphing badge, `ExpressiveMoneyText` price header, right-aligned monumental tabular day countdown (`40sp`), and hairline neutral billing-date prompt (when unconfirmed).
  - **Interactive "Cycle Time-Machine" Wavy Scrubber**: Dragging or tapping along `WavyProgressIndicator` flattens wave amplitude, thickens the stroke, emits per-day and boundary haptics, and live-previews future rotation dates and phase states (`Active` / `Cancel window` / `Blackout break`) without mutating persisted data.
  - **50/50 Asymmetric `1+(2×2)` Bento Command Deck**: Left hero superellipse tile carries the single focal Botanical Emerald billing CTA; right `2×2` tactile grid houses the anticipatory press-preview (`Circle ↔ Gem`) Auto-Renew toggle, `+30d` break skip (when eligible), Edit, and inline-expanding Delete confirmation (triggering two-beat collapse + 3D stacked Undo snack).

### 4. Pixel-Locked Split Floating Pill Toolbar, Scalloped Controls & Kinetic Surfaces
- **Split Floating Pill Toolbar (`SubTrackNavHost.kt`)**: Crisp neutral floating capsule (`RoundedCornerShape(999.dp)`, bounded width) where the active tab fills available pill space (`weight(1f)`) with **Icon + Label** inside a `primaryContainer` pill while inactive tabs hold fixed `52dp` icon circles, paired side-by-side with a **detached `60dp` anticipatory `Cookie12 ↔ Sunny` morphing `+` button** anchored pixel-stably across all 3 tabs.
- **Role-Split Spring Physics (`Motion.kt`)**: `ExpressiveMotion` separates bouncy spatial choreography (`ExpressiveSpatialFast/Default/Slow`), disciplined structural moves (`StandardSpatialFast/Default`), and critically damped color/alpha effects (`EffectsFast/Default` + `clampEffectProgress`), with universal `rememberReducedMotionEnabled()` / `orSnapIfReduced()` support.
- **3D Stacked Floating Snacks (`ExpressiveSnackHost.kt`)**: Depth-dealt stadium pills with rotated `ArchLeft`/`ArchRight` badge chips, hold-to-pause timers, swipe-down dismiss, and duplicate impulse wobble.
- **Connected Section Stacks (`ExpressiveSectionStack` / `ExpressiveSectionRow`)**: `28dp` outer caps, `4dp` inner seams, `2dp` gaps, and `14dp` press-bloom seams used across Settings (`CalendarAwarenessComponents.kt`), Billing Destination Picker (`CancellationOptionsDialog.kt`), Savings Breakdown (`AnalyticsScreen.kt`), and Onboarding Welcome rows.
- **Inline Check-In Deck & Kinetic Planner (`CycleCheckInDeck.kt`, `PlannerScreen.kt`, `OnboardingScreen.kt`)**: Spring-dealt top-of-feed check-in cards with `ExpressiveStepDots` + `Cookie9` celebration, morphing `Cookie9 ↔ Sunny` paired-swap baton visualizer, 4×2 interactive `BotanicalVaultMosaic` welcome grid, fanned selection badge stack (`FannedBadgeStack`), ambient off-corner `ShapeBloomBackdrop`, and a scalloped `Cookie12` home-screen widget (`SubCyclerWidgetProvider.kt`).

### 5. Touch- & Interaction-Reactive Animated Icons (`AnimatedExpressiveIcons.kt`)
- **Primitives**: `ReactiveIcon`, `ExpressiveReactiveIconButton`, and `ReactiveMotionType`.
- **Dual-Stage Response**: Press-hold squash (`0.84f`) + micro-tilt, followed by bespoke GPU-accelerated `Animatable` spring choreographies (`GEAR_SPIN`, `PLUS_TWIST`, `CYCLE_SPIN`, `SWAP_FLIP`, `BELL_RING`, `NUDGE_LEFT`/`NUDGE_RIGHT`, `PIGGY_BOUNCE`, `POP_BOUNCE`).

### 6. Interactive Cycle Strategy Studio & Multi-Step Add/Edit Form (`InteractiveCycleStrategyPicker.kt`, `AddEditSubscriptionDialog.kt`)
- **Multi-Step Sheet**: Split into 3 compact, breathable steps (`Basics`, `Cycle`, `Notes`) inside a whisper-quiet neutral capsule switcher with spring-stretching `ExpressiveStepDots`, **automatic real-time saving** (zero Save buttons, race-free insert-then-update), and swipe-down dismissal blocked (`confirmValueChange = { it != SheetValue.Hidden }`) so users close deliberately via `×`, `Done`, or Back without losing work. Unfinished entries save cleanly in a progressive **Incomplete** state (`Setup · 1/4`, `Needs price`, `Needs date`, `Needs cycle`) without triggering premature alerts or savings estimates, and jump directly to their first missing step via **`Finish setup`**.
- **Interactive Cycle Strategy Studio**: Single neutral `surfaceContainer` card pairing 4 mode pills (`Always`, `Smart`, `Swap`, `Custom`), a plain-English bills/year & live savings badge, an animated spring-proportional 90-day rhythm ribbon (`Active` vs `Break (not billed)`), a tactile `Break between bills` slider (`0d–45d`) with haptic ticks and clickable scale marks, and a collapsible `Active window` stepper under `Custom`.
- **Single App-Wide Currency Model**: Chosen upfront on the first-launch Welcome screen (and adjustable during onboarding), with **no per-subscription currency selector** in Add/Edit forms. Changing **App currency** later in Settings shows an explicit offline manual-conversion warning dialog before unifying existing subscriptions to the new currency without altering numeric amounts.

---

## 5. Mandatory Rules for AI Agents

1. ❌ **NEVER hardcode dark-only hex backgrounds or neon wireframe borders** (e.g. `#000000`, `#0D0F14`, `#38BDF8` outlines). Always use `MaterialTheme.colorScheme` surface containers and `ExpressivePalettes` composable helpers so Light and Dark modes stay seamless.
2. ❌ **NEVER enable Android dynamic wallpaper colors** (`dynamicLightColorScheme` / `dynamicDarkColorScheme`). SubCycler maintains its own distinct brand identity.
3. ❌ **NEVER bundle third-party trademarked brand logos**. Use trademark-safe organic `ExpressiveServiceBadge` shapes and shared `ExpressivePalettes.initialsForService()` monograms.
4. ❌ **NEVER add emojis to status badges or headers** (e.g. no `🟢`, `⏸️`, `⚡`, `🔔`). Use reactive vector icons and typography.
5. ✅ **ALWAYS preserve zero `INTERNET` and zero `USE_EXACT_ALARM` permissions**, offline-only regional price estimation without fabricated currency conversion, reduced-motion (`ANIMATOR_DURATION_SCALE`) snapping via `ExpressiveMotion`, and Room migration safety.
6. ✅ **ALWAYS paint `RoundedPolygonShape` / `MorphPolygonShape` via `Modifier.expressivePolygonSurface(...)` (NEVER `Modifier.clip(shape)` or `Surface(shape = ...)` on `Outline.Generic`)**: SubCycler targets `minSdk = 26` (Android 8.0). On API 26–28 legacy HWUI GL pipelines, clipping a `RenderNode` to a non-convex/cubic-Bézier `Outline.Generic` drops the entire layer (0 pixels rendered). `Modifier.expressivePolygonSurface(...)` paints fills and borders directly via `drawOutline` (`Canvas.drawPath`), guaranteeing identical Material 3 Expressive silhouettes across API 26–36+. Similarly, `Modifier.progressiveEdgeFade(...)` automatically uses a zero-FBO gradient scrim below API 29 (`Build.VERSION_CODES.Q`) to keep scrolling smooth and prevent nested `LazyRow` clipping artifacts on older GPUs.

---

## 6. Design Inspiration & Expressive v4.0 Specification

SubCycler tracks open-source Material 3 Expressive craft and maintains its architectural design notes inside [`docs/`](docs):

- **[`docs/BUNPOD_DESIGN_INSPIRATION.md`](docs/BUNPOD_DESIGN_INSPIRATION.md)**: Architectural and visual analysis of [`kamranbekirovyz/bunpod`](https://github.com/kamranbekirovyz/bunpod)—covering `RoundedPolygon` morphing, anticipatory press-preview geometry, dual-layer vessel cards, role-split spring tokens, stacked 3D stadium snacks, and how each pattern adapts to SubCycler's 70/30 Botanical Emerald & 100% offline constraints.
- **[`docs/MATERIAL_EXPRESSIVE_UPGRADES_PROPOSAL.md`](docs/MATERIAL_EXPRESSIVE_UPGRADES_PROPOSAL.md)**: Implemented & verified **v4.0 ("Botanical Kinetic Architecture II")** specification—documenting all 12 delivered upgrades including `androidx.graphics:graphics-shapes` lifecycle morphing, scroll-collapsible Efficiency HUD, interactive "Cycle Time-Machine" wavy scrubbing, the 50/50 asymmetric Detail Command Deck, positional connected card stacks, 3D stacked snacks, inline Check-In Deck, Vault Mosaic welcome, and scalloped home-screen widget.
