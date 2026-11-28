# Design Inspiration Study: BunPod (`kamranbekirovyz/bunpod`)

> **Reference Repository**: [https://github.com/kamranbekirovyz/bunpod](https://github.com/kamranbekirovyz/bunpod)  
> **Author & License**: Kamran Bekirov · Apache License 2.0  
> **Relevance to SubCycler**: Local-first Material 3 Expressive mobile craft, topological polygon morphing, spring physics discipline, vessel progress cards, and tactile bottom-weighted action surfaces.

---

## 1. Why BunPod Resonates with SubCycler

[**BunPod**](https://github.com/kamranbekirovyz/bunpod) is an open-source, local-first podcast player built with a custom ground-up implementation of **Material 3 Expressive** (including six standalone UI/physics packages under `packages/`). Despite operating in a different domain (podcasts vs. recurring subscriptions), BunPod shares deep product and visual DNA with **SubCycler**:

1. **Local-First Ownership & Zero Sign-Up**: BunPod keeps the user's library locally in SQLite (`Drift`) with portable OPML import/export and zero account walls—mirroring SubCycler's 100% offline Room + JSON/CSV backup model.
2. **Time-Remaining & Cadence Mental Model**: Every podcast episode in BunPod revolves around *active progress, remaining time (`52m left`), paused vs. active states, and completion stamps*—the exact temporal primitives SubCycler uses for *30-day active windows, 15-day blackout breaks, and banked cycle savings*.
3. ** Craft-First Material 3 Expressive Primitives**: Rather than treating Material 3 Expressive as "bigger corner radii," BunPod treats **shape, spring velocity, clip geometry, and haptics** as functional state indicators.

---

## 2. Key Material 3 Expressive Patterns Extracted from BunPod

### 2.1 Full 35-Shape `MaterialShapes` Geometry & Topological Morphing
- **In BunPod (`packages/expressive_shapes`, `ShapeValues`, `FeedSyncLogo`)**:
  - Ports AndroidX's cubic-Bézier `RoundedPolygon` and `Morph` engine across all 35 Material 3 Expressive shapes (`bun`, `sunny`, `cookie4/6/7/9/12Sided`, `clover4/8Leaf`, `gem`, `pentagon`, `arch`, `softBurst`, `flower`, `puffy`, `pill`, etc.).
  - Badges and controls do not cross-fade between shapes; they **topologically interpolate control points (`lerpTo`)** driven by spring simulations (e.g., `clover4Leaf` ↔ `cookie7Sided` on covers, `cookie7Sided` ↔ `sunny` on hero play buttons, and `bun` → `cookie4` → `clover4` → `flower` with `π` half-turn hops during sync).
- **Takeaway for SubCycler**:
  - Upgrade `:core:ui` from fixed polar-harmonic `lineTo` approximations (`ScallopedCookieShape`) to official **`androidx.graphics:graphics-shapes`** (`RoundedPolygon` + `Morph`), enabling hardware-smooth morphs across subscription lifecycle phases (*Active* → *Coasting/Cancelled* → *Paused Break* → *Action Due*).

### 2.2 Anticipatory Press-Preview Shape Morphing
- **In BunPod (`FavButton`, `PlayButton`, `SeekButton`)**:
  - **Shape previews intent while pressed; color confirms commit on release.**
  - In `FavButton`, pressing down (`onTapDown`) immediately springs the button's outline from `Circle` toward `Gem` (`effective = _down ? !fav : fav`), while the `tertiary` color fill waits until release (`onTap`).
  - In `PlayButton` and `SeekButton`, resting shapes (`0.42r` superellipse or `Circle`) tighten crisp corners (`16dp` / `12dp`) under finger pressure before blooming into their active silhouette.
- **Takeaway for SubCycler**:
  - Apply **split press-preview vs. commit-color springs** to SubCycler's `"Mark auto-renew as cancelled"` control, primary billing CTAs, and interactive Bento badges so every touch feels physically responsive before finger lift.

### 2.3 Dual-Layer Vessel Progress Cards (`_FillClipper`)
- **In BunPod (`FeedEpisodeCard`)**:
  - Instead of relegating playback progress to a thin bottom line, an in-progress episode card turns its **entire card body into a horizontal split-fill vessel** (`ClipRect` from `0` to `width * progress`).
  - Two synchronized content layers (`onSurface` on the base layer, `onPrimary` inside the clipped foreground layer) ensure typography, monograms, and tabular time remaining (`-52m`) invert cleanly right across the moving fill seam.
  - Completed items replace the trailing tabular duration with a `30dp` `cookie7Sided` checkmark stamp.
- **Takeaway for SubCycler**:
  - Introduce **Tonal Vessel Cards** for Urgent Renewal/Cancel banners, Planner timeline rows, and optional Dashboard Bento progress—turning days elapsed in a 30-day active window or 15-day break into an unmistakable spatial fill, culminating in a scalloped `"Bank savings"` stamp when a break finishes.

### 2.4 Scroll-Linked Hero-to-Pill Continuity & Off-Corner Shape Watermarks
- **In BunPod (`FeedNowPlayingCard` + `FeedDockedPlayer`)**:
  - The `220dp` Hero card features an off-corner `180dp` `MaterialShapes.sunny` watermark silhouette (`top: -44, right: -44`, `12%` opacity) that gives tonal depth without competing with text.
  - As the feed scrolls (`collapse: 0 → 1`), the Hero card interpolates its corner radius (`36dp → 32dp`) and morphs into a compact `64dp` docked floating capsule.
- **Takeaway for SubCycler**:
  - Collapse `FinancialEfficiencyHud` on scroll into a sleek **Pinned Efficiency Capsule** (`¥1,650/mo · -19%` + mini scalloped `Save ¥4.5k/yr` chip) so portfolio totals remain glanceable while browsing large subscription stacks.

### 2.5 Monumental Tabular Scrubbing & Asymmetric Bento Control Matrix
- **In BunPod (`PlayerPage`, `PlayerProgress`, `PlayerControls`)**:
  - Pairs airy editorial top headers (clickable uppercase eyebrow + bold display headline) with a **bottom-weighted tactile control deck**:
    1. **Monumental Tabular Readout + Reactive Wave**: Oversized `56sp` right-aligned tabular figures (`-52m`) above a scrubbable `WavyLinearProgressIndicator` whose wave **flattens (`amplitude → 0`) and thickens (`4dp → 6dp`)** while dragged, firing 20-step granular selection haptics and rigid end-stop haptics.
    2. **50 / 50 Asymmetric Bento Action Grid**: Left half is one giant square `PlayButton`; right half is a tight `2×2` sub-grid (`6dp` inner seams) of shape-morphing secondary actions (`-10s`, `+10s`, `Download`, `Favorite`).
- **Takeaway for SubCycler**:
  - Reimagine `SubscriptionDetailBottomSheet` around a **Scrubbable Cycle Time-Machine Wave** (drag across the 45-day cycle to inspect exact future cancel/re-subscribe dates and per-break savings) above an **Asymmetric Bento Command Deck** (50% Hero Billing/Lifecycle Square + 50% `2×2` Auto-Renew, Skip Break, Edit, and Delete tiles).

### 2.6 Semantic Spring Discipline & Clamped/Unclamped Channel Splitting
- **In BunPod (`FeedChannelTabs`, `ExpressiveSheetRoute`, `SnackView`)**:
  - **Role-Based Spring Selection**: Navigation chrome (`FeedChannelTabs`) uses calm *Standard Spatial Fast* springs (no overshoot) and nudges selected chips only into the comfortable middle `25%..75%` viewport band rather than jarring dead-center jumps. Focal spatial transitions (sheets, arrivals, badges) use bouncy *Expressive Spatial* springs.
  - **Clamped Effects vs. Unclamped Spatial Motion**: Modal barriers and fade animations read a `_ClampedAnimation` (`0.0..1.0`) so scrims never flicker during spring overshoot, while sheet translation (`_overdragResistance = 100` + fling velocity handoff) and list slot height read the **raw unclamped spring**.
- **Takeaway for SubCycler**:
  - Formalize `ExpressiveMotion` tokens in `:core:ui` separating `ExpressiveSpatial` (bouncy), `StandardSpatial` (calm chrome), and `Effects` (clamped color/alpha).

### 2.7 Stacked 3D Floating `SnackStack` with Concentric `Arch` Icon Chip
- **In BunPod (`packages/expressive_snack`)**:
  - Replaces flat bottom bars with bottom-anchored `inverseSurface` **Stadium pills** dealt into a 3D card stack (`12dp` vertical peek, `5%` scale reduction, and `15%` progressive shadow tint per depth step).
  - Features an LTR/RTL-aware rotated `MaterialShapes.arch` icon chip (`32dp`) whose curved dome nests concentrically inside the stadium cap, hold-to-pause countdowns, swipe-down dismissal, and a **real spring-velocity horizontal wobble (`_shakeVelocity = 600`)** on duplicate triggers.
- **Takeaway for SubCycler**:
  - Upgrade SubCycler's undo-delete and offline backup feedback to a floating **`ExpressiveSnackStack`** hovering cleanly above the Split Floating Pill Toolbar.

### 2.8 Two-Beat Physical List Choreography & Interactive Resolution Decks
- **In BunPod (`RemovalCollapse`, `FeedArrival`, `OpmlSuggestionDeck`, `ChannelWall`, `MenuSection`)**:
  - **Two-Beat Removal (`RemovalCollapse`)**: Beat 1 implodes the removed row in place (`scale` + `opacity` → `0`); Beat 2 snaps the empty slot shut on an expressive spatial spring whose unclamped rebound makes neighboring cards visibly collide and settle.
  - **Positional Section Stacks (`MenuSection`)**: Groups settings/menu items with `28dp` outer radii, `4dp` inner radii, and `2dp` gaps instead of repetitive standalone cards.
  - **Interactive Suggestion Deck (`OpmlSuggestionDeck`)**: Resolves multi-item user decisions one card at a time with spring-stretching stadium progress dots (`8dp → 28dp`) and a `cookie9Sided` `"All sorted"` completion state.
  - **Gapless-to-Expressive Mosaic (`ChannelWall`)**: Starts as an edge-to-edge grid of squares, pauses `300ms`, then morphs each cell into a distinct `MaterialShapes` polygon—with a tap-to-morph Easter egg.

---

## 3. SubCycler Adaptation Matrix (What We Adopt, Adapt & Preserve)

SubCycler never copies blindly; every inspiration from BunPod is filtered through SubCycler's mandatory architectural and brand constraints (`DESIGN.md` & `ARCHITECTURE.md`):

| BunPod Pattern | SubCycler Strategy | Rationale & Guardrails |
| :--- | :--- | :--- |
| **35 `MaterialShapes` + `Morph` (`expressive_shapes`)** | **Adopt natively** via Jetpack `androidx.graphics:graphics-shapes` | Pure local math library (~35 KB, zero network/permissions); unlocks true cubic-Bézier morphing in Compose. |
| **Per-Show Cover Art Dynamic Color Extraction** | **Adapt to 70/30 Brand Palette** (Reject dynamic image extraction) | SubCycler strictly preserves its **70% Architectural Neutral / 30% Botanical Emerald** identity (`WarmSand`, `CoolSlate`, `FreshMint`, `CoolJade`, `BoldPrimary`) and zero `INTERNET` permission. |
| **Podcast Cover Artwork (`ChannelWall`, `_Cover`)** | **Adapt to Organic Monogram Mosaic** | SubCycler never bundles or fetches third-party trademarked brand logos; shapes frame `Bricolage Grotesque` monograms (`NF`, `D+`, `AI`, `SP`). |
| **Google Fonts (`Google Sans` + `Unbounded`)** | **Preserve SubCycler Offline Variable Fonts** | Keep bundled SIL OFL `Bricolage Grotesque` + `Plus Jakarta Sans` variable fonts in `:core:ui/res/font` (zero network fetches), adopting BunPod's monumental tabular numeral hierarchy (`56sp`, tight tracking). |
| **Full-Bleed Primary-Colored Episode Vessel (`FeedEpisodeCard`)** | **Adapt with 70/30 Tonal Restraint** | Full-saturation fills on *every* 2-column Bento card would overpower SubCycler's calm neutral canvas; use whisper-soft tonal/hatched vessels on Bento cards and full dual-layer contrast inversion on focal Urgent Banners & Planner timelines. |
| **Asymmetric 50/50 Player Controls & Wavy Scrubber** | **Adopt & Reimagine** as `SubscriptionCommandDeck` + Cycle Time-Machine | Transforms `SubscriptionDetailBottomSheet` from a vertical button list into a tactile M3 Expressive instrument. |
| **Stacked `expressive_snack`, `MenuSection` & `RemovalCollapse`** | **Adopt natively** in `:core:ui` | Directly elevates Delete/Undo, Backup notices, Settings grouping, and Savings Breakdown polish while respecting `ANIMATOR_DURATION_SCALE` reduced motion. |

---

## 4. Related Documentation

- **[`MATERIAL_EXPRESSIVE_UPGRADES_PROPOSAL.md`](MATERIAL_EXPRESSIVE_UPGRADES_PROPOSAL.md)**: Detailed v4.0 creative upgrade specification, wireframes, motion token tables, and phased Jetpack Compose roadmap.
- **[`../DESIGN.md`](../DESIGN.md)**: Current v3.5.0 Botanical Emerald Material 3 Expressive baseline and mandatory visual rules.
