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

package com.android.settings.spa.system

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.android.settings.R
import com.android.settings.localepicker.AppLocalePickerActivity
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.compose.toState
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.ui.SettingsBody
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage

object AppLanguagesPageProvider : SettingsPageProvider {
    override val name = "AppLanguages"

    @Composable
    override fun Page(arguments: Bundle?) {
        AppListPage(
            title = stringResource(R.string.app_locales_picker_menu_title),
            listModel = rememberContext(::AppLanguagesListModel),
            header = {
                Box(Modifier.padding(SettingsDimension.itemPadding)) {
                    SettingsBody(stringResource(R.string.desc_app_locale_selection_supported))
                }
            },
        ) { AppLanguageItem() }
    }

    @Composable
    fun EntryItem() {
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.app_locales_picker_menu_title)
            override val summary = stringResource(R.string.app_locale_picker_summary).toState()
            override val onClick = navigator(name)
        })
    }
}

@Composable
private fun AppListItemModel<AppLanguagesRecord>.AppLanguageItem() {
    val context = LocalContext.current
    AppListItem {
        val intent = Intent(context, AppLocalePickerActivity::class.java).apply {
            data = Uri.parse("package:${record.app.packageName}")
            putExtra("uid", record.app.uid)
        }
        context.startActivity(intent)
    }
}
