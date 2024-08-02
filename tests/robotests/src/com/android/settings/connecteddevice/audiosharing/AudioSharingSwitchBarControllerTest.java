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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.FeatureFlagUtils;
import android.util.Pair;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CompoundButton;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
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
import com.google.common.collect.Iterables;
import com.google.common.truth.Correspondence;

import org.junit.After;
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
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.List;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowThreadUtils.class,
        })
public class AudioSharingSwitchBarControllerTest {
    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final int TEST_DEVICE_GROUP_ID1 = 1;
    private static final int TEST_DEVICE_GROUP_ID2 = 2;
    private static final Correspondence<Fragment, String> TAG_EQUALS =
            Correspondence.from(
                    (Fragment fragment, String tag) ->
                            fragment instanceof DialogFragment
                                    && ((DialogFragment) fragment).getTag() != null
                                    && ((DialogFragment) fragment).getTag().equals(tag),
                    "is equal to");

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
    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    @Mock private BluetoothDevice mDevice1;
    @Mock private BluetoothDevice mDevice2;
    private SettingsMainSwitchBar mSwitchBar;
    private AudioSharingSwitchBarController mController;
    private FakeFeatureFactory mFeatureFactory;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private boolean mOnAudioSharingStateChanged;
    private boolean mOnAudioSharingServiceConnected;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private Fragment mParentFragment;

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
        LocalBluetoothManager localBluetoothManager = Utils.getLocalBtManager(mContext);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(localBluetoothManager.getProfileManager()).thenReturn(mBtProfileManager);
        when(localBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice1.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(false);
        when(mDeviceManager.findDevice(mDevice2)).thenReturn(mCachedDevice2);
        when(mCachedDevice2.getDevice()).thenReturn(mDevice2);
        when(mCachedDevice2.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID2);
        when(mCachedDevice2.getName()).thenReturn(TEST_DEVICE_NAME2);
        when(mCachedDevice2.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(true);
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
        mSwitchBar = spy(new SettingsMainSwitchBar(mContext));
        mSwitchBar.setDisabledByAdmin(mock(RestrictedLockUtils.EnforcedAdmin.class));
        mOnAudioSharingStateChanged = false;
        mOnAudioSharingServiceConnected = false;
        AudioSharingSwitchBarController.OnAudioSharingStateChangedListener listener =
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
        mController = new AudioSharingSwitchBarController(mContext, mSwitchBar, listener);
        mParentFragment = new Fragment();
        FragmentController.setupFragment(
                mParentFragment,
                FragmentActivity.class,
                0 /* containerViewId */,
                null /* bundle */);
        mController.init(mParentFragment);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
        ShadowThreadUtils.reset();
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
        verify(mContext, never())
                .registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class), anyInt());
        verify(mBroadcast, never())
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant, never())
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mBtProfileManager, never()).addServiceListener(mController);
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
        verify(mBroadcast, never())
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant, never())
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
        verify(mBtProfileManager, never()).addServiceListener(mController);
        assertThat(mSwitchBar.isChecked()).isTrue();
        assertThat(mSwitchBar.isEnabled()).isTrue();
    }

    @Test
    public void onStop_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mContext, never()).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mBroadcast, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mBtProfileManager, never()).removeServiceListener(mController);
    }

    @Test
    public void onStop_flagOn_notRegistered_doNothing() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.setCallbacksRegistered(false);
        doNothing().when(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        mController.onStop(mLifecycleOwner);

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mBtProfileManager).removeServiceListener(mController);
        verify(mBroadcast, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant, never())
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
        verify(mBroadcast, never()).startPrivateBroadcast();
    }

    @Test
    public void onCheckedChangedToChecked_noConnectedLeaDevices_flagOn_notStartAudioSharing() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of());
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        assertThat(mSwitchBar.isChecked()).isFalse();
        verify(mBroadcast, never()).startPrivateBroadcast();
    }

    @Test
    public void onCheckedChangedToChecked_noConnectedLeaDevices_flagOff_startAudioSharing() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, false);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of());
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        verify(mBroadcast).startPrivateBroadcast();
    }

    @Test
    public void onCheckedChangedToChecked_notSharing_withConnectedLeaDevices_startAudioSharing() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice1));
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
        verify(mBroadcast, never()).stopBroadcast(anyInt());
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

    @Test
    public void onPlaybackStarted_notInit_noDialog() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice2, mDevice1));
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController =
                new AudioSharingSwitchBarController(
                        mContext,
                        mSwitchBar,
                        new AudioSharingSwitchBarController.OnAudioSharingStateChangedListener() {
                            @Override
                            public void onAudioSharingStateChanged() {}

                            @Override
                            public void onAudioSharingProfilesConnected() {}
                        });
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        verify(mBroadcast).startPrivateBroadcast();
        mController.mBroadcastCallback.onPlaybackStarted(0, 0);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(Context.class), eq(SettingsEnums.ACTION_AUTO_JOIN_AUDIO_SHARING));

        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).isEmpty();
    }

    @Test
    public void onPlaybackStarted_showJoinAudioSharingDialog() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice2, mDevice1));
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        verify(mBroadcast).startPrivateBroadcast();
        mController.mBroadcastCallback.onPlaybackStarted(0, 0);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(Context.class), eq(SettingsEnums.ACTION_AUTO_JOIN_AUDIO_SHARING));

        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(TAG_EQUALS)
                .containsExactly(AudioSharingDialogFragment.tag());

        AudioSharingDialogFragment fragment =
                (AudioSharingDialogFragment) Iterables.getOnlyElement(childFragments);
        Pair<Integer, Object>[] eventData = fragment.getEventData();
        assertThat(eventData)
                .asList()
                .containsExactly(
                        Pair.create(
                                AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID.ordinal(),
                                SettingsEnums.AUDIO_SHARING_SETTINGS),
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
    }

    @Test
    public void testBluetoothLeBroadcastCallbacks_updateSwitch() {
        mOnAudioSharingStateChanged = false;
        mSwitchBar.setChecked(false);
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.mBroadcastCallback.onBroadcastStartFailed(/* reason= */ 1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mSwitchBar.isChecked()).isFalse();
        assertThat(mOnAudioSharingStateChanged).isFalse();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_START_FAILED,
                        SettingsEnums.AUDIO_SHARING_SETTINGS);

        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.mBroadcastCallback.onBroadcastStarted(/* reason= */ 1, /* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mSwitchBar.isChecked()).isTrue();
        assertThat(mOnAudioSharingStateChanged).isTrue();

        mOnAudioSharingStateChanged = false;
        mController.mBroadcastCallback.onBroadcastStopFailed(/* reason= */ 1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mSwitchBar.isChecked()).isTrue();
        assertThat(mOnAudioSharingStateChanged).isFalse();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_STOP_FAILED,
                        SettingsEnums.AUDIO_SHARING_SETTINGS);

        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.mBroadcastCallback.onBroadcastStopped(/* reason= */ 1, /* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mSwitchBar.isChecked()).isFalse();
        assertThat(mOnAudioSharingStateChanged).isTrue();
    }

    @Test
    public void testBluetoothLeBroadcastCallbacks_doNothing() {
        BluetoothLeBroadcastMetadata metadata = mock(BluetoothLeBroadcastMetadata.class);
        mController.mBroadcastCallback.onBroadcastMetadataChanged(/* reason= */ 1, metadata);
        mController.mBroadcastCallback.onBroadcastUpdated(/* reason= */ 1, /* broadcastId= */ 1);
        mController.mBroadcastCallback.onPlaybackStarted(/* reason= */ 1, /* broadcastId= */ 1);
        mController.mBroadcastCallback.onPlaybackStopped(/* reason= */ 1, /* broadcastId= */ 1);
        mController.mBroadcastCallback.onBroadcastUpdateFailed(
                /* reason= */ 1, /* broadcastId= */ 1);
        verify(mSwitchBar, never()).setChecked(anyBoolean());
        assertThat(mOnAudioSharingStateChanged).isFalse();
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_logAction() {
        BluetoothLeBroadcastMetadata metadata = mock(BluetoothLeBroadcastMetadata.class);
        mController.mBroadcastAssistantCallback.onSourceAddFailed(
                mDevice1, metadata, /* reason= */ 1);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_JOIN_FAILED,
                        SettingsEnums.AUDIO_SHARING_SETTINGS);
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_doNothing() {
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        BluetoothLeBroadcastMetadata metadata = mock(BluetoothLeBroadcastMetadata.class);

        // Do nothing
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(
                mDevice1, /* sourceId= */ 1, state);
        mController.mBroadcastAssistantCallback.onSearchStarted(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStartFailed(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStopped(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStopFailed(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceAdded(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceRemoved(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceRemoveFailed(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceModified(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceModifyFailed(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceFound(metadata);
        mController.mBroadcastAssistantCallback.onSourceLost(/* broadcastId= */ 1);
        verifyNoMoreInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    public void testAccessibilityDelegate() {
        View view = new View(mContext);
        AccessibilityEvent event =
                new AccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        event.setContentChangeTypes(AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);
        assertThat(
                        mSwitchBar
                                .getRootView()
                                .getAccessibilityDelegate()
                                .onRequestSendAccessibilityEvent(mSwitchBar, view, event))
                .isTrue();

        event.setContentChangeTypes(AccessibilityEvent.CONTENT_CHANGE_TYPE_ENABLED);
        assertThat(
                        mSwitchBar
                                .getRootView()
                                .getAccessibilityDelegate()
                                .onRequestSendAccessibilityEvent(mSwitchBar, view, event))
                .isFalse();
    }
}
