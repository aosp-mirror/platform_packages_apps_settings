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

package com.android.settings.biometrics.fingerprint

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.biometrics.fingerprint.feature.SfpsEnrollmentFeatureImpl
import com.android.settings.biometrics.fingerprint.feature.SfpsRestToUnlockFeatureImpl
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class FingerprintFeatureProviderImplTest {

    @Mock
    private lateinit var mContext: Context

    private lateinit var mFingerprintFeatureProviderImpl: FingerprintFeatureProviderImpl

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mFingerprintFeatureProviderImpl = FingerprintFeatureProviderImpl()
    }

    @Test
    fun getSfpsEnrollmentFeature_returnDefaultImpl() {
        assertThat(mFingerprintFeatureProviderImpl.sfpsEnrollmentFeature)
            .isInstanceOf(SfpsEnrollmentFeatureImpl::class.java)
    }

    @Test
    fun getSfpsRestToUnlockFeature_returnDefaultImpl() {
        assertThat(mFingerprintFeatureProviderImpl.getSfpsRestToUnlockFeature(mContext))
            .isInstanceOf(SfpsRestToUnlockFeatureImpl::class.java)
    }
}