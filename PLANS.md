# SubCycler Engineering & Feature Plans 🗺️

> Central directory of feature specifications, architectural blueprints, and engineering roadmaps for **SubCycler**.

---

## 🚀 Active & Baseline Plans

| Feature / Blueprint | Category | Target Release | Status | Document Link |
| :--- | :--- | :--- | :--- | :--- |
| **Core Architecture & Rotation Engine** | Foundation & Architecture | v1.0.0 | `Completed` | [CORE.md](plans/CORE.md) |
| **First-Launch Service Stack Onboarding** | Onboarding & UX | v1.1.0 | `Implemented` | [ONBOARDING_SERVICE_PICKER_FEATURE.md](plans/features/ONBOARDING_SERVICE_PICKER_FEATURE.md) |
| **Assisted Cancellation Links** | Subscription Management | v1.1.0 | `Implemented; device QA pending` | [CANCELLATION_DEEP_LINKS_FEATURE.md](plans/features/CANCELLATION_DEEP_LINKS_FEATURE.md) |
| **Optional Calendar Awareness** | Reminders & Recommendations | v1.1.0 | `Implemented; device QA ongoing` | [OPTIONAL_CALENDAR_AWARENESS_FEATURE.md](plans/features/OPTIONAL_CALENDAR_AWARENESS_FEATURE.md) |
| **Minimal Home UX & Subscribe/Unsubscribe Progress Notification** | Core UX & Notifications | v1.2.0 | `Implemented` | [MINIMAL_UX_AND_ACTION_NOTIFICATION_FEATURE.md](plans/features/MINIMAL_UX_AND_ACTION_NOTIFICATION_FEATURE.md) |
| **Material 3 Expressive Redesign, Auto-Saving Multi-Step UX & Offline Resilience** | Design System, Core UX & Backup | v1.3.0 | `Implemented & Device-Verified` | [MATERIAL_EXPRESSIVE_AND_UX_REVISION_FEATURE.md](plans/features/MATERIAL_EXPRESSIVE_AND_UX_REVISION_FEATURE.md) |

---

## 📌 Feature & Blueprint Summaries

### 1. [Core Architecture & Mathematical Rotation Engine](plans/CORE.md)
* **Goal**: Establish the multi-module scaffolding, pure Kotlin 45-day mathematical rotation algorithm, 100% offline Room database, and OLED Obsidian design system.
* **Key Components**:
  * **Pure Domain `RotationEngine`**: Evaluates 30d active / 15d blackout windows and computes 33.3% direct savings and 50% paired savings with zero Android dependencies.
  * **Offline Room Persistence**: SQLite storage with non-blocking Kotlin Coroutines and reactive Flow streams.
  * **OLED Obsidian Precision Design**: True pitch black `#000000` canvas with 3-tier accent hierarchy and hairline borders.
  * **Alarms & Background Safety Net**: Android `AlarmManager` exact triggers + `DailyCycleEvaluationWorker`.
* **Full Specification**: [Read the complete blueprint](plans/CORE.md).

### 2. [First-Launch Service Stack Onboarding](plans/features/ONBOARDING_SERVICE_PICKER_FEATURE.md)
* **Goal**: Reach a useful starter portfolio in under a minute through an explicit, playful service picker without installed-app access or account linking.
* **Key Components**:
  * **Upfront App Currency & Welcome**: Sets SubCycler's single app-wide currency directly on the Welcome screen (locale-suggested, editable anytime in Settings with an offline manual-conversion warning).
  * **Build Your Stack Picker**: Searchable, category-filtered service tiles with organic monograms, haptics, and live selection feedback.
  * **Quick Review & Regional Tiers**: Offline curated 1-tap regional tier chips (`USD`, `JPY`, `EUR`, `GBP`, `CAD`, `AUD`, `INR`, `KRW`), `Mo`/`Yr` cadence toggle, and optional next-billing dates before anything is saved.
  * **Starter Plan Reveal**: Personalized tracked spend and potential savings followed by one atomic batch import.
* **Full Specification**: [Read the complete implementation plan](plans/features/ONBOARDING_SERVICE_PICKER_FEATURE.md).

### 3. [Minimal Home UX & Assisted Subscribe / Unsubscribe Progress Notification](plans/features/MINIMAL_UX_AND_ACTION_NOTIFICATION_FEATURE.md)
* **Goal**: Deliver a clutter-free minimalist Home Screen, 3-tab navigation, and a 100% offline subscribe/unsubscribe tracking flow powered by a silent active notification and persistent next-launch recovery popup.
* **Key Components**:
  * **Decluttered 3-Tab Navigation**: Consolidated `Subscriptions`, `Planner` (with integrated Paired Swap), and `Savings` tabs, retiring `:feature:rotator`.
  * **Non-Interruptive Active Notification**: Silent `IMPORTANCE_LOW` notification with direct **Completed** and **Cancel** actions when subscribing or unsubscribing.
  * **Launch / Resume Recovery Popup**: DataStore persistence ensures that if the notification does not survive, the user is prompted with a simple one-tap **Completed / Cancel** dialog on the next app launch or resume.
