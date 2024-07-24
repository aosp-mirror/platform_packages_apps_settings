/*
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

package com.android.settings.remoteauth

import android.content.Context
import android.util.FeatureFlagUtils
import com.android.settings.biometrics.BiometricStatusPreferenceController

class RemoteAuthStatusPreferenceController(
    private val context: Context,
    key: String = KEY_REMOTE_AUTHENTICATOR_SETTINGS
) : BiometricStatusPreferenceController(
    context, key
) {
    override fun isDeviceSupported(): Boolean {
        // TODO(b/290768873): Change based on RemoteAuthManager.
        return FeatureFlagUtils.isEnabled(
            context,
            FeatureFlagUtils.SETTINGS_REMOTEAUTH_ENROLLMENT_SETTINGS
        )
    }

    override fun isHardwareSupported(): Boolean {
        // TODO(b/290768873): Change based on RemoteAuthManager.
        return FeatureFlagUtils.isEnabled(
            context,
            FeatureFlagUtils.SETTINGS_REMOTEAUTH_ENROLLMENT_SETTINGS
        )
    }

    override fun getSummaryText() = RemoteAuthStatusUtils.getSummary(context)

    override fun getSettingsClassName() = RemoteAuthStatusUtils.getSettingsClassName()

    private companion object {
        /**
         * Preference key.
         *
         * This must match the key found in security_settings_combined_biometric.xml
         **/
        const val KEY_REMOTE_AUTHENTICATOR_SETTINGS = "biometric_remote_authenticator_settings"
    }
}