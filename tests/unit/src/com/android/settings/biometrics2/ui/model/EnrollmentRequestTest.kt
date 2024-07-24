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
package com.android.settings.biometrics2.ui.model

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.biometrics.BiometricEnrollActivity
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnrollmentRequestTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testIsSuw() {
        // Default false
        Truth.assertThat(EnrollmentRequest(Intent(), context, true).isSuw).isFalse()
        Truth.assertThat(EnrollmentRequest(Intent(), context, false).isSuw).isFalse()
        val trueIntent = Intent()
        trueIntent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true)
        Truth.assertThat(EnrollmentRequest(trueIntent, context, true).isSuw).isTrue()
        Truth.assertThat(EnrollmentRequest(trueIntent, context, false).isSuw).isFalse()
        val falseIntent = Intent()
        trueIntent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, false)
        Truth.assertThat(EnrollmentRequest(falseIntent, context, true).isSuw).isFalse()
        Truth.assertThat(EnrollmentRequest(falseIntent, context, false).isSuw).isFalse()
    }

    @Test
    fun testIsAfterSuwOrSuwSuggestedAction() {
        // Default false
        Truth.assertThat(
            EnrollmentRequest(Intent(), context, true)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
        Truth.assertThat(
            EnrollmentRequest(Intent(), context, false)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
        val deferredTrueIntent = Intent()
        deferredTrueIntent.putExtra(WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP, true)
        Truth.assertThat(
            EnrollmentRequest(deferredTrueIntent, context, true)
                .isAfterSuwOrSuwSuggestedAction
        ).isTrue()
        Truth.assertThat(
            EnrollmentRequest(deferredTrueIntent, context, false)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
        val deferredFalseIntent = Intent()
        deferredFalseIntent.putExtra(WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP, false)
        Truth.assertThat(
            EnrollmentRequest(deferredFalseIntent, context, false)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
        Truth.assertThat(
            EnrollmentRequest(deferredFalseIntent, context, false)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
        val portalTrueIntent = Intent()
        portalTrueIntent.putExtra(WizardManagerHelper.EXTRA_IS_PORTAL_SETUP, true)
        Truth.assertThat(
            EnrollmentRequest(portalTrueIntent, context, true)
                .isAfterSuwOrSuwSuggestedAction
        ).isTrue()
        Truth.assertThat(
            EnrollmentRequest(portalTrueIntent, context, false)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
        val portalFalseIntent = Intent()
        portalFalseIntent.putExtra(WizardManagerHelper.EXTRA_IS_PORTAL_SETUP, false)
        Truth.assertThat(
            EnrollmentRequest(portalFalseIntent, context, false)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
        Truth.assertThat(
            EnrollmentRequest(portalFalseIntent, context, false)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
        val suggestedTrueIntent = Intent()
        suggestedTrueIntent.putExtra(WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW, true)
        Truth.assertThat(
            EnrollmentRequest(suggestedTrueIntent, context, true)
                .isAfterSuwOrSuwSuggestedAction
        ).isTrue()
        Truth.assertThat(
            EnrollmentRequest(suggestedTrueIntent, context, false)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
        val suggestedFalseIntent = Intent()
        suggestedFalseIntent.putExtra(WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW, false)
        Truth.assertThat(
            EnrollmentRequest(suggestedFalseIntent, context, false)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
        Truth.assertThat(
            EnrollmentRequest(suggestedFalseIntent, context, false)
                .isAfterSuwOrSuwSuggestedAction
        ).isFalse()
    }

    @Test
    fun testGetSuwExtras_inSuw() {
        val suwIntent = Intent()
        suwIntent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true)
        val setupRequest = EnrollmentRequest(suwIntent, context, true)
        val bundle = setupRequest.suwExtras
        Truth.assertThat(bundle).isNotNull()
        Truth.assertThat(bundle.size()).isAtLeast(1)
        Truth.assertThat(bundle.getBoolean(WizardManagerHelper.EXTRA_IS_SETUP_FLOW)).isTrue()
    }

    @Test
    fun testGetSuwExtras_notInSuw() {
        val suwIntent = Intent()
        suwIntent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true)
        val setupRequest = EnrollmentRequest(suwIntent, context, false)
        val bundle = setupRequest.suwExtras
        Truth.assertThat(bundle).isNotNull()
        Truth.assertThat(bundle.size()).isEqualTo(0)
    }

    @Test
    fun testIsSkipIntro() {
        // Default false
        Truth.assertThat(EnrollmentRequest(Intent(), context, true).isSkipIntro).isFalse()
        Truth.assertThat(EnrollmentRequest(Intent(), context, false).isSkipIntro).isFalse()
        val trueIntent = Intent()
        trueIntent.putExtra(BiometricEnrollActivity.EXTRA_SKIP_INTRO, true)
        Truth.assertThat(EnrollmentRequest(trueIntent, context, true).isSkipIntro).isTrue()
        Truth.assertThat(EnrollmentRequest(trueIntent, context, false).isSkipIntro).isTrue()
        val falseIntent = Intent()
        falseIntent.putExtra(BiometricEnrollActivity.EXTRA_SKIP_INTRO, false)
        Truth.assertThat(EnrollmentRequest(falseIntent, context, false).isSkipIntro).isFalse()
        Truth.assertThat(EnrollmentRequest(falseIntent, context, false).isSkipIntro).isFalse()
    }

    @Test
    fun testIsSkipFindSensor() {
        // Default false
        Truth.assertThat(EnrollmentRequest(Intent(), context, true).isSkipFindSensor)
            .isFalse()
        Truth.assertThat(EnrollmentRequest(Intent(), context, false).isSkipFindSensor)
            .isFalse()
        val trueIntent = Intent()
        trueIntent.putExtra(EnrollmentRequest.EXTRA_SKIP_FIND_SENSOR, true)
        Truth.assertThat(EnrollmentRequest(trueIntent, context, true).isSkipFindSensor).isTrue()
        Truth.assertThat(EnrollmentRequest(trueIntent, context, false).isSkipFindSensor).isTrue()
        val falseIntent = Intent()
        falseIntent.putExtra(EnrollmentRequest.EXTRA_SKIP_FIND_SENSOR, false)
        Truth.assertThat(EnrollmentRequest(falseIntent, context, false).isSkipFindSensor)
            .isFalse()
        Truth.assertThat(EnrollmentRequest(falseIntent, context, false).isSkipFindSensor)
            .isFalse()
    }
}
