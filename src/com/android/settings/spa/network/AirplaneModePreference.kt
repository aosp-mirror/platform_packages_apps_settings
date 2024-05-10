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
import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.settings.AirplaneModeEnabler
import com.android.settings.AirplaneModeEnabler.OnAirplaneModeChangedListener
import com.android.settings.R
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.ui.SettingsIcon

@Composable
fun AirplaneModePreference() {
    val context = LocalContext.current
    val controller = remember { AirplaneModeController(context) }
    if (!controller.isAvailable()) return
    val checked by controller.airplaneModeState.observeAsState(
        initial = controller.isAirplaneModeOn()
    )
    SwitchPreference(object : SwitchPreferenceModel {
        override val title = context.getString(R.string.airplane_mode)
        override val checked = { checked }
        override val onCheckedChange = { newChecked: Boolean ->
            controller.setChecked(newChecked)
        }
        override val icon = @Composable {
            SettingsIcon(imageVector = Icons.Outlined.AirplanemodeActive)
        }
    })
}

private class AirplaneModeController(private val context: Context) : OnAirplaneModeChangedListener {
    private var airplaneModeEnabler = AirplaneModeEnabler(context, this)
    private val _airplaneModeState = MutableLiveData<Boolean>()
    val airplaneModeState: LiveData<Boolean>
        get() = _airplaneModeState

    override fun onAirplaneModeChanged(isAirplaneModeOn: Boolean) {
        _airplaneModeState.postValue(isAirplaneModeOn)
    }

    fun isAvailable(): Boolean {
        return context.resources.getBoolean(R.bool.config_show_toggle_airplane)
                && !context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    fun isAirplaneModeOn(): Boolean {
        return airplaneModeEnabler.isAirplaneModeOn()
    }

    fun setChecked(newChecked: Boolean) {
        if (isAirplaneModeOn() == newChecked) {
            return
        }
        airplaneModeEnabler.setAirplaneMode(newChecked)
    }

}
