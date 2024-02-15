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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.Looper;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
        })
public class CallsAndAlarmsPreferenceControllerTest {
    private static final String PREF_KEY = "calls_and_alarms";
    private static final String SUMMARY_EMPTY = "No active device in sharing";
    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final String TEST_SETTINGS_KEY =
            "bluetooth_le_broadcast_fallback_active_group_id";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private PreferenceScreen mScreen;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mBtEventManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private CachedBluetoothDeviceManager mCacheManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private BluetoothDevice mDevice1;
    @Mock private BluetoothDevice mDevice2;
    @Mock private BluetoothDevice mDevice3;
    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    @Mock private CachedBluetoothDevice mCachedDevice3;
    @Mock private BluetoothLeBroadcastReceiveState mState;
    private CallsAndAlarmsPreferenceController mController;
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
        when(mLocalBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        mController = new CallsAndAlarmsPreferenceController(mContext);
        mPreference = new Preference(mContext);
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);
    }

    @Test
    public void onStart_registerCallback() {
        mController.onStart(mLifecycleOwner);
        verify(mBtEventManager).registerCallback(mController);
        verify(mAssistant)
                .registerServiceCallBack(any(), any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void onStop_unregisterCallback() {
        mController.onStop(mLifecycleOwner);
        verify(mBtEventManager).unregisterCallback(mController);
        verify(mAssistant)
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
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
    public void updateVisibility_broadcastOffBluetoothOff() {
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mShadowBluetoothAdapter.setEnabled(false);
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_broadcastOnBluetoothOff() {
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mShadowBluetoothAdapter.setEnabled(false);
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_broadcastOffBluetoothOn() {
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_broadcastOnBluetoothOn() {
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        when(mAssistant.getConnectedDevices()).thenReturn(new ArrayList<BluetoothDevice>());
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary().toString()).isEqualTo(SUMMARY_EMPTY);
    }

    @Test
    public void onProfileConnectionStateChanged_updatePreference() {
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        when(mAssistant.getConnectedDevices()).thenReturn(new ArrayList<BluetoothDevice>());
        mController.displayPreference(mScreen);
        mController.onProfileConnectionStateChanged(
                mCachedDevice1, BluetoothAdapter.STATE_DISCONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary().toString()).isEqualTo(SUMMARY_EMPTY);
    }

    @Test
    public void updatePreference_showCorrectSummary() {
        final int groupId1 = 1;
        final int groupId2 = 2;
        Settings.Secure.putInt(mContext.getContentResolver(), TEST_SETTINGS_KEY, groupId1);
        when(mCachedDevice1.getGroupId()).thenReturn(groupId1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice2.getGroupId()).thenReturn(groupId1);
        when(mCachedDevice2.getDevice()).thenReturn(mDevice2);
        when(mCachedDevice1.getMemberDevice()).thenReturn(ImmutableSet.of(mCachedDevice2));
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice3.getGroupId()).thenReturn(groupId2);
        when(mCachedDevice3.getDevice()).thenReturn(mDevice3);
        when(mCachedDevice3.getName()).thenReturn(TEST_DEVICE_NAME2);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCacheManager);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mCacheManager.findDevice(mDevice2)).thenReturn(mCachedDevice2);
        when(mCacheManager.findDevice(mDevice3)).thenReturn(mCachedDevice3);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice2, mDevice3);
        when(mAssistant.getConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary().toString()).isEqualTo(TEST_DEVICE_NAME1);
    }
}
