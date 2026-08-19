# SubCycler Architecture & Modernization Plan 🏛️

This document outlines the architecture, design principles, layer separation, and future scaling roadmap for **SubCycler (SubTrack)**.

---

## 1. Architectural Principles & Patterns

The application follows the official **Google Recommended Architecture Guide for Android**, enforcing **Clean Architecture** and strict **SOLID principles**:

- **Single Responsibility Principle (SRP)**: ViewModels handle UI state and user intents; Use Cases encapsulate discrete domain business rules; Repositories manage data retrieval/persistence; Workers handle background tasks.
- **Open/Closed Principle (OCP)**: Systems are extensible via interfaces (e.g. `TimeProvider`, `SubscriptionRepository`, `UserPreferencesRepository`) without modifying caller code.
- **Liskov Substitution Principle (LSP)**: Interface implementations (e.g., `FakeTimeProvider`, `FakeSubscriptionRepository`) substitute seamlessly in tests without affecting domain logic.
- **Interface Segregation Principle (ISP)**: Granular, focused use cases (`GetSubscriptionCycleStatusesUseCase`, `GetPortfolioSummaryUseCase`, `AlignPairedSubscriptionsUseCase`, `ManageSubscriptionUseCase`) instead of monolithic God-classes.
- **Dependency Inversion Principle (DIP)**: High-level business logic depends on domain abstractions, not concrete Room DAOs or system APIs. Injected via **Hilt (Dagger)**.

---

## 2. High-Level Layer Hierarchy

