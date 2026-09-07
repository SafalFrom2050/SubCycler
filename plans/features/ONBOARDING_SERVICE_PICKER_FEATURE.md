# Feature Specification: First-Launch Service Stack Onboarding ✨

> **Target File**: `plans/features/ONBOARDING_SERVICE_PICKER_FEATURE.md`
> **Status**: Implemented
> **Target Release**: v1.1.0
> **Platform**: Android 8.0 (API 26) through Android 15 (API 35)
> **Privacy**: Fully local; no installed-app access, account linking, or network dependency

---

## 1. Executive Summary

SubCycler replaces device app scanning with a fast, playful first-launch experience called **Build Your Stack**. Users select familiar services from a curated catalog, review editable price estimates and optional renewal dates, and receive an immediate starter-plan payoff before entering the dashboard.

The experience is designed to:

- Reach a useful dashboard in under one minute for a typical 3–5 service portfolio.
- Feel tactile and rewarding without turning financial setup into a game.
- Avoid sensitive installed-app visibility and third-party account linking.
- Clearly label prices and rotation choices as editable suggestions.
- Remain fully skippable for users who prefer manual entry later.

---

## 2. Product Principles

### Privacy Before Automation

The picker never inspects installed packages, requests package visibility, contacts subscription providers, or infers whether a user has a paid plan. Selection is explicit and user-controlled.

### Progressive Commitment

The flow asks only for information needed at the current moment:

1. Recognize and tap services.
2. Correct known prices and renewal dates.
3. See the value of the resulting starter plan.

Unknown renewal dates can remain unset and be refined later.

### Fun Through Feedback

Selection uses brand-colored monograms, light haptic feedback, friendly stack-building copy, live selection counts, and a personalized completion reveal. There are no streaks, points, artificial urgency, or manipulative rewards.

---

## 3. User Flow

### Step 1: Build Your Stack

- Presents popular services in a compact three-column grid.
- Supports category filters for video, music, software, gaming, and cloud services.
- Search operates across the entire catalog, regardless of the active category.
- Suggests the ISO 4217 currency associated with the device locale and keeps a searchable currency selector beside the custom-service action.
- Treats the locale currency as a suggestion, not a fixed country rule; the user can switch currencies before choosing services.
- **Add a custom service** is always visible and accepts a name, category, and the user's actual monthly price.
- A non-matching search term pre-fills the custom-service form instead of ending in a dead end.
- Every tap produces light haptic feedback and an immediate selected state.
- The primary action includes the live selection count.
- **I’ll add services later** completes onboarding without creating records.

Primary copy:

> **What are you paying for?**
> Tap everything that looks familiar. No logins, linking, or detective work.

### Step 2: Quick Check

- Shows only selected services.
- Pre-fills editable USD price estimates from `PresetLibrary` only when USD is selected; estimates may vary by tier, promotion, and region.
- Leaves preset prices blank for every other currency so a USD estimate is never relabeled as local money or presented as a guessed exchange conversion.
- Formats symbols, grouping, decimal separators, and currency-specific fraction digits using the selected currency and device locale.
- Allows an optional next billing date per service.
- Labels the suggested plan as either continuous or a 45-day rotation.
- Rejects empty, zero, or invalid prices before continuing.

When a next billing date is provided, the initial cycle start is derived by subtracting the preset active-period length. When omitted, today becomes the initial reference date and the subscription is marked for later refinement in its notes.

### Step 3: Starter Plan Reveal

- Displays the selected service stack.
- Shows estimated tracked monthly spend.
- Shows potential annual savings based on the existing `RotationEngine`.
- States how many services received suggested smart rotations.
- Reinforces that the experience is private, offline, and editable.
- Imports all selected services atomically before opening the dashboard.

---

## 4. First-Launch and Upgrade Behavior

`UserPreferences.isOnboardingCompleted` is persisted in DataStore.

| Situation | Result |
| :--- | :--- |
| Fresh install with an empty database | Show onboarding |
| User completes or skips onboarding | Persist completion and open dashboard |
| Existing installation with subscriptions but no onboarding preference | Mark onboarding complete and preserve the existing dashboard |
| Returning user with onboarding complete | Open dashboard immediately |

The production Room database no longer inserts demonstration subscriptions on creation. This allows genuinely fresh installs to reach onboarding with an empty portfolio.

Preset names describe durable services or named plan families, not volatile storage capacities. Google One and iCloud+ therefore stay generic, while Google AI Plus, Pro, and Ultra remain separate named plan presets. The user confirms the price they actually pay before import.

### Currency behavior

- On first use, `UserPreferences.preferredCurrency` defaults to the ISO currency suggested by the primary device locale (for example JPY in Japan or EUR in Germany), falling back safely to USD when the locale has no valid tender currency.
- The onboarding picker exposes current country-associated ISO 4217 currencies with code, symbol, localized name, and search; obsolete and accounting-only registry entries are excluded.
- A user's explicit choice is persisted and wins over a late preference load or future locale changes.
- New custom services and manually added subscriptions inherit the preferred currency; every saved subscription also retains its own currency code.
- Portfolio summaries use one explicit currency at a time. If legacy or manually edited data contains multiple currencies, other-currency services are excluded from the aggregate with a visible explanation; individual rows and notifications still use each subscription's own currency.
- SubCycler does not perform foreign-exchange conversion. Calculations that combine two subscriptions are disabled when their currencies differ, with an explanation asking the user to align them first.

