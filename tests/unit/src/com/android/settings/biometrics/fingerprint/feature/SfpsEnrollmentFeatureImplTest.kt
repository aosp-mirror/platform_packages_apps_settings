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

import android.animation.Animator
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_CENTER
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_FINGERTIP
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_LEFT_EDGE
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_NO_ANIMATION
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_RIGHT_EDGE
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.STAGE_UNKNOWN
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class SfpsEnrollmentFeatureImplTest {
    private val STAGE_0 = 0
    private val STAGE_1 = 1
    private val STAGE_2 = 2
    private val STAGE_3 = 3
    private val STAGE_4 = 4

    private val THRESHOLD_0 = 0f
    private val THRESHOLD_1 = .36f
    private val THRESHOLD_2 = .52f
    private val THRESHOLD_3 = .76f
    private val THRESHOLD_4 = 1f

    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var mockFingerprintManager: FingerprintManager

    private val mSfpsEnrollmentFeatureImpl: SfpsEnrollmentFeatureImpl = SfpsEnrollmentFeatureImpl()

    @Before
    fun setUp() {
        whenever(context.getSystemService(FingerprintManager::class.java))
            .thenReturn(mockFingerprintManager)
        doReturn(THRESHOLD_0).`when`(mockFingerprintManager).getEnrollStageThreshold(STAGE_0)
        doReturn(THRESHOLD_1).`when`(mockFingerprintManager).getEnrollStageThreshold(STAGE_1)
        doReturn(THRESHOLD_2).`when`(mockFingerprintManager).getEnrollStageThreshold(STAGE_2)
        doReturn(THRESHOLD_3).`when`(mockFingerprintManager).getEnrollStageThreshold(STAGE_3)
        doReturn(THRESHOLD_4).`when`(mockFingerprintManager).getEnrollStageThreshold(STAGE_4)
    }

    @Test
    fun testGetEnrollStageThreshold() {
        assertThat(mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, STAGE_0))
            .isEqualTo(THRESHOLD_0)
        assertThat(mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, STAGE_1))
            .isEqualTo(THRESHOLD_1)
        assertThat(mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, STAGE_2))
            .isEqualTo(THRESHOLD_2)
        assertThat(mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, STAGE_3))
            .isEqualTo(THRESHOLD_3)
        assertThat(mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, STAGE_4))
            .isEqualTo(THRESHOLD_4)
    }

    @Test
    fun testGetHelpAnimator() {
        val mockView: View = mock(View::class.java)
        val animator: Animator = mSfpsEnrollmentFeatureImpl.getHelpAnimator(mockView)
        assertThat(animator.duration).isEqualTo(SfpsEnrollmentFeatureImpl.HELP_ANIMATOR_DURATION)
    }

    @Test
    fun testGetCurrentSfpsEnrollStage() {
        assertThat(mSfpsEnrollmentFeatureImpl.getCurrentSfpsEnrollStage(0, null))
            .isEqualTo(STAGE_UNKNOWN)
        val mapper = { i: Int ->
            (25 * mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, i)).roundToInt()
        }
        assertThat(mSfpsEnrollmentFeatureImpl.getCurrentSfpsEnrollStage(-1, mapper))
            .isEqualTo(SFPS_STAGE_NO_ANIMATION)
        assertThat(mSfpsEnrollmentFeatureImpl.getCurrentSfpsEnrollStage(0, mapper))
            .isEqualTo(SFPS_STAGE_CENTER)
        assertThat(mSfpsEnrollmentFeatureImpl.getCurrentSfpsEnrollStage(9, mapper))
            .isEqualTo(SFPS_STAGE_FINGERTIP)
        assertThat(mSfpsEnrollmentFeatureImpl.getCurrentSfpsEnrollStage(13, mapper))
            .isEqualTo(SFPS_STAGE_LEFT_EDGE)
        assertThat(mSfpsEnrollmentFeatureImpl.getCurrentSfpsEnrollStage(19, mapper))
            .isEqualTo(SFPS_STAGE_RIGHT_EDGE)
        assertThat(mSfpsEnrollmentFeatureImpl.getCurrentSfpsEnrollStage(25, mapper))
            .isEqualTo(SFPS_STAGE_RIGHT_EDGE)
    }

    @Test
    fun testGetFeaturedStageHeaderResource() {
        val type = "string"
        assertThat(
            mSfpsEnrollmentFeatureImpl.getFeaturedStageHeaderResource(SFPS_STAGE_NO_ANIMATION)
        ).isEqualTo(
            getSettingsResourcesId(type, "security_settings_fingerprint_enroll_repeat_title")
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getFeaturedStageHeaderResource(SFPS_STAGE_CENTER)
        ).isEqualTo(
            getSettingsResourcesId(type, "security_settings_sfps_enroll_finger_center_title")
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getFeaturedStageHeaderResource(SFPS_STAGE_FINGERTIP)
        ).isEqualTo(
            getSettingsResourcesId(type, "security_settings_sfps_enroll_fingertip_title")
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getFeaturedStageHeaderResource(SFPS_STAGE_LEFT_EDGE)
        ).isEqualTo(
            getSettingsResourcesId(type, "security_settings_sfps_enroll_left_edge_title")
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getFeaturedStageHeaderResource(SFPS_STAGE_RIGHT_EDGE)
        ).isEqualTo(
            getSettingsResourcesId(type, "security_settings_sfps_enroll_right_edge_title")
        )
    }

    @Test
    fun testGetSfpsEnrollLottiePerStage() {
        val type = "raw"
        assertThat(
            mSfpsEnrollmentFeatureImpl.getSfpsEnrollLottiePerStage(SFPS_STAGE_NO_ANIMATION)
        ).isEqualTo(getSettingsResourcesId(type, "sfps_lottie_no_animation"))
        assertThat(
            mSfpsEnrollmentFeatureImpl.getSfpsEnrollLottiePerStage(SFPS_STAGE_CENTER)
        ).isEqualTo(getSettingsResourcesId(type, "sfps_lottie_pad_center"))
        assertThat(
            mSfpsEnrollmentFeatureImpl.getSfpsEnrollLottiePerStage(SFPS_STAGE_FINGERTIP)
        ).isEqualTo(getSettingsResourcesId(type, "sfps_lottie_tip"))
        assertThat(
            mSfpsEnrollmentFeatureImpl.getSfpsEnrollLottiePerStage(SFPS_STAGE_LEFT_EDGE)
        ).isEqualTo(getSettingsResourcesId(type, "sfps_lottie_left_edge"))
        assertThat(
            mSfpsEnrollmentFeatureImpl.getSfpsEnrollLottiePerStage(SFPS_STAGE_RIGHT_EDGE)
        ).isEqualTo(getSettingsResourcesId(type, "sfps_lottie_right_edge"))
    }

    private fun getSettingsResourcesId(type: String, name: String) : Int {
        return context.resources.getIdentifier(name, type, context.packageName)
    }
}