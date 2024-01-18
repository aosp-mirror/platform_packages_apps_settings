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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.RestrictedSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class SfpsRestToUnlockFeatureImplTest {

    private lateinit var mContext: Context

    private lateinit var mSfpsRestToUnlockFeatureImpl: SfpsRestToUnlockFeatureImpl

    private lateinit var mRestrictedSwitchPreferenceSpy: RestrictedSwitchPreference

    @Before
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()
        mSfpsRestToUnlockFeatureImpl = SfpsRestToUnlockFeatureImpl()
        mRestrictedSwitchPreferenceSpy = Mockito.spy(RestrictedSwitchPreference(mContext))
    }

    @Test
    fun getDescriptionForSfps_isNotNull() {
        assertThat(mSfpsRestToUnlockFeatureImpl)
            .isInstanceOf(SfpsRestToUnlockFeatureImpl::class.java)
        assertThat(mSfpsRestToUnlockFeatureImpl.getDescriptionForSfps(mContext))
            .isNotNull()
    }

    @Test
    fun getRestToUnlockLayout_isNull() {
        assertThat(mSfpsRestToUnlockFeatureImpl)
            .isInstanceOf(SfpsRestToUnlockFeatureImpl::class.java)
        assertThat(mSfpsRestToUnlockFeatureImpl.getRestToUnlockLayout(mContext))
            .isNull()
    }

    @Test
    fun fingerprint_settings_setupFingerprintUnlockCategoryPreferences() {
        assertThat(mSfpsRestToUnlockFeatureImpl.getRestToUnlockPreference(mContext))
            .isNull()
    }
}