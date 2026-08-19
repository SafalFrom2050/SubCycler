# SubCycler Design System: OLED Obsidian Precision

> **Document Version**: 1.0.0  
> **Target Theme**: Pure Pitch Black OLED (`#000000`) with Material Sky Blue & Cyber Indigo Accents  
> **Intended Audience**: Human Engineers & Future AI Coding Agents

---

## 1. Design Philosophy & Aesthetic Identity

SubCycler uses the **OLED Obsidian Precision** design system. It is designed for maximum visual clarity, zero battery drain on OLED screens, and a minimalist luxury feel.

### Core Principles:
1. **True OLED Pitch Black Canvas (`#000000`)**: The root scaffold background is always pure `#000000` (0-watt pixel power draw, infinite contrast).
2. **Elevation via Luminance & Hairline Outlines**: Shadows are invisible on pitch black. Elevation is created through subtle surface stepping (`#000000` -> `#0A0A0A` -> `#0D0D0D` -> `#161616`) combined with whisper-thin **1dp hairline borders** (`Color.White.copy(alpha = 0.08f)` or `Color(0xFF1C1C1E)`).
3. **Zero Diffuse Glows / Zero Heavy Gradients**: Reject neon blurs, halo glows, or saturated card fills. Keep all vector geometry flat, crisp, and matte.
4. **Strict 3-Tier Accent Color Hierarchy**: Accent colors must be used sparingly to avoid chromatic noise.

---

## 2. Color Palette & Token System

### 🌌 Canvas & Surface Hierarchy
| Token | Hex / Value | Purpose |
| :--- | :--- | :--- |
| `OledBlack` | `#000000` | Root background for all screens and scaffolds |
| `DarkSurface` | `#0A0A0A` | Top navigation bars, Bottom navigation bar, Explainer cards |
| `DarkSurfaceVariant` / `DarkCard` | `#0D0D0D` | App Drawer Grid tiles, interactive cards |
| `DarkSurfaceElevated` | `#121212` / `#161616` | Dialog surfaces, Modal Bottom Sheets, Monogram icon boxes |
| `DarkBorder` | `#1C1C1E` | 1dp hairline border for cards, dialogs, and dividers |
| `DarkBorderSubtle` | `Color.White.copy(alpha = 0.08f)` | Hairline stroke for subtle component framing |

---

### 🎨 Accent Color Hierarchy

```
Primary Accent (Sky Blue #38BDF8)
   └── Active cycles, ROI badges, Active progress bars, Selected tabs/chips, FAB
        │
        ▼
Secondary Accent (Cyber Indigo #6366F1)
   └── Blackout / Paused cycles, Paused progress bars, Secondary timeline blocks
        │
        ▼
Semantic Alert (Crimson Red #EF4444)
   └── Urgent cancellation deadlines (due within 24h), Delete buttons
```

| Token | Hex / Value | Usage Rules |
| :--- | :--- | :--- |
| **`PrimarySky`** | `#38BDF8` | **Primary Brand Accent**: Savings ROI pills, active status pills, active progress bars, selected navigation tab pill, selected filter chip, circular FAB. |
| **`PrimarySkyDark`** | `#0EA5E9` | Pressed state / gradient endpoint for Sky Blue elements. |
| **`PrimarySkyContainer`**| `PrimarySky.copy(alpha = 0.15f)` | Container tint for active pills, selected chips, and badges. |
| **`CyberIndigo`** | `#6366F1` | **Secondary Accent (Sparse)**: 15-day blackout / paused status pills and partner rotation indicators. |
| **`CyberIndigoLight`** | `#818CF8` | Blackout badge text on dark surfaces. |
| **`AlertCrimson`** / `ErrorRose` | `#EF4444` | **Semantic Alert**: Urgent cancellation alerts (`ActionUrgency.ACTION_DUE`), destructive actions. |
| **`WarningAmber`** | `#F59E0B` | **Warning**: Approaching deadlines, non-critical action notices. |

---

### ✍️ Typography Colors
| Token | Hex / Value | Purpose |
| :--- | :--- | :--- |
| `TextPrimaryDark` | `#EDEDED` / `#FFFFFF` | Primary headers, hero metric numbers, card titles |
| `TextSecondaryDark` | `#8E8E93` / `#A1A1AA` | Subtitles, prices, strategy tags, inactive chip text |
| `TextMutedDark` | `#71717A` / `#52525B` | Section labels, captions, metadata |

---

## 3. Core Component Guidelines

### 1. Financial Efficiency Bar HUD (`FinancialEfficiencyHud.kt`)
- **Container**: **Borderless**. Sits directly on the root `#000000` canvas (never wrap in a bordered card).
- **Hero Annual Number**: Stark white `34sp` / `36sp` font (`FontWeight.Black`, letter-spacing `-0.5sp`).
- **ROI Pill**: Compact `#121212` container with `1dp PrimarySky.copy(alpha = 0.35f)` border and `PrimarySky` bold text (`-26.1%`).
- **Efficiency Track**: `4dp` height with `2dp` rounded corners.
  - Active spend segment: `PrimarySky`.
  - Retained savings segment: `Color(0xFF27272A)` (muted zinc).
