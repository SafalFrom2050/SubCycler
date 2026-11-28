<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/readme-hero-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="docs/assets/readme-hero-light.svg">
  <img alt="SubCycler — Minimal, 100% offline 45-day subscription rotation for Android" src="docs/assets/readme-hero-light.svg" width="100%">
</picture>

<br>

**Binge your watchlist. Take a 15-day break. Cut recurring subscription bills by 33%–50% — with total local privacy and zero internet permission.**

<p>
  <a href="#the-45-day-cycle-strategy">Strategy</a> &nbsp;·&nbsp;
  <a href="#capabilities">Capabilities</a> &nbsp;·&nbsp;
  <a href="#home-screen-widgets">Widgets</a> &nbsp;·&nbsp;
  <a href="#expressive-alerts">Alerts</a> &nbsp;·&nbsp;
  <a href="#interface">Interface</a> &nbsp;·&nbsp;
  <a href="#design-system--documentation">Design System</a> &nbsp;·&nbsp;
  <a href="#quick-start">Quick Start</a>
</p>

</div>

---

## The 45-Day Cycle Strategy

Standard streaming and digital subscriptions lock you into 12 back-to-back auto-debits per year — even during weeks when you barely open them. SubCycler turns that passive billing loop into a calm, intentional rhythm.

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/cycle-rhythm-dark.svg">
    <source media="(prefers-color-scheme: light)" srcset="docs/assets/cycle-rhythm-light.svg">
    <img alt="90-day comparison of Standard Auto-Renew, SubCycler 45-Day Rotation, and Paired Swap" src="docs/assets/cycle-rhythm-light.svg" width="100%">
  </picture>
</p>

| Cadence | Cycle Rhythm | Billings / Year | Annual Savings |
| :--- | :--- | :---: | :---: |
| **Standard Auto-Renew** | 30d paid, continuous | 12 | 0% |
| **SubCycler 45-Day** | 30d active · 15d unbilled break | 8 | **33.3%** |
| **Paired Swap (30d / 30d)** | Two alternating services, one active at a time | 6 each | **50.0%** |

---

## Capabilities

