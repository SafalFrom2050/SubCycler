# Cancellation links: integration and testing handoff

Status: Implemented; device acceptance testing pending.
Implementation date: 2026-09-08. Base: `8d04a12` on `main`.

## User flow

1. Open Dashboard and tap a subscription tile.
2. Tap **Cancellation options** in its detail sheet. The sheet scrolls on smaller displays.
3. Choose the service account/help page, **Google Play subscriptions**, or **Apple billing instructions**, according to the receipt. Apple routes are deduplicated for Apple Music/TV.
4. Android opens the HTTPS link in the user's browser, an app that handles that link, or the system chooser. The provider handles authentication and cancellation.
5. Return to SubCycler and tap **Done**. Opening/returning/dismissing never changes stored subscription state. For active rotating subscriptions, the existing **Mark auto-renew as cancelled** checkbox remains a separate manual action after provider confirmation.

This is assisted navigation, not automatic cancellation. An accepted Android intent is not evidence of cancellation or even successful page loading. No WebView, credential collection, cloud AI, API keys, background task, package scanning, new permission, or database migration was added.

For an unknown/custom service, the dialog gives instructions to visit its official billing page and still offers Google Play/Apple. It never constructs a URL from user input or guesses a service by substring. Carrier/partner billing has explicit guidance. If Android cannot launch the link, the dialog displays the URL and a **Copy link** action; retrying successfully clears the error.

## Integration points

| File | Responsibility |
| --- | --- |
| `core/domain/src/main/java/com/subtrack/core/domain/model/CancellationLinks.kt` | Pure Kotlin destination catalog, explicit aliases, billing alternatives and deduplication. |
| `core/ui/src/main/java/com/subtrack/core/ui/components/CancellationOptionsDialog.kt` | Scrollable billing chooser, launch-error/copy feedback; `openCancellationDestination` launches `ACTION_VIEW` + `CATEGORY_BROWSABLE` and catches missing/blocked handlers. |
| `core/ui/src/main/java/com/subtrack/core/ui/components/SubscriptionDetailBottomSheet.kt` | Entry button available for every strategy/phase; preserves existing manual checkbox rules. Dialog visibility is saveable and keyed by subscription ID. |
| `feature/dashboard/src/main/java/com/subtrack/feature/dashboard/DashboardScreen.kt` | Stores selected subscription ID and derives the sheet's status from current `uiState`, fixing stale checkbox state after repository updates. |
| `core/domain/src/test/java/com/subtrack/core/domain/model/CancellationLinksTest.kt` | Catalog coverage, aliases/locale, custom-name handling, billing choices, HTTPS/official-host invariants. |
| `app/src/androidTest/java/com/subtrack/app/cancellation/` | Compose interaction and Android intent/error tests with fake launchers. No real provider accounts are touched by these tests. |

The navigation function has no repository dependency and does not dispatch `ToggleAutoRenew`. The flow is entered from Dashboard; planner/rotator screens and notifications do not get additional buttons in this change. The existing checkbox remains limited to active, non-continuous subscriptions. Continuous/blackout subscriptions can open cancellation options but this feature does not change their rotation or tracking semantics.

