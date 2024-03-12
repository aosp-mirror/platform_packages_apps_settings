/**
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

package com.android.settings.spa.network

import android.content.Context
import android.os.Bundle
import android.os.UserHandle.myUserId
import android.os.UserManager
import android.os.UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.Utils
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.SettingsIcon

object NetworkAndInternetPageProvider : SettingsPageProvider {
    override val name = "NetworkAndInternet"
    private val owner = createSettingsPage()

    override fun isEnabled(arguments: Bundle?) = false

    @Composable
    override fun Page(arguments: Bundle?) {
        RegularScaffold(title = getTitle(arguments)) {
            AirplaneModePreference()
        }
    }

    override fun getTitle(arguments: Bundle?): String {
        return SpaEnvironmentFactory.instance.appContext.getString(R.string.network_dashboard_title)
    }

    fun buildInjectEntry(): SettingsEntryBuilder {
        return SettingsEntryBuilder.createInject(owner = owner)
            .setUiLayoutFn {
                val summary = stringResource(getSummaryResId())
                Preference(object : PreferenceModel {
                    override val title = stringResource(R.string.network_dashboard_title)
                    override val summary = { summary }
                    override val onClick = navigator(name)
                    override val icon = @Composable {
                        SettingsIcon(imageVector = Icons.Outlined.Wifi)
                    }
                })
            }
    }

    @Composable
    private fun getSummaryResId(): Int {
        val isMobileAvailable = remember { isMobileAvailable() }
        var summary = if (isMobileAvailable) {
            R.string.network_dashboard_summary_mobile
        } else {
            R.string.network_dashboard_summary_no_mobile
        }
        return summary
    }

    private fun isMobileAvailable(): Boolean {
        val context = SpaEnvironmentFactory.instance.appContext
        return !isUserRestricted(context) && !Utils.isWifiOnly(context)
    }

    private fun isUserRestricted(context: Context): Boolean {
        val userManager: UserManager = context.getSystemService(UserManager::class.java)!!
        return !userManager.isAdminUser || RestrictedLockUtilsInternal.hasBaseUserRestriction(
            context, DISALLOW_CONFIG_MOBILE_NETWORKS, myUserId()
        )
    }
}
