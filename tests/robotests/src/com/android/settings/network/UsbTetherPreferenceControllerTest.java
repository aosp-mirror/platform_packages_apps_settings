/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network;

import static com.android.settings.network.TetherEnabler.USB_TETHER_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class UsbTetherPreferenceControllerTest {

    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private SharedPreferences mSharedPreferences;

    private Context mContext;
    private UsbTetherPreferenceController mController;
    private SwitchPreference mSwitchPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(
                mConnectivityManager);
        when(mConnectivityManager.getTetherableUsbRegexs()).thenReturn(new String[]{""});
        when(mContext.getSharedPreferences(TetherEnabler.SHARED_PREF, Context.MODE_PRIVATE))
                .thenReturn(mSharedPreferences);
        mController = new UsbTetherPreferenceController(mContext, USB_TETHER_KEY);
        mSwitchPreference = spy(SwitchPreference.class);
        ReflectionHelpers.setField(mController, "mPreference", mSwitchPreference);
    }

    @Test
    public void lifecycle_shouldRegisterReceiverOnStart() {
        mController.onStart();

        verify(mContext).registerReceiver(eq(mController.mUsbChangeReceiver), any());
    }

    @Test
    public void lifecycle_shouldUnregisterReceiverOnStop() {
        mController.onStart();
        mController.onStop();

        verify(mContext).unregisterReceiver(eq(mController.mUsbChangeReceiver));
    }

    @Test
    public void display_availableChangedCorrectly() {
        when(mConnectivityManager.getTetherableUsbRegexs()).thenReturn(new String[]{""});
        assertThat(mController.isAvailable()).isTrue();

        when(mConnectivityManager.getTetherableUsbRegexs()).thenReturn(new String[0]);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void switch_shouldCheckedWhenSharedPreferencesIsTrue() {
        when(mSharedPreferences.getBoolean(USB_TETHER_KEY, false)).thenReturn(true);
        mController.onSharedPreferenceChanged(mSharedPreferences, USB_TETHER_KEY);

        verify(mSwitchPreference).setChecked(true);
    }

    @Test
    public void switch_shouldUnCheckedWhenSharedPreferencesIsFalse() {
        when(mSharedPreferences.getBoolean(USB_TETHER_KEY, false)).thenReturn(false);
        mController.onSharedPreferenceChanged(mSharedPreferences, USB_TETHER_KEY);

        verify(mSwitchPreference).setChecked(false);
    }
}
