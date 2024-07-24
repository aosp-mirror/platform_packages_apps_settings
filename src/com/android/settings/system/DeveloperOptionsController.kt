/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.system

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.development.DevelopmentSettingsDashboardFragment
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.ui.SettingsIcon
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.model.enterprise.Restrictions
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBooleanFlow
import com.android.settingslib.spaprivileged.template.preference.RestrictedPreference

class DeveloperOptionsController(context: Context, preferenceKey: String) :
    ComposePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus() =
        if (mContext.userManager.isAdminUser) AVAILABLE
        else DISABLED_FOR_USER

    private val isDevelopmentSettingsEnabledFlow = context.settingsGlobalBooleanFlow(
        name = Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
        defaultValue = Build.IS_ENG,
    )

    @Composable
    override fun Content() {
        val isDevelopmentSettingsEnabled by isDevelopmentSettingsEnabledFlow
            .collectAsStateWithLifecycle(initialValue = false)
        if (isDevelopmentSettingsEnabled) {
            DeveloperOptionsPreference()
        }
    }

    @VisibleForTesting
    @Composable
    fun DeveloperOptionsPreference() {
        RestrictedPreference(
            model = object : PreferenceModel {
                override val title =
                    stringResource(com.android.settingslib.R.string.development_settings_title)
                override val icon = @Composable {
                    SettingsIcon(ImageVector.vectorResource(R.drawable.ic_settings_development))
                }
                override val onClick = {
                    SubSettingLauncher(mContext).apply {
                        setDestination(DevelopmentSettingsDashboardFragment::class.qualifiedName)
                        setSourceMetricsCategory(SettingsEnums.SETTINGS_SYSTEM_CATEGORY)
                    }.launch()
                }
            },
            restrictions = Restrictions(keys = listOf(UserManager.DISALLOW_DEBUGGING_FEATURES)),
        )
    }
}
