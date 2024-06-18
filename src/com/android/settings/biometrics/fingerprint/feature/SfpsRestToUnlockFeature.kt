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

package com.android.settings.biometrics.fingerprint.feature

import android.content.Context
import android.view.View
import androidx.preference.Preference

/**
 * Defines the feature provided by rest to unlock.
 */
interface SfpsRestToUnlockFeature {
    /**
     * Gets the content view hierarchy for SFPS rest to unlock feature which is used by
     * [com.android.settings.biometrics.fingerprint.FingerprintEnrollFinish].
     * @param context the context of
     * [com.android.settings.biometrics.fingerprint.FingerprintEnrollFinish].
     */
    fun getRestToUnlockLayout(context: Context) : View? = null

    /**
     * Gets the SFPS rest to unlock preference which is used in
     * [com.android.settings.biometrics.fingerprint.FingerprintSettings].
     * @param context the context of
     * [com.android.settings.biometrics.fingerprint.FingerprintSettings].
     */
    fun getRestToUnlockPreference(context: Context) : Preference? = null

    /**
     * Gets the specific description used in
     * [com.android.settings.biometrics.fingerprint.FingerprintEnrollFinish] for SFPS devices.
     * @return the description text for SFPS devices.
     */
    fun getDescriptionForSfps(context: Context) : String
}