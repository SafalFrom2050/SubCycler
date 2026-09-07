package com.subtrack.app.ui.navigation

import com.subtrack.app.BuildConfig
import javax.inject.Inject

class AppLaunchConfig(
    val forceOnboardingOnEveryLaunch: Boolean
) {
    @Inject
    constructor() : this(
        forceOnboardingOnEveryLaunch = BuildConfig.FORCE_ONBOARDING_ON_EVERY_LAUNCH
    )
}
