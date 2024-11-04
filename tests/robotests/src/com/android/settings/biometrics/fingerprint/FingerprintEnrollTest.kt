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

import android.app.Activity
import android.content.Intent
import com.android.settings.overlay.FeatureFactory
import com.android.settings.testutils.FakeFeatureFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class FingerprintEnrollTest {

    private lateinit var featureFactory: FeatureFactory

    private companion object {
        const val INTENT_KEY = "testKey"
        const val INTENT_VALUE = "testValue"
        val INTENT = Intent().apply {
            putExtra(INTENT_KEY, INTENT_VALUE)
        }
    }

    private val activityProvider = FingerprintEnrollActivityClassProvider()

    @Before
    fun setUp() {
        featureFactory = FakeFeatureFactory.setupForTest()
        `when`(featureFactory.fingerprintFeatureProvider.enrollActivityClassProvider)
            .thenReturn(activityProvider)
    }

    private fun setupActivity(activityClass: Class<out FingerprintEnroll>): FingerprintEnroll {
        return Robolectric.buildActivity(activityClass, INTENT).create().get()
    }

    @Test
    fun testFinishAndLaunchDefaultActivity() {
        // Run
        val activity = setupActivity(FingerprintEnroll::class.java)

        // Verify
        verifyLaunchNextActivity(activity, activityProvider.default)
    }

    @Test
    fun testFinishAndLaunchSetupActivity() {
        // Run
        val activity = setupActivity(FingerprintEnroll.SetupActivity::class.java)

        // Verify
        verifyLaunchNextActivity(activity, activityProvider.setup)
    }

    @Test
    fun testFinishAndLaunchInternalActivity() {
        // Run
        val activity = setupActivity(FingerprintEnroll.InternalActivity::class.java)

        // Verify
        verifyLaunchNextActivity(activity, activityProvider.internal)
    }

    private fun verifyLaunchNextActivity(
        currentActivityInstance : FingerprintEnroll,
        nextActivityClass: Class<out Activity>
    ) {
        assertThat(currentActivityInstance.isFinishing).isTrue()
        val nextActivityIntent = Shadows.shadowOf(currentActivityInstance).nextStartedActivity
        assertThat(nextActivityIntent.component!!.className).isEqualTo(nextActivityClass.name)
        assertThat(nextActivityIntent.extras!!.size()).isEqualTo(1)
        assertThat(nextActivityIntent.getStringExtra(INTENT_KEY)).isEqualTo(INTENT_VALUE)
    }
}
