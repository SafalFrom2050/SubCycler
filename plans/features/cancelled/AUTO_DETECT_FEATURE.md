# Feature Specification: Auto-Detect Installed Subscriptions & Native Icons 📱✨

> **Archived File**: `plans/features/cancelled/AUTO_DETECT_FEATURE.md`
> **Status**: Cancelled
> **Target Android Versions**: Android 8.0 (API 26) to Android 15 (API 35)
> **Compliance**: 100% Google Play Store Compliant (Zero Dangerous Permissions)

> **Cancellation note (2026-08-30):** Replaced by the explicit, privacy-first [First-Launch Service Stack Onboarding](../ONBOARDING_SERVICE_PICKER_FEATURE.md). Installed-app presence cannot prove an active paid subscription, price, or billing status, and the service picker provides a faster path without package-visibility review risk.

---

## 1. Executive Summary & Vision

Typing subscription names, pricing, and cycle details manually creates onboarding friction. The **Auto-Detect Feature** allows SubCycler to scan the user's device for known subscription apps (Netflix, Spotify, Disney+, ChatGPT, YouTube, etc.) in under 15ms.

It presents an interactive **"Scan & 1-Tap Import"** checklist that pre-populates:
- The app's **official high-resolution native icon** and localized app title from the OS.
- Suggested pricing, categories, and optimal rotation strategies (e.g. 45-day cycle vs continuous).
- One-tap batch import directly into the Room SQLite database.

---

## 2. Privacy & Google Play Store Compliance

### ❌ What We Avoid: `QUERY_ALL_PACKAGES`
Google Play restricts `android.permission.QUERY_ALL_PACKAGES` to device launchers, antivirus suites, and file managers. Using this permission would cause app rejection during review.

### ✅ What We Use: Manifest `<queries>` Filtering
We explicitly declare a curated list of target subscription package names in `AndroidManifest.xml` via the `<queries>` element. This:
1. Requires **zero runtime permissions** or dangerous manifests.
2. Complies **100% with Google Play's Package Visibility policy**.
3. Keeps device scanning lightning-fast (<15ms) and 100% local/offline.

---

## 3. Architecture & Modular Breakdown

Following the project's Clean Architecture & SOLID guidelines:

```
SubCycler
 ├── :core:domain
 │    ├── model/InstalledApp.kt               (Domain model for detected apps)
 │    ├── repository/AppScannerRepository.kt  (Interface for scanning device apps)
 │    └── usecase/ScanInstalledAppsUseCase.kt (Cross-references detected packages with PresetLibrary)
 │
 ├── :core:data
 │    ├── scanner/AndroidAppScannerImpl.kt    (PackageManager implementation with <queries>)
 │    └── di/ScannerModule.kt                 (Hilt binding for AppScannerRepository)
 │
 ├── :core:ui
 │    ├── components/InstalledAppIcon.kt      (Composable for rendering native Bitmap icons)
 │    └── dialogs/AutoDetectImportSheet.kt    (OLED Obsidian multi-select import bottom sheet)
 │
 └── :feature:dashboard
      ├── DashboardViewModel.kt               (Handles ScanIntent, DetectedApps state, BatchImport)
      └── DashboardScreen.kt                  (Adds "Scan Installed" banner/action button)
```

---

## 4. Manifest Package Declarations (`AndroidManifest.xml`)

