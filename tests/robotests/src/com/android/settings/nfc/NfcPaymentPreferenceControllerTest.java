/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.nfc;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;
import com.android.settings.testutils.shadow.ShadowNfcAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowNfcAdapter.class)
public class NfcPaymentPreferenceControllerTest {

    private static final String PREF_KEY = PaymentSettingsTest.PAYMENT_KEY;

    @Mock
    private PaymentBackend mPaymentBackend;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PackageManager mManager;

    private Context mContext;
    private NfcPaymentPreference mPreference;
    private NfcPaymentPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mManager);
        mController = new NfcPaymentPreferenceController(mContext, PREF_KEY);
        mPreference = spy(new NfcPaymentPreference(mContext, null));
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus_noNFC_DISABLED() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(false);

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(NfcPaymentPreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noPaymentApps_DISABLED() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(true);
        mController.setPaymentBackend(mPaymentBackend);
        when(mPaymentBackend.getPaymentAppInfos()).thenReturn(null);

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(NfcPaymentPreferenceController.AVAILABLE);

        when(mPaymentBackend.getPaymentAppInfos()).thenReturn(new ArrayList<>());

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(NfcPaymentPreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasPaymentApps_AVAILABLE() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(true);
        mController.setPaymentBackend(mPaymentBackend);
        final ArrayList<PaymentAppInfo> appInfos = new ArrayList<>();
        appInfos.add(new PaymentAppInfo());
        when(mPaymentBackend.getPaymentAppInfos()).thenReturn(appInfos);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(NfcPaymentPreferenceController.AVAILABLE);
    }

    @Test
    public void onStart_shouldRegisterCallback() {
        mController.setPaymentBackend(mPaymentBackend);

        mController.onStart();

        verify(mPaymentBackend).registerCallback(mController);
    }

    @Test
    public void onStop_shouldUnregisterCallback() {
        mController.setPaymentBackend(mPaymentBackend);
        mController.onStart();

        mController.onStop();

        verify(mPaymentBackend).unregisterCallback(mController);
    }

    @Test
    public void displayPreference_shouldInitialize() {
        mController.setPaymentBackend(mPaymentBackend);

        mController.displayPreference(mScreen);

        verify(mPreference).initialize(mController);
    }

    @Test
    public void onPaymentAppsChanged_shouldRefreshSummary() {
        mController.setPaymentBackend(mPaymentBackend);
        mController.displayPreference(mScreen);
        when(mPaymentBackend.getDefaultApp()).thenReturn(null);

        mController.onPaymentAppsChanged();

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.nfc_payment_default_not_set));

        final PaymentAppInfo appInfo = new PaymentAppInfo();
        appInfo.label = "test label";
        when(mPaymentBackend.getDefaultApp()).thenReturn(appInfo);

        mController.onPaymentAppsChanged();

        assertThat(mPreference.getSummary()).isEqualTo(appInfo.label);
    }
}