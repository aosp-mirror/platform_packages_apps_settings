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

package com.android.settings.biometrics2.ui.model;

import static com.android.settings.biometrics.BiometricEnrollActivity.EXTRA_SKIP_INTRO;
import static com.android.settings.biometrics2.ui.model.EnrollmentRequest.EXTRA_SKIP_FIND_SENSOR;

import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_PORTAL_SETUP;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW;
import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW;
import static com.google.common.truth.Truth.assertThat;

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EnrollmentRequestTest {

    @NonNull
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void testIsSuw() {
        // Default false
        assertThat(new EnrollmentRequest(new Intent(), mContext, true).isSuw()).isFalse();
        assertThat(new EnrollmentRequest(new Intent(), mContext, false).isSuw()).isFalse();

        final Intent trueIntent = new Intent();
        trueIntent.putExtra(EXTRA_IS_SETUP_FLOW, true);
        assertThat(new EnrollmentRequest(trueIntent, mContext, true).isSuw()).isTrue();
        assertThat(new EnrollmentRequest(trueIntent, mContext, false).isSuw()).isFalse();

        final Intent falseIntent = new Intent();
        trueIntent.putExtra(EXTRA_IS_SETUP_FLOW, false);
        assertThat(new EnrollmentRequest(falseIntent, mContext, true).isSuw()).isFalse();
        assertThat(new EnrollmentRequest(falseIntent, mContext, false).isSuw()).isFalse();
    }

    @Test
    public void testIsAfterSuwOrSuwSuggestedAction() {
        // Default false
        assertThat(new EnrollmentRequest(new Intent(), mContext, true)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();
        assertThat(new EnrollmentRequest(new Intent(), mContext, false)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();

        final Intent deferredTrueIntent = new Intent();
        deferredTrueIntent.putExtra(EXTRA_IS_DEFERRED_SETUP, true);
        assertThat(new EnrollmentRequest(deferredTrueIntent, mContext, true)
                .isAfterSuwOrSuwSuggestedAction()).isTrue();
        assertThat(new EnrollmentRequest(deferredTrueIntent, mContext, false)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();

        final Intent deferredFalseIntent = new Intent();
        deferredFalseIntent.putExtra(EXTRA_IS_DEFERRED_SETUP, false);
        assertThat(new EnrollmentRequest(deferredFalseIntent, mContext, false)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();
        assertThat(new EnrollmentRequest(deferredFalseIntent, mContext, false)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();

        final Intent portalTrueIntent = new Intent();
        portalTrueIntent.putExtra(EXTRA_IS_PORTAL_SETUP, true);
        assertThat(new EnrollmentRequest(portalTrueIntent, mContext, true)
                .isAfterSuwOrSuwSuggestedAction()).isTrue();
        assertThat(new EnrollmentRequest(portalTrueIntent, mContext, false)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();

        final Intent portalFalseIntent = new Intent();
        portalFalseIntent.putExtra(EXTRA_IS_PORTAL_SETUP, false);
        assertThat(new EnrollmentRequest(portalFalseIntent, mContext, false)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();
        assertThat(new EnrollmentRequest(portalFalseIntent, mContext, false)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();

        final Intent suggestedTrueIntent = new Intent();
        suggestedTrueIntent.putExtra(EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW, true);
        assertThat(new EnrollmentRequest(suggestedTrueIntent, mContext, true)
                .isAfterSuwOrSuwSuggestedAction()).isTrue();
        assertThat(new EnrollmentRequest(suggestedTrueIntent, mContext, false)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();

        final Intent suggestedFalseIntent = new Intent();
        suggestedFalseIntent.putExtra(EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW, false);
        assertThat(new EnrollmentRequest(suggestedFalseIntent, mContext, false)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();
        assertThat(new EnrollmentRequest(suggestedFalseIntent, mContext, false)
                .isAfterSuwOrSuwSuggestedAction()).isFalse();
    }

    @Test
    public void testGetSuwExtras_inSuw() {
        final Intent suwIntent = new Intent();
        suwIntent.putExtra(EXTRA_IS_SETUP_FLOW, true);
        final EnrollmentRequest setupRequest = new EnrollmentRequest(suwIntent, mContext, true);

        final Bundle bundle = setupRequest.getSuwExtras();
        assertThat(bundle).isNotNull();
        assertThat(bundle.size()).isAtLeast(1);
        assertThat(bundle.getBoolean(EXTRA_IS_SETUP_FLOW)).isTrue();
    }

    @Test
    public void testGetSuwExtras_notInSuw() {
        final Intent suwIntent = new Intent();
        suwIntent.putExtra(EXTRA_IS_SETUP_FLOW, true);
        final EnrollmentRequest setupRequest = new EnrollmentRequest(suwIntent, mContext, false);

        final Bundle bundle = setupRequest.getSuwExtras();
        assertThat(bundle).isNotNull();
        assertThat(bundle.size()).isEqualTo(0);
    }

    @Test
    public void testIsSkipIntro() {
        // Default false
        assertThat(new EnrollmentRequest(new Intent(), mContext, true).isSkipIntro()).isFalse();
        assertThat(new EnrollmentRequest(new Intent(), mContext, false).isSkipIntro()).isFalse();

        final Intent trueIntent = new Intent();
        trueIntent.putExtra(EXTRA_SKIP_INTRO, true);
        assertThat(new EnrollmentRequest(trueIntent, mContext, true).isSkipIntro()).isTrue();
        assertThat(new EnrollmentRequest(trueIntent, mContext, false).isSkipIntro()).isTrue();

        final Intent falseIntent = new Intent();
        falseIntent.putExtra(EXTRA_SKIP_INTRO, false);
        assertThat(new EnrollmentRequest(falseIntent, mContext, false).isSkipIntro()).isFalse();
        assertThat(new EnrollmentRequest(falseIntent, mContext, false).isSkipIntro()).isFalse();
    }

    @Test
    public void testIsSkipFindSensor() {
        // Default false
        assertThat(new EnrollmentRequest(new Intent(), mContext, true).isSkipFindSensor())
                .isFalse();
        assertThat(new EnrollmentRequest(new Intent(), mContext, false).isSkipFindSensor())
                .isFalse();

        final Intent trueIntent = new Intent();
        trueIntent.putExtra(EXTRA_SKIP_FIND_SENSOR, true);
        assertThat(new EnrollmentRequest(trueIntent, mContext, true).isSkipFindSensor()).isTrue();
        assertThat(new EnrollmentRequest(trueIntent, mContext, false).isSkipFindSensor()).isTrue();

        final Intent falseIntent = new Intent();
        falseIntent.putExtra(EXTRA_SKIP_FIND_SENSOR, false);
        assertThat(new EnrollmentRequest(falseIntent, mContext, false).isSkipFindSensor())
                .isFalse();
        assertThat(new EnrollmentRequest(falseIntent, mContext, false).isSkipFindSensor())
                .isFalse();
    }

}
