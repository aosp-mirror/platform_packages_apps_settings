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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.print.PrintRepository.PrintServiceDisplayInfo
import com.android.settings.print.PrintSettingsFragment.EXTRA_CHECKED
import com.android.settings.print.PrintSettingsFragment.EXTRA_SERVICE_COMPONENT_NAME
import com.android.settings.print.PrintSettingsFragment.EXTRA_TITLE
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsOpacity
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.SettingsIcon
import com.android.settingslib.spaprivileged.settingsprovider.settingsSecureStringFlow
import com.android.settingslib.spaprivileged.template.common.UserProfilePager
import kotlinx.coroutines.flow.Flow

object PrintSettingsPageProvider : SettingsPageProvider {
    override val name = "PrintSettings"

    @Composable
    override fun Page(arguments: Bundle?) {
        RegularScaffold(title = stringResource(R.string.print_settings)) {
            val context = LocalContext.current
            val printRepository = remember(context) { PrintRepository(context) }
            UserProfilePager { PrintServices(printRepository) }
        }
    }

    @Composable
    private fun PrintServices(printRepository: PrintRepository) {
        val printServiceDisplayInfos by
            remember { printRepository.printServiceDisplayInfosFlow() }
                .collectAsStateWithLifecycle(initialValue = emptyList())
        if (printServiceDisplayInfos.isEmpty()) {
            NoServicesInstalled()
        } else {
            Category(title = stringResource(R.string.print_settings_title)) {
                for (printServiceDisplayInfo in printServiceDisplayInfos) {
                    PrintService(printServiceDisplayInfo)
                }
            }
        }
        AddPrintService()
    }

    @Composable
    private fun NoServicesInstalled() {
        Column(
            modifier = Modifier.fillMaxSize().padding(SettingsDimension.itemPaddingAround),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.Print,
                contentDescription = null,
                modifier =
                    Modifier.size(110.dp)
                        .padding(SettingsDimension.itemPaddingAround)
                        .alpha(SettingsOpacity.SurfaceTone),
            )
            Text(
                text = stringResource(R.string.print_no_services_installed),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }

    @VisibleForTesting
    @Composable
    fun PrintService(displayInfo: PrintServiceDisplayInfo) {
        val context = LocalContext.current
        Preference(
            object : PreferenceModel {
                override val title = displayInfo.title
                override val summary = { displayInfo.summary }
                override val icon: @Composable () -> Unit = {
                    Image(
                        painter = rememberDrawablePainter(displayInfo.icon),
                        contentDescription = null,
                        modifier = Modifier.size(SettingsDimension.appIconItemSize),
                    )
                }
                override val onClick = { launchPrintServiceSettings(context, displayInfo) }
            }
        )
    }

    private fun launchPrintServiceSettings(context: Context, displayInfo: PrintServiceDisplayInfo) {
        SubSettingLauncher(context)
            .apply {
                setDestination(PrintServiceSettingsFragment::class.qualifiedName)
                setArguments(
                    bundleOf(
                        EXTRA_CHECKED to displayInfo.isEnabled,
                        EXTRA_TITLE to displayInfo.title,
                        EXTRA_SERVICE_COMPONENT_NAME to displayInfo.componentName
                    )
                )
                setSourceMetricsCategory(SettingsEnums.PRINT_SETTINGS)
            }
            .launch()
    }

    @Composable
    fun AddPrintService(
        searchUriFlow: Flow<String> = rememberContext { context ->
            context.settingsSecureStringFlow(Settings.Secure.PRINT_SERVICE_SEARCH_URI)
        },
    ) {
        val context = LocalContext.current
        val searchUri by searchUriFlow.collectAsStateWithLifecycle("")
        if (searchUri.isEmpty()) return
        Preference(
            object : PreferenceModel {
                override val title = stringResource(R.string.print_menu_item_add_service)
                override val icon = @Composable { SettingsIcon(imageVector = Icons.Outlined.Add) }
                override val onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchUri)))
                }
            }
        )
    }
}
