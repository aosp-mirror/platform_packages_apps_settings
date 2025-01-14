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

package com.android.settings.biometrics.fingerprint.feature

import android.content.Context
import androidx.annotation.XmlRes
import androidx.preference.PreferenceScreen
import com.android.settingslib.RestrictedPreference

/**
 * Provides a list of preference resource IDs (from res/xml) for Fingerprint Settings page under
 * category FingerprintSettings::mFingerprintUnlockCategory
 *
 * @see com.android.settings.biometrics.fingerprint.FingerprintSettings
 */
open class FingerprintExtPreferencesProvider {

    open val size: Int = 0

    open fun newPreference(
        index: Int,
        inflater: PreferenceInflater,
        context: Context
    ): RestrictedPreference? = null

    interface PreferenceInflater {
        fun inflateFromResource(@XmlRes resId: Int): PreferenceScreen
    }
}
