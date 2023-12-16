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

package com.android.settings.biometrics

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.settings.flags.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when` as whenever
import org.mockito.Spy
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BiometricSettingsProviderTest {
  @Spy private var context: Context = spy(RuntimeEnvironment.application)
  @Spy private var resources: Resources = spy(context.resources)
  private lateinit var provider: BiometricSettingsProvider

  @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

  @Before
  fun setUp() {
    whenever(context.resources).thenReturn(resources)
    provider = BiometricSettingsProvider()
    provider.attachInfo(context, null)
  }

  @Test(expected = UnsupportedOperationException::class)
  fun query_shouldCrash() {
    provider.query(Uri.EMPTY, null, null, null, null)
  }

  @Test(expected = UnsupportedOperationException::class)
  fun getType_shouldCrash() {
    provider.getType(Uri.EMPTY)
  }

  @Test(expected = UnsupportedOperationException::class)
  fun insert_shouldCrash() {
    provider.insert(Uri.EMPTY, null)
  }

  @Test(expected = UnsupportedOperationException::class)
  fun delete_shouldCrash() {
    provider.delete(Uri.EMPTY, null, null)
  }

  @Test(expected = UnsupportedOperationException::class)
  fun update_shouldCrash() {
    provider.update(Uri.EMPTY, null, null, null)
  }

  @Test
  @RequiresFlagsEnabled(Flags.FLAG_BIOMETRIC_SETTINGS_PROVIDER)
  fun getSuggestionState_shouldQueryFeatureProvider() {
    val expectedValue = false
    setSupportFaceEnroll(expectedValue)

    val bundle = provider.call(BiometricSettingsProvider.GET_SUW_FACE_ENABLED, null, Bundle())
    assertThat(bundle!!.getBoolean(BiometricSettingsProvider.SUW_FACE_ENABLED))
      .isEqualTo(expectedValue)
  }

  private fun setSupportFaceEnroll(toThis: Boolean) {
    whenever(resources.getBoolean(com.android.settings.R.bool.config_suw_support_face_enroll))
      .thenReturn(toThis)
  }
}
