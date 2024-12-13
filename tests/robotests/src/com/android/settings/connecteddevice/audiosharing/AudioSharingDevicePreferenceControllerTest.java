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

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_BLUETOOTH_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import com.google.common.collect.ImmutableList;

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
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowFragment.class,
        })
public class AudioSharingDevicePreferenceControllerTest {
    private static final String KEY = "audio_sharing_device_list";
    private static final String KEY_AUDIO_SHARING_SETTINGS =
            "connected_device_audio_sharing_settings";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private AudioSharingBluetoothDeviceUpdater mBluetoothDeviceUpdater;
    @Mock private PreferenceManager mPreferenceManager;
    @Mock private CachedBluetoothDevice mCachedDevice;
    @Mock private BluetoothDevice mDevice;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mEventManager;
    @Mock private LocalBluetoothProfileManager mProfileManager;
    @Mock private CachedBluetoothDeviceManager mDeviceManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private BluetoothLeBroadcastReceiveState mState;
    @Mock private BluetoothLeBroadcastMetadata mSource;
    @Mock private PreferenceScreen mScreen;
    @Mock private AudioSharingDialogHandler mDialogHandler;
    @Mock private DashboardFragment mFragment;
    @Mock private FragmentActivity mActivity;
    @Mock private LeAudioProfile mLeAudioProfile;
    @Mock private A2dpProfile mA2dpProfile;
    @Mock private HeadsetProfile mHeadsetProfile;

