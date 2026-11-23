# Optional calendar-aware reminders and recommendations

Status: **Implemented — JVM/build validation and Huawei API 26 device validation complete; broader device/provider coverage remains.**
Product decisions agreed: 2026-09-10. Source baseline inspected: `3616867`.
Target: existing Android app, minimum API 26; verify current SDK configuration before implementation.

## 1. Product contract

Calendar awareness complements subscription tracking only after the user explicitly enables it. It identifies relevant overlaps between existing calendar events and subscription decisions, then offers a brief review suggestion. It does not determine that an event makes a service unnecessary.

The following requirements are agreed with the user and take precedence over earlier calendar proposals:

- Disabled by default for new and upgraded installations, even if Android calendar permission is already granted.
- One Settings toggle: **Calendar-aware suggestions**.
- Request calendar permission only after the user attempts to enable the feature. Let them select calendars during setup.
- No manual plans, trip-entry forms, event editing, per-event tagging workflow, or per-service availability forms.
- No additional calendar tab, standalone calendar screen, onboarding step, permanent dashboard calendar section, or enablement promotion.
- Show a compact suggestion only when a relevant event overlaps a subscription decision. Otherwise render nothing.
- Turning the feature off stops calendar reads and clears calendar-derived suggestions/reminders. Ordinary subscription tracking and reminders continue.
- Opening cancellation links never updates cancellation status. Provider cancellation and the existing manual tracking confirmation remain separate.
- Calendar permission denial or unavailable calendar data must not disrupt the ordinary app experience.

This replaces the earlier broad “Calendar Event Sync” roadmap item. Calendar export, calendar writes, holiday-pack downloads and cloud calendar account linking are outside this release.

## 2. Minimal user experience

The inspected app has no Settings destination. Add a small, accessible Settings action to the Dashboard toolbar opening a compact Settings sheet; do not add a bottom-navigation item or build a general settings dashboard. If a settings surface exists by implementation time, reuse it.

### Enablement

1. Settings initially shows an OFF switch and one supporting sentence: “Use selected device calendars for timely subscription suggestions.”
2. Switching ON explains that event titles and dates are processed locally, then requests `READ_CALENDAR` if needed. Do not request notification permission in the same sequence.
3. Show a compact calendar-selection sheet. Calendars are unchecked initially; show enough account context to distinguish identical names. Read event occurrences only after selection is confirmed.
4. Save enabled state and selected calendar IDs together after at least one accessible calendar is selected. Cancelling setup or selecting none leaves the feature OFF. Never silently select every calendar.
5. When enabled, Settings offers “Calendars” to revise the selection. Access problems and notification-disabled status appear here, not as repeated dashboard prompts.

Android grants calendar read permission broadly; the app's calendar selector limits what the implementation queries. Do not describe the OS permission as being restricted to individual calendars.

Model transient **CONFIGURING** separately from **DISABLED** and **ENABLED**. Explicit enablement may enumerate calendar names after permission is granted while CONFIGURING; it must not read event occurrences or start background work. Cancelling returns to DISABLED. All zero-access assertions for OFF refer to DISABLED, not this user-initiated setup transaction. Configuration state must not survive process death as implicit consent.

### Suggestions

Reuse the Dashboard's existing next-action area. Allow at most one visible action card in that area; merge context into an existing relevant subscription reminder where possible. A due ordinary billing action takes priority over speculative calendar advice.

| Situation | Example copy | Primary action |
| --- | --- | --- |
| Recognized trip overlaps an upcoming subscription boundary | “Your upcoming trip overlaps Netflix's next planned cycle. Review whether you'll need it.” | Review cancellation |
| Recognized holiday/festival overlaps a planned blackout | “A holiday on your calendar falls during Disney+'s planned break. Review your subscription.” | Review subscription |
| Business tour overlaps a work-service decision | Prefer silence in the initial rules; work travel does not establish that software is unnecessary. | None |
| No relevant event, insufficient confidence, or no upcoming subscription decision | No card, placeholder, count or empty state. | None |

The secondary action is **Dismiss**. Do not add plan-management controls. “Review cancellation” opens the existing `CancellationOptionsDialog` for the associated subscription; “Review subscription” opens its detail sheet. Notification taps should open that context in SubCycler, not immediately launch an external provider.

Use “planned cycle” or “planned break” for dates derived from the current rotation engine. Use “billing/renewal date” only when the data model actually provides a confirmed billing date. Avoid event descriptions and raw event titles in dashboard/notification copy; generic context such as “upcoming trip” is sufficient. Lock-screen notification text should be generic.