`ACTION_VIEW` is launched without `resolveActivity` or an installed-app query. This follows [Android's package-visibility guidance](https://developer.android.com/training/package-visibility/use-cases#open-urls): handle `ActivityNotFoundException`, without adding `<queries>`. `SecurityException` also produces the fallback. Only HTTPS destinations with a host are accepted; production callers supply curated catalog entries. No arbitrary URL editor is exposed.

## Catalog and official references

All 18 current onboarding presets have a destination. These are account pages where available and official instructions where a reliable direct cancellation route is not appropriate. Public documentation was checked on 2026-09-08; authenticated landing pages, redirects, regional availability and real cancellation outcomes have NOT been tested on Android.

| Preset(s) | Destination | Official reference |
| --- | --- | --- |
| Netflix | `https://www.netflix.com/cancelplan` | [Netflix cancellation](https://help.netflix.com/en/node/407) |
| Disney+ | `https://www.disneyplus.com/account` | [Disney+ cancellation](https://help.disneyplus.com/en-GB/article/disneyplus-en-pt-cancel) |
| Max (HBO) | `https://www.hbomax.com/subscription` | [HBO Max cancellation](https://help.hbomax.com/tv-en/Answer/Detail/000002526) |
| YouTube Premium | `https://www.youtube.com/paid_memberships` | [YouTube cancellation](https://support.google.com/youtube/answer/6308278) |
| Spotify | `https://www.spotify.com/account/subscription/manage/` | [Spotify cancellation](https://support.spotify.com/us/article/cancel-premium/) |
| Apple Music, Apple TV+ | `https://support.apple.com/118428` | [Apple cancellation instructions](https://support.apple.com/118428) |
| Amazon Prime Video | `https://www.primevideo.com/help?nodeId=GWGDSNXVPJ93UW5V` | [Prime Video instructions](https://www.primevideo.com/help?nodeId=GWGDSNXVPJ93UW5V); distinguishes standalone Video from a bundled Prime membership. |
| ChatGPT Plus | `https://help.openai.com/en/articles/7232927` | [ChatGPT cancellation instructions](https://help.openai.com/en/articles/7232927) |
| Claude Pro | `https://support.claude.com/en/articles/8325617` | [Claude cancellation instructions](https://support.claude.com/en/articles/8325617) |
| Google AI Plus/Pro/Ultra, Google One | `https://one.google.com/settings` | [Google One membership cancellation](https://support.google.com/googleone/answer/9056360); confirm AI-plan/account routing during device QA. |
| Notion Plus | `https://www.notion.com/help/upgrade-or-downgrade-your-plan` | [Notion plan instructions](https://www.notion.com/help/upgrade-or-downgrade-your-plan) |
| Xbox Game Pass Ultimate | `https://account.microsoft.com/services` | [Microsoft cancellation](https://support.microsoft.com/en-us/accounts-billing/subscriptions/cancel-your-microsoft-subscription) |
| PlayStation Plus | `https://www.playstation.com/en-us/support/store/cancel-ps-store-subscription/` | [PlayStation instructions](https://www.playstation.com/en-us/support/store/cancel-ps-store-subscription/) |
| iCloud+ | `https://support.apple.com/108318` | [iCloud+ downgrade/cancellation](https://support.apple.com/108318); may require another device. |
| Any service billed through Google Play | `https://play.google.com/store/account/subscriptions` | [Google Play cancellation](https://support.google.com/googleplay/answer/7018481) |
| Any service billed through Apple | `https://support.apple.com/118428` | [Apple cancellation](https://support.apple.com/118428) |

Aliases are explicit in `CancellationLinks.kt`. Renaming a subscription outside those aliases falls back to custom-service guidance. Billing channel is not persisted or inferred. To add/change a provider, verify its official instructions, update the catalog and this table, then rerun catalog tests and its device checks. Never add cancellation execution endpoints, third-party cancellation brokers, account-specific URLs, or tokens.

## Local verification

Final local verification on 2026-09-08: **BUILD SUCCESSFUL** (1m 27s on the final incremental run). Physical-device and emulator execution are deliberately deferred because the user has no phone available.

| Check | Result |
| --- | --- |
| `:core:domain:testDebugUnitTest` | PASS: 15 tests, including 5 new cancellation catalog tests; zero failures/errors/skips. |
| `:feature:dashboard:testDebugUnitTest` | PASS: 5 tests; zero failures/errors/skips. |
| `:app:assembleDebug` | PASS: app APK built with the integrated flow. |
| `:app:assembleDebugAndroidTest` | PASS: test APK built, including all 6 new instrumentation tests. This is compilation/packaging proof only. |
| `git diff --check` | PASS. Existing Windows LF/CRLF normalization notices are informational. |
| Six instrumentation tests | NOT RUN: require a device/emulator. |
| Device UI, lifecycle, browser routing, provider authentication/cancellation | NOT RUN. Use the checklist below. |

Unit-test HTML reports: `core/domain/build/reports/tests/testDebugUnitTest/index.html` and `feature/dashboard/build/reports/tests/testDebugUnitTest/index.html`. Generated APKs/reports are ignored build outputs and are reproducible with the commands below. A Compose test import error found during the first compilation was corrected before the successful final build.

```powershell
Set-Location 'D:\Workspace\antigravity projects\subscriptions-tracker'
.\gradlew.bat :core:domain:testDebugUnitTest :feature:dashboard:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
```

On this Windows host, the first attempt failed before compilation with `java.io.IOException: Unable to establish loopback connection`. Retry in a fresh PowerShell process using a short process-local temporary directory (no persistent environment changes):

```powershell
$env:TEMP = 'C:\Temp\subcycler-gradle'
$env:TMP = 'C:\Temp\subcycler-gradle'
New-Item -ItemType Directory -Force -Path $env:TEMP | Out-Null
.\gradlew.bat :core:domain:testDebugUnitTest :feature:dashboard:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon --console=plain
```

Environment observed: JDK 17 at `C:\jdk-17`, Gradle wrapper 8.7, Android SDK from ignored `local.properties`, compile/target SDK 35, minimum SDK 26. Android CLI is installed and `android info` works with access to its local cache.

## Next-agent test checklist

Start by reading this file, checking `git status --short --branch`, and confirming the reviewed source revision/diff. The pre-existing local changes in README.md and ARCHITECTURE.md belong to the user. Do not discard them.

1. **Prepare a device or emulator.** Use an API 26 device/emulator for minimum-SDK coverage and API 35+ for modern link handling; record Android version, locale, browser, and whether Google Play/provider apps are installed. An emulator is sufficient for fake interaction tests. Do not assume an emulator proves physical browser/provider behavior.
2. **Build and install.** Use the command above. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Run `android info`, then `android install --device=<serial> --apks="app/build/outputs/apk/debug/app-debug.apk"`. If signing conflicts exist, use an isolated emulator or obtain approval before uninstalling any populated app.
3. **Run the six instrumentation tests.** These fake navigation and never open live subscription accounts:

   ```powershell
   .\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.package=com.subtrack.app.cancellation' --console=plain
   ```

4. **Navigate from the real Dashboard.** Debug builds currently force onboarding on each new app launch (`FORCE_ONBOARDING_ON_EVERY_LAUNCH=true` in `app/build.gradle.kts`). Complete/skip it; do not mistake this existing QA switch for a cancellation regression. Create Netflix on an active rotation, Spotify continuous, a blackout-phase subscription, Apple Music, and a custom service named `My gym`.
5. **Check the matrix below.** Capture `android layout --pretty` / `android layout --diff` and screenshots where useful. For live provider pages, stop at login or the cancellation page. Do not cancel a real paid account unless the user explicitly authorizes that separate action.
6. **Record results** in a dated section of this document: device/OS, source revision, each case PASS/FAIL/BLOCKED, final public URL/redirect, screenshots, and exact reproduction steps. Redact account identifiers; never record passwords, cookies or payment details. Rerun affected tests after fixes.

| Scenario | Acceptance criteria |
| --- | --- |
| Netflix/service direct route | Detail -> Cancellation options -> correct provider destination; provider/browser opens; Back returns to dialog/sheet. |
| Google Play route, with and without Play app | Correct subscription-management URL is handed to Android; browser fallback/chooser works; user can choose the billed account. |
| Apple Music/TV and iCloud+ | No duplicate Apple button; instructions load; other-device requirements are understandable. |
| Every other catalog destination | Confirm provider-owned landing page and regional redirects. Record any page that is inaccessible or demands desktop use; don't claim authenticated completion. |
| Unknown/custom names, including `Netflix alternative` | Generic direct-billing guidance plus store options; no guessed Netflix link or URL from input. |
| Carrier/partner billing | Guidance clearly directs user to their billing partner instead of suggesting the service route always works. |
| Browser failure | Instrumentation test exercises no-handler and security failures; dialog offers exact URL, Copy link and retry, without crashing. |
| Offline/browser error page | Provider load may fail after Android accepts the intent. App does not report successful cancellation or modify records. |
| Return without cancelling | `isAutoRenewCancelled`, schedule, cost and saved subscription count stay unchanged. Reopen app/detail to verify persistence. |
| Manual checkbox on active rotation | After closing dialog, toggling updates sheet immediately from current state; closing/reopening retains value; a second toggle reverses it. This only updates tracking. |
| Already marked / continuous / blackout | Cancellation options remain accessible. Existing checkbox visibility and cycle behavior are preserved. No automated status or phase changes. |
| Configuration change, background, process recreation | Sheet/dialog restoration does not launch a URL or toggle a flag automatically. Correct service remains selected when restored. |
| Small display, landscape, large fonts, TalkBack | Detail and dialog scroll; all provider choices, error/copy action, checkbox and Done are reachable; labels are announced. |

## Release gate and limitations

Do not label this feature device-verified until the above results are recorded. Direct links can change and store/partner routing depends on the user's actual billing account. Some providers expose only instructions or require a desktop/Apple device. The existing fixed-day billing model, savings math and cycle confirmation semantics are outside this change. Billing-channel persistence, custom URLs, notification shortcuts, AI cancellation and automatic result verification remain future work.
