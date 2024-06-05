/*
 * Copyright (C) 2023 The Android Open Source Project
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.FeatureFlagUtils;
import android.widget.CompoundButton;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import com.google.common.collect.ImmutableList;

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

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowThreadUtils.class,
        })
public class AudioSharingSwitchBarControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private CachedBluetoothDeviceManager mDeviceManager;
    @Mock private LocalBluetoothProfileManager mBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private CompoundButton mBtnView;
    @Mock private CachedBluetoothDevice mCachedDevice;
    @Mock private BluetoothDevice mDevice;
    private SettingsMainSwitchBar mSwitchBar;
    private AudioSharingSwitchBarController mController;
    private AudioSharingSwitchBarController.OnAudioSharingStateChangedListener mListener;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private boolean mOnAudioSharingStateChanged;
    private boolean mOnAudioSharingServiceConnected;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private LocalBluetoothManager mLocalBluetoothManager;

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
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mBtProfileManager);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.findDevice(mDevice)).thenReturn(mCachedDevice);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getGroupId()).thenReturn(1);
        when(mCachedDevice.getName()).thenReturn("test");
        when(mBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        when(mBtProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mVolumeControl.isProfileReady()).thenReturn(true);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        doNothing()
                .when(mBroadcast)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        doNothing()
                .when(mBroadcast)
                .unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
        when(mAssistant.isProfileReady()).thenReturn(true);
        doNothing()
                .when(mAssistant)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        doNothing()
                .when(mAssistant)
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        mSwitchBar = new SettingsMainSwitchBar(mContext);
        mSwitchBar.setDisabledByAdmin(mock(RestrictedLockUtils.EnforcedAdmin.class));
        mOnAudioSharingStateChanged = false;
        mOnAudioSharingServiceConnected = false;
        mListener =
                new AudioSharingSwitchBarController.OnAudioSharingStateChangedListener() {
                    @Override
                    public void onAudioSharingStateChanged() {
                        mOnAudioSharingStateChanged = true;
                    }

                    @Override
                    public void onAudioSharingProfilesConnected() {
                        mOnAudioSharingServiceConnected = true;
                    }
                };
        mController = new AudioSharingSwitchBarController(mContext, mSwitchBar, mListener);
    }

    @Test
    public void bluetoothOff_switchDisabled() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mSwitchBar.isEnabled()).isTrue();
        mContext.registerReceiver(
                mController.mReceiver,
                mController.mIntentFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);
        mShadowBluetoothAdapter.setEnabled(false);
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        mContext.sendBroadcast(intent);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mSwitchBar.isEnabled()).isFalse();
        assertThat(mSwitchBar.isChecked()).isFalse();
        assertThat(mOnAudioSharingStateChanged).isTrue();
        assertThat(mOnAudioSharingServiceConnected).isFalse();
    }

    @Test
    public void onServiceConnected_switchEnabled() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        mController.onServiceConnected();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mSwitchBar.isEnabled()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();
        assertThat(mOnAudioSharingStateChanged).isTrue();
        assertThat(mOnAudioSharingServiceConnected).isTrue();
        verify(mBtProfileManager).removeServiceListener(mController);
    }

    @Test
    public void getAvailabilityStatus_flagOn() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagOff() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void onStart_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mContext, times(0))
                .registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class), anyInt());
        verify(mBroadcast, times(0))
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant, times(0))
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mBtProfileManager, times(0)).addServiceListener(mController);
    }

    @Test
    public void onStart_flagOnProfileNotReady_registerProfileCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mContext)
                .registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class), anyInt());
        verify(mBroadcast, times(0))
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant, times(0))
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mBtProfileManager).addServiceListener(mController);
        assertThat(mSwitchBar.isChecked()).isFalse();
        assertThat(mSwitchBar.isEnabled()).isFalse();
    }

    @Test
    public void onStart_flagOn_registerCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mContext)
                .registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class), anyInt());
        verify(mBroadcast)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mBtProfileManager, times(0)).addServiceListener(mController);
        assertThat(mSwitchBar.isChecked()).isTrue();
        assertThat(mSwitchBar.isEnabled()).isTrue();
    }

    @Test
    public void onStop_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mContext, times(0)).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mBroadcast, times(0))
                .unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant, times(0))
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mBtProfileManager, times(0)).removeServiceListener(mController);
    }

    @Test
    public void onStop_flagOn_notRegistered_doNothing() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.setCallbacksRegistered(false);
        doNothing().when(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        mController.onStop(mLifecycleOwner);

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mBtProfileManager).removeServiceListener(mController);
        verify(mBroadcast, times(0))
                .unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant, times(0))
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void onStop_flagOn_registered_unregisterCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.setCallbacksRegistered(true);
        mContext.registerReceiver(
                mController.mReceiver,
                mController.mIntentFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);
        mController.onStop(mLifecycleOwner);
        verify(mContext).unregisterReceiver(mController.mReceiver);
        verify(mBtProfileManager).removeServiceListener(mController);
        verify(mBroadcast).unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant)
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void onCheckedChangedToChecked_sharing_doNothing() {
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        verify(mBroadcast, times(0)).startPrivateBroadcast();
    }

    @Test
    public void onCheckedChangedToChecked_noConnectedLeaDevices_flagOn_notStartAudioSharing() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(ImmutableList.of());
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        assertThat(mSwitchBar.isChecked()).isFalse();
        verify(mBroadcast, times(0)).startPrivateBroadcast();
    }

    @Test
    public void onCheckedChangedToChecked_noConnectedLeaDevices_flagOff_startAudioSharing() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, false);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(ImmutableList.of());
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        verify(mBroadcast).startPrivateBroadcast();
    }

    @Test
    public void onCheckedChangedToChecked_notSharing_withConnectedLeaDevices_startAudioSharing() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(ImmutableList.of(mDevice));
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        verify(mBroadcast).startPrivateBroadcast();
    }

    @Test
    public void onCheckedChangedToUnChecked_notSharing_doNothing() {
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        mController.onCheckedChanged(mBtnView, /* isChecked= */ false);
        verify(mBroadcast, times(0)).stopBroadcast(anyInt());
    }

    @Test
    public void onCheckedChangedToUnChecked_sharing_stopAudioSharing() {
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        doNothing().when(mBroadcast).stopBroadcast(anyInt());
        mController.onCheckedChanged(mBtnView, /* isChecked= */ false);
        verify(mBroadcast).stopBroadcast(1);
    }
}
