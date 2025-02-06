/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.supervision

import android.app.supervision.flags.Flags
import android.content.Context
import com.android.settings.R
import androidx.preference.Preference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata

class SupervisionPinRecoveryPreference : PreferenceMetadata,
    PreferenceAvailabilityProvider, Preference.OnPreferenceClickListener {
    override val key: String
        get() = SupervisionPinRecoveryPreference.KEY

    override val title: Int
        get() = R.string.supervision_add_forgot_pin_preference_title

    override fun isAvailable(context: Context) = Flags.enableSupervisionPinRecoveryScreen()

    // TODO(b/393657542): trigger re-authentication flow to confirm user credential before PIN
    // recovery.
    override fun onPreferenceClick(preference: Preference): Boolean {
        return true
    }

    companion object {
        const val KEY = "supervision_pin_recovery"
    }
}