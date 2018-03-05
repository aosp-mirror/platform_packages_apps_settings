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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;

import com.android.settings.R;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(SettingsRobolectricTestRunner.class)
public class SecuritySettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private SummaryLoader mSummaryLoader;
    @Mock
    private FingerprintManager mFingerprintManager;
    private SecuritySettings.SummaryProvider mSummaryProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.FINGERPRINT_SERVICE))
                .thenReturn(mFingerprintManager);

        mSummaryProvider = new SecuritySettings.SummaryProvider(mContext, mSummaryLoader);
    }

    @Test
    public void testSummaryProvider_notListening() {
        mSummaryProvider.setListening(false);

        verifyNoMoreInteractions(mSummaryLoader);
    }

    @Test
    public void testSummaryProvider_hasFingerPrint_hasStaticSummary() {
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);

        mSummaryProvider.setListening(true);

        verify(mContext).getString(R.string.security_dashboard_summary);
    }

    @Test
    public void testSummaryProvider_noFpFeature_shouldSetSummaryWithNoFingerprint() {
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(false);

        mSummaryProvider.setListening(true);

        verify(mContext).getString(R.string.security_dashboard_summary_no_fingerprint);
    }

    @Test
    public void testSummaryProvider_noFpHardware_shouldSetSummaryWithNoFingerprint() {
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);

        mSummaryProvider.setListening(true);

        verify(mContext).getString(R.string.security_dashboard_summary_no_fingerprint);
    }
}
