# SubCycler Engineering & Feature Plans 🗺️

> Central directory of feature specifications, architectural blueprints, and engineering roadmaps for **SubCycler**.

---

## 🚀 Active & Baseline Plans

| Feature / Blueprint | Category | Target Release | Status | Document Link |
| :--- | :--- | :--- | :--- | :--- |
| **Core Architecture & Rotation Engine** | Foundation & Architecture | v1.0.0 | `Completed` | [CORE.md](plans/CORE.md) |
| **First-Launch Service Stack Onboarding** | Onboarding & UX | v1.1.0 | `Implemented` | [ONBOARDING_SERVICE_PICKER_FEATURE.md](plans/features/ONBOARDING_SERVICE_PICKER_FEATURE.md) |
| **Assisted Cancellation Links** | Subscription Management | Next release | `Implemented; device QA pending` | [CANCELLATION_DEEP_LINKS_FEATURE.md](plans/features/CANCELLATION_DEEP_LINKS_FEATURE.md) |
| **Optional Calendar Awareness** | Reminders & Recommendations | TBD | `Implemented; device QA ongoing` | [OPTIONAL_CALENDAR_AWARENESS_FEATURE.md](plans/features/OPTIONAL_CALENDAR_AWARENESS_FEATURE.md) |
| **AI In-App WebView Cancellation Co-Pilot** | Agentic AI & Automation | TBD | `Proposal` | [AI_CANCELLATION_COPILOT_FEATURE.md](plans/features/AI_CANCELLATION_COPILOT_FEATURE.md) |

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
  * **Build Your Stack Picker**: Searchable, category-filtered service tiles with brand monograms, haptics, and live selection feedback.
  * **Quick Review**: Editable price estimates and optional next-billing dates before anything is saved.
  * **Starter Plan Reveal**: Personalized tracked spend and potential savings followed by one atomic batch import.
* **Full Specification**: [Read the complete implementation plan](plans/features/ONBOARDING_SERVICE_PICKER_FEATURE.md).

### 3. [AI In-App WebView Cancellation Co-Pilot](plans/features/AI_CANCELLATION_COPILOT_FEATURE.md)
* **Goal**: Autonomous, zero-touch subscription cancellation co-pilot utilizing pre-authenticated in-app WebViews and Cloud AI reasoning.
* **Key Components**:
  * **Persistent Session Pre-Login**: The user logs in once; Android `CookieManager` preserves valid sessions for months, bypassing 2FA and CAPTCHAs.
  * **Semantic DOM Pruner**: Injected JS compresses 2.5MB HTML pages into 4KB semantic JSON trees of actionable buttons and text.
  * **Cloud AI Reasoning**: AI agent detects retention dark patterns ("Stay for $6.99") and autonomously guides navigation to the final cancellation confirmation.
  * **Human-in-the-Loop Fallback**: Smooth transition to a visible WebView if a re-authentication prompt or security puzzle is triggered.
* **Full Specification**: [Read the complete implementation plan](plans/features/AI_CANCELLATION_COPILOT_FEATURE.md).

---

## Assisted Cancellation Links

Implemented in the Dashboard subscription detail sheet. Users choose a curated service account/help page, Google Play, or Apple billing instructions. Opening a link leaves subscription state unchanged. The [integration and next-agent testing handoff](plans/features/CANCELLATION_DEEP_LINKS_FEATURE.md) records the catalog, code entry points, local validation, and pending device acceptance checks.

---

## Optional Calendar Awareness

Disabled by default. After opt-in, selected device calendars inform subscription suggestions. The [feature plan](plans/features/OPTIONAL_CALENDAR_AWARENESS_FEATURE.md) records implemented behavior and remaining device checks.

## 🔮 Future Roadmap Items (Under Consideration)

- **CSV & JSON Portfolio Backup/Restore**: Encrypted export and import of subscription rotation schedules.

---

## 🗃️ Cancelled Plans

- **Auto-Detect Installed Apps & Native Icons**: Cancelled in favor of explicit service selection. [Archived specification](plans/features/cancelled/AUTO_DETECT_FEATURE.md).
