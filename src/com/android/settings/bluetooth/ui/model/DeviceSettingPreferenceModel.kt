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

package com.android.settings.bluetooth.ui.model

import com.android.settingslib.bluetooth.devicesettings.DeviceSettingId
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.bluetooth.devicesettings.shared.model.ToggleModel

/** Models a device setting preference. */
sealed interface DeviceSettingPreferenceModel {
    @DeviceSettingId
    val id: Int

    /** Models a plain preference. */
    data class PlainPreference(
        @DeviceSettingId override val id: Int,
        val title: String,
        val summary: String? = null,
        val icon: DeviceSettingIcon? = null,
        val onClick: (() -> Unit)? = null,
    ) : DeviceSettingPreferenceModel

    /** Models a switch preference. */
    data class SwitchPreference(
        @DeviceSettingId override val id: Int,
        val title: String,
        val summary: String? = null,
        val icon: DeviceSettingIcon? = null,
        val checked: Boolean,
        val onCheckedChange: ((Boolean) -> Unit),
        val onPrimaryClick: (() -> Unit)? = null,
    ) : DeviceSettingPreferenceModel

    /** Models a multi-toggle preference. */
    data class MultiTogglePreference(
        @DeviceSettingId override val id: Int,
        val title: String,
        val toggles: List<ToggleModel>,
        val isActive: Boolean,
        val selectedIndex: Int,
        val isAllowedChangingState: Boolean,
        val onSelectedChange: (Int) -> Unit,
    ) : DeviceSettingPreferenceModel

    /** Models a footer preference. */
    data class FooterPreference(
        @DeviceSettingId override val id: Int,
        val footerText: String,
    ) : DeviceSettingPreferenceModel

    /** Models a preference which could navigate to more settings fragment. */
    data class MoreSettingsPreference(
        @DeviceSettingId override val id: Int,
    ) : DeviceSettingPreferenceModel

    /** Models a help button on the top right corner of the fragment. */
    data class HelpPreference(
        @DeviceSettingId override val id: Int,
        val icon: DeviceSettingIcon,
        val onClick: (() -> Unit),
    ) : DeviceSettingPreferenceModel
}
