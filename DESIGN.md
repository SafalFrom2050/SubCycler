# SubCycler Design System: Botanical Emerald Monochromatic Material 3 Expressive

> **Document Version**: 3.5.0  
> **Target Theme**: True Edge-to-Edge Adaptive Light & Dark 70% Neutral / 30% Botanical Emerald Material 3 Expressive ("Botanical Kinetic Architecture") with Continuous Shape Primitives, Connected Segmented Groups, Cycle Rhythm Tracks & Variable Editorial Typography  
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

### 1. Expressive Hero Bento Card (`FinancialEfficiencyHud.kt`)
- Masked silk-waves duotone hero card (`RoundedCornerShape(28.dp)`, `20dp` padding).
- Left column: `ExpressiveEyebrow("EFFECTIVE MONTHLY")`, `46sp` split-scale `ExpressiveMoneyText` total, `"Down from $X/mo · -32%"` reconciliation subtitle, and an active/paused summary pill.
- Right side: Touch-reactive tilted (`-6°`) `90dp` `ExpressiveCookie12Shape` scalloped savings sticker (`SAVE $X/yr`) in `boldPrimary()` tonal fill that navigates directly to the Savings tab.

### 2. Expressive 2-Column Bento Cards (`SubscriptionGridItem.kt`)
- **Layout**: 2-column responsive bento grid (`chunked(2)` in `LazyColumn`, equal row height via `IntrinsicSize.Min`, `12dp` spacing) with bouncy press-scale (`0.95f`).
- **Styling**: `RoundedCornerShape(24.dp)` in `surfaceContainer` with subtle `1dp` `outlineVariant` hairline strokes in Light mode (softly tinted `tertiaryContainer` when Paused or `errorContainer` when Action Due).
- **Content**: Top row pairs a `46dp` borderless organic `ExpressiveServiceBadge` (with curated `ExpressivePalettes.initialsForService()` monograms in `DisplayFontFamily`) and a full-pill status badge (`29d left`, `Paused · 12d`, `Yearly · 340d`). Bottom block displays the full readable service name in `DisplayFontFamily` (up to 2 lines), tabular price (`tnum`) with cadence (`$17.99/mo · 45d`), and a slim cycle progress bar.

### 3. Interactive Detail Bottom Sheet (`SubscriptionDetailBottomSheet.kt`)
- `ModalBottomSheet` (`32dp` top corners, `surfaceContainerLow`) obeying strict 70/30 Neutral-to-Emerald restraint and minimalism: `54dp` organic badge, `ExpressiveMoneyText` price header, hairline neutral billing-date prompt (when unconfirmed), one unified neutral `surfaceContainer` Status & Annual Savings card (quiet status-dot pill, animated emerald `WavyProgressIndicator`, inline renewal note, hairline divider, and `Estimated annual cost` with emerald `Saves $X/yr` text), contextual break-skip button only when a break is within 10 days, ONE focal Botanical Emerald billing CTA, clean manual cancel/resume toggle below it, and neutral `Edit` / neutral-with-red-ink `Delete` pills (with confirmation + Undo snackbar).

### 4. Pixel-Locked Split Floating Pill Toolbar & Scalloped Cookie Controls
- **Split Floating Pill Toolbar (`SubTrackNavHost.kt`)**: Crisp neutral floating capsule (`RoundedCornerShape(999.dp)`, bounded width) where the active tab fills available pill space (`weight(1f)`) with **Icon + Label** inside a `primaryContainer` pill while inactive tabs hold fixed `52dp` icon circles, paired side-by-side with a **detached `60dp` `ExpressiveCookie12Shape` `+` button** anchored pixel-stably across all 3 tabs.
- **Scalloped Settings (`DashboardScreen.kt`)**: `44dp` `ExpressiveCookie12Shape` Settings button (`minimumInteractiveComponentSize()`, `Role.Button`) opening the consolidated offline Settings sheet.

### 5. Touch- & Interaction-Reactive Animated Icons (`AnimatedExpressiveIcons.kt`)
- **Primitives**: `ReactiveIcon`, `ExpressiveReactiveIconButton`, and `ReactiveMotionType`.
- **Dual-Stage Response**: Press-hold squash (`0.84f`) + micro-tilt, followed by bespoke GPU-accelerated `Animatable` spring choreographies (`GEAR_SPIN`, `PLUS_TWIST`, `CYCLE_SPIN`, `SWAP_FLIP`, `BELL_RING`, `NUDGE_LEFT`/`NUDGE_RIGHT`, `PIGGY_BOUNCE`, `POP_BOUNCE`).

### 6. Interactive Cycle Strategy Studio & Multi-Step Add/Edit Form (`InteractiveCycleStrategyPicker.kt`, `AddEditSubscriptionDialog.kt`)
- **Multi-Step Sheet**: Split into 3 compact, breathable steps (`Basics`, `Cycle`, `Notes`) inside a whisper-quiet neutral capsule switcher with spring transitions, **automatic real-time saving** (zero Save buttons, race-free insert-then-update), and swipe-down dismissal blocked (`confirmValueChange = { it != SheetValue.Hidden }`) so users close deliberately via `×`, `Done`, or Back without losing work. Unfinished entries save cleanly in a progressive **Incomplete** state (`Setup · 1/4`, `Needs price`, `Needs date`, `Needs cycle`) without triggering premature alerts or savings estimates, and jump directly to their first missing step via **`Finish setup`**.
- **Interactive Cycle Strategy Studio**: Single neutral `surfaceContainer` card pairing 4 mode pills (`Always`, `Smart`, `Swap`, `Custom`), a plain-English bills/year & live savings badge, an animated spring-proportional 90-day rhythm ribbon (`Active` vs `Break (not billed)`), a tactile `Break between bills` slider (`0d–45d`) with haptic ticks and clickable scale marks, and a collapsible `Active window` stepper under `Custom`.
- **Single App-Wide Currency Model**: Chosen upfront on the first-launch Welcome screen (and adjustable during onboarding), with **no per-subscription currency selector** in Add/Edit forms. Changing **App currency** later in Settings shows an explicit offline manual-conversion warning dialog before unifying existing subscriptions to the new currency without altering numeric amounts.

---

## 5. Mandatory Rules for AI Agents

1. ❌ **NEVER hardcode dark-only hex backgrounds or neon wireframe borders** (e.g. `#000000`, `#0D0F14`, `#38BDF8` outlines). Always use `MaterialTheme.colorScheme` surface containers and `ExpressivePalettes` composable helpers so Light and Dark modes stay seamless.
2. ❌ **NEVER enable Android dynamic wallpaper colors** (`dynamicLightColorScheme` / `dynamicDarkColorScheme`). SubCycler maintains its own distinct brand identity.
3. ❌ **NEVER bundle third-party trademarked brand logos**. Use trademark-safe organic `ExpressiveServiceBadge` shapes and shared `ExpressivePalettes.initialsForService()` monograms.
4. ❌ **NEVER add emojis to status badges or headers** (e.g. no `🟢`, `⏸️`, `⚡`, `🔔`). Use reactive vector icons and typography.
5. ✅ **ALWAYS preserve zero `INTERNET` and zero `USE_EXACT_ALARM` permissions**, offline-only regional price estimation without fabricated currency conversion, and Room migration safety.
