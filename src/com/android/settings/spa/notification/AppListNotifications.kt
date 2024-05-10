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

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.template.app.AppListPage

object AppListNotificationsPageProvider : SettingsPageProvider {
    override val name = "AppListNotifications"

    @Composable
    override fun Page(arguments: Bundle?) {
        AppListPage(
            title = stringResource(R.string.app_notifications_title),
            listModel = rememberContext(::AppNotificationsListModel),
        )
    }

    @Composable
    fun EntryItem() {
        val summary = stringResource(R.string.app_notification_field_summary)
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.app_notifications_title)
            override val summary = { summary }
            override val onClick = navigator(name)
        })
    }
}
