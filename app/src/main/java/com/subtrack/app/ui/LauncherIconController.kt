package com.subtrack.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.subtrack.core.domain.model.AppLauncherIcon

/**
 * Safely switches between SubCycler's pre-declared curated `<activity-alias>` launcher icons.
 *
 * Always enables the target alias BEFORE disabling the previously active aliases using
 * [PackageManager.DONT_KILL_APP] so launchers never observe a zero-launcher-icon state.
 */
object LauncherIconController {

    fun applyLauncherIcon(context: Context, selected: AppLauncherIcon): Boolean {
        val pm = context.packageManager
        val packageName = context.packageName
        var changed = false

        runCatching {
            val targetComponent = ComponentName(packageName, selected.aliasClassName)
            val currentState = pm.getComponentEnabledSetting(targetComponent)
            val isAlreadyEnabled = currentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (currentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && selected == AppLauncherIcon.BOTANICAL)

            if (!isAlreadyEnabled) {
                pm.setComponentEnabledSetting(
                    targetComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                changed = true
            }

            AppLauncherIcon.entries
                .filter { it != selected }
                .forEach { other ->
                    val otherComponent = ComponentName(packageName, other.aliasClassName)
                    val otherState = pm.getComponentEnabledSetting(otherComponent)
                    val shouldDisable = otherState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
                        !(otherState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && other != AppLauncherIcon.BOTANICAL)
                    if (shouldDisable) {
                        pm.setComponentEnabledSetting(
                            otherComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                        changed = true
                    }
                }
        }
        return changed
    }
}
