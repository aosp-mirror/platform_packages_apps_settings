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
import com.android.settings.R
import com.android.settings.spa.about.AboutPhonePageProvider
import com.android.settings.spa.app.AppsMainPageProvider
import com.android.settings.spa.network.NetworkAndInternetPageProvider
import com.android.settings.spa.notification.NotificationMainPageProvider
import com.android.settings.spa.system.SystemMainPageProvider
import com.android.settingslib.spa.framework.common.SettingsEntry
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.common.createSettingsPage

object HomePageProvider : SettingsPageProvider {
    override val name = "Home"
    private val owner = createSettingsPage()

    override fun isEnabled(arguments: Bundle?) = false

    override fun buildEntry(arguments: Bundle?): List<SettingsEntry> {
        return listOf(

            NetworkAndInternetPageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
            AppsMainPageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
            NotificationMainPageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
            SystemMainPageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
            AboutPhonePageProvider.buildInjectEntry().setLink(fromPage = owner).build(),
        )
    }

    override fun getTitle(arguments: Bundle?): String {
        return SpaEnvironmentFactory.instance.appContext.getString(R.string.settings_label)
    }
}
