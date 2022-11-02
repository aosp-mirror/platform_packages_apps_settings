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

package com.android.settings.spa.home

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.spa.app.AppsMainPageProvider
import com.android.settings.spa.notification.NotificationMainPageProvider
import com.android.settings.spa.system.SystemMainPageProvider
import com.android.settingslib.spa.framework.common.SettingsEntry
import com.android.settingslib.spa.framework.common.SettingsPage
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.widget.scaffold.HomeScaffold

object HomePageProvider : SettingsPageProvider {
    override val name = "Home"

    @Composable
    override fun Page(arguments: Bundle?) {
        HomePage()
    }

    override fun buildEntry(arguments: Bundle?): List<SettingsEntry> {
        val owner = SettingsPage.create(name, parameter = parameter, arguments = arguments)
        return listOf(
            AppsMainPageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
        )
    }
}

@Composable
private fun HomePage() {
    HomeScaffold(title = stringResource(R.string.settings_label)) {
        AppsMainPageProvider.EntryItem()
        NotificationMainPageProvider.EntryItem()
        SystemMainPageProvider.EntryItem()
    }
}
