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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.flags.Flags;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Correspondence;

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
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
        })
public class AudioSharingDialogHandlerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final String TEST_DEVICE_NAME3 = "test3";
    private static final String TEST_DEVICE_NAME4 = "test4";
    private static final String TEST_DEVICE_ADDRESS = "xx:xx:xx:xx";
    private static final Correspondence<Fragment, String> TAG_EQUALS =
            Correspondence.from(
                    (Fragment fragment, String tag) ->
                            fragment instanceof DialogFragment
                                    && ((DialogFragment) fragment).getTag().equals(tag),
                    "is equal to");

    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private CachedBluetoothDeviceManager mCacheManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    @Mock private CachedBluetoothDevice mCachedDevice3;
    @Mock private CachedBluetoothDevice mCachedDevice4;
    @Mock private BluetoothDevice mDevice1;
    @Mock private BluetoothDevice mDevice2;
    @Mock private BluetoothDevice mDevice3;
    @Mock private BluetoothDevice mDevice4;
    @Mock private LeAudioProfile mLeAudioProfile;
    private Fragment mParentFragment;
    @Mock private BluetoothLeBroadcastReceiveState mState;
    private Context mContext;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private AudioSharingDialogHandler mHandler;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mLocalBtManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mLocalBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(mState.getBisSyncState()).thenReturn(bisSyncState);
        when(mLeAudioProfile.isEnabled(any())).thenReturn(true);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice1.getProfiles()).thenReturn(List.of(mLeAudioProfile));
        when(mCachedDevice1.getGroupId()).thenReturn(1);
        when(mCachedDevice2.getName()).thenReturn(TEST_DEVICE_NAME2);
        when(mCachedDevice2.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);
        when(mCachedDevice2.getDevice()).thenReturn(mDevice2);
        when(mCachedDevice2.getProfiles()).thenReturn(List.of());
        when(mCachedDevice2.getGroupId()).thenReturn(2);
        when(mCachedDevice3.getName()).thenReturn(TEST_DEVICE_NAME3);
        when(mCachedDevice3.getDevice()).thenReturn(mDevice3);
        when(mCachedDevice3.getProfiles()).thenReturn(List.of(mLeAudioProfile));
        when(mCachedDevice3.getGroupId()).thenReturn(3);
        when(mCachedDevice4.getName()).thenReturn(TEST_DEVICE_NAME4);
        when(mCachedDevice4.getDevice()).thenReturn(mDevice4);
        when(mCachedDevice4.getProfiles()).thenReturn(List.of(mLeAudioProfile));
        when(mCachedDevice4.getGroupId()).thenReturn(4);
        when(mLocalBtManager.getCachedDeviceManager()).thenReturn(mCacheManager);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mCacheManager.findDevice(mDevice2)).thenReturn(mCachedDevice2);
        when(mCacheManager.findDevice(mDevice3)).thenReturn(mCachedDevice3);
        when(mCacheManager.findDevice(mDevice4)).thenReturn(mCachedDevice4);
        mParentFragment = new Fragment();
        FragmentController.setupFragment(
                mParentFragment,
                FragmentActivity.class,
                0 /* containerViewId */,
                null /* bundle */);
        mHandler = new AudioSharingDialogHandler(mContext, mParentFragment);
    }

    @Test
    public void handleUserTriggeredNonLeaDeviceConnected_noSharing_setActive() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice2);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCachedDevice2).setActive();
    }

    @Test
    public void handleUserTriggeredNonLeaDeviceConnected_sharing_showStopDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice2);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingStopDialogFragment.tag());
    }

    @Test
    public void handleUserTriggeredLeaDeviceConnected_noSharingNoTwoLeaDevices_setActive() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCachedDevice1).setActive();
    }

    @Test
    public void handleUserTriggeredLeaDeviceConnected_noSharingTwoLeaDevices_showJoinDialog() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingJoinDialogFragment.tag());
    }

    @Test
    public void handleUserTriggeredLeaDeviceConnected_sharing_showJoinDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice1)).thenReturn(ImmutableList.of());
        when(mAssistant.getAllSources(mDevice3)).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingJoinDialogFragment.tag());
    }

    @Test
    public void
            handleUserTriggeredLeaDeviceConnected_sharingWithTwoLeaDevices_showDisconnectDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3, mDevice4);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice1)).thenReturn(ImmutableList.of());
        when(mAssistant.getAllSources(mDevice3)).thenReturn(ImmutableList.of(mState));
        when(mAssistant.getAllSources(mDevice4)).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingDisconnectDialogFragment.tag());
    }

    @Test
    public void handleNonLeaDeviceConnected_noSharing_doNothing() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice2);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCachedDevice2, times(0)).setActive();
    }

    @Test
    public void handleNonLeaDeviceConnected_sharing_showStopDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice2);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingStopDialogFragment.tag());
    }

    @Test
    public void handleLeaDeviceConnected_noSharingNoTwoLeaDevices_doNothing() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCachedDevice1, times(0)).setActive();
    }

    @Test
    public void handleLeaDeviceConnected_noSharingTwoLeaDevices_showJoinDialog() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingJoinDialogFragment.tag());
    }

    @Test
    public void handleLeaDeviceConnected_sharing_showJoinDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice1)).thenReturn(ImmutableList.of());
        when(mAssistant.getAllSources(mDevice3)).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingJoinDialogFragment.tag());
    }

    @Test
    public void handleLeaDeviceConnected_sharingWithTwoLeaDevices_showDisconnectDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3, mDevice4);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice1)).thenReturn(ImmutableList.of());
        when(mAssistant.getAllSources(mDevice3)).thenReturn(ImmutableList.of(mState));
        when(mAssistant.getAllSources(mDevice4)).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingDisconnectDialogFragment.tag());
    }

    @Test
    public void closeOpeningDialogsForLeaDevice_closeJoinDialog() {
        // Show join dialog
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingJoinDialogFragment.tag());
        // Close opening dialogs
        mHandler.closeOpeningDialogsForLeaDevice(mCachedDevice1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments()).isEmpty();
    }

    @Test
    public void closeOpeningDialogsForNonLeaDevice_closeStopDialog() {
        // Show stop dialog
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice2);
        when(mAssistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED}))
                .thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingStopDialogFragment.tag());
        // Close opening dialogs
        mHandler.closeOpeningDialogsForNonLeaDevice(mCachedDevice2);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments()).isEmpty();
    }

    private void setUpBroadcast(boolean isBroadcasting) {
        when(mBroadcast.isEnabled(any())).thenReturn(isBroadcasting);
    }
}
