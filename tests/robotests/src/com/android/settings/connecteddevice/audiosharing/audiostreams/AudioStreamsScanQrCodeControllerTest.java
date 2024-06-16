/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothUtils.class,
            ShadowAudioStreamsHelper.class,
        })
public class AudioStreamsScanQrCodeControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mBluetoothEventManager;
    @Mock private PreferenceScreen mScreen;
    @Mock private AudioStreamsDashboardFragment mFragment;
    @Mock private CachedBluetoothDevice mDevice;
    private Preference mPreference;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private AudioStreamsScanQrCodeController mController;
    private Context mContext;

    @Before
    public void setUp() {
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        when(mLocalBtManager.getEventManager()).thenReturn(mBluetoothEventManager);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mContext = ApplicationProvider.getApplicationContext();
        mController =
                new AudioStreamsScanQrCodeController(
                        mContext, AudioStreamsScanQrCodeController.KEY);
        mPreference = spy(new Preference(mContext));
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(AudioStreamsScanQrCodeController.KEY);
    }

    @After
    public void tearDown() {
        ShadowAudioStreamsHelper.reset();
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void getAvailabilityStatus() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getPreferenceKey() {
        var key = mController.getPreferenceKey();

        assertThat(key).isEqualTo(AudioStreamsScanQrCodeController.KEY);
    }

    @Test
    public void onStart_registerCallback() {
        mController.onStart(mLifecycleOwner);

        verify(mBluetoothEventManager).registerCallback(any());
    }

    @Test
    public void onStop_unregisterCallback() {
        mController.onStop(mLifecycleOwner);

        verify(mBluetoothEventManager).unregisterCallback(any());
    }

    @Test
    public void onDisplayPreference_setOnclick() {
        mController.displayPreference(mScreen);

        verify(mPreference).setOnPreferenceClickListener(any());
    }

    @Test
    public void onPreferenceClick_noFragment_doNothing() {
        mController.displayPreference(mScreen);

        var listener = mPreference.getOnPreferenceClickListener();
        assertThat(listener).isNotNull();
        var clicked = listener.onPreferenceClick(mPreference);
        assertThat(clicked).isFalse();
    }

    @Test
    public void onPreferenceClick_hasFragment_launchSubSetting() {
        mController.displayPreference(mScreen);
        mController.setFragment(mFragment);

        var listener = mPreference.getOnPreferenceClickListener();
        assertThat(listener).isNotNull();
        var clicked = listener.onPreferenceClick(mPreference);
        assertThat(clicked).isTrue();
    }

    @Test
    public void updateVisibility_noConnected_invisible() {
        mController.displayPreference(mScreen);
        mController.mBluetoothCallback.onActiveDeviceChanged(mDevice, BluetoothProfile.LE_AUDIO);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_hasConnected_visible() {
        mController.displayPreference(mScreen);
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);
        mController.mBluetoothCallback.onActiveDeviceChanged(mDevice, BluetoothProfile.LE_AUDIO);

        assertThat(mPreference.isVisible()).isTrue();
    }
}