Add the `<queries>` block directly inside `<manifest>`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Target Package Declarations for Subscription Detection -->
    <queries>
        <!-- Video Streaming -->
        <package android:name="com.netflix.mediaclient" />
        <package android:name="com.disney.disneyplus" />
        <package android:name="com.wbd.stream" /> <!-- Max -->
        <package android:name="com.hbo.hbonow" /> <!-- Max Legacy -->
        <package android:name="com.amazon.avod.thirdpartyclient" /> <!-- Prime Video -->
        <package android:name="com.google.android.youtube" />
        <package android:name="com.apple.atve.androidtv.appletv" /> <!-- Apple TV+ -->
        <package android:name="com.paramountplus" />
        <package android:name="com.peacocktv.peacockandroid" />
        <package android:name="com.crunchyroll.crunchyroid" />
        <package android:name="com.hulu.plus" />

        <!-- Music & Podcasts -->
        <package android:name="com.spotify.music" />
        <package android:name="com.apple.android.music" />
        <package android:name="deezer.android.app" />
        <package android:name="com.tidal.mobile" />
        <package android:name="com.audible.application" />

        <!-- AI & Productivity -->
        <package android:name="com.openai.chatgpt" />
        <package android:name="com.anthropic.claude" />
        <package android:name="com.microsoft.copilot" />
        <package android:name="com.duolingo" />
        <package android:name="com.notion.id" />

        <!-- Gaming & Cloud -->
        <package android:name="com.gamepass" /> <!-- Xbox Game Pass -->
        <package android:name="com.scee.psxandroid" /> <!-- PlayStation App -->
        <package android:name="com.google.android.apps.subscriptions.red" /> <!-- Google One -->
    </queries>

</manifest>
```

---

## 5. Domain Models & Use Case Logic

### A. Domain Model (`:core:domain`)
```kotlin
package com.subtrack.core.domain.model

data class DetectedSubscriptionApp(
    val packageName: String,
    val appName: String,
    val preset: SubscriptionPreset?,
    val isAlreadyAdded: Boolean,
    val isSelectedForImport: Boolean = true
)
```

### B. Repository Contract (`:core:domain`)
```kotlin
package com.subtrack.core.domain.repository

import com.subtrack.core.domain.model.DetectedSubscriptionApp

interface AppScannerRepository {
    suspend fun getInstalledSubscriptionApps(): List<DetectedSubscriptionApp>
    fun getAppIconBitmap(packageName: String, sizePx: Int = 120): Any?
}
```

### C. Use Case (`:core:domain`)
```kotlin
package com.subtrack.core.domain.usecase

import com.subtrack.core.domain.model.DetectedSubscriptionApp
import com.subtrack.core.domain.repository.AppScannerRepository
import com.subtrack.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ScanInstalledSubscriptionsUseCase @Inject constructor(
    private val scannerRepository: AppScannerRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(): List<DetectedSubscriptionApp> {
        val existingSubscriptions = subscriptionRepository.getAllSubscriptions().first()
        val existingNames = existingSubscriptions.map { it.name.lowercase().trim() }.toSet()

        val detected = scannerRepository.getInstalledSubscriptionApps()

        return detected.map { app ->
            val alreadyInDb = existingNames.contains(app.appName.lowercase().trim()) ||
                    (app.preset != null && existingNames.contains(app.preset.name.lowercase().trim()))
            app.copy(
                isAlreadyAdded = alreadyInDb,
                isSelectedForImport = !alreadyInDb
            )
        }
    }
}
```

---

## 6. Data Layer Implementation (`:core:data`)

```kotlin
package com.subtrack.core.data.scanner

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import com.subtrack.core.domain.model.DetectedSubscriptionApp
import com.subtrack.core.domain.model.PresetLibrary
import com.subtrack.core.domain.repository.AppScannerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppScannerRepository {

    private val packageToPresetMap = mapOf(
        "com.netflix.mediaclient" to "Netflix",
        "com.disney.disneyplus" to "Disney+",
        "com.wbd.stream" to "Max (HBO)",
        "com.hbo.hbonow" to "Max (HBO)",
        "com.google.android.youtube" to "YouTube Premium",
        "com.spotify.music" to "Spotify",
        "com.apple.atve.androidtv.appletv" to "Apple TV+",
        "com.amazon.avod.thirdpartyclient" to "Amazon Prime Video",
        "com.openai.chatgpt" to "ChatGPT Plus",
        "com.gamepass" to "Xbox Game Pass Ultimate",
        "com.scee.psxandroid" to "PlayStation Plus"
    )

    override suspend fun getInstalledSubscriptionApps(): List<DetectedSubscriptionApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val results = mutableListOf<DetectedSubscriptionApp>()

        packageToPresetMap.forEach { (pkg, presetName) ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val preset = PresetLibrary.presets.find { it.name.equals(presetName, ignoreCase = true) }

                results.add(
                    DetectedSubscriptionApp(
                        packageName = pkg,
                        appName = label,
                        preset = preset,
                        isAlreadyAdded = false
                    )
                )
            } catch (_: PackageManager.NameNotFoundException) {
                // App not installed on device
            }
        }
        results
    }

    override fun getAppIconBitmap(packageName: String, sizePx: Int): Bitmap? {
        return try {
            val pm = context.packageManager
            val drawable: Drawable = pm.getApplicationIcon(packageName)
            drawable.toBitmap(width = sizePx, height = sizePx, config = Bitmap.Config.ARGB_8888)
        } catch (_: Exception) {
            null
        }
    }
}
```

---

## 7. UI / UX Design (OLED Obsidian Precision)

### UI Flow & Styling
1. **Trigger**:
   - On the primary Dashboard, when the list is empty OR via a dedicated TopBar icon button (`Icons.Default.AutoAwesome` with label *"Scan Installed Apps"*).
2. **Bottom Sheet (`AutoDetectImportSheet.kt`)**:
   - **Background**: `#0F0F0F` with hairline border `Color.White.copy(alpha = 0.08f)`.
   - **Detected Item Row**:
     - 40x40dp rounded native app icon.
     - App name (`15sp`, Bold, `#EDEDED`) + Preset Category & Price (`12sp`, Zinc `#8E8E93`).
     - Strategy badge: "45-Day Cycle" (`PrimarySky`) or "Continuous" (`Zinc`).
     - Selection checkbox with `PrimarySky` (`#38BDF8`) tint.
     - Disabled state for already-added services: *"Already in your portfolio"*.
   - **Hero Action Button**:
     - `PrimarySky` solid pill: *"Import Selected (3 Services)"*.