    private Context mContext;
    private AudioSharingDevicePreferenceController mController;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private PreferenceCategory mPreferenceGroup;
    private Preference mAudioSharingPreference;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getEventManager()).thenReturn(mEventManager);
        when(mLocalBtManager.getProfileManager()).thenReturn(mProfileManager);
        when(mLocalBtManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        when(mProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        when(mAssistant.isProfileReady()).thenReturn(true);
        when(mVolumeControl.isProfileReady()).thenReturn(true);
        when(mDevice.getAnonymizedAddress()).thenReturn("");
        doReturn(mDevice).when(mCachedDevice).getDevice();
        when(mDeviceManager.findDevice(mDevice)).thenReturn(mCachedDevice);
        when(mHeadsetProfile.getProfileId()).thenReturn(BluetoothProfile.HEADSET);
        when(mA2dpProfile.getProfileId()).thenReturn(BluetoothProfile.A2DP);
        when(mLeAudioProfile.getProfileId()).thenReturn(BluetoothProfile.LE_AUDIO);
        when(mLeAudioProfile.isEnabled(mDevice)).thenReturn(true);
        when(mScreen.getContext()).thenReturn(mContext);
        mPreferenceGroup = spy(new PreferenceCategory(mContext));
        doReturn(mPreferenceManager).when(mPreferenceGroup).getPreferenceManager();
        mAudioSharingPreference = new Preference(mContext);
        mPreferenceGroup.addPreference(mAudioSharingPreference);
        when(mPreferenceGroup.findPreference(KEY_AUDIO_SHARING_SETTINGS))
                .thenReturn(mAudioSharingPreference);
        when(mScreen.findPreference(KEY)).thenReturn(mPreferenceGroup);
        mController = new AudioSharingDevicePreferenceController(mContext);
        mController.init(mFragment);
        mController.setBluetoothDeviceUpdater(mBluetoothDeviceUpdater);
        mController.setDialogHandler(mDialogHandler);
        doReturn(mActivity).when(mFragment).getActivity();
        mController.setHostFragment(mFragment);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void onStart_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mEventManager, never()).registerCallback(any(BluetoothCallback.class));
        verify(mDialogHandler, never()).registerCallbacks(any(Executor.class));
        verify(mAssistant, never())
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mBluetoothDeviceUpdater, never()).registerCallback();
        verify(mBluetoothDeviceUpdater, never()).refreshPreference();
    }

    @Test
    public void onStart_flagOn_registerCallbacks() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mEventManager).registerCallback(any(BluetoothCallback.class));
        verify(mDialogHandler).registerCallbacks(any(Executor.class));
        verify(mAssistant)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mBluetoothDeviceUpdater).registerCallback();
        verify(mBluetoothDeviceUpdater).refreshPreference();
    }

    @Test
    public void onStop_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mEventManager, never()).unregisterCallback(any(BluetoothCallback.class));
        verify(mDialogHandler, never()).unregisterCallbacks();
        verify(mAssistant, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mBluetoothDeviceUpdater, never()).unregisterCallback();
    }

    @Test
    public void onStop_flagOn_unregisterCallbacks() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mEventManager).unregisterCallback(any(BluetoothCallback.class));
        verify(mDialogHandler).unregisterCallbacks();
        verify(mAssistant)
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mBluetoothDeviceUpdater).unregisterCallback();
    }

    @Test
    public void displayPreference_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.displayPreference(mScreen);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
        assertThat(mAudioSharingPreference.isVisible()).isFalse();
        verify(mBluetoothDeviceUpdater, never()).forceUpdate();
    }

    @Test
    public void displayPreference_flagOn_updateDeviceList() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.displayPreference(mScreen);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
        assertThat(mAudioSharingPreference.isVisible()).isFalse();
        verify(mBluetoothDeviceUpdater).setPrefContext(mContext);
        verify(mBluetoothDeviceUpdater).forceUpdate();
    }

    @Test
    public void getPreferenceKey_returnsCorrectKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(KEY);
    }

    @Test
    public void getAvailabilityStatus_flagOff_returnUnSupported() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_flagOn_returnSupported() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void onDeviceAdded_firstDevice_updateVisibility() {
        mController.displayPreference(mScreen);
        Preference preference = new Preference(mContext);
        mController.onDeviceAdded(preference);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreferenceGroup.isVisible()).isTrue();
        assertThat(mAudioSharingPreference.isVisible()).isTrue();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onDeviceRemoved_lastDevice_updateVisibility() {
        Preference preference = new Preference(mContext);
        mPreferenceGroup.addPreference(preference);
        mController.displayPreference(mScreen);
        mController.onDeviceRemoved(preference);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreferenceGroup.isVisible()).isFalse();
        assertThat(mAudioSharingPreference.isVisible()).isFalse();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void onProfileConnectionStateChanged_notMediaDevice_doNothing() {
        doReturn(ImmutableList.of()).when(mCachedDevice).getUiAccessibleProfiles();
        mController.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothAdapter.STATE_CONNECTED, BluetoothProfile.HID_DEVICE);
        verifyNoInteractions(mDialogHandler);
    }

    @Test
    public void onProfileConnectionStateChanged_leaDeviceDisconnected_closeOpeningDialogsForIt() {
        // Test when LEA device LE_AUDIO_BROADCAST_ASSISTANT disconnected.
        when(mDevice.isConnected()).thenReturn(true);
        doReturn(ImmutableList.of(mLeAudioProfile)).when(mCachedDevice).getUiAccessibleProfiles();
        doReturn(ImmutableList.of(mLeAudioProfile)).when(mCachedDevice).getProfiles();
        mController.onProfileConnectionStateChanged(
                mCachedDevice,
                BluetoothAdapter.STATE_DISCONNECTED,
                BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);
        verify(mDialogHandler).closeOpeningDialogsForLeaDevice(mCachedDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_assistantProfileConnecting_doNothing() {
        // Test when LEA device LE_AUDIO_BROADCAST_ASSISTANT connecting
        doReturn(ImmutableList.of(mLeAudioProfile)).when(mCachedDevice).getUiAccessibleProfiles();
        doReturn(ImmutableList.of(mLeAudioProfile)).when(mCachedDevice).getProfiles();
        mController.onProfileConnectionStateChanged(
                mCachedDevice,
                BluetoothAdapter.STATE_CONNECTING,
                BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);
        verifyNoInteractions(mDialogHandler);
    }

    @Test
    public void onProfileConnectionStateChanged_otherProfileConnected_doNothing() {
        // Test when LEA device other profile connected
        when(mDevice.isConnected()).thenReturn(true);
        doReturn(ImmutableList.of(mLeAudioProfile)).when(mCachedDevice).getUiAccessibleProfiles();
        doReturn(ImmutableList.of(mLeAudioProfile)).when(mCachedDevice).getProfiles();
        mController.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothAdapter.STATE_CONNECTED, BluetoothProfile.A2DP);
        verifyNoInteractions(mDialogHandler);
    }

    @Test
    public void onProfileConnectionStateChanged_otherProfileConnecting_doNothing() {
        // Test when LEA device other profile connecting
        when(mDevice.isConnected()).thenReturn(true);
        doReturn(ImmutableList.of(mLeAudioProfile)).when(mCachedDevice).getUiAccessibleProfiles();
        doReturn(ImmutableList.of(mLeAudioProfile)).when(mCachedDevice).getProfiles();
        mController.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothAdapter.STATE_CONNECTING, BluetoothProfile.A2DP);
        verifyNoInteractions(mDialogHandler);
    }

    @Test
    public void onProfileConnectionStateChanged_assistantProfileConnected_handle() {
        // Test when LEA device LE_AUDIO_BROADCAST_ASSISTANT connected
        when(mDevice.isConnected()).thenReturn(true);
        doReturn(ImmutableList.of(mLeAudioProfile)).when(mCachedDevice).getUiAccessibleProfiles();
        doReturn(ImmutableList.of(mLeAudioProfile)).when(mCachedDevice).getProfiles();
        mController.onProfileConnectionStateChanged(
                mCachedDevice,
                BluetoothAdapter.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);
        verify(mDialogHandler).handleDeviceConnected(mCachedDevice, false);
    }

    @Test
    public void
            onProfileConnectionStateChanged_nonLeaDeviceDisconnected_closeOpeningDialogsForIt() {
        // Test when non-LEA device totally disconnected
        when(mLeAudioProfile.isEnabled(mDevice)).thenReturn(false);
        doReturn(ImmutableList.of(mA2dpProfile)).when(mCachedDevice).getUiAccessibleProfiles();
        doReturn(ImmutableList.of(mLeAudioProfile, mA2dpProfile)).when(mCachedDevice).getProfiles();
        when(mCachedDevice.isConnected()).thenReturn(false);
        mController.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothAdapter.STATE_DISCONNECTED, BluetoothProfile.A2DP);
        verify(mDialogHandler).closeOpeningDialogsForNonLeaDevice(mCachedDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_nonLeaNotFirstProfileConnected_doNothing() {
        // Test when non-LEA device LE_AUDIO_BROADCAST_ASSISTANT connecting
        when(mDevice.isConnected()).thenReturn(true);
        when(mHeadsetProfile.getConnectionStatus(mDevice))
                .thenReturn(BluetoothAdapter.STATE_CONNECTED);
        doReturn(ImmutableList.of(mA2dpProfile, mHeadsetProfile))
                .when(mCachedDevice)
                .getUiAccessibleProfiles();
        doReturn(ImmutableList.of(mA2dpProfile, mHeadsetProfile)).when(mCachedDevice).getProfiles();
        mController.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothAdapter.STATE_CONNECTED, BluetoothProfile.A2DP);
        verifyNoInteractions(mDialogHandler);
    }

    @Test
    public void onProfileConnectionStateChanged_nonLeaFirstProfileConnected_handle() {
        // Test when non-LEA device LE_AUDIO_BROADCAST_ASSISTANT connecting
        when(mDevice.isConnected()).thenReturn(true);
        when(mHeadsetProfile.getConnectionStatus(mDevice))
                .thenReturn(BluetoothAdapter.STATE_DISCONNECTED);
        doReturn(ImmutableList.of(mA2dpProfile, mHeadsetProfile))
                .when(mCachedDevice)
                .getUiAccessibleProfiles();
        doReturn(ImmutableList.of(mA2dpProfile, mHeadsetProfile)).when(mCachedDevice).getProfiles();
        mController.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothAdapter.STATE_CONNECTED, BluetoothProfile.A2DP);
        verify(mDialogHandler).handleDeviceConnected(mCachedDevice, false);
    }

    @Test
    public void handleDeviceClickFromIntent_noDevice_doNothing() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, new Bundle());
        doReturn(intent).when(mActivity).getIntent();
        mController.displayPreference(mScreen);

        verify(mDeviceManager, never()).findDevice(any(BluetoothDevice.class));
        verify(mDialogHandler, never())
                .handleDeviceConnected(any(CachedBluetoothDevice.class), anyBoolean());
    }

    @Test
    public void handleDeviceClickFromIntent_profileNotReady_doNothing() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isProfileReady()).thenReturn(false);
        Bundle arg = new Bundle();
        arg.putParcelable(EXTRA_BLUETOOTH_DEVICE, mDevice);
        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, arg);
        doReturn(intent).when(mActivity).getIntent();
        when(mDevice.isConnected()).thenReturn(false);
        mController.displayPreference(mScreen);

        verify(mDeviceManager, never()).findDevice(any(BluetoothDevice.class));
        verify(mDialogHandler, never())
                .handleDeviceConnected(any(CachedBluetoothDevice.class), anyBoolean());
    }

    @Test
    public void handleDeviceClickFromIntent_intentHandled_handle() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        Bundle arg = new Bundle();
        arg.putParcelable(EXTRA_BLUETOOTH_DEVICE, mDevice);
        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, arg);
        doReturn(intent).when(mActivity).getIntent();
        when(mDevice.isConnected()).thenReturn(true);
        when(mCachedDevice.isConnected()).thenReturn(true);
        mController.setIntentHandled(true);
        mController.displayPreference(mScreen);

        verify(mDeviceManager, never()).findDevice(any(BluetoothDevice.class));
        verify(mDialogHandler, never())
                .handleDeviceConnected(any(CachedBluetoothDevice.class), anyBoolean());
    }

    @Test
    public void handleDeviceClickFromIntent_disconnectedDevice_connect() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        Bundle arg = new Bundle();
        arg.putParcelable(EXTRA_BLUETOOTH_DEVICE, mDevice);
        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, arg);
        doReturn(intent).when(mActivity).getIntent();
        when(mDevice.isConnected()).thenReturn(false);
        mController.displayPreference(mScreen);

        verify(mCachedDevice).connect();
    }

    @Test
    public void handleDeviceClickFromIntent_connectedDevice_handle() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        Bundle arg = new Bundle();
        arg.putParcelable(EXTRA_BLUETOOTH_DEVICE, mDevice);
        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, arg);
        doReturn(intent).when(mActivity).getIntent();
        when(mDevice.isConnected()).thenReturn(true);
        when(mCachedDevice.isConnected()).thenReturn(true);
        mController.displayPreference(mScreen);

        verify(mDialogHandler).handleDeviceConnected(mCachedDevice, true);
    }

    @Test
    public void handleDeviceClickFromIntent_onServiceConnected_handle() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        Bundle arg = new Bundle();
        arg.putParcelable(EXTRA_BLUETOOTH_DEVICE, mDevice);
        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, arg);
        doReturn(intent).when(mActivity).getIntent();
        when(mDevice.isConnected()).thenReturn(true);
        when(mCachedDevice.isConnected()).thenReturn(true);
        mController.onServiceConnected();

        verify(mDialogHandler).handleDeviceConnected(mCachedDevice, true);
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_updateGroup() {
        // onReceiveStateChanged with unconnected state will do nothing
        when(mState.getBisSyncState()).thenReturn(new ArrayList<>());
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(
                mDevice, /* sourceId= */ 1, mState);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mBluetoothDeviceUpdater, never()).forceUpdate();
        verify(mDialogHandler, never()).closeOpeningDialogsForLeaDevice(mCachedDevice);

        // onReceiveStateChanged with connected state will update group preference and handle
        // stale dialogs
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(mState.getBisSyncState()).thenReturn(bisSyncState);
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(
                mDevice, /* sourceId= */ 1, mState);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mBluetoothDeviceUpdater).forceUpdate();
        verify(mDialogHandler).closeOpeningDialogsForLeaDevice(mCachedDevice);

        // onSourceRemoved will update group preference
        mController.mBroadcastAssistantCallback.onSourceRemoved(
                mDevice, /* sourceId= */ 1, /* reason= */ 1);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mBluetoothDeviceUpdater, times(2)).forceUpdate();
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_logAction() {
        mController.mBroadcastAssistantCallback.onSourceAddFailed(
                mDevice, mSource, /* reason= */ 1);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_JOIN_FAILED,
                        SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY);

        mController.mBroadcastAssistantCallback.onSourceRemoveFailed(
                mDevice, /* sourceId= */ 1, /* reason= */ 1);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_LEAVE_FAILED,
                        SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY);
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_doNothing() {
        mController.mBroadcastAssistantCallback.onSearchStarted(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStartFailed(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStopped(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStopFailed(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceAdded(
                mDevice, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceModified(
                mDevice, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceModifyFailed(
                mDevice, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceFound(mSource);
        mController.mBroadcastAssistantCallback.onSourceLost(/* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();

        // Above callbacks won't update group preference and log actions
        verify(mBluetoothDeviceUpdater, never()).forceUpdate();
        verifyNoMoreInteractions(mFeatureFactory.metricsFeatureProvider);
    }
}
