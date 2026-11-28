package com.subtrack.app.onboarding

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import com.subtrack.core.ui.theme.SubTrackTheme
import com.subtrack.feature.onboarding.OnboardingAction
import com.subtrack.feature.onboarding.OnboardingScreen
import com.subtrack.feature.onboarding.OnboardingUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class WelcomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun welcomeDefaultsToAdaptiveMoodLocksManualIconAndUnlocksCustomThemes() {
        var state by mutableStateOf(OnboardingUiState(currencyCode = "JPY"))
        var darkTheme by mutableStateOf(true)

        compose.setContent {
            val swirl = BrandColorHarmonizer.resolveSwirlPetalColors(
                activeSubscriptions = emptyList(),
                useStackMood = state.useStackHeroMood,
                fallbackIcon = state.selectedLauncherIcon
            )
            SubTrackTheme(
                darkTheme = darkTheme,
                accentIcon = state.selectedLauncherIcon,
                swirlColorsOverride = swirl
            ) {
                OnboardingScreen(
                    uiState = state,
                    onAction = { action ->
                        when (action) {
                            is OnboardingAction.SetUseStackHeroMood -> {
                                state = state.copy(
                                    useStackHeroMood = action.enabled,
                                    selectedLauncherIcon = if (action.enabled) {
                                        AppLauncherIcon.BOTANICAL
                                    } else {
                                        state.selectedLauncherIcon
                                    }
                                )
                            }
                            is OnboardingAction.SetLauncherIcon -> {
                                state = state.copy(
                                    useStackHeroMood = false,
                                    selectedLauncherIcon = action.icon
                                )
                            }
                            else -> Unit
                        }
                    }
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previewDir = (context.getExternalFilesDir("welcome_previews")
            ?: File(context.cacheDir, "welcome_previews")).apply { mkdirs() }

        compose.waitForIdle()
        compose.onNodeWithText("Matches your subscriptions").assertIsDisplayed()
        compose.onNodeWithText("App icon follows active mood").assertIsDisplayed()
        compose.onNodeWithText("30d active").assertIsDisplayed()
        compose.onNodeWithText("Build your stack →").assertIsDisplayed()
        saveCapture(File(previewDir, "welcome_adapt_dark.png"))

        // Switch to Custom mode -> manual theme/icon picker unlocks
        compose.onNodeWithText("Custom").performClick()
        compose.waitForIdle()
        assertFalse(state.useStackHeroMood)
        compose.onNodeWithText("Cinema").assertIsDisplayed()
        compose.onNodeWithText("Cinema").performClick()
        compose.waitForIdle()
        assertEquals(AppLauncherIcon.CINEMA, state.selectedLauncherIcon)
        saveCapture(File(previewDir, "welcome_custom_cinema_dark.png"))

        // Capture light mode Custom -> Ocean
        compose.runOnUiThread { darkTheme = false }
        compose.onNodeWithText("Ocean").performClick()
        compose.waitForIdle()
        assertEquals(AppLauncherIcon.OCEAN, state.selectedLauncherIcon)
        saveCapture(File(previewDir, "welcome_custom_ocean_light.png"))

        // Switch back to Auto -> locks back to adaptive mood
        compose.onNodeWithText("Auto").performClick()
        compose.waitForIdle()
        assertTrue(state.useStackHeroMood)
        compose.onNodeWithText("Matches your subscriptions").assertIsDisplayed()
        saveCapture(File(previewDir, "welcome_adapt_light.png"))
    }

    private fun saveCapture(file: File) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
    }
}
