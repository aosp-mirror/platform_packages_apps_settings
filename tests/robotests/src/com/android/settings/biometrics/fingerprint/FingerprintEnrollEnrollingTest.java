/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics.fingerprint;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;
import android.os.CancellationSignal;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowUtils;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class FingerprintEnrollEnrollingTest {

    @Mock
    private FingerprintManager mFingerprintManager;

    private FingerprintEnrollEnrolling mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUtils.setFingerprintManager(mFingerprintManager);

        FakeFeatureFactory.setupForTest();
        mActivity = Robolectric.buildActivity(
                FingerprintEnrollEnrolling.class,
                new Intent()
                        // Set the challenge token so the confirm screen will not be shown
                        .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]))
                .setup().get();
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void fingerprintEnrollHelp_shouldShowHelpText() {
        EnrollmentCallback enrollmentCallback = verifyAndCaptureEnrollmentCallback();

        enrollmentCallback.onEnrollmentProgress(123);
        enrollmentCallback.onEnrollmentHelp(
                FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS,
                "test enrollment help");

        TextView errorText = mActivity.findViewById(R.id.error_text);
        assertThat(errorText.getText()).isEqualTo("test enrollment help");
    }

    private EnrollmentCallback verifyAndCaptureEnrollmentCallback() {
        ArgumentCaptor<EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(EnrollmentCallback.class);
        verify(mFingerprintManager).enroll(
                any(byte[].class),
                any(CancellationSignal.class),
                anyInt(),
                anyInt(),
                callbackCaptor.capture());

        return callbackCaptor.getValue();
    }
}
