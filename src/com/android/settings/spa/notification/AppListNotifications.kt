/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.notification

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.applications.AppInfoBase
import com.android.settings.notification.app.AppNotificationSettings
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.compose.toState
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage
import com.android.settingslib.spaprivileged.template.app.AppListSwitchItem

object AppListNotificationsPageProvider : SettingsPageProvider {
    override val name = "AppListNotifications"

    @Composable
    override fun Page(arguments: Bundle?) {
        AppListPage(
            title = stringResource(R.string.app_notifications_title),
            listModel = rememberContext(::AppNotificationsListModel),
        ) { AppNotificationsItem() }
    }

    @Composable
    fun EntryItem() {
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.app_notifications_title)
            override val summary = stringResource(R.string.app_notification_field_summary).toState()
            override val onClick = navigator(name)
        })
    }
}

@Composable
private fun AppListItemModel<AppNotificationsRecord>.AppNotificationsItem() {
    val appNotificationsRepository = rememberContext(::AppNotificationRepository)
    val context = LocalContext.current
    AppListSwitchItem(
        onClick = {
            navigateToAppNotificationSettings(
                context = context,
                app = record.app,
            )
        },
        checked = record.controller.isEnabled.observeAsState(),
        changeable = produceState(initialValue = false) {
            value = appNotificationsRepository.isChangeable(record.app)
        },
        onCheckedChange = record.controller::setEnabled,
    )
}

private fun navigateToAppNotificationSettings(context: Context, app: ApplicationInfo) {
    AppInfoBase.startAppInfoFragment(
        AppNotificationSettings::class.java,
        context.getString(R.string.notifications_title),
        app,
        context,
        SettingsEnums.MANAGE_APPLICATIONS_NOTIFICATIONS,
    )
}
