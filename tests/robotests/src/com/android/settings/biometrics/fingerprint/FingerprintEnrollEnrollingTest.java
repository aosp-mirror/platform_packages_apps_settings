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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;
import android.hardware.fingerprint.FingerprintSensorProperties;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.CancellationSignal;
import android.os.Vibrator;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class FingerprintEnrollEnrollingTest {

    @Mock private FingerprintManager mFingerprintManager;

    @Mock private Vibrator mVibrator;

    private FingerprintEnrollEnrolling mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
    }

    @Test
    @Ignore
    public void fingerprintEnrollHelp_shouldShowHelpText() {
        EnrollmentCallback enrollmentCallback = verifyAndCaptureEnrollmentCallback();

        enrollmentCallback.onEnrollmentProgress(123);
        enrollmentCallback.onEnrollmentHelp(
                FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS, "test enrollment help");

        TextView errorText = mActivity.findViewById(R.id.error_text);
        assertThat(errorText.getText()).isEqualTo("test enrollment help");
    }

    @Test
    public void fingerprintUdfpsEnrollSuccessProgress_shouldNotVibrate() {
        initializeActivityFor(FingerprintSensorProperties.TYPE_UDFPS_OPTICAL);

        mActivity.onEnrollmentProgressChange(1, 1);

        verify(mVibrator, never()).vibrate(anyInt(), anyString(), any(), anyString(), any());
    }

    @Test
    public void fingerprintRearEnrollSuccessProgress_shouldNotVibrate() {
        initializeActivityFor(FingerprintSensorProperties.TYPE_REAR);

        mActivity.onEnrollmentProgressChange(1, 1);

        verify(mVibrator, never()).vibrate(anyInt(), anyString(), any(), anyString(), any());
    }

    private void initializeActivityFor(int sensorType) {
        final List<ComponentInfoInternal> componentInfo = new ArrayList<>();
        final FingerprintSensorPropertiesInternal prop =
                new FingerprintSensorPropertiesInternal(
                        0 /* sensorId */,
                        SensorProperties.STRENGTH_STRONG,
                        1 /* maxEnrollmentsPerUser */,
                        componentInfo,
                        sensorType,
                        true /* resetLockoutRequiresHardwareAuthToken */);
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        props.add(prop);
        when(mFingerprintManager.getSensorPropertiesInternal()).thenReturn(props);

        mActivity = spy(FingerprintEnrollEnrolling.class);
        doReturn(true).when(mActivity).shouldShowLottie();
        doReturn(mFingerprintManager).when(mActivity).getSystemService(FingerprintManager.class);
        doReturn(mVibrator).when(mActivity).getSystemService(Vibrator.class);

        ActivityController.of(mActivity).create();
    }

    private EnrollmentCallback verifyAndCaptureEnrollmentCallback() {
        ArgumentCaptor<EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(EnrollmentCallback.class);
        verify(mFingerprintManager)
                .enroll(
                        any(byte[].class),
                        any(CancellationSignal.class),
                        anyInt(),
                        callbackCaptor.capture(),
                        eq(FingerprintManager.ENROLL_ENROLL));

        return callbackCaptor.getValue();
    }
}
