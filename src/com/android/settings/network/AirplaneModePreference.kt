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

package com.android.settings.network

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings.Global.AIRPLANE_MODE_ON
import androidx.annotation.DrawableRes
import com.android.settings.R
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
class AirplaneModePreference :
    SwitchPreference(AIRPLANE_MODE_ON, R.string.airplane_mode),
    PreferenceAvailabilityProvider {

    override val icon: Int
        @DrawableRes get() = R.drawable.ic_airplanemode_active

    override fun storage(context: Context) = SettingsGlobalStore.get(context)

    override fun isAvailable(context: Context) =
        (context.resources.getBoolean(R.bool.config_show_toggle_airplane)
                && !context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
}
// LINT.ThenChange(AirplaneModePreferenceController.java)
