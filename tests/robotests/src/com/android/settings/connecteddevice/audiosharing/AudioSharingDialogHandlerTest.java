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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
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
import com.google.common.collect.Iterables;
import com.google.common.truth.Correspondence;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
        })
public class AudioSharingDialogHandlerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final int TEST_SOURCE_ID = 1;
    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final String TEST_DEVICE_NAME3 = "test3";
    private static final String TEST_DEVICE_NAME4 = "test4";
    private static final String TEST_DEVICE_ADDRESS = "xx:xx:xx:xx";
    private static final Correspondence<Fragment, String> TAG_EQUALS =
            Correspondence.from(
                    (Fragment fragment, String tag) ->
                            fragment instanceof DialogFragment
                                    && ((DialogFragment) fragment).getTag() != null
                                    && ((DialogFragment) fragment).getTag().equals(tag),
                    "is equal to");

    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private CachedBluetoothDeviceManager mCacheManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private AudioManager mAudioManager;
    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    @Mock private CachedBluetoothDevice mCachedDevice3;
    @Mock private CachedBluetoothDevice mCachedDevice4;
    @Mock private BluetoothDevice mDevice1;
    @Mock private BluetoothDevice mDevice2;
    @Mock private BluetoothDevice mDevice3;
    @Mock private BluetoothDevice mDevice4;
    @Mock private LeAudioProfile mLeAudioProfile;
    @Mock private BluetoothLeBroadcastReceiveState mState;
    @Mock private BluetoothLeBroadcastMetadata mMetadata;
    private Fragment mParentFragment;
    private Context mContext;
    private AudioSharingDialogHandler mHandler;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setup() {
        mParentFragment = new Fragment();
        FragmentController.setupFragment(
                mParentFragment,
                FragmentActivity.class,
                0 /* containerViewId */,
                null /* bundle */);
        mContext = spy(mParentFragment.getContext());
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        ShadowBluetoothAdapter shadowBluetoothAdapter =
                Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mLocalBtManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mLocalBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        when(mContext.getSystemService(AudioManager.class)).thenReturn(mAudioManager);
        when(mAudioManager.getMode()).thenReturn(AudioManager.MODE_NORMAL);
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(mState.getBisSyncState()).thenReturn(bisSyncState);
        when(mState.getSourceId()).thenReturn(TEST_SOURCE_ID);
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
        mHandler = new AudioSharingDialogHandler(mContext, mParentFragment);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void handleUserTriggeredDeviceConnected_inCall_setActive() {
        when(mAudioManager.getMode()).thenReturn(AudioManager.MODE_IN_CALL);
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCachedDevice1).setActive();
    }

    @Test
    public void handleUserTriggeredNonLeaDeviceConnected_noSharing_setActive() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice2);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCachedDevice2).setActive();
    }

    @Test
    public void handleUserTriggeredNonLeaDeviceConnected_sharing_showStopDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice2);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingStopDialogFragment.tag());

        AudioSharingStopDialogFragment fragment =
                (AudioSharingStopDialogFragment) Iterables.getOnlyElement(childFragments);
        Pair<Integer, Object>[] eventData = fragment.getEventData();
        assertThat(eventData)
                .asList()
                .containsExactly(
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID.ordinal(),
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_PAGE_ID.ordinal(),
                                SettingsEnums.DIALOG_STOP_AUDIO_SHARING),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_USER_TRIGGERED.ordinal(), 1),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_DEVICE_COUNT_IN_SHARING
                                        .ordinal(),
                                1),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_CANDIDATE_DEVICE_COUNT
                                        .ordinal(),
                                0));
    }

    @Test
    public void handleUserTriggeredLeaDeviceConnected_noSharingNoTwoLeaDevices_setActive() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCachedDevice1).setActive();
    }

    @Test
    public void handleUserTriggeredLeaDeviceConnected_noSharingLeaDeviceInErrorState_setActive() {
        setUpBroadcast(false);
        when(mCachedDevice1.getGroupId()).thenReturn(-1);
        when(mLeAudioProfile.getGroupId(mDevice1)).thenReturn(-1);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).isEmpty();
        verify(mCachedDevice1).setActive();
    }

    @Test
    public void handleUserTriggeredLeaDeviceConnected_noSharingTwoLeaDevices_showJoinDialog() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingJoinDialogFragment.tag());

        AudioSharingJoinDialogFragment fragment =
                (AudioSharingJoinDialogFragment) Iterables.getOnlyElement(childFragments);
        Pair<Integer, Object>[] eventData = fragment.getEventData();
        assertThat(eventData)
                .asList()
                .containsExactly(
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID.ordinal(),
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_PAGE_ID.ordinal(),
                                SettingsEnums.DIALOG_START_AUDIO_SHARING),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_USER_TRIGGERED.ordinal(), 1),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_DEVICE_COUNT_IN_SHARING
                                        .ordinal(),
                                0),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_CANDIDATE_DEVICE_COUNT
                                        .ordinal(),
                                2));
        AudioSharingJoinDialogFragment.DialogEventListener listener = fragment.getListener();
        assertThat(listener).isNotNull();
        listener.onShareClick();
        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(argumentCaptor.capture());
        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AudioSharingDashboardFragment.class.getName());
        assertThat(intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS))
                .isNotNull();
        Bundle args = intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(args).isNotNull();
        assertThat(args.getBoolean(LocalBluetoothLeBroadcast.EXTRA_START_LE_AUDIO_SHARING))
                .isTrue();
        listener.onCancelClick();
        verify(mCachedDevice1).setActive();
    }

    @Test
    public void handleUserTriggeredLeaDeviceConnected_sharing_showJoinDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice1)).thenReturn(ImmutableList.of());
        when(mAssistant.getAllSources(mDevice3)).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingJoinDialogFragment.tag());

        AudioSharingJoinDialogFragment fragment =
                (AudioSharingJoinDialogFragment) Iterables.getOnlyElement(childFragments);
        Pair<Integer, Object>[] eventData = fragment.getEventData();
        assertThat(eventData)
                .asList()
                .containsExactly(
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID.ordinal(),
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_PAGE_ID.ordinal(),
                                SettingsEnums.DIALOG_AUDIO_SHARING_ADD_DEVICE),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_USER_TRIGGERED.ordinal(), 1),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_DEVICE_COUNT_IN_SHARING
                                        .ordinal(),
                                1),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_CANDIDATE_DEVICE_COUNT
                                        .ordinal(),
                                1));
        AudioSharingJoinDialogFragment.DialogEventListener listener = fragment.getListener();
        assertThat(listener).isNotNull();
        listener.onCancelClick();
        verify(mAssistant, never()).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
        listener.onShareClick();
        verify(mAssistant).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
    }

    @Test
    public void
            handleUserTriggeredLeaDeviceConnected_sharingWithTwoLeaDevices_showDisconnectDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3, mDevice4);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice1)).thenReturn(ImmutableList.of());
        when(mAssistant.getAllSources(mDevice3)).thenReturn(ImmutableList.of(mState));
        when(mAssistant.getAllSources(mDevice4)).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingDisconnectDialogFragment.tag());

        AudioSharingDisconnectDialogFragment fragment =
                (AudioSharingDisconnectDialogFragment) Iterables.getOnlyElement(childFragments);
        Pair<Integer, Object>[] eventData = fragment.getEventData();
        assertThat(eventData)
                .asList()
                .containsExactly(
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID.ordinal(),
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_PAGE_ID.ordinal(),
                                SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_USER_TRIGGERED.ordinal(), 1),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_DEVICE_COUNT_IN_SHARING
                                        .ordinal(),
                                2),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_CANDIDATE_DEVICE_COUNT
                                        .ordinal(),
                                1));
        AudioSharingDisconnectDialogFragment.DialogEventListener listener = fragment.getListener();
        assertThat(listener).isNotNull();
        listener.onItemClick(AudioSharingUtils.buildAudioSharingDeviceItem(mCachedDevice3));
        verify(mAssistant).removeSource(mDevice3, TEST_SOURCE_ID);
        verify(mAssistant).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
    }

    @Test
    public void handleDeviceConnected_inCall_doNothing() {
        when(mAudioManager.getMode()).thenReturn(AudioManager.MODE_IN_CALL);
        setUpBroadcast(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCachedDevice2, never()).setActive();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).isEmpty();
    }

    @Test
    public void handleNonLeaDeviceConnected_noSharing_doNothing() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice2);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCachedDevice2, never()).setActive();
    }

    @Test
    public void handleNonLeaDeviceConnected_sharing_showStopDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingStopDialogFragment.tag());

        AudioSharingStopDialogFragment fragment =
                (AudioSharingStopDialogFragment) Iterables.getOnlyElement(childFragments);
        Pair<Integer, Object>[] eventData = fragment.getEventData();
        assertThat(eventData)
                .asList()
                .containsExactly(
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID.ordinal(),
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_PAGE_ID.ordinal(),
                                SettingsEnums.DIALOG_STOP_AUDIO_SHARING),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_USER_TRIGGERED.ordinal(), 0),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_DEVICE_COUNT_IN_SHARING
                                        .ordinal(),
                                1),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_CANDIDATE_DEVICE_COUNT
                                        .ordinal(),
                                0));
    }

    @Test
    public void handleLeaDeviceConnected_noSharingNoTwoLeaDevices_doNothing() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCachedDevice1, never()).setActive();
    }

    @Test
    public void handleLeaDeviceConnected_noSharingLeaDeviceInErrorState_doNothing() {
        setUpBroadcast(false);
        when(mCachedDevice1.getGroupId()).thenReturn(-1);
        when(mLeAudioProfile.getGroupId(mDevice1)).thenReturn(-1);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).isEmpty();
        verify(mCachedDevice1, never()).setActive();
    }

    @Test
    public void handleLeaDeviceConnected_noSharingTwoLeaDevices_showJoinDialog() {
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingJoinDialogFragment.tag());

        AudioSharingJoinDialogFragment fragment =
                (AudioSharingJoinDialogFragment) Iterables.getOnlyElement(childFragments);
        Pair<Integer, Object>[] eventData = fragment.getEventData();
        assertThat(eventData)
                .asList()
                .containsExactly(
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID.ordinal(),
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_PAGE_ID.ordinal(),
                                SettingsEnums.DIALOG_START_AUDIO_SHARING),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_USER_TRIGGERED.ordinal(), 0),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_DEVICE_COUNT_IN_SHARING
                                        .ordinal(),
                                0),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_CANDIDATE_DEVICE_COUNT
                                        .ordinal(),
                                2));
        AudioSharingJoinDialogFragment.DialogEventListener listener = fragment.getListener();
        assertThat(listener).isNotNull();
        listener.onShareClick();
        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(argumentCaptor.capture());
        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AudioSharingDashboardFragment.class.getName());
        assertThat(intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS))
                .isNotNull();
        Bundle args = intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(args).isNotNull();
        assertThat(args.getBoolean(LocalBluetoothLeBroadcast.EXTRA_START_LE_AUDIO_SHARING))
                .isTrue();
        listener.onCancelClick();
        verify(mCachedDevice1, never()).setActive();
    }

    @Test
    public void handleLeaDeviceConnected_sharing_showJoinDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice1)).thenReturn(ImmutableList.of());
        when(mAssistant.getAllSources(mDevice3)).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingJoinDialogFragment.tag());

        AudioSharingJoinDialogFragment fragment =
                (AudioSharingJoinDialogFragment) Iterables.getOnlyElement(childFragments);
        Pair<Integer, Object>[] eventData = fragment.getEventData();
        assertThat(eventData)
                .asList()
                .containsExactly(
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID.ordinal(),
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_PAGE_ID.ordinal(),
                                SettingsEnums.DIALOG_AUDIO_SHARING_ADD_DEVICE),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_USER_TRIGGERED.ordinal(), 0),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_DEVICE_COUNT_IN_SHARING
                                        .ordinal(),
                                1),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_CANDIDATE_DEVICE_COUNT
                                        .ordinal(),
                                1));
        AudioSharingJoinDialogFragment.DialogEventListener listener = fragment.getListener();
        assertThat(listener).isNotNull();
        listener.onCancelClick();
        verify(mAssistant, never()).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
        listener.onShareClick();
        verify(mAssistant).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
    }

    @Test
    public void handleLeaDeviceConnected_sharingWithTwoLeaDevices_showDisconnectDialog() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3, mDevice4);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice1)).thenReturn(ImmutableList.of());
        when(mAssistant.getAllSources(mDevice3)).thenReturn(ImmutableList.of(mState));
        when(mAssistant.getAllSources(mDevice4)).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingDisconnectDialogFragment.tag());

        AudioSharingDisconnectDialogFragment fragment =
                (AudioSharingDisconnectDialogFragment) Iterables.getOnlyElement(childFragments);
        Pair<Integer, Object>[] eventData = fragment.getEventData();
        assertThat(eventData)
                .asList()
                .containsExactly(
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID.ordinal(),
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_PAGE_ID.ordinal(),
                                SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_USER_TRIGGERED.ordinal(), 0),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_DEVICE_COUNT_IN_SHARING
                                        .ordinal(),
                                2),
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_CANDIDATE_DEVICE_COUNT
                                        .ordinal(),
                                1));
        AudioSharingDisconnectDialogFragment.DialogEventListener listener = fragment.getListener();
        assertThat(listener).isNotNull();
        listener.onItemClick(AudioSharingUtils.buildAudioSharingDeviceItem(mCachedDevice3));
        verify(mAssistant).removeSource(mDevice3, TEST_SOURCE_ID);
        verify(mAssistant).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
    }

    @Test
    public void closeOpeningDialogsForLeaDevice_closeJoinDialog() {
        // Show join dialog
        setUpBroadcast(false);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
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
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS,
                        SettingsEnums.DIALOG_START_AUDIO_SHARING);
    }

    @Test
    public void closeOpeningDialogsForLeaDevice_unattachedFragment_doNothing() {
        mParentFragment = new Fragment();
        mHandler = new AudioSharingDialogHandler(mContext, mParentFragment);
        mHandler.closeOpeningDialogsForLeaDevice(mCachedDevice1);
        shadowOf(Looper.getMainLooper()).idle();
        verifyNoMoreInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    public void closeOpeningDialogsForLeaDevice_closeDisconnectDialog() {
        // Show disconnect dialog
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1, mDevice3, mDevice4);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice1)).thenReturn(ImmutableList.of());
        when(mAssistant.getAllSources(mDevice3)).thenReturn(ImmutableList.of(mState));
        when(mAssistant.getAllSources(mDevice4)).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments())
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingDisconnectDialogFragment.tag());
        // Close opening dialogs
        mHandler.closeOpeningDialogsForLeaDevice(mCachedDevice1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mParentFragment.getChildFragmentManager().getFragments()).isEmpty();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS,
                        SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE);
    }

    @Test
    public void closeOpeningDialogsForNonLeaDevice_unattachedFragment_doNothing() {
        mParentFragment = new Fragment();
        mHandler = new AudioSharingDialogHandler(mContext, mParentFragment);
        mHandler.closeOpeningDialogsForNonLeaDevice(mCachedDevice2);
        shadowOf(Looper.getMainLooper()).idle();
        verifyNoMoreInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    public void closeOpeningDialogsForNonLeaDevice_closeStopDialog() {
        // Show stop dialog
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
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
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS,
                        SettingsEnums.DIALOG_STOP_AUDIO_SHARING);
    }

    @Test
    public void closeOpeningDialogsOtherThan() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice3);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice3)).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ true);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingStopDialogFragment.tag());

        deviceList = ImmutableList.of(mDevice1, mDevice3);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(mDevice1)).thenReturn(ImmutableList.of());
        mHandler.handleDeviceConnected(mCachedDevice1, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingJoinDialogFragment.tag());

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS,
                        SettingsEnums.DIALOG_STOP_AUDIO_SHARING);
    }

    @Test
    public void registerCallbacks() {
        Executor executor = mock(Executor.class);
        mHandler.registerCallbacks(executor);
        verify(mBroadcast)
                .registerServiceCallBack(eq(executor), any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void unregisterCallbacks() {
        mHandler.unregisterCallbacks();
        verify(mBroadcast).unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void onBroadcastStopFailed_logAction() {
        setUpBroadcast(true);
        ImmutableList<BluetoothDevice> deviceList = ImmutableList.of(mDevice1);
        when(mAssistant.getAllConnectedDevices()).thenReturn(deviceList);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mHandler.handleDeviceConnected(mCachedDevice2, /* userTriggered= */ false);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingStopDialogFragment.tag());

        AudioSharingStopDialogFragment fragment =
                (AudioSharingStopDialogFragment) Iterables.getOnlyElement(childFragments);
        AudioSharingStopDialogFragment.DialogEventListener listener = fragment.getListener();
        assertThat(listener).isNotNull();
        listener.onStopSharingClick();

        mHandler.mBroadcastCallback.onBroadcastStopFailed(/* reason= */ 1);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_STOP_FAILED,
                        SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY);
    }

    @Test
    public void testBluetoothLeBroadcastCallbacks_doNothing() {
        mHandler.mBroadcastCallback.onBroadcastStarted(/* reason= */ 1, /* broadcastId= */ 1);
        mHandler.mBroadcastCallback.onBroadcastStartFailed(/* reason= */ 1);
        mHandler.mBroadcastCallback.onBroadcastMetadataChanged(/* reason= */ 1, mMetadata);
        mHandler.mBroadcastCallback.onBroadcastUpdated(/* reason= */ 1, /* broadcastId= */ 1);
        mHandler.mBroadcastCallback.onPlaybackStarted(/* reason= */ 1, /* broadcastId= */ 1);
        mHandler.mBroadcastCallback.onPlaybackStopped(/* reason= */ 1, /* broadcastId= */ 1);
        mHandler.mBroadcastCallback.onBroadcastUpdateFailed(/* reason= */ 1, /* broadcastId= */ 1);

        verify(mAssistant, never())
                .addSource(
                        any(BluetoothDevice.class),
                        any(BluetoothLeBroadcastMetadata.class),
                        anyBoolean());
        verify(mAssistant, never()).removeSource(any(BluetoothDevice.class), anyInt());
        verifyNoMoreInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    private void setUpBroadcast(boolean isBroadcasting) {
        when(mBroadcast.isEnabled(any())).thenReturn(isBroadcasting);
        if (isBroadcasting) {
            when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(mMetadata);
        }
    }
}
