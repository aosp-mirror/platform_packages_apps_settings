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
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val settingsPackageName = "com.android.settings"

    private lateinit var settingsContext: Context

    @Mock
    private lateinit var mockFingerprintManager: FingerprintManager

    private val mSfpsEnrollmentFeatureImpl: SfpsEnrollmentFeatureImpl = SfpsEnrollmentFeatureImpl()

    @Before
    fun setUp() {
        assertThat(mSfpsEnrollmentFeatureImpl).isInstanceOf(SfpsEnrollmentFeatureImpl::class.java)
        whenever(context.getSystemService(FingerprintManager::class.java))
            .thenReturn(mockFingerprintManager)
        doReturn(0f).`when`(mockFingerprintManager).getEnrollStageThreshold(0)
        doReturn(0.36f).`when`(mockFingerprintManager).getEnrollStageThreshold(1)
        doReturn(0.52f).`when`(mockFingerprintManager).getEnrollStageThreshold(2)
        doReturn(0.76f).`when`(mockFingerprintManager).getEnrollStageThreshold(3)
        doReturn(1f).`when`(mockFingerprintManager).getEnrollStageThreshold(4)
        settingsContext = context.createPackageContext(settingsPackageName, 0)
    }

    @Test
    fun testGetEnrollStageThreshold() {
        assertThat(mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, 0)).isEqualTo(0f)
        assertThat(mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, 1)).isEqualTo(0.36f)
        assertThat(mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, 2)).isEqualTo(0.52f)
        assertThat(mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, 3)).isEqualTo(0.76f)
        assertThat(mSfpsEnrollmentFeatureImpl.getEnrollStageThreshold(context, 4)).isEqualTo(1f)
    }

    @Test
    fun testGetHelpAnimator() {
        val mockView: View = mock(View::class.java)
        val animator: Animator = mSfpsEnrollmentFeatureImpl.getHelpAnimator(mockView)
        assertThat(animator.duration).isEqualTo(550)
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
            settingsContext.resources.getIdentifier(
                "security_settings_fingerprint_enroll_repeat_title",
                type,
                settingsPackageName)
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getFeaturedStageHeaderResource(SFPS_STAGE_CENTER)
        ).isEqualTo(
            settingsContext.resources.getIdentifier(
                "security_settings_sfps_enroll_finger_center_title",
                type,
                settingsPackageName)
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getFeaturedStageHeaderResource(SFPS_STAGE_FINGERTIP)
        ).isEqualTo(
            settingsContext.resources.getIdentifier(
                "security_settings_sfps_enroll_fingertip_title",
                type,
                settingsPackageName)
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getFeaturedStageHeaderResource(SFPS_STAGE_LEFT_EDGE)
        ).isEqualTo(
            settingsContext.resources.getIdentifier(
                "security_settings_sfps_enroll_left_edge_title",
                type,
                settingsPackageName)
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getFeaturedStageHeaderResource(SFPS_STAGE_RIGHT_EDGE)
        ).isEqualTo(
            settingsContext.resources.getIdentifier(
                "security_settings_sfps_enroll_right_edge_title",
                type,
                settingsPackageName)
        )
    }

    @Test
    fun testGetSfpsEnrollLottiePerStage() {
        val type = "raw"
        assertThat(
            mSfpsEnrollmentFeatureImpl.getSfpsEnrollLottiePerStage(SFPS_STAGE_NO_ANIMATION)
        ).isEqualTo(
            settingsContext.resources.getIdentifier(
                "sfps_lottie_no_animation",
                type,
                settingsPackageName)
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getSfpsEnrollLottiePerStage(SFPS_STAGE_CENTER)
        ).isEqualTo(
            settingsContext.resources.getIdentifier(
                "sfps_lottie_pad_center",
                type,
                settingsPackageName)
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getSfpsEnrollLottiePerStage(SFPS_STAGE_FINGERTIP)
        ).isEqualTo(
            settingsContext.resources.getIdentifier(
                "sfps_lottie_tip",
                type,
                settingsPackageName)
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getSfpsEnrollLottiePerStage(SFPS_STAGE_LEFT_EDGE)
        ).isEqualTo(
            settingsContext.resources.getIdentifier(
                "sfps_lottie_left_edge",
                type,
                settingsPackageName)
        )
        assertThat(
            mSfpsEnrollmentFeatureImpl.getSfpsEnrollLottiePerStage(SFPS_STAGE_RIGHT_EDGE)
        ).isEqualTo(
            settingsContext.resources.getIdentifier(
                "sfps_lottie_right_edge",
                type,
                settingsPackageName)
        )
    }
}