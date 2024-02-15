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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@RequiresFlagsDisabled(android.permission.flags.Flags.FLAG_WALLET_ROLE_ENABLED)
public class NfcForegroundPreferenceControllerTest {

    private static final String PREF_KEY = PaymentSettingsTest.FOREGROUND_KEY;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private PaymentBackend mPaymentBackend;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PackageManager mManager;

    private Context mContext;
    private ListPreference mPreference;
    private NfcForegroundPreferenceController mController;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mManager);
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new NfcForegroundPreferenceController(mContext, PREF_KEY);
        mPreference = new ListPreference(mContext);
        mPreference.setEntries(R.array.nfc_payment_favor);
        mPreference.setEntryValues(R.array.nfc_payment_favor_values);
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

        mPreference.setValueIndex(1);
        mPreference.callChangeListener(mPreference.getEntryValues()[1]);
        verify(mPaymentBackend).setForegroundMode(true);
        assertThat(mPreference.getEntry()).isEqualTo(favorOpen);
        assertThat(mPreference.getSummary()).isEqualTo(favorOpen);

        mPreference.setValueIndex(0);
        mPreference.callChangeListener(mPreference.getEntryValues()[0]);
        verify(mPaymentBackend).setForegroundMode(false);
        assertThat(mPreference.getEntry()).isEqualTo(favorDefault);
        assertThat(mPreference.getSummary()).isEqualTo(favorDefault);
    }

    @Test
    public void changeOptions_checkMetrics() {
        initPaymentApps();
        mController.displayPreference(mScreen);
        mController.onPaymentAppsChanged();

        mPreference.setValueIndex(1);
        mPreference.callChangeListener(mPreference.getEntryValues()[1]);
        verify(mPaymentBackend).setForegroundMode(true);
        verify(mFakeFeatureFactory.metricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_NFC_PAYMENT_FOREGROUND_SETTING);

        mPreference.setValueIndex(0);
        mPreference.callChangeListener(mPreference.getEntryValues()[0]);
        verify(mPaymentBackend).setForegroundMode(false);
        verify(mFakeFeatureFactory.metricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_NFC_PAYMENT_ALWAYS_SETTING);
    }
}
