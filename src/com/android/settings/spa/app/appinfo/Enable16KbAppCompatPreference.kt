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

package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.development.Enable16kUtils
import com.android.settings.flags.Flags
import com.android.settingslib.spa.framework.compose.OverridableFlow
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import kotlinx.coroutines.flow.flow

@Composable
fun Enable16KbAppCompatPreference(
    app: ApplicationInfo
) {
    val context = LocalContext.current
    val presenter = remember(app) { Enable16KbAppCompatSwitchPresenter(context, app) }
    if (!presenter.isAvailable()) return

    val isCheckedState = presenter.isCheckedFlow.collectAsStateWithLifecycle(initialValue = null)
    SwitchPreference(remember {
        object : SwitchPreferenceModel {
            override val title =
                context.getString(R.string.enable_16k_app_compat_title)

            override val summary = {
                context.getString(R.string.enable_16k_app_compat_details)
            }

            override val checked = {
                isCheckedState.value
            }

            override val onCheckedChange = presenter::onCheckedChange
        }
    })
}

private class Enable16KbAppCompatSwitchPresenter(context: Context, private val app: ApplicationInfo) {
    private val packageManager = context.packageManager
    fun isAvailable(): Boolean {
        return Enable16kUtils.isUsing16kbPages() && Flags.pageSizeAppCompatSetting()
    }

    private val isChecked = OverridableFlow(flow {
        emit(packageManager.isPageSizeCompatEnabled(app.packageName))
    })

    val isCheckedFlow = isChecked.flow
    fun onCheckedChange(newChecked: Boolean) {
        try {
            packageManager.setPageSizeAppCompatFlagsSettingsOverride(app.packageName, newChecked)
            isChecked.override(newChecked)
        } catch (e: RuntimeException) {
            Log.e("Enable16KbAppCompat", "Failed to set" +
                    "setPageSizeAppCompatModeSettingsOverride", e);
        }
    }
}