```
┌────────────────────────────────────────────────────────┐
│  ✨ Auto-Detected Subscriptions                       │
│  Found 4 supported services installed on this device   │
├────────────────────────────────────────────────────────┤
│  [✓] 🎬 Netflix            $15.49/mo  [ 45d Cycle ]    │
│  [✓] 🎵 Spotify            $11.99/mo  [ Continuous ]   │
│  [✓] 📺 Disney+            $13.99/mo  [ 45d Cycle ]    │
│  [ ] 🤖 ChatGPT Plus       $20.00/mo  [ 45d Cycle ]    │
│  [-] ☁️ Google One         (Already added)             │
├────────────────────────────────────────────────────────┤
│          [ Import 3 Subscriptions • Save $68/yr ]       │
└────────────────────────────────────────────────────────┘
```

---

## 8. Verification & Testing Plan

### Automated Unit Tests
- `ScanInstalledSubscriptionsUseCaseTest`:
  - Mock `AppScannerRepository` returning mock packages.
  - Verify that already added subscriptions are marked `isAlreadyAdded = true` and `isSelectedForImport = false`.
  - Verify batch addition saves all entities with default cycle dates (`startDate = today`).

### Manual Testing Matrix
| Test Case | Expected Result |
| :--- | :--- |
| **Fresh Install with Netflix & Spotify on phone** | Modal shows both apps pre-checked with official icons. |
| **Device with no supported subscription apps** | Friendly empty-state: *"No supported subscription apps found on device. Tap (+) to add manually."* |
| **All detected apps already in SubCycler** | Items marked as "Already Added", Import button disabled until a new one is selected. |
| **Dark Theme Fidelity** | Perfect OLED Obsidian contrast, crisp native icon corners, no layout shift. |

---

## 9. Implementation Checklist

- [ ] Add `<queries>` block to `app/src/main/AndroidManifest.xml`.
- [ ] Create `DetectedSubscriptionApp.kt` in `:core:domain:model`.
- [ ] Define `AppScannerRepository.kt` in `:core:domain:repository`.
- [ ] Create `ScanInstalledSubscriptionsUseCase.kt` in `:core:domain:usecase`.
- [ ] Implement `AndroidAppScannerImpl.kt` and `ScannerModule.kt` in `:core:data`.
- [ ] Build `InstalledAppIcon.kt` and `AutoDetectImportSheet.kt` in `:core:ui`.
- [ ] Wire scan actions into `DashboardViewModel.kt` and `DashboardScreen.kt`.
- [ ] Add unit tests for use cases and scanner mappings.
