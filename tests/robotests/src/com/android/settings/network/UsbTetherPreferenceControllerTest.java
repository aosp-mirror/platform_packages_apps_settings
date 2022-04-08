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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.TetheringManager;

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
    private TetherEnabler mTetherEnabler;

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
        mController = new UsbTetherPreferenceController(mContext, "USB");
        mController.setTetherEnabler(mTetherEnabler);
        mSwitchPreference = spy(SwitchPreference.class);
        ReflectionHelpers.setField(mController, "mPreference", mSwitchPreference);
    }

    @Test
    public void lifecycle_shouldRegisterReceiverOnStart() {
        mController.onStart();

        verify(mContext).registerReceiver(eq(mController.mUsbChangeReceiver), any());
    }

    @Test
    public void lifecycle_shouldAddListenerOnResume() {
        mController.onResume();
        verify(mTetherEnabler).addListener(mController);
    }

    @Test
    public void lifecycle_shouldRemoveListenrOnPause() {
        mController.onPause();
        verify(mTetherEnabler).removeListener(mController);
    }

    @Test
    public void lifecycle_shouldUnregisterReceiverOnStop() {
        mController.onStart();
        mController.onStop();

        verify(mContext).unregisterReceiver(eq(mController.mUsbChangeReceiver));
    }

    @Test
    public void shouldShow_noTetherableUsb() {
        when(mConnectivityManager.getTetherableUsbRegexs()).thenReturn(new String[0]);
        assertThat(mController.shouldShow()).isFalse();
    }

    @Test
    public void shouldEnable_noUsbConnected() {
        ReflectionHelpers.setField(mController, "mUsbConnected", false);
        assertThat(mController.shouldEnable()).isFalse();
    }

    @Test
    public void setChecked_shouldStartUsbTethering() {
        mController.setChecked(true);
        verify(mTetherEnabler).startTethering(TetheringManager.TETHERING_USB);
    }

    @Test
    public void setUnchecked_shouldStopUsbTethering() {
        mController.setChecked(false);
        verify(mTetherEnabler).stopTethering(TetheringManager.TETHERING_USB);
    }

    @Test
    public void switch_shouldCheckedWhenUsbTethering() {
        mController.onTetherStateUpdated(TetherEnabler.TETHERING_USB_ON);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void switch_shouldUnCheckedWhenUsbNotTethering() {
        mController.onTetherStateUpdated(TetherEnabler.TETHERING_OFF);
        assertThat(mController.isChecked()).isFalse();
    }
}
