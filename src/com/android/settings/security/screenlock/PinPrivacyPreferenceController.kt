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

package com.android.settings.security.screenlock

import android.content.Context
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.android.internal.widget.LockPatternUtils
import com.android.internal.widget.LockPatternUtils.*
import com.android.settings.core.PreferenceControllerMixin
import com.android.settingslib.core.AbstractPreferenceController

class PinPrivacyPreferenceController(
    context: Context,
    private val userId: Int,
    private val lockPatternUtils: LockPatternUtils
) : AbstractPreferenceController(context), PreferenceControllerMixin,
    Preference.OnPreferenceChangeListener {

    companion object {
        private const val PREF_KEY = "enhancedPinPrivacy"
    }

    override fun isAvailable(): Boolean {
        val credentialType = lockPatternUtils.getCredentialTypeForUser(userId)
        return credentialType == CREDENTIAL_TYPE_PIN
    }

    override fun getPreferenceKey(): String {
        return PREF_KEY
    }

    override fun onPreferenceChange(preference: Preference, value: Any): Boolean {
        lockPatternUtils.setPinEnhancedPrivacyEnabled((value as Boolean), userId)
        return true
    }

    override fun updateState(preference: Preference) {
        (preference as TwoStatePreference).isChecked = getCurrentPreferenceState()
    }

    private fun getCurrentPreferenceState(): Boolean {
        return lockPatternUtils.isPinEnhancedPrivacyEnabled(userId)
    }
}