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
 * limitations under the License.
 */

package com.android.settings.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TopLevelSecurityPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private FaceManager mFaceManager;
    private TopLevelSecurityEntryPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.FINGERPRINT_SERVICE))
                .thenReturn(mFingerprintManager);
        when(mContext.getSystemService(Context.FACE_SERVICE))
                .thenReturn(mFaceManager);
        mController = new TopLevelSecurityEntryPreferenceController(mContext, "test_key");
    }

    @Test
    public void geSummary_hasFace_hasStaticSummary() {
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE))
                .thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        mController.getSummary();

        verify(mContext).getText(R.string.security_dashboard_summary_face);
    }

    @Test
    public void geSummary_hasFingerPrint_hasStaticSummary() {
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE))
                .thenReturn(false);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);

        mController.getSummary();

        verify(mContext).getText(R.string.security_dashboard_summary);
    }

    @Test
    public void geSummary_noFpFeature_shouldSetSummaryWithNoBiometrics() {
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(false);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE))
                .thenReturn(false);

        mController.getSummary();

        verify(mContext).getText(R.string.security_dashboard_summary_no_fingerprint);
    }

    @Test
    public void geSummary_noFpHardware_shouldSetSummaryWithNoBiometrics() {
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE))
                .thenReturn(false);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);

        mController.getSummary();

        verify(mContext).getText(R.string.security_dashboard_summary_no_fingerprint);
    }

    @Test
    public void geSummary_noFaceFeature_shouldSetSummaryWithNoBiometrics() {
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(false);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE))
                .thenReturn(false);

        mController.getSummary();

        verify(mContext).getText(R.string.security_dashboard_summary_no_fingerprint);
    }

    @Test
    public void geSummary_noFaceHardware_shouldSetSummaryWithNoBiometrics() {
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE))
                .thenReturn(true);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(false);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        mController.getSummary();

        verify(mContext).getText(R.string.security_dashboard_summary_no_fingerprint);
    }
}