* **Full Specification**: [Read the complete implementation plan](plans/features/MINIMAL_UX_AND_ACTION_NOTIFICATION_FEATURE.md).

### 4. [Material 3 Expressive Redesign, Auto-Saving Multi-Step UX & Offline Resilience](plans/features/MATERIAL_EXPRESSIVE_AND_UX_REVISION_FEATURE.md)
* **Goal**: Elevate SubCycler with an adaptive Light & Dark Botanical Emerald Material 3 Expressive visual system, effortless auto-saving multi-step subscription setup with progressive Incomplete tracking, single app-wide currency clarity, offline JSON/CSV backup, and a home-screen widget.
* **Key Components**:
  * **Botanical Kinetic Architecture (`DESIGN.md` v3.5.0)**: Adaptive 70% Neutral / 30% Botanical Emerald Light & Dark theme, bundled offline OFL variable fonts (`Bricolage Grotesque` + `Plus Jakarta Sans`), masked silk-waves hero backdrop, scalloped shape primitives, and spring-reactive icons.
  * **Auto-Saving 3-Step Form & Incomplete State**: Compact `Basics / Cycle / Notes` sheet with whisper-quiet capsule navigation, automatic debounced + `Mutex`-guarded saving (zero Save buttons), swipe-down dismissal protection, and first-class `Incomplete` setup tracking (`Finish setup` jumps to first missing step).
  * **Single App-Wide Currency & Interactive Cycle Strategy Studio**: Upfront currency selection with explicit offline manual-conversion warning in Settings, plus an interactive 90-day live rhythm ribbon and tactile `0d–45d` break slider.
  * **Offline Backup, Deduplicated Reminders & Widget**: Local JSON/CSV export & import (`SubscriptionBackupCodec`), Room v1→v5 migrations, inexact `AlarmManager` + `WorkManager` + `ReminderDeduplicator` (zero `USE_EXACT_ALARM`), and `SubCyclerWidgetProvider`.
* **Full Specification**: [Read the complete implementation plan & decision log](plans/features/MATERIAL_EXPRESSIVE_AND_UX_REVISION_FEATURE.md).

---

## Assisted Cancellation Links

Implemented in the Dashboard subscription detail sheet. Users choose a curated service account/help page, Google Play, or Apple billing instructions. The [integration and next-agent testing handoff](plans/features/CANCELLATION_DEEP_LINKS_FEATURE.md) records the catalog, code entry points, local validation, and pending device acceptance checks.

---

## Optional Calendar Awareness

Disabled by default. After opt-in, selected device calendars inform subscription suggestions. The [feature plan](plans/features/OPTIONAL_CALENDAR_AWARENESS_FEATURE.md) records implemented behavior and remaining device checks.

---

## 🔮 Future Roadmap Items (Under Consideration)

- **Material 3 Expressive v4.0 ("Botanical Kinetic Architecture II")**: Next-generation expressive upgrades inspired by [`kamranbekirovyz/bunpod`](https://github.com/kamranbekirovyz/bunpod)—including `androidx.graphics:graphics-shapes` lifecycle polygon morphing, anticipatory press-preview geometry, scroll-collapsible Efficiency HUD, a scrubbable "Cycle Time-Machine" wave + 50/50 asymmetric Bento Command Deck in the Detail sheet, 3D stacked floating snack pills, and two-beat collision list choreography. See [BunPod Design Study](docs/BUNPOD_DESIGN_INSPIRATION.md) and [Material Expressive Upgrades Proposal](docs/MATERIAL_EXPRESSIVE_UPGRADES_PROPOSAL.md).
- **Passphrase-Encrypted Offline Backups**: Optional AES-GCM encryption layer on top of SubCycler's existing local JSON/CSV export and import.
- **Expanded Accessibility & Large-Screen Layouts**: Adaptive two-pane tablet/foldable layouts and formal TalkBack / font-scale audits across all expressive components.

---

## ⏸️ Postponed Plans

- **AI In-App WebView Cancellation Co-Pilot**: Postponed in favor of a 100% offline, zero-internet-permission architecture with active notification tracking and next-launch recovery. [Archived specification](plans/features/postponed/AI_CANCELLATION_COPILOT_FEATURE.md).

---

## 🗃️ Cancelled Plans

- **Auto-Detect Installed Apps & Native Icons**: Cancelled in favor of explicit service selection. [Archived specification](plans/features/cancelled/AUTO_DETECT_FEATURE.md).
