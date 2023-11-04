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

package com.android.settings.spa.about

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PermDeviceInformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.SettingsIcon

object AboutPhonePageProvider : SettingsPageProvider {
    override val name = "AboutPhone"
    private val owner = createSettingsPage()

    @Composable
    override fun Page(arguments: Bundle?) {
        RegularScaffold(title = getTitle(arguments)) {
            BasicInfoCategory.CategoryItems()
        }
    }

    override fun getTitle(arguments: Bundle?): String =
        SpaEnvironmentFactory.instance.appContext.getString(R.string.about_settings)

    fun buildInjectEntry(): SettingsEntryBuilder {
        return SettingsEntryBuilder.createInject(owner = owner)
            .setUiLayoutFn {
                val context = LocalContext.current
                val deviceNamePresenter = remember { DeviceNamePresenter(context) }
                Preference(object : PreferenceModel {
                    override val title = stringResource(R.string.about_settings)
                    override val summary = { deviceNamePresenter.deviceName }
                    override val onClick = navigator(name)
                    override val icon = @Composable {
                        SettingsIcon(imageVector = Icons.Outlined.PermDeviceInformation)
                    }
                })
            }
    }
}