# SubCycler 🔄

> **The Agentic 45-Day Subscription Cycler & Auto-Cancel Co-Pilot.**\
> *Binge your watchlist. Take a 15-day blackout. Let AI handle the auto-cancellation and cut your recurring bills by 33%–50%.*

**Available today.** SubCycler is a local-first Android subscription tracker for recurring bills, rotations, and renewal reminders. Automatic cancellation is on the roadmap.

---

## 🧠 The 45-Day Cycle Hack

Standard subscriptions trap you in endless 30-day auto-debit loops—even when you're too busy to watch anything.

```
Standard:   [==== 30d Paid ====][==== 30d Paid ====]  --> 12 billings / yr
SubCycler:  [==== 30d Active ====][-- 15d Blackout --] -->  8 billings / yr
```

- **30 Days Active**: Binge your watchlist for 1 paid month.
- **15 Days Blackout**: Take 2 weeks off while new episodes accumulate and you catch up on life.
- **The Result**: Cut annual subscription bills by **33.3%** (or **50%** when pairing two alternating services).

---

## ✨ Features

- ⚡ **Sub-100ms Instant Launch**: 100% native Kotlin + Jetpack Compose (Material 3).
- 🔒 **100% Offline & Private**: Zero cloud tracking. All your data stays locally on your device via Room Database (SQLite).
- ⏰ **Cancel & Resubscribe Alarms**: Android `AlarmManager` alerts you before auto-renewal hits and notifies you when blackout ends.
- 🔄 **Paired Service Swapping**: Alternate two streaming apps (e.g. Netflix in Jan, Disney+ in Feb) for continuous entertainment at half the price.
- 📊 **Financial ROI Simulator**: Live annual savings counter + interactive compounding multi-year projections.
- 🎨 **Popular Presets**: Netflix, Disney+, Max, Spotify, YouTube Premium, ChatGPT Plus, Claude Pro, Apple TV+, Xbox Game Pass, iCloud+.
- 🔗 **Assisted Cancellation** opens official billing or help pages from the subscription details.
- 🗓️ **Optional Calendar Awareness** uses selected device calendars to suggest actions after you opt in.

Android's system backup and device transfer settings can include the subscription database.

<details>
<summary>See the dashboard with sample subscriptions</summary>

<img src="docs/screenshots/dashboard.png" alt="SubCycler dashboard showing projected savings and three sample subscriptions" width="300">

</details>

---

## 🔮 Roadmap: Agentic AI & Playwright Browser Automation

The biggest friction with subscription hopping is remembering to cancel and reactivate on account portals. **SubCycler** is evolving into a zero-touch autonomous agent:

```
[45-Day Rotation Plan]
         │
         ▼
[SubCycler Local Agent / Playwright Engine]
         │
         ├── Day 28: Headless browser securely logs in & cancels auto-renewal
         └── Day 45: Headless browser reactivates subscription for the next active binge
```

- 🤖 **Zero-Touch Subscription Cycling**: Autonomous Playwright browser agents that navigate account portals to cancel and reactivate on schedule.
- 🛡️ **Local-First Security Enclave**: Passwords and session cookies never leave your device; automation runs locally in a sandboxed headless browser instance.
- 📱 **One-Tap Confirmations**: Agent sends you a push notification: *"Ready to blackout Netflix and save $15.49? [Approve Action]"*.

---

## 🤖 AI Agent Documentation Map

This repository contains structured architectural blueprints and guardrails tailored for autonomous AI agents:

- **[`PLANS.md`](PLANS.md)**: Master feature registry, roadmap statuses, and specification links.
- **[`plans/CORE.md`](plans/CORE.md)**: Foundational architecture, 45-day rotation math, and persistence blueprint.
- **[`plans/features/`](plans/features)**: Detailed feature specifications with state machines, code templates, and test matrices.
- **[`ARCHITECTURE.md`](ARCHITECTURE.md)**: Clean Architecture boundaries, SOLID guidelines, and multi-module dependency graphs.
- **[`DESIGN.md`](DESIGN.md)**: OLED Obsidian design tokens and strict negative constraints ("anti-rules") for agents.

---

## 🛠️ Tech Stack

- **Mobile Client**: Kotlin 2.0 + Jetpack Compose + Material 3 (Dynamic Color)
- **Local Persistence**: Room (SQLite) + Kotlin Coroutines & Flow
- **Background Scheduling**: `AlarmManager` + BroadcastReceivers
- **Upcoming AI Engine**: Python / Node.js Playwright Headless Agent + Local LLM Orchestration
- **Build System**: Gradle 8.7 + Android Gradle Plugin 8.5+

---

## 🚀 Quick Start

Use JDK 17 and install Android SDK Platform 35 through [Android Studio](https://developer.android.com/studio) or the SDK Manager. Run the app on an Android 8.0 (API 26) or newer device or emulator. For terminal builds, set `ANDROID_HOME` if Android Studio has yet to create `local.properties`, and add SDK Platform-Tools to your `PATH` for `adb`.

If you're starting fresh, clone [SubCycler](https://github.com/SafalFrom2050/SubCycler) and run these commands from the repository root. On Windows PowerShell, use `.\gradlew.bat` for the Gradle commands below.

### 1. Build the Android App
```bash
./gradlew assembleDebug
```
The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Install on Device / Emulator
```bash
adb install "app/build/outputs/apk/debug/app-debug.apk"
```

### 3. Run Unit Tests
```bash
./gradlew test
```

---

## 🤖 AI Agent & Android CLI Workflow

This project is optimized for **Agent-First Development** using the official **Android CLI** (`android`). AI agents and developers should use the following workflow when setting up, testing, and interacting with the app:

### 1. Environment Setup & Skill Initialization
```bash
# Initialize Android CLI environment and agent skills
android init

# Verify SDK path, environment variables, and connected devices
android info
```

### 2. Fast Incremental Build & Deployment
Use fast delta installs (transfers only modified bytecode & resources):
```bash
# Build, delta-install, and launch in one step
android run --use-delta-install

# Or install debug APK directly with delta optimization
android install --use-delta-install --apks="app/build/outputs/apk/debug/app-debug.apk"
```

### 3. Agent UI Inspection & Debugging
Instead of taking full screenshots, agents should inspect the active UI hierarchy directly via structured JSON:
```bash
# Dump active UI layout tree in JSON
android layout --pretty

# Inspect only UI elements that changed after an action (fast diffing)
android layout --diff

# Visual fallback with numbered target bounding boxes
android screen capture --annotate -o app_screen.png
```

### 4. Compose Preview Rendering
```bash
# Render Compose previews from CLI without launching full emulator
android studio render-compose-preview
```

---

## 🤝 Contribute

Pull requests are welcome. Pick a [good first issue](https://github.com/SafalFrom2050/SubCycler/contribute) or open a [bug report or feature request](https://github.com/SafalFrom2050/SubCycler/issues/new/choose).

See the [contributor guide](CONTRIBUTING.md) for the fork-to-PR steps and CI commands, and the [security policy](SECURITY.md) for private vulnerability reports.

---

## 📄 Open Source License

Distributed under the **[MIT License](LICENSE)**. Free to use, fork, and build upon.

The bundled Gradle Wrapper files are available under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
