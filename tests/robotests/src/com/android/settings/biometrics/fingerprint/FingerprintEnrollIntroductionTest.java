/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorProperties;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;

import com.android.settings.R;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class FingerprintEnrollIntroductionTest {

    @Mock private FingerprintManager mFingerprintManager;

    private Context mContext;

    private FingerprintEnrollIntroduction mFingerprintEnrollIntroduction;

    private static final int MAX_ENROLLMENTS = 5;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());

        final List<ComponentInfoInternal> componentInfo = new ArrayList<>();
        final FingerprintSensorPropertiesInternal prop =
                new FingerprintSensorPropertiesInternal(
                        0 /* sensorId */,
                        SensorProperties.STRENGTH_STRONG,
                        MAX_ENROLLMENTS /* maxEnrollmentsPerUser */,
                        componentInfo,
                        FingerprintSensorProperties.TYPE_REAR,
                        true /* resetLockoutRequiresHardwareAuthToken */);
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        props.add(prop);
        when(mFingerprintManager.getSensorPropertiesInternal()).thenReturn(props);
    }

    void setupFingerprintEnrollIntroWith(Intent intent) {
        ActivityController<FingerprintEnrollIntroduction> controller =
                Robolectric.buildActivity(FingerprintEnrollIntroduction.class, intent);
        mFingerprintEnrollIntroduction = spy(controller.get());
        ReflectionHelpers.setField(
                mFingerprintEnrollIntroduction, "mFingerprintManager", mFingerprintManager);
        controller.create();
    }

    void setFingerprintManagerToHave(int numEnrollments) {
        List<Fingerprint> fingerprints = new ArrayList<>();
        for (int i = 0; i < numEnrollments; i++) {
            fingerprints.add(
                    new Fingerprint(
                            "Fingerprint " + i /* name */, 1 /*fingerId */, 1 /* deviceId */));
        }
        when(mFingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(fingerprints);
    }

    @Test
    public void intro_CheckCanEnrollNormal() {
        setupFingerprintEnrollIntroWith(new Intent());
        setFingerprintManagerToHave(3 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void intro_CheckMaxEnrolledNormal() {
        setupFingerprintEnrollIntroWith(new Intent());
        setFingerprintManagerToHave(7 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(R.string.fingerprint_intro_error_max);
    }

    @Test
    public void intro_CheckCanEnrollDuringSUW() {
        // This code path should depend on suw_max_fingerprints_enrollable versus
        // FingerprintManager.getSensorProperties...maxEnrollmentsPerUser()
        Resources resources = mock(Resources.class);
        when(resources.getInteger(anyInt())).thenReturn(5);
        when(mContext.getResources()).thenReturn(resources);

        setupFingerprintEnrollIntroWith(
                new Intent()
                        .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true)
                        .putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true));
        setFingerprintManagerToHave(0 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void intro_CheckMaxEnrolledDuringSUW() {
        // This code path should depend on suw_max_fingerprints_enrollable versus
        // FingerprintManager.getSensorProperties...maxEnrollmentsPerUser()
        Resources resources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(resources);
        when(resources.getInteger(anyInt())).thenReturn(1);

        setupFingerprintEnrollIntroWith(
                new Intent()
                        .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true)
                        .putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true));
        setFingerprintManagerToHave(1 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(R.string.fingerprint_intro_error_max);
    }

    @Test
    public void intro_CheckCanEnrollDuringDeferred() {
        setupFingerprintEnrollIntroWith(
                new Intent().putExtra(WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP, true));
        setFingerprintManagerToHave(2 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void intro_CheckMaxEnrolledDuringDeferred() {
        setupFingerprintEnrollIntroWith(
                new Intent().putExtra(WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP, true));
        setFingerprintManagerToHave(6 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(R.string.fingerprint_intro_error_max);
    }

    @Test
    public void intro_CheckCanEnrollDuringPortal() {
        setupFingerprintEnrollIntroWith(
                new Intent().putExtra(WizardManagerHelper.EXTRA_IS_PORTAL_SETUP, true));
        setFingerprintManagerToHave(2 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void intro_CheckMaxEnrolledDuringPortal() {
        setupFingerprintEnrollIntroWith(
                new Intent().putExtra(WizardManagerHelper.EXTRA_IS_PORTAL_SETUP, true));
        setFingerprintManagerToHave(6 /* numEnrollments */);
        int result = mFingerprintEnrollIntroduction.checkMaxEnrolled();

        assertThat(result).isEqualTo(R.string.fingerprint_intro_error_max);
    }
}
