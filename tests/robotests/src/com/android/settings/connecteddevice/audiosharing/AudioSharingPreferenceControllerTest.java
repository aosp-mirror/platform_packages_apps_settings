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

package com.android.settings.connecteddevice.audiosharing;

import static android.bluetooth.BluetoothAdapter.STATE_OFF;
import static android.bluetooth.BluetoothAdapter.STATE_ON;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class AudioSharingPreferenceControllerTest {
    private static final String PREF_KEY = "audio_sharing_settings";
    private static final String SUMMARY_ON = "On";
    private static final String SUMMARY_OFF = "Off";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private PreferenceScreen mScreen;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mBtEventManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    private AudioSharingPreferenceController mController;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private LocalBluetoothManager mLocalBluetoothManager;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private Preference mPreference;

    @Before
    public void setUp() {
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getEventManager()).thenReturn(mBtEventManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        mController = new AudioSharingPreferenceController(mContext, PREF_KEY);
        mPreference = new Preference(mContext);
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);
    }

    @Test
    public void onStart_registerCallback() {
        mController.onStart(mLifecycleOwner);
        verify(mBtEventManager).registerCallback(mController);
        verify(mBroadcast).registerServiceCallBack(any(), any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void onStop_unregisterCallback() {
        mController.onStop(mLifecycleOwner);
        verify(mBtEventManager).unregisterCallback(mController);
        verify(mBroadcast).unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void getAvailabilityStatus_flagOn() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void getAvailabilityStatus_flagOff() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getSummary_broadcastOn() {
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        assertThat(mController.getSummary().toString()).isEqualTo(SUMMARY_ON);
    }

    @Test
    public void getSummary_broadcastOff() {
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        assertThat(mController.getSummary().toString()).isEqualTo(SUMMARY_OFF);
    }

    @Test
    public void onBluetoothStateChanged_refreshSummary() {
        mController.displayPreference(mScreen);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.onBluetoothStateChanged(STATE_ON);
        assertThat(mPreference.getSummary().toString()).isEqualTo(SUMMARY_ON);
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.onBluetoothStateChanged(STATE_OFF);
        assertThat(mPreference.getSummary().toString()).isEqualTo(SUMMARY_OFF);
    }
}