### Privacy-First Local Core
- **Zero Internet & Zero Exact-Alarm Permissions** — SubCycler never declares `android.permission.INTERNET` or `USE_EXACT_ALARM`. No ads, no telemetry, no accounts, and no cloud sync. Everything stays in local Room (SQLite) and Preferences DataStore *(Android's system backup and device transfer settings can include the local database)*.
- **Single App-Wide Currency & Offline Regional Tiers** — Choose your currency once during onboarding with curated offline plan tiers (`USD`, `JPY`, `EUR`, `GBP`, `CAD`, `AUD`, `INR`, `KRW`). Changing currency later in Settings shows an explicit offline manual-conversion notice instead of fabricating exchange rates.
- **Optional Local Calendar Awareness** — Disabled by default; once enabled, reads selected on-device calendars locally to suggest timely subscription reviews around travel or busy stretches.

### Material 3 Expressive Craft
- **70/30 Neutral-to-Accent Restraint & Icon-Driven Theming** — Calm Alabaster (Light) and Obsidian-Graphite (Dark) surfaces keep the interface quiet, while **6 curated launcher icons** (`Botanical`, `Cinema`, `Ocean`, `Terracotta`, `Aurora`, `Obsidian`, plus Android 13+ monochrome themed icons) drive the app's accent color across the wordmark, savings seal, active controls, detached scalloped `+` button, notifications, and widgets.
- **Living Stack Mood & Dynamic 3-Petal Swirl** — Raw brand colors across Top 20 presets are harmonized into soft WCAG AAA pastel tonal palettes (zero third-party trademarked logos). With **Living stack mood** enabled, the Hero silk-waves backdrop, top-bar 3-petal swirl mark, and splash transition softly reflect your top 3 active subscriptions.
- **Gliding Liquid Dock & Fluid Navigation** — Always-visible tab labels inside a gliding pill indicator (`Subs`, `Planner`, `Savings`), directional horizontal tab transitions, full-height filter swiping (`All`, `Active`, `Paused`, `Rotating`), and a multi-level nested Settings sheet.
- **Offline Variable Typography** — Bundles SIL OFL `Bricolage Grotesque` (editorial display numerals and headlines) and `Plus Jakarta Sans` (body, labels, and wide-tracked eyebrows) directly in the APK.

### Rotation Studio, Planner & Offline Stack Sharing
- **Auto-Saving 3-Step Setup & Incomplete Tracking** — Split into **Basics**, **Cycle**, and **Notes** with zero Save buttons and swipe-down protection. Partially filled items save cleanly with progressive badges (`Needs price`, `Needs date`, `Needs cycle`) without triggering premature alerts.
- **Interactive Cycle Strategy Studio** — Switch between `Always`, `Smart` (45-day), `Swap`, and `Custom` schedules with a live 90-day active/break ribbon and tactile `0d–45d` break slider.
- **Planner, Paired Swap & Editorial Savings** — Inspect future billing/break windows on duration-proportional timelines, coordinate alternating 30d/30d services with a 60-day Baton-Pass visualizer, and track banked savings alongside `1Y → 3Y → 5Y` compounding projections.
- **Offline QR Stack Cards & Short Links** — Export a branded PNG **Stack Card** with embedded QR code, scan live in-app QRs, or share compact `subcycler://s/...` deep links and `.subcycler` backup files with a 1-tap privacy toggle for notes and savings history.

---

## Home-Screen Widgets

Two 100% offline expressive widgets sync live with your active **App Icon accent**, **Living Stack Mood**, and **Light/Dark Theme**:

- **SubCycler · Living Pulse** — Responsive across **`2×2`** (Compact Seal + mini rhythm bar), **`4×2`** (Hero + frosted Next-Up ribbon), and **`4×3`** (Hero + top 3 individually clickable chromatic Cycle Vessel rows).
- **SubCycler · Next Up Cookie** — Sculptural **`2×2`** floating silhouette (`10`-petal cookie for rotating cycles, squircle for continuous, `12`-petal sunny seal when a break is ready to bank) on a transparent canvas with a sweeping perimeter progress arc.

<div align="center">

| Living Pulse · 2×2 (Dark) | Next Up Cookie · 2×2 (Floating Silhouette) |
| :---: | :---: |
| <img src="docs/screenshots/widget_living_2x2.png" alt="Living Stack Pulse 2x2 widget in Dark Mode with dynamic swirl logo, effective monthly spend, mini rhythm bar, and tilted 12-petal savings seal" width="220"> | <img src="docs/screenshots/widget_next_up_cookie.png" alt="Next Up Cookie 2x2 sculptural scalloped countdown widget tinted in Disney+ slate-blue pastel with perimeter cycle progress arc" width="220"> |

| Living Pulse · 4×2 (Ocean Accent) | Living Pulse · 4×3 (Light · Cycle Vessel Deck) |
| :---: | :---: |
| <img src="docs/screenshots/widget_living_4x2_ocean.png" alt="Living Stack Pulse 4x2 wide widget synced to the Ocean icon accent with a frosted Next-Up cancellation ribbon" width="380"> | <img src="docs/screenshots/widget_living_4x3_light.png" alt="Living Stack Pulse 4x3 tall widget in Light Mode pairing the silk-waves hero card with 3 clickable chromatic service rows" width="380"> |

</div>

---

## Expressive Alerts

Local `AlarmManager` and `WorkManager` reminders carry SubCycler's visual system straight into the Android notification shade:

- **Organic Harmonized Monogram Badges** — Scalloped `LargeIcon` badges tinted per service and accented by your chosen launcher icon.
- **Expanded Cycle Rhythm Cards (`BigPictureStyle`)** — Displays the live 2-zone wavy active + `45°` hatched break track alongside a tilted scalloped savings seal.
- **1-Tap Inline Shade Actions** — Tap **`Already cancelled`**, **`Keep (+30d)`**, or **`Bank & Resume`** right from the shade to update Room, reschedule alarms, and refresh widgets without opening the app.
- **Quiet 2-Step Action Progress & Settings Preview** — Opening a provider billing link posts a silent, non-minimized progress notification (`Step 1 · In Progress` → `Step 2 · Tap Completed`) with next-launch recovery. Preview both alert styles safely anytime from **Settings → Notifications & Calendar → Preview expressive alerts**.

<div align="center">

| Expanded Cycle Rhythm Card | 2-Step Action Progress Banner |
| :---: | :---: |
| <img src="docs/screenshots/notification_rhythm_card.png" alt="Expanded notification Cycle Rhythm Card showing organic monogram badge, status pill, tilted 12-petal savings seal, and wavy active plus hatched break track" width="410"> | <img src="docs/screenshots/notification_progress_card.png" alt="2-step Subscribe and Unsubscribe notification progress banner guiding users from provider cancellation to tapping Completed" width="410"> |

</div>

---

## Interface

<div align="center">

| Home · Light | Home · Dark (Ocean + Living Mood) | Swirl-to-Mood Splash |
| :---: | :---: | :---: |
| <img src="docs/screenshots/dashboard.png" alt="SubCycler home dashboard in adaptive Light Mode with Botanical Emerald hero card and 2-column Bento cards" width="240"> | <img src="docs/screenshots/dashboard_dark.png" alt="SubCycler home dashboard in Dark Mode showing Ocean accent theming, Living Stack Mood hero wash, and Gliding Liquid Dock" width="240"> | <img src="docs/screenshots/splash_screen.png" alt="Swirl-to-mood launch splash screen animating SubCycler's 3-petal swirl mark over monochrome silk waves" width="240"> |

| Auto-Saving Cycle Studio | Cycle Timeline & Date Inspector | Paired Swap (30d / 30d) |
| :---: | :---: | :---: |
| <img src="docs/screenshots/cycle_strategy.png" alt="Auto-saving 3-step subscription form showing the Interactive Cycle Strategy Studio with 90-day rhythm preview and break slider" width="240"> | <img src="docs/screenshots/planner.png" alt="Planner Cycle Timeline showing Date Inspector and duration-proportional hatched break blocks" width="240"> | <img src="docs/screenshots/paired_swap.png" alt="Paired Swap planner showing the 60-day Baton-Pass alternating visualizer and yearly savings preview" width="240"> |

| Editorial Savings & 5-Year ROI | Appearance & Icon Studio | Expressive Notification Shade |
| :---: | :---: | :---: |
| <img src="docs/screenshots/savings.png" alt="Savings screen showing projected 5-year compounding growth curve, earned savings, and per-service breakdown" width="240"> | <img src="docs/screenshots/settings_appearance.png" alt="Nested Appearance and Icon settings page featuring Theme Mode selector, 6 curated launcher icon accents with Stack Fit recommendation, and Living Stack Mood toggle" width="240"> | <img src="docs/screenshots/notification_shade.png" alt="Live Android notification shade displaying the expanded Cycle Rhythm renewal card with inline actions and quiet 2-step unsubscribe progress alert" width="240"> |

</div>

---

## Design System & Documentation

SubCycler maintains architectural blueprints and design specifications alongside the codebase:

| Document | Scope |
| :--- | :--- |
| [`DESIGN.md`](DESIGN.md) | Botanical Emerald Material 3 Expressive v4.0 tokens, typography, shapes, and motion |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Clean Architecture boundaries, SOLID rules, and multi-module dependency graph |
| [`PLANS.md`](PLANS.md) | Master feature registry, status logs, and links into [`plans/features/`](plans/features) |
| [`plans/CORE.md`](plans/CORE.md) | Core 45-day rotation math, scheduling guarantees, and local persistence blueprint |
| [`docs/MATERIAL_EXPRESSIVE_UPGRADES_PROPOSAL.md`](docs/MATERIAL_EXPRESSIVE_UPGRADES_PROPOSAL.md) | Kinetic Architecture II specification: polygon morphing, wave scrubbing, and command decks |
| [`docs/BUNPOD_DESIGN_INSPIRATION.md`](docs/BUNPOD_DESIGN_INSPIRATION.md) | Material 3 Expressive craft study and offline adaptation guardrails |

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| **UI & Motion** | Kotlin 2.0 · Jetpack Compose · Material 3 · `androidx.graphics:graphics-shapes` |
| **Local Data** | Room (SQLite) · Preferences DataStore · Kotlin Coroutines & Flow |
| **Scheduling & Widgets** | `AlarmManager` · `WorkManager` · `BroadcastReceiver` · `AppWidgetProvider` + Canvas Art |
| **DI & Build** | Hilt · Gradle 8.7 · Android Gradle Plugin 8.5+ · Android 8.0+ (API 26–35) |

---

## Quick Start

Install **JDK 17+** (or JDK 21) and **Android SDK Platform 35** via [Android Studio](https://developer.android.com/studio) or the SDK Manager. Run on any Android 8.0 (API 26) or newer device or emulator. On Windows PowerShell, use `.\gradlew.bat`.

```bash
# 1. Build the debug APK
./gradlew assembleDebug

# 2. Install on a connected device or emulator
adb install "app/build/outputs/apk/debug/app-debug.apk"

# 3. Run unit tests
./gradlew testDebugUnitTest

# 4. Run on-device instrumentation tests (includes offline permission & widget render checks)
./gradlew connectedDebugAndroidTest
```

### Android CLI Workflow

SubCycler also supports the official **Android CLI** (`android`) for fast incremental loops and UI inspection:

```bash
android init && android info
android run --use-delta-install
android layout --pretty
android screen capture --annotate -o app_screen.png
```

---

## Contributing & License

Pull requests, bug reports, and design suggestions are welcome — browse [good first issues](https://github.com/SafalFrom2050/SubCycler/contribute) or open a [new issue](https://github.com/SafalFrom2050/SubCycler/issues/new/choose).

- Contribution guide: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Security policy: [`SECURITY.md`](SECURITY.md)
- License: **[MIT License](LICENSE)** (bundled Gradle Wrapper files under Apache License 2.0)
