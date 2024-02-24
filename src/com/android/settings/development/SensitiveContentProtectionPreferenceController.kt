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
package com.android.settings.development

import android.content.Context
import android.permission.flags.Flags.sensitiveNotificationAppProtection
import android.provider.Settings
import android.view.flags.Flags.sensitiveContentAppProtection
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.android.server.notification.Flags.screenshareNotificationHiding
import com.android.settings.core.PreferenceControllerMixin
import com.android.settingslib.development.DeveloperOptionsPreferenceController

class SensitiveContentProtectionPreferenceController(val context: Context) :
    DeveloperOptionsPreferenceController(context),
    Preference.OnPreferenceChangeListener,
    PreferenceControllerMixin {

    override fun getPreferenceKey(): String =
        DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS_KEY

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val isEnabled = newValue as Boolean
        Settings.Global.putInt(
            mContext.getContentResolver(),
            Settings.Global.DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS,
            if (isEnabled) SETTING_VALUE_ON else SETTING_VALUE_OFF
        )
        return true
    }

    override fun updateState(preference: Preference?) {
        val mode = Settings.Global.getInt(
            mContext.getContentResolver(),
            Settings.Global.DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS,
            0)
        (mPreference as TwoStatePreference).isChecked = mode != SETTING_VALUE_OFF
    }

    // Overriding as public, kotlin tests can not invoke a protected method
    public override fun onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled()
        Settings.Global.putInt(
            mContext.getContentResolver(),
            Settings.Global.DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS,
            SETTING_VALUE_OFF
        )
        (mPreference as TwoStatePreference).isChecked = false
    }

    override fun isAvailable(): Boolean {
        return sensitiveNotificationAppProtection() || screenshareNotificationHiding()
            || sensitiveContentAppProtection()
    }

    companion object {
        private const val DISABLE_SCREEN_SHARE_PROTECTIONS_FOR_APPS_AND_NOTIFICATIONS_KEY =
            "disable_screen_share_protections_for_apps_and_notifications"

        @VisibleForTesting
        val SETTING_VALUE_ON = 1

        @VisibleForTesting
        val SETTING_VALUE_OFF = 0
    }
}