- **Telemetry Row**: `12sp` bold micro-metrics (`$Active/mo`, `$Saved/mo` in Sky Blue, `Active · Paused` counts).

---

### 2. App Drawer Grid Tile (`SubscriptionGridItem.kt`)
- **Layout**: 3-column responsive grid (`GridCells.Fixed(3)` or chunked rows of 3 in `LazyColumn`).
- **Card Styling**: `RoundedCornerShape(14.dp)`, `Color(0xFF0D0D0D)` container, `1dp` subtle outline border.
- **Monogram Icon**: Centered `40x40dp` box (`RoundedCornerShape(10.dp)`), `Color(0xFF181818)` background, `1dp` outline, stark white 2-letter monogram. **Do not use random saturated brand colors**.
- **Floating Alert Dot `(!)`**: If `action.urgency == ACTION_DUE`, float a `16dp` Crimson circular badge with `Icons.Default.PriorityHigh` in the top-right corner.
- **Service Name & Cost**: 1-line title (`13sp`, `FontWeight.Bold`, ellipsis) + Price (`11sp`, muted zinc).
- **Micro Status Pill**:
  - Active: `PrimarySky.copy(alpha = 0.10f)` background, `1dp PrimarySky.copy(alpha = 0.25f)` border, `PrimarySky` bold text (`1d left`).
  - Paused: `Color(0xFF161616)` background, subtle outline border, muted zinc text (`Paused · 10d`).
- **Hairline Progress**: `2dp` height progress bar (Active = `PrimarySky`, Paused = `#3F3F46`).

---

### 3. Interactive Detail Bottom Sheet (`SubscriptionDetailBottomSheet.kt`)
- **Container**: `ModalBottomSheet` with `containerColor = Color(0xFF0F0F0F)`.
- **Drag Handle**: `#333333`.
- **Content**:
  - Header: Monogram (`46dp`) + Full Service Name (`18sp`, Bold) + Category + Cost.
  - Status pill + `3dp` progress indicator.
  - Next Action callout (if any action pending).
  - Auto-renew cancellation toggle checkbox with `PrimarySky` checkmark.
  - Optimized annual savings summary.
  - Action buttons: Outlined "Edit" and Outlined "Delete" (in `ErrorRose`).

---

### 4. Navigation & Floating Action Button
- **Bottom Navigation Bar (`SubTrackNavHost.kt`)**:
  - Full-width dark obsidian (`#0A0A0A`) with a `1dp` top hairline divider line.
  - Selected tab: `PrimarySky` icon and text with `PrimarySky.copy(alpha = 0.15f)` indicator pill.
  - Inactive tabs: Muted zinc (`#71717A`).
  - Exactly 4 Tabs: `Subscriptions`, `45d Planner`, `Paired Swap`, `Savings & ROI`.
- **Floating Action Button (FAB)**:
  - `CircleShape`, container = `PrimarySky` (`#38BDF8`), content = `OledBlack` (`#000000`), `Icons.Default.Add` (`24dp`).
  - Nested Scaffold insets: Parent `NavHost` must use `Modifier.padding(innerPadding)` so child screens place their FABs directly floating above the bottom navigation bar.

---

### 5. Filter Chips
- **Container**: `RoundedCornerShape(8.dp)`, `1dp` border.
- **Active State**:
  - `containerColor = PrimarySky.copy(alpha = 0.16f)`
  - `selectedLabelColor = PrimarySky`
  - `borderColor = PrimarySky.copy(alpha = 0.85f)`
  - `fontWeight = FontWeight.Bold`
- **Inactive State**:
  - `containerColor = Color(0xFF0D0D0D)`
  - `labelColor = Color(0xFF8E8E93)`
  - `borderColor = Color(0xFF222222)`
  - `fontWeight = FontWeight.Medium`

---

## 4. Mandatory Rules for AI Agents

1. ❌ **NEVER add diffuse glow/shadow effects** (e.g. `Modifier.blur()`, `Modifier.shadow()` with colored glows).
2. ❌ **NEVER put multicolored brand backgrounds on avatars** (e.g. green for Spotify, blue for Disney, pink for Netflix). Always use monochromatic `#181818` with stark white initials.
3. ❌ **NEVER wrap the top HUD in a standard card container**. The top HUD must remain borderless on the pitch-black OLED background.
4. ❌ **NEVER add emojis to status badges or headers** (e.g. no `🟢`, `⏸️`, `⚡`, `🔔`). Use vector icons and typography.
5. ✅ **ALWAYS maintain the 3-tier accent hierarchy**: `PrimarySky` for active, `CyberIndigo` for paused, and `AlertCrimson` only for urgent deadlines.
6. ✅ **ALWAYS keep borders subtle**: Standard border is `1dp` with `alpha = 0.25f` to `0.35f`. Do not make thick or high-contrast white borders.