### Disablement and access loss

Persist OFF first, invalidate in-flight work, unregister calendar observers, cancel calendar-specific jobs and notifications, and delete derived data, selection IDs and dismissal state. A completed stale query must not recreate any of these after disablement. The OS permission may remain granted, but calendar querying stays disabled.

On permission revocation, turn the feature OFF and perform equivalent cleanup at the next execution opportunity; gate every query, display and notification emission so revoked access is never treated as usable cached data. Do not promise immediate cleanup while Android has stopped the process. Re-enabling requires an explicit action in Settings.

If one selected calendar disappears, remove its derived suggestions and use remaining valid selections. If no selections remain accessible, switch OFF and show a quiet Settings explanation. Never switch to another account/calendar automatically.

## 3. Recommendation rules

The following are conservative engineering defaults for the first implementation, not additional user-facing configuration:

- Query a bounded window from today through the next 30 days, including events that started earlier but still overlap it. No full-history scan.
- Evaluate already tracked subscriptions against actual event occurrences and upcoming planned cycle boundaries. Do not invent subscriptions or modify existing schedules.
- Show contextual suggestions only for decisions within the next seven days. Overlap by itself is not enough; there must be an upcoming subscription action or planned blackout relevant to the event.
- Use a small deterministic local classifier, with an explicit tested vocabulary for trips, vacations, holidays and festivals. Begin with English phrases and named festivals such as Dashain/Tihar; unsupported titles produce no inference. This language limitation must be documented, not hidden behind a claim of universal understanding.
- Match bounded words/phrases, not arbitrary substrings. An all-day flag, “busy” status, meeting invitation, calendar color or a calendar named “Work” does not establish a trip or festival. Ignore cancelled events and occurrences the user declined when that status is available. Do not interpret tentative/ambiguous events as firm plans.
- For travel, initially limit cancellation-review suggestions to entertainment categories (streaming and gaming), explicit travel/vacation markers, and event durations of at least two calendar days. Do not recommend cancellation of cloud storage, work software, fitness or other services merely because the user is away.
- For recognized festivals/holidays, offer a neutral review only when an existing streaming/gaming subscription is projected to be in blackout during the event. Do not assume religious observance or tell users they must reactivate.
- Rank due subscription actions first, then earliest decision date, then stronger event confidence. Use stable ordering to avoid cards changing on every recomposition.
- Dismissal applies to the subscription plus source occurrence and suggestion type. Keep it through the occurrence's end; do not resurrect it after a title-only edit, app restart or periodic refresh. Recurring instances have distinct occurrence identities. Expire old dismissal entries.
- Conflicting events must produce at most one neutral review suggestion; never issue opposing cancel/reactivate recommendations together.
- No cloud AI, external classification service, calendar-title telemetry, or background interpretation beyond these rules.

### Savings boundaries

Calendar suggestions in this release must not introduce numerical savings claims. The current model stores a start date and fixed active/blackout lengths; it does not provide verified provider billing schedules or charge history. Calendar overlap therefore cannot establish an avoided charge.

Keep existing financial calculations unchanged. Do not subtract unused days from a monthly price, count postponed payments as realized savings, or mark a payment saved because a user opened a link. A future billing-aware model can support defensible incremental estimates without adding forms to this feature.

## 4. Android data access and lifecycle

Use Android's Calendar Provider through `ContentResolver`, with `CalendarContract.Calendars` for selection and `CalendarContract.Instances` for occurrences. Reading requires `READ_CALENDAR`; recurring events have separate instances. See the [official Calendar Provider guide](https://developer.android.com/identity/providers/calendar-provider).

Implementation requirements:

- Query only selected calendar IDs, on an IO dispatcher, with cancellation support and bounded results. Handle null cursors, provider absence, errors and permission changes without crashing.
- Read only fields needed for source identity, occurrence dates, all-day/time-zone treatment, local classification and event status. Do not fetch attendees, descriptions, meeting URLs or locations.
- Handle recurring-event exceptions, moved occurrences, deleted events and duplicates. Use provider occurrence identity rather than a title as the key.
- Keep all-day dates as calendar dates; preserve exclusive end-date semantics. For timed events, preserve instants and relevant time-zone information. Test midnight, daylight-saving transitions and crossing date boundaries while travelling.
- A calendar application's presence does not prove its accounts are exposed through Calendar Provider. Feature availability is based on accessible data, not an app-name allowlist. No Google Play Services dependency is required for the local provider adapter.
- Work-profile and cloud-only calendars may be inaccessible. Do not attempt to bypass those boundaries or silently add Google/Microsoft account authentication. The fallback is ordinary SubCycler behavior, with no manual-plan UI.
- Refresh on enabled app foreground, confirmed calendar selection changes and relevant calendar changes while running. Use a debounced content observer while the UI is active; periodic reconciliation handles changes missed while the app is stopped.
- Store raw event titles only transiently for classification; persist only minimal derived context/identities if needed. Calendar-derived storage and device-specific consent/selection must be excluded from backup and device transfer. Start OFF after restore rather than reusing calendar IDs on a different device.

