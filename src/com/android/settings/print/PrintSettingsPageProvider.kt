/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.print

import android.app.settings.SettingsEnums
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.print.PrintRepository.PrintServiceDisplayInfo
import com.android.settings.print.PrintSettingsFragment.EXTRA_CHECKED
import com.android.settings.print.PrintSettingsFragment.EXTRA_SERVICE_COMPONENT_NAME
import com.android.settings.print.PrintSettingsFragment.EXTRA_TITLE
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spaprivileged.template.common.UserProfilePager

object PrintSettingsPageProvider : SettingsPageProvider {
    override val name = "PrintSettings"

    @Composable
    override fun Page(arguments: Bundle?) {
        RegularScaffold(title = stringResource(R.string.print_settings)) {
            val context = LocalContext.current
            val printRepository = remember(context) { PrintRepository(context) }
            UserProfilePager {
                PrintServices(printRepository)
            }
        }
    }

    @Composable
    private fun PrintServices(printRepository: PrintRepository) {
        val printServiceDisplayInfos by remember {
            printRepository.printServiceDisplayInfosFlow()
        }.collectAsStateWithLifecycle(initialValue = emptyList())
        Category(title = stringResource(R.string.print_settings_title)) {
            for (printServiceDisplayInfo in printServiceDisplayInfos) {
                PrintService(printServiceDisplayInfo)
            }
        }
    }

    @VisibleForTesting
    @Composable
    fun PrintService(displayInfo: PrintServiceDisplayInfo) {
        val context = LocalContext.current
        Preference(model = object : PreferenceModel {
            override val title = displayInfo.title
            override val summary = { displayInfo.summary }
            override val icon: @Composable () -> Unit = {
                Image(
                    painter = rememberDrawablePainter(displayInfo.icon),
                    contentDescription = null,
                    modifier = Modifier.size(SettingsDimension.appIconItemSize),
                )
            }
            override val onClick = {
                SubSettingLauncher(context).apply {
                    setDestination(PrintServiceSettingsFragment::class.qualifiedName)
                    setArguments(
                        bundleOf(
                            EXTRA_CHECKED to displayInfo.isEnabled,
                            EXTRA_TITLE to displayInfo.title,
                            EXTRA_SERVICE_COMPONENT_NAME to displayInfo.componentName
                        )
                    )
                    setSourceMetricsCategory(SettingsEnums.PRINT_SETTINGS)
                }.launch()
            }
        })
    }
}
