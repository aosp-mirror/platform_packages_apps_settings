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

package com.android.settings.applications.defaultapps;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.TestConfig;
import com.android.settings.nfc.PaymentBackend;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DefaultPaymentSettingsPreferenceControllerTest {

    @Mock
    private NfcAdapter mNfcAdapter;
    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PaymentBackend mPaymentBackend;

    private DefaultPaymentSettingsPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = new DefaultPaymentSettingsPreferenceController(mContext);
        ReflectionHelpers.setField(mController, "mNfcAdapter", mNfcAdapter);

        mPreference = new Preference(RuntimeEnvironment.application);
    }

    @Test
    public void isAvailable_hasNfc_shouldReturnTrue() {
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mNfcAdapter.isEnabled()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noNfcAdapter_shouldReturnFalse() {
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        when(mUserManager.isAdminUser()).thenReturn(true);
        ReflectionHelpers.setField(mController, "mNfcAdapter", null);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_shouldSetSummaryToDefaultPaymentApp() {
        final PaymentBackend.PaymentAppInfo defaultApp = mock(PaymentBackend.PaymentAppInfo.class);
        defaultApp.label = "test_payment_app";
        when(mPaymentBackend.getDefaultApp()).thenReturn(defaultApp);
        ReflectionHelpers.setField(mController, "mPaymentBackend", mPaymentBackend);

        mController.updateState(mPreference);

        verify(mPaymentBackend).refresh();
        assertThat(mPreference.getSummary()).isEqualTo(defaultApp.label);
    }
}