This is monetary localization, independent of translated interface copy. Additional app-language translations can be added separately without changing the currency model.

### Temporary QA launch mode

Debug builds currently set `FORCE_ONBOARDING_ON_EVERY_LAUNCH=true`. Each new app session therefore starts in onboarding even when onboarding was previously completed or subscriptions already exist. Completing or skipping onboarding still opens the dashboard for the remainder of that session. Release builds keep the one-time production behavior described above.

---

## 5. Notification Permission Timing

Android 13+ notification permission is no longer requested immediately when `MainActivity` launches.

- Completing onboarding with at least one subscription requests notification permission after the starter stack has been saved.
- Skipping onboarding does not request notification permission.
- Existing users are not interrupted by the onboarding migration path.

This ensures that the permission request follows a demonstrated reminder use case.

---

## 6. Architecture

```text
SubCycler
 ├── :app
 │    ├── MainActivity.kt
 │    └── ui/navigation/AppViewModel.kt
 ├── :core:domain
 │    ├── model/UserPreferences.kt
 │    ├── model/SubscriptionPreset.kt
 │    ├── repository/SubscriptionRepository.kt
 │    └── usecase/ManageSubscriptionUseCase.kt
 ├── :core:data
 │    ├── local/AppDatabase.kt
 │    ├── local/SubscriptionDao.kt
 │    └── preferences/UserPreferencesRepositoryImpl.kt
 └── :feature:onboarding
      ├── OnboardingViewModel.kt
      └── OnboardingScreen.kt
```

### Presentation

- `OnboardingViewModel` owns the three-step state machine, selected services, editable drafts, validation, preview calculations, and save state.
- `OnboardingScreen` is stateless apart from the transient date-picker target.
- `OnboardingAction` provides a single UDF/MVI action entry point.

### Persistence

- `SubscriptionRepository.addSubscriptions()` provides batch persistence.
- Room’s list insert is a single atomic DAO operation.
- Onboarding completion is written only after subscription insertion succeeds.
- Cross-store retries are idempotent: service names already inserted into Room are filtered before another batch attempt.
- Failure leaves the user on the reveal screen with a retry action instead of silently entering an incomplete dashboard.

---

## 7. Curated Service Catalog

The catalog is backed by `PresetLibrary` and currently covers popular video, music, AI/software, gaming, and cloud services. Each preset contains:

- Display name
- Editable default monthly estimate
- Category
- Brand color and fallback icon name
- Suggested rotation strategy
- Suggested active and blackout lengths

The catalog can be expanded without Android manifest changes or Play Store package-visibility considerations.

---

## 8. Accessibility and Responsive Behavior

- Every service tile is a semantic button with a visible selected state.
- Selection never relies on color alone; selected tiles also show a check icon and border.
- Controls use Android-native focus, keyboard, date-picker, and screen-reader behavior.
- The grid remains three columns while review rows scroll vertically on compact devices.
- Primary and secondary actions meet mobile touch-target sizing.
- System back returns from reveal to review and review to service selection.

---

## 9. Verification

### Automated Tests

- Empty fresh installs route to onboarding.
- Completed installs route directly to main content.
- Legacy installs with subscriptions migrate without showing onboarding.
- Search finds services outside the active category.
- Selection advances to review only when at least one service is selected.
- Invalid prices block the starter-plan reveal.
- Batch completion persists all selected subscriptions and onboarding state.
- Skipping persists completion without subscriptions or a notification request.

### Manual Matrix

| Test | Expected result |
| :--- | :--- |
| Select three popular services | Live count and haptics update after each tap |
| Search for a cloud service while Popular is selected | Matching cloud service appears |
| Leave renewal dates unset | Import succeeds and dates remain editable later |
| Enter an invalid price | Inline error prevents continuing |
| Finish with selected services on Android 13+ | Dashboard opens, then notification permission is requested |
| Skip onboarding | Empty dashboard opens without a notification prompt |
| Upgrade an existing populated install | Existing dashboard opens without onboarding |

---

## 10. Implementation Checklist

- [x] Create `:feature:onboarding` module.
- [x] Build service selection, search, and category filtering.
- [x] Build editable price and optional billing-date review.
- [x] Build personalized starter-plan reveal.
- [x] Add atomic batch subscription persistence.
- [x] Persist onboarding completion in DataStore.
- [x] Remove automatic production sample data.
- [x] Add legacy-install startup compatibility.
- [x] Defer notification permission until onboarding completion.
- [x] Add ViewModel and startup routing tests.
- [x] Suggest currency from the device locale and provide a searchable override.
- [x] Apply locale-aware ISO currency formatting throughout the app.
- [x] Avoid fabricated conversion by leaving non-USD preset estimates blank.
- [x] Guard multi-subscription arithmetic when currencies differ.
