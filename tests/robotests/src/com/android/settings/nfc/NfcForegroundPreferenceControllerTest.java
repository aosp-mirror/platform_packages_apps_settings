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

import androidx.preference.DropDownPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class NfcForegroundPreferenceControllerTest {

    private static final String PREF_KEY = PaymentSettingsTest.FOREGROUND_KEY;

    @Mock
    private PaymentBackend mPaymentBackend;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PackageManager mManager;

    private Context mContext;
    private DropDownPreference mPreference;
    private NfcForegroundPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mManager);
        mController = new NfcForegroundPreferenceController(mContext, PREF_KEY);
        mPreference = new DropDownPreference(mContext);
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus_noNFC_DISABLED() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(false);

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(NfcForegroundPreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noPaymentBackend_DISABLED() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(NfcForegroundPreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noPaymentApps_DISABLED() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(true);
        mController.setPaymentBackend(mPaymentBackend);
        when(mPaymentBackend.getPaymentAppInfos()).thenReturn(null);

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(NfcForegroundPreferenceController.AVAILABLE);

        when(mPaymentBackend.getPaymentAppInfos()).thenReturn(new ArrayList<>());

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(NfcForegroundPreferenceController.AVAILABLE);
    }

    private void initPaymentApps() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(true);
        mController.setPaymentBackend(mPaymentBackend);
        final ArrayList<PaymentBackend.PaymentAppInfo> appInfos = new ArrayList<>();
        appInfos.add(new PaymentBackend.PaymentAppInfo());
        when(mPaymentBackend.getPaymentAppInfos()).thenReturn(appInfos);
    }

    @Test
    public void getAvailabilityStatus_hasPaymentApps_AVAILABLE() {
        initPaymentApps();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(NfcForegroundPreferenceController.AVAILABLE);
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
    public void changeOptions_shouldUpdateEntryAndSummary() {
        initPaymentApps();
        mController.displayPreference(mScreen);
        mController.onPaymentAppsChanged();

        final CharSequence favorDefault = mContext.getText(R.string.nfc_payment_favor_default);
        final CharSequence favorOpen = mContext.getText(R.string.nfc_payment_favor_open);

        assertThat(mPreference.getEntry()).isEqualTo(favorDefault);
        assertThat(mPreference.getSummary()).isEqualTo(favorDefault);

        mPreference.setValueIndex(0);
        mPreference.callChangeListener(mPreference.getEntryValues()[0]);
        verify(mPaymentBackend).setForegroundMode(true);
        assertThat(mPreference.getEntry()).isEqualTo(favorOpen);
        assertThat(mPreference.getSummary()).isEqualTo(favorOpen);

        mPreference.setValueIndex(1);
        mPreference.callChangeListener(mPreference.getEntryValues()[1]);
        verify(mPaymentBackend).setForegroundMode(false);
        assertThat(mPreference.getEntry()).isEqualTo(favorDefault);
        assertThat(mPreference.getSummary()).isEqualTo(favorDefault);
    }
}