## 5. Reminders and background behavior

Prefer adding calendar context to an ordinary subscription reminder over generating another notification. Where a calendar-only review reminder is justified, use a separate identifiable job/notification namespace so disabling this feature cannot cancel ordinary reminders.

- Use one uniquely named calendar reconciliation job, created only after successful enablement. A proposed starting cadence is daily, with immediate refresh on foreground and calendar changes. Do not scan in a tight loop.
- Re-read enabled state, current permission, selection and a generation/version token before querying, persisting, posting, or following an old notification. Refresh relevant source data before issuing calendar-context notifications; on refresh failure, suppress contextual copy and let ordinary reminders continue.
- On disable/revocation, cancel calendar-only work and notifications. If an ordinary reminder contains calendar context, replace its content with the ordinary version while preserving its original schedule.
- Avoid duplicate reminders for the same decision. Limit calendar-only notifications to one in any rolling 24-hour period and once per subscription/occurrence/suggestion. Persist deduplication state without raw titles.
- Do not request exact-alarm access for this feature. Recommendations are advance notices, not exact-minute commitments. WorkManager execution depends on system scheduling and constraints; it is not an exact timer. See [Android background scheduling](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
- Respect notification permission and channel settings. Calendar opt-in is not notification consent. With notifications unavailable, retain in-app suggestions and a quiet Settings status. Android 13+ uses notification runtime permission; see [Android notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission).
- Reconcile time-zone/date changes, reboot and app restoration only when enabled. Never start calendar reads from application startup, boot handlers, injected constructors or ordinary reminder workers when OFF.

The existing `AlarmScheduler` has an exact-alarm failure path that only logs the error, and the daily worker uses fixed-cycle dates. Do not build new precise-deadline promises on those assumptions. Repair any shared scheduling behavior required for safe integration with explicit regression tests, but keep a broader alarm/billing-model redesign outside this feature.

## 6. Proposed code integration

Names below are implementation suggestions; preserve the existing module boundaries.

| Area | Work |
| --- | --- |
| `core/domain` | Add calendar access state, normalized occurrence, derived suggestion and dismissal identity models; repository contracts; a pure `GetCalendarAwareSuggestionsUseCase`. Keep Android provider types out of domain contracts. |
| `core/data` | Implement the provider adapter, DataStore opt-in/selection, minimal derived/dismissal storage and cleanup; Hilt bindings. Extend preference interfaces and fakes together. If using Room, add and test a migration rather than destructive recreation. |
| `core/ui` | Compact Settings/calendar selector sheets and optional suggestion presentation. Reuse `CancellationOptionsDialog` and `SubscriptionDetailBottomSheet`. |
| `feature/dashboard` | Add Settings entry; combine ordinary state with an optional suggestion. Extend `NextActionBanner` through presentation inputs/actions rather than querying the provider inside a composable. Preserve stable subscription-ID selection. |
| `app` | Runtime permission launcher, background reconciliation/lifecycle coordination, context-opening notification intents, backup exclusions and `READ_CALENDAR` manifest declaration. No `WRITE_CALENDAR` or new exact-alarm permission. |

Inspect these existing paths first:

- `core/domain/src/main/java/com/subtrack/core/domain/model/UserPreferences.kt`
- `core/domain/src/main/java/com/subtrack/core/domain/repository/UserPreferencesRepository.kt`
- `core/data/src/main/java/com/subtrack/core/data/preferences/UserPreferencesRepositoryImpl.kt`
- `feature/dashboard/src/main/java/com/subtrack/feature/dashboard/DashboardScreen.kt`
- `feature/dashboard/src/main/java/com/subtrack/feature/dashboard/DashboardViewModel.kt`
- `core/ui/src/main/java/com/subtrack/core/ui/components/NextActionBanner.kt`
- `app/src/main/java/com/subtrack/app/worker/WorkSchedulerManager.kt`
- `app/src/main/java/com/subtrack/app/worker/DailyCycleEvaluationWorker.kt`
- `app/src/main/java/com/subtrack/app/notification/AlarmScheduler.kt`
- `app/src/main/java/com/subtrack/app/ui/navigation/SubTrackNavHost.kt`
- `app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml`

The existing 45d Planner remains unchanged; do not expand it with event-entry or manual availability features. See [cancellation integration and pending device tests](CANCELLATION_DEEP_LINKS_FEATURE.md) for the action flow to reuse.

## 7. Implementation sequence

1. Confirm current checkout and applicable instructions. Preserve unrelated README/architecture edits. Establish baseline tests and keep this task scoped to the approved product contract.
2. Implement opt-in state, device-local persistence and cleanup first, with tests proving zero calendar access while OFF, including restart/boot and an in-flight disable race.
3. Add the provider adapter and fake data source. Test occurrence handling, filtering, permission loss and stale-source invalidation before UI wiring.
4. Implement the pure rule engine and dismissal logic with fixed clocks and synthetic events. Include negative cases, not just matches.
5. Integrate the minimal Settings sheet and single optional Dashboard suggestion; wire existing review/cancellation actions. Verify ordinary UI is unchanged when disabled.
6. Add gated reconciliation, notification deduplication and disable cleanup. Verify ordinary reminders survive every calendar-state transition.
7. Run unit/build checks and device acceptance tests. Update this document with actual results, source revision, limitations and remaining cases. Only mark implemented/device-verified to the extent demonstrated.

## 8. Acceptance and testing handoff

The initial documentation-only handoff had no calendar implementation evidence. Current implementation and device results are recorded below; existing cancellation results are treated only as regression coverage and do not substitute for calendar-specific checks.

| Test group | Required evidence |
| --- | --- |
| OFF by default | Fresh install/upgrade with calendar permission already granted: zero provider queries, observers, calendar jobs, derived cards or notifications. Ordinary subscriptions still work. |
| Enable/cancel/deny | Permission requested only from Settings; no event read before selection; cancelled/empty/denied setup stays OFF without repeated prompts. |
| Selected calendars | Only selected IDs queried; duplicate calendar names distinguishable; no access to a newly added account without selection. |
| Disable race | Pause a fake query, disable, then complete it: no persisted suggestions or notifications return. Ordinary reminder scheduling is retained. |
| Permission revoked/provider missing | No crash or stale calendar advice; cleanup is performed on next execution; status only in Settings. |
| Rule precision | Trip with a relevant upcoming action yields one neutral card; ambiguous meetings, short trips, unsupported titles and unrelated events yield none. Cloud/work tools are not cancellation targets based on travel. |
| Festival/holiday | Recognized event plus projected entertainment blackout can produce review; no automatic activation, inferred observance or fabricated savings. |
| Event correctness | Repetition/exceptions, cancellations, all-day exclusive end dates, midnight, DST, time-zone travel, deleted events and duplicate instances covered. |
| Dismissal/deduplication | Dismiss survives refresh/restart/title edits; next recurring occurrence can qualify; one card and the notification cap are respected. |
| Review actions | Correct subscription opens; link navigation and dismiss never change `isAutoRenewCancelled`, schedules, cost or record count. |
| Notification restrictions | Permission/channel disabled, battery saver, Doze, reboot and force-stop: no exact-delivery claim; in-app state reconciles when execution resumes. |
| Restore/backup | Calendar titles/derived context not exported; consent and provider IDs do not silently reactivate on another device. |
| Minimal/accessibility UI | No new navigation tab/manual forms/permanent section; OFF dashboard parity; small screen, large text, landscape and TalkBack usable. |

Use JVM fakes for logic and lifecycle races; provider integration tests with synthetic calendar fixtures; Compose tests for opt-in/selection/card behavior. Tests must not edit or delete a user's personal calendar. Prefer an isolated emulator/test calendar for seeded events and clean up only fixtures the test created.

Device coverage: API 26, API 33 permission behavior, API 35 and a current Android release; Pixel and Samsung plus another vendor when available. Include a device/emulator without a synced calendar or Google services. Record OS, provider, accessible account type, permission state and outcomes. Emulator proof does not establish vendor background-delivery reliability. Do not claim universal calendar-account visibility or guaranteed notifications on every device.

Suggested local checks after implementation (adjust for any added modules/tests):

```powershell
.\gradlew.bat :core:domain:testDebugUnitTest :core:data:testDebugUnitTest :feature:dashboard:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
```

On this Windows host the previously verified Gradle loopback workaround uses process-local `TEMP`/`TMP` at `C:\Temp\subcycler-gradle` and `--no-daemon`; see the cancellation handoff for commands. Revalidate the toolchain rather than changing global settings. Leave device-only checks pending if no device is available; do not silently substitute compilation for execution.

### Implementation validation — 2026-09-14

Implemented from source baseline `3616867` in the current working tree. The feature remains opt-in and uses only `READ_CALENDAR`; it does not request `WRITE_CALENDAR` or add calendar mutation paths.

Local validation passed with the process-local Gradle workaround described above:

```powershell
$env:TEMP='C:\Temp\subcycler-gradle'
$env:TMP='C:\Temp\subcycler-gradle'
.\gradlew.bat :core:domain:testDebugUnitTest :core:data:testDebugUnitTest :feature:dashboard:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon --console=plain
```

Result: `BUILD SUCCESSFUL` in 1m 5s; 301 tasks, 56 executed and 245 up-to-date. The focused JVM coverage includes conservative classification, negative cases, selected-ID query gating, no occurrence reads while OFF/configuring, disable-vs-refresh generation races, notification identity/capping, all-day exclusive ends, timed midnight/DST normalization, and Dashboard setup/disable coordination.

ADB validation used a connected Huawei WAS-LX2J running Android 8.0/API 26:

- Rebuilt app and test APKs installed successfully with `adb install -r`.
- The complete instrumentation suite passed on the final rebuilt artifacts: `OK (10 tests)` in 9.753s. Four tests cover calendar UI/manifest behavior; six existing cancellation tests remained green.
- The installed-upgrade flow showed the Settings switch OFF initially. Toggling it first showed the local-processing explanation; calendar permission was requested only after continuing.
- The real Calendar Provider selector distinguished a selected Google calendar from an unchecked Phone calendar and showed account/provider context. No calendar event was created, edited or deleted.
- After disabling, Settings showed OFF and the calendar DataStore contained only `enabled=false` plus the incremented generation token. Selected IDs, derived suggestions, dismissals and notification state were absent. Android's `READ_CALENDAR` grant remained, as intended; `WRITE_CALENDAR` was absent. The ordinary daily subscription WorkManager job remained scheduled.

Not yet demonstrated: deriving a visible suggestion from a seeded real-provider occurrence, recurrence/exception behavior against multiple vendor providers, Android 15 permission behavior, Samsung coverage, TalkBack/large-text/landscape checks, or reliable background notification timing under Doze/force-stop. These remain device/provider acceptance work and are not inferred from compilation or the Huawei/Pixel passes.

### Pixel 4 (Android 13 / API 33) Device Validation — 2026-11-18

ADB and `android layout` validation used a connected **Google Pixel 4 (`flame`) running Android 13 (API 33)**:

- Full instrumentation test suite (`CalendarAwarenessComponentsTest`, `CancellationLinkLauncherTest`, `CancellationOptionsDialogTest`) passed on-device: `OK (12 tests)` in 8.5s.
- Verified the opt-in flow on Android 13:
  1. Settings opens with **Calendar-aware suggestions** OFF by default.
  2. Toggling ON displays the local-processing explanation dialog before requesting Android's `READ_CALENDAR` runtime permission.
  3. Granting `READ_CALENDAR` loads the `Choose calendars` sheet with all visible Google calendars and account contexts (`<redacted>@gmail.com · com.google`), all unchecked initially and `Use selected` disabled until at least one calendar is checked.
  4. Selecting two calendars (`Holidays in Japan`, `Holidays in Nepal`) and tapping `Use selected` persists the selection and updates Settings to show `Calendars · 2 selected` (`Change`) alongside the quiet notification-disabled notice when `POST_NOTIFICATIONS` is off.
- **UX Bug Caught & Fixed on Pixel 4**: When a device has 5+ calendars across multiple accounts, `CalendarSelectionSheet` previously opened in Material 3's partially-expanded bottom sheet state, pushing the `Cancel` and `Use selected` buttons below the screen fold until manually dragged up. Fixed by setting `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, applying `Modifier.heightIn(max = 360.dp)` to the scrollable calendar list, and aligning both `CalendarSettingsSheet` and `CalendarSelectionSheet` with the `#0F0F0F` OLED Obsidian surface palette.

## 9. Completion criteria

The implementation is complete when opt-in gates all calendar access, the optional suggestions work from selected real provider data, OFF cleanup cannot disturb ordinary reminders, the UI remains minimal, and test evidence plus device limitations are recorded. Any broader calendar sync, manual-plan UI, automatic cancellation, cloud classification or new savings computation requires a separate product decision.
