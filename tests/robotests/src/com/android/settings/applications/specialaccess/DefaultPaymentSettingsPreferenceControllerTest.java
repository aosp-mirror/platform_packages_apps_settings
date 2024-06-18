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

package com.android.settings.applications.specialaccess;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.UserManager;
import android.permission.flags.Flags;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class DefaultPaymentSettingsPreferenceControllerTest {

    private static final String PREF_KEY = "key";

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();
    @Mock
    private NfcAdapter mNfcAdapter;
    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private RoleManager mRoleManager;
    @Mock
    private Preference mPreference;

    private DefaultPaymentSettingsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mContext.getSystemService(RoleManager.class)).thenReturn(mRoleManager);
        mController = new DefaultPaymentSettingsPreferenceController(mContext, PREF_KEY);
        ReflectionHelpers.setField(mController, "mNfcAdapter", mNfcAdapter);
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
    public void getAvailabilityStatus_NfcIsDisabled_shouldReturnDisabled() {
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mNfcAdapter.isEnabled()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                DefaultPaymentSettingsPreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_WALLET_ROLE_ENABLED)
    public void handlePreferenceTreeClick_walletRoleEnabled_shouldReturnTrue() {
        when(mRoleManager.isRoleAvailable(anyString())).thenReturn(true);
        when(mPreference.getKey()).thenReturn(PREF_KEY);
        ArgumentCaptor<String> roleTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();
        verify(mRoleManager).isRoleAvailable(roleTypeCaptor.capture());
        verify(mContext).startActivity(intentArgumentCaptor.capture());
        assertThat(roleTypeCaptor.getValue()).isEqualTo(RoleManager.ROLE_WALLET);
        assertThat(intentArgumentCaptor.getValue().getAction())
                .isEqualTo(CardEmulation.ACTION_CHANGE_DEFAULT);
    }
}