```
┌────────────────────────────────────────────────────────────────────────┐
│                                UI Layer                                │
│  - Jetpack Compose (MVI-style UDF: UiState + UiAction)                │
│  - Type-Safe Navigation Compose (Navigation 2.8+ @Serializable)        │
│  - Stateful Routes (*Route) + Stateless Screens (*Screen)              │
│  - Scoped @HiltViewModel with StateFlow<UiState>                      │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ consumes
┌───────────────────────────────────┴────────────────────────────────────┐
│                              Domain Layer                              │
│  - Pure Kotlin Business Logic (Zero Android Framework Dependencies)    │
│  - Domain Models: Subscription, CycleStatus, PortfolioSummary, etc.    │
│  - Single-Responsibility Use Cases                                     │
│  - RotationEngine (Mathematical cycle calculations & savings)          │
│  - TimeProvider Abstraction (Deterministic system time testing)        │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ provides
┌───────────────────────────────────┴────────────────────────────────────┐
│                               Data Layer                               │
│  - Room Database (SubscriptionEntity, SubscriptionDao, AppDatabase)    │
│  - Bidirectional Data Mappers (SubscriptionMapper.kt)                  │
│  - SubscriptionRepository & SubscriptionRepositoryImpl                 │
│  - Jetpack DataStore (UserPreferencesRepository)                       │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ manages
┌───────────────────────────────────┴────────────────────────────────────┐
│                    Background & Autonomous Engine                      │
│  - WorkManager 2.9 (Battery-optimized 24h periodic cycle evaluation)   │
│  - HiltWorkerFactory with on-demand Application Configuration.Provider │
│  - DailyCycleEvaluationWorker (Notifications & Autonomous Action Hooks)│
│  - WorkSchedulerManager (Device boot & trigger management)             │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Layer Breakdown

### A. Domain Layer (`com.subtrack.app.domain`)
- **Pure Kotlin**: Free of Android SDK references (`android.*`), allowing rapid local JVM unit testing (<20ms).
- **Time Abstraction (`TimeProvider`)**: All time checks invoke `timeProvider.today()` or `timeProvider.nowInstant()`, allowing deterministic historical and future simulations.
- **Domain Use Cases**:
  - `GetSubscriptionCycleStatusesUseCase`: Transforms subscription entities into real-time cycle phases (Active vs Blackout) and computes urgent cancellation actions.
  - `GetPortfolioSummaryUseCase`: Computes aggregate annual costs, optimized rotation spending, and compounding multi-year ROI.
  - `AlignPairedSubscriptionsUseCase`: Synchronizes two paired subscriptions (e.g., Netflix + Disney+) to alternate 30d/30d without service blackout overlap.
  - `ManageSubscriptionUseCase`: Handles subscription lifecycle (CRUD, toggle auto-renew cancellation).

### B. Data Layer (`com.subtrack.app.data`)
- **Entity & Domain Separation**: Room entities (`SubscriptionEntity`) reside strictly in `data/local/entity/`. The domain model (`Subscription`) contains no database annotations (`@Entity`, `@PrimaryKey`), isolating database schema evolution from business logic.
- **DataStore Preferences**: `UserPreferencesRepository` provides non-blocking, reactive `Flow<UserPreferences>` using Jetpack DataStore Preferences for user currency, notification thresholds, and dark mode.
- **Repository Implementation**: `SubscriptionRepositoryImpl` handles database mapping and reactive `Flow` transformations on IO dispatchers.

### C. UI Layer (`com.subtrack.app.ui`)
- **Unidirectional Data Flow (UDF)**:
  - `UiState`: Single immutable state data class per screen (`DashboardUiState`, `PlannerUiState`, `PairedRotatorUiState`, `AnalyticsUiState`).
  - `UiAction`: Sealed interface capturing all user interactions (`DashboardAction`, `PlannerAction`, etc.).
- **Stateful Route vs Stateless Screen**:
  - `*Route`: Injects `@HiltViewModel`, collects state with lifecycle awareness (`collectAsStateWithLifecycle()`), and binds actions.
  - `*Screen`: Pure stateless composable accepting `(uiState, onAction)`, preview-ready in Android Studio and isolated for Compose UI testing.
- **Type-Safe Navigation Compose**: Uses Kotlin `@Serializable` route destinations (`DashboardDestination`, `PlannerDestination`, `RotatorDestination`, `AnalyticsDestination`) with Navigation Compose 2.8+.

### D. Dependency Injection (`com.subtrack.app.di`)
- **Hilt Modules**:
  - `DispatchersModule`: Qualifiers (`@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher`) for testable thread switching.
  - `DataModule`: Provides Singleton instances of `AppDatabase`, `SubscriptionDao`, `SubscriptionRepository`, `UserPreferencesRepository`, and `TimeProvider`.
  - `UseCaseModule`: Provides Singleton domain Use Cases.
- **Application & Activities**: `@HiltAndroidApp`, `@AndroidEntryPoint`, and `@HiltViewModel`.

### E. Background Processing (`com.subtrack.app.worker`)
- **WorkManager**: Periodic 24-hour execution via `DailyCycleEvaluationWorker` utilizing `@HiltWorker` and `HiltWorkerFactory`.
- **Battery Optimization**: On-demand WorkManager initialization via `Configuration.Provider` in `SubTrackApplication`, with `Constraints.setRequiresBatteryNotLow(true)`.

---

## 4. Testing Strategy

The architecture supports 100% test coverage without requiring physical devices or emulators:
- **Local Unit Tests (`test/`)**:
  - `RotationEngineTest`: Verifies rotation mathematical cycles, phase dates, and cost savings.
  - `SubscriptionMapperTest`: Verifies bidirectional conversion accuracy.
  - `GetSubscriptionCycleStatusesUseCaseTest`: Verifies stream status calculation with `FakeSubscriptionRepository` and `FakeTimeProvider`.
  - `AlignPairedSubscriptionsUseCaseTest`: Verifies paired schedule alignment logic.
  - `GetPortfolioSummaryUseCaseTest`: Verifies portfolio financial metrics.
  - `DashboardViewModelTest`: Verifies MVI actions, coroutine scheduling with `StandardTestDispatcher`, and UI state emission.
  - `UserPreferencesTest`: Verifies DataStore reactive updates.
  - `DailyCycleEvaluationLogicTest`: Verifies background notification triggers and lead thresholds.

---

## 5. Multi-Module Target Roadmap

As SubCycler grows, the single `:app` module can be factored into a modular multi-module architecture:

```
:app
 ├── :feature:dashboard
 ├── :feature:planner
 ├── :feature:rotator
 ├── :feature:analytics
 ├── :core:domain       (Pure Kotlin use cases, models, RotationEngine)
 ├── :core:data         (Room database, DataStore, Repository implementations)
 ├── :core:ui           (Compose design system, theme, common components)
 └── :core:common       (Dispatchers, time abstractions, utilities)
```
