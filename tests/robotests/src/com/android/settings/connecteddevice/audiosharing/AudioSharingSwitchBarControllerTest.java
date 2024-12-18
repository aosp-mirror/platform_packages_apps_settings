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
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_START_LE_AUDIO_SHARING;

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
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

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
import android.os.Bundle;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.FeatureFlagUtils;
import android.util.Pair;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
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
                ShadowAlertDialogCompat.class
        })
public class AudioSharingSwitchBarControllerTest {
    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final String TEST_DEVICE_ANONYMIZED_ADDR1 = "XX:XX:01";
    private static final String TEST_DEVICE_ANONYMIZED_ADDR2 = "XX:XX:02";
    private static final int TEST_DEVICE_GROUP_ID1 = 1;
    private static final int TEST_DEVICE_GROUP_ID2 = 2;
    private static final Correspondence<Fragment, String> CLAZZNAME_EQUALS =
            Correspondence.from(
                    (Fragment fragment, String clazzName) ->
                            fragment instanceof DialogFragment
                                    && ((DialogFragment) fragment).getClass().getName() != null
                                    && ((DialogFragment) fragment).getClass().getName().equals(
                                    clazzName),
                    "is equal to");

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private CachedBluetoothDeviceManager mDeviceManager;
    @Mock private BluetoothEventManager mEventManager;
    @Mock private LocalBluetoothProfileManager mBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private BluetoothLeBroadcastMetadata mMetadata;
    @Mock private BluetoothLeBroadcastReceiveState mState;
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
        when(localBluetoothManager.getEventManager()).thenReturn(mEventManager);
        when(mDevice1.getAnonymizedAddress()).thenReturn(TEST_DEVICE_ANONYMIZED_ADDR1);
        when(mDeviceManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice1.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(false);
        when(mDevice2.getAnonymizedAddress()).thenReturn(TEST_DEVICE_ANONYMIZED_ADDR2);
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
        ShadowAlertDialogCompat.reset();
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
        verify(mEventManager, never()).registerCallback(any(BluetoothCallback.class));
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
        verify(mEventManager).registerCallback(any(BluetoothCallback.class));
        verify(mBtProfileManager, never()).addServiceListener(mController);
        assertThat(mSwitchBar.isChecked()).isTrue();
        assertThat(mSwitchBar.isEnabled()).isTrue();
    }

    @Test
    public void onStart_flagOn_updateSwitch() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of());
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mSwitchBar.isChecked()).isFalse();
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
        verify(mEventManager, never()).unregisterCallback(any(BluetoothCallback.class));
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
        verify(mEventManager, never()).unregisterCallback(any(BluetoothCallback.class));
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
        verify(mEventManager).unregisterCallback(any(BluetoothCallback.class));
    }

    @Test
    public void onCheckedChangedToChecked_sharing_doNothing() {
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        verify(mBroadcast, never()).startPrivateBroadcast();
    }

    @Test
    public void onCheckedChangedToChecked_noConnectedLeaDevices_flagOn_showDialog() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of());
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mSwitchBar.isChecked()).isFalse();
        verify(mBroadcast, never()).startPrivateBroadcast();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(CLAZZNAME_EQUALS)
                .containsExactly(AudioSharingConfirmDialogFragment.class.getName());

        childFragments.forEach(fragment -> ((DialogFragment) fragment).dismiss());
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
        when(mAssistant.getAllSources(any(BluetoothDevice.class))).thenReturn(ImmutableList.of());
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(mMetadata);
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
        shadowOf(Looper.getMainLooper()).idle();

        verify(mBroadcast).startPrivateBroadcast();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        // No progress dialog.
        assertThat(childFragments).isEmpty();

        mController.mBroadcastCallback.onPlaybackStarted(0, 0);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(Context.class), eq(SettingsEnums.ACTION_AUTO_JOIN_AUDIO_SHARING));

        childFragments = mParentFragment.getChildFragmentManager().getFragments();
        // No audio sharing dialog.
        assertThat(childFragments).isEmpty();
    }

    @Test
    public void onPlaybackStarted_hasLocalSource_noDialog() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice2, mDevice1));
        when(mState.getBroadcastId()).thenReturn(1);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        when(mAssistant.getAllSources(mDevice2)).thenReturn(ImmutableList.of(mState));
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(mMetadata);
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mBroadcast).startPrivateBroadcast();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).comparingElementsUsing(CLAZZNAME_EQUALS).containsExactly(
                AudioSharingProgressDialogFragment.class.getName());

        mController.mBroadcastCallback.onPlaybackStarted(0, 0);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mAssistant, never()).addSource(any(), any(), anyBoolean());
        verify(mFeatureFactory.metricsFeatureProvider, never())
                .action(any(Context.class), eq(SettingsEnums.ACTION_AUTO_JOIN_AUDIO_SHARING));

        childFragments = mParentFragment.getChildFragmentManager().getFragments();
        // No audio sharing dialog.
        assertThat(childFragments).comparingElementsUsing(CLAZZNAME_EQUALS).doesNotContain(
                AudioSharingDialogFragment.class.getName());

        childFragments.forEach(fragment -> ((DialogFragment) fragment).dismiss());
    }

    @Test
    public void onPlaybackStarted_singleActiveDevice_showJoinAudioSharingDialog() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice2));
        when(mAssistant.getAllSources(any(BluetoothDevice.class))).thenReturn(ImmutableList.of());
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mBroadcast).startPrivateBroadcast();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).comparingElementsUsing(CLAZZNAME_EQUALS).containsExactly(
                AudioSharingProgressDialogFragment.class.getName());

        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(mMetadata);
        mController.mBroadcastCallback.onPlaybackStarted(0, 0);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(Context.class), eq(SettingsEnums.ACTION_AUTO_JOIN_AUDIO_SHARING));

        when(mState.getBisSyncState()).thenReturn(ImmutableList.of(1L));
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(mDevice2, /* sourceId= */ 1,
                mState);
        shadowOf(Looper.getMainLooper()).idle();

        childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(CLAZZNAME_EQUALS)
                .containsExactly(AudioSharingDialogFragment.class.getName());

        Pair<Integer, Object>[] eventData = new Pair[0];
        for (Fragment fragment : childFragments) {
            if (fragment instanceof AudioSharingDialogFragment) {
                eventData = ((AudioSharingDialogFragment) fragment).getEventData();
                break;
            }
        }
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
                                0));

        childFragments.forEach(fragment -> ((DialogFragment) fragment).dismiss());
    }

    @Test
    public void onPlaybackStarted_oneActiveOnConnected_showJoinAudioSharingDialog() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice2, mDevice1));
        when(mAssistant.getAllSources(any(BluetoothDevice.class))).thenReturn(ImmutableList.of());
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mBroadcast).startPrivateBroadcast();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).comparingElementsUsing(CLAZZNAME_EQUALS).containsExactly(
                AudioSharingProgressDialogFragment.class.getName());
        AudioSharingProgressDialogFragment progressFragment =
                (AudioSharingProgressDialogFragment) Iterables.getOnlyElement(childFragments);
        // TODO: use string res once finalized
        String expectedMessage = "Starting audio stream...";
        checkProgressDialogMessage(progressFragment, expectedMessage);

        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(mMetadata);
        mController.mBroadcastCallback.onPlaybackStarted(0, 0);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(Context.class), eq(SettingsEnums.ACTION_AUTO_JOIN_AUDIO_SHARING));
        // TODO: use string res once finalized
        expectedMessage = "Sharing with " + TEST_DEVICE_NAME2 + "...";
        checkProgressDialogMessage(progressFragment, expectedMessage);

        childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments)
                .comparingElementsUsing(CLAZZNAME_EQUALS)
                .containsExactly(AudioSharingDialogFragment.class.getName(),
                        AudioSharingProgressDialogFragment.class.getName());

        Pair<Integer, Object>[] eventData = new Pair[0];
        for (Fragment fragment : childFragments) {
            if (fragment instanceof AudioSharingDialogFragment) {
                eventData = ((AudioSharingDialogFragment) fragment).getEventData();
                break;
            }
        }
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

        childFragments.forEach(fragment -> ((DialogFragment) fragment).dismiss());
    }

    @Test
    public void onPlaybackStarted_oneActiveOnConnected_clickShareBtnOnDialog_addSource() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice2, mDevice1));
        when(mAssistant.getAllSources(any(BluetoothDevice.class))).thenReturn(ImmutableList.of());
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mBroadcast).startPrivateBroadcast();
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(mMetadata);
        mController.mBroadcastCallback.onPlaybackStarted(0, 0);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mAssistant).addSource(mDevice2, mMetadata, /* isGroupOp= */ false);

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        View btnView = dialog.findViewById(R.id.positive_btn);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();

        verify(mAssistant).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
        assertThat(dialog.isShowing()).isFalse();
        // Progress dialog shows sharing progress for the user chosen sink.
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).comparingElementsUsing(CLAZZNAME_EQUALS).containsExactly(
                AudioSharingProgressDialogFragment.class.getName());
        AudioSharingProgressDialogFragment progressFragment =
                (AudioSharingProgressDialogFragment) Iterables.getOnlyElement(childFragments);
        // TODO: use string res once finalized
        String expectedMessage = "Sharing with " + TEST_DEVICE_NAME1 + "...";
        checkProgressDialogMessage(progressFragment, expectedMessage);

        childFragments.forEach(fragment -> ((DialogFragment) fragment).dismiss());
    }

    @Test
    public void onPlaybackStarted_oneActiveOnConnected_clickCancelBtnOnDialog_doNothing() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice2, mDevice1));
        when(mAssistant.getAllSources(any(BluetoothDevice.class))).thenReturn(ImmutableList.of());
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mBroadcast).startPrivateBroadcast();
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(mMetadata);
        mController.mBroadcastCallback.onPlaybackStarted(0, 0);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mAssistant).addSource(mDevice2, mMetadata, /* isGroupOp= */ false);

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        View btnView = dialog.findViewById(R.id.negative_btn);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();

        verify(mAssistant, never()).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
        assertThat(dialog.isShowing()).isFalse();
        // Progress dialog shows sharing progress for the auto add active sink.
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).comparingElementsUsing(CLAZZNAME_EQUALS).containsExactly(
                AudioSharingProgressDialogFragment.class.getName());
        AudioSharingProgressDialogFragment progressFragment =
                (AudioSharingProgressDialogFragment) Iterables.getOnlyElement(childFragments);
        // TODO: use string res once finalized
        String expectedMessage = "Sharing with " + TEST_DEVICE_NAME2 + "...";
        checkProgressDialogMessage(progressFragment, expectedMessage);

        childFragments.forEach(fragment -> ((DialogFragment) fragment).dismiss());
    }

    @Test
    public void testBroadcastCallbacks_updateSwitch() {
        mOnAudioSharingStateChanged = false;
        mSwitchBar.setChecked(false);
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice1, mDevice2));
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedDevice1, mCachedDevice2));
        mController.mBroadcastCallback.onBroadcastStartFailed(/* reason= */ 1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mSwitchBar.isChecked()).isFalse();
        assertThat(mSwitchBar.isEnabled()).isTrue();
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
        assertThat(mSwitchBar.isEnabled()).isTrue();
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
        when(mCachedDevice2.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(false);
        mController.mBroadcastCallback.onBroadcastStopped(/* reason= */ 1, /* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mSwitchBar.isChecked()).isFalse();
        assertThat(mSwitchBar.isEnabled()).isFalse();
        assertThat(mOnAudioSharingStateChanged).isTrue();
    }

    @Test
    public void testBroadcastCallbacks_doNothing() {
        mController.mBroadcastCallback.onBroadcastMetadataChanged(/* reason= */ 1, mMetadata);
        mController.mBroadcastCallback.onBroadcastUpdated(/* reason= */ 1, /* broadcastId= */ 1);
        mController.mBroadcastCallback.onPlaybackStarted(/* reason= */ 1, /* broadcastId= */ 1);
        mController.mBroadcastCallback.onPlaybackStopped(/* reason= */ 1, /* broadcastId= */ 1);
        mController.mBroadcastCallback.onBroadcastUpdateFailed(
                /* reason= */ 1, /* broadcastId= */ 1);
        verify(mSwitchBar, never()).setChecked(anyBoolean());
        assertThat(mOnAudioSharingStateChanged).isFalse();
    }

    @Test
    public void testAssistantCallbacks_onSourceAddFailed_twoDevices_showErrorAndLogAction() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST, true);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice2, mDevice1));
        when(mAssistant.getAllSources(any(BluetoothDevice.class))).thenReturn(ImmutableList.of());
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mBroadcast).startPrivateBroadcast();

        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(mMetadata);
        mController.mBroadcastCallback.onPlaybackStarted(0, 0);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mAssistant).addSource(mDevice2, mMetadata, /* isGroupOp= */ false);

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        View btnView = dialog.findViewById(R.id.positive_btn);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();

        verify(mAssistant).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
        assertThat(dialog.isShowing()).isFalse();

        mController.mBroadcastAssistantCallback.onSourceAddFailed(
                mDevice1, mMetadata, /* reason= */ 1);
        shadowMainLooper().idle();

        // Progress dialog shows sharing progress for the user chosen sink.
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).comparingElementsUsing(CLAZZNAME_EQUALS).containsExactly(
                AudioSharingErrorDialogFragment.class.getName());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_JOIN_FAILED,
                        SettingsEnums.AUDIO_SHARING_SETTINGS);

        childFragments.forEach(fragment -> ((DialogFragment) fragment).dismiss());
    }

    @Test
    public void testAssistantCallbacks_onReceiveStateChanged_dismissProgressDialog() {
        AudioSharingProgressDialogFragment.show(mParentFragment, TEST_DEVICE_NAME1);
        shadowOf(Looper.getMainLooper()).idle();
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).comparingElementsUsing(CLAZZNAME_EQUALS).containsExactly(
                AudioSharingProgressDialogFragment.class.getName());

        when(mState.getBisSyncState()).thenReturn(ImmutableList.of(1L));
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(mDevice1, /* sourceId= */ 1,
                mState);
        shadowOf(Looper.getMainLooper()).idle();
        childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).isEmpty();
    }

    @Test
    public void testAssistantCallbacks_doNothing() {
        // Do nothing
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(
                mDevice1, /* sourceId= */ 1, mState);
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
        mController.mBroadcastAssistantCallback.onSourceFound(mMetadata);
        mController.mBroadcastAssistantCallback.onSourceLost(/* broadcastId= */ 1);
        verifyNoMoreInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    public void onActiveDeviceChanged_leaProfile_updateSwitch() {
        mSwitchBar.setChecked(true);
        mSwitchBar.setEnabled(false);
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice2, mDevice1));
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedDevice2, mCachedDevice1));
        mController.onActiveDeviceChanged(mCachedDevice2, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mSwitchBar.isChecked()).isFalse();
        verify(mSwitchBar).setEnabled(true);
    }

    @Test
    public void onActiveDeviceChanged_a2dpProfile_updateSwitch() {
        mSwitchBar.setChecked(true);
        mSwitchBar.setEnabled(false);
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice1, mDevice2));
        when(mCachedDevice2.isActiveDevice(BluetoothProfile.LE_AUDIO)).thenReturn(false);
        when(mCachedDevice2.isActiveDevice(BluetoothProfile.A2DP)).thenReturn(true);
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedDevice1, mCachedDevice2));
        mController.onActiveDeviceChanged(mCachedDevice2, BluetoothProfile.A2DP);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mSwitchBar.isChecked()).isFalse();
        verify(mSwitchBar).setEnabled(true);
    }

    @Test
    public void onActiveDeviceChanged_nullActiveDevice_doNothing() {
        mController.onActiveDeviceChanged(/* activeDevice= */ null, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mSwitchBar, never()).setEnabled(anyBoolean());
        verify(mSwitchBar, never()).setChecked(anyBoolean());
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

    @Test
    public void handleStartAudioSharingFromIntent_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        var unused = setUpFragmentWithStartSharingIntent();
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mSwitchBar, never()).setChecked(true);
    }

    @Test
    public void handleStartAudioSharingFromIntent_profileNotReady_doNothing() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mAssistant.isProfileReady()).thenReturn(false);
        var unused = setUpFragmentWithStartSharingIntent();
        mController.onServiceConnected();
        shadowOf(Looper.getMainLooper()).idle();

        verify(mSwitchBar, never()).setChecked(true);
    }

    @Test
    public void handleStartAudioSharingFromIntent_argFalse_doNothing() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mSwitchBar, never()).setChecked(true);
    }

    @Test
    public void handleStartAudioSharingFromIntent_handle() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBtnView.isEnabled()).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice2, mDevice1));
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(mMetadata);
        Fragment parentFragment = setUpFragmentWithStartSharingIntent();
        mController.onServiceConnected();
        shadowOf(Looper.getMainLooper()).idle();

        verify(mSwitchBar).setChecked(true);
        doNothing().when(mBroadcast).startPrivateBroadcast();
        mController.onCheckedChanged(mBtnView, /* isChecked= */ true);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mBroadcast).startPrivateBroadcast();
        mController.mBroadcastCallback.onPlaybackStarted(0, 0);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(Context.class), eq(SettingsEnums.ACTION_AUTO_JOIN_AUDIO_SHARING));
        verify(mAssistant).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
        verify(mAssistant).addSource(mDevice2, mMetadata, /* isGroupOp= */ false);
        List<Fragment> childFragments = parentFragment.getChildFragmentManager().getFragments();
        // Skip audio sharing dialog.
        assertThat(childFragments).comparingElementsUsing(CLAZZNAME_EQUALS).containsExactly(
                AudioSharingProgressDialogFragment.class.getName());
        // Progress dialog shows sharing progress for the auto add second sink.
        AudioSharingProgressDialogFragment progressFragment =
                (AudioSharingProgressDialogFragment) Iterables.getOnlyElement(childFragments);
        // TODO: use string res once finalized
        String expectedMessage = "Sharing with " + TEST_DEVICE_NAME1 + "...";
        checkProgressDialogMessage(progressFragment, expectedMessage);

        childFragments.forEach(fragment -> ((DialogFragment) fragment).dismiss());
    }

    @Test
    public void handleAutoAddSourceAfterPair() {
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice1));
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(mMetadata);
        mController.handleAutoAddSourceAfterPair(mDevice1);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mAssistant).addSource(mDevice1, mMetadata, /* isGroupOp= */ false);
        List<Fragment> childFragments = mParentFragment.getChildFragmentManager().getFragments();
        assertThat(childFragments).comparingElementsUsing(CLAZZNAME_EQUALS).containsExactly(
                AudioSharingProgressDialogFragment.class.getName());
    }

    private Fragment setUpFragmentWithStartSharingIntent() {
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_START_LE_AUDIO_SHARING, true);
        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        Fragment fragment = new Fragment();
        FragmentController.of(fragment, intent)
                .create(/* containerViewId= */ 0, /* bundle= */ null)
                .start()
                .resume()
                .visible()
                .get();
        shadowOf(Looper.getMainLooper()).idle();
        mController.init(fragment);
        return fragment;
    }

    private void checkProgressDialogMessage(
            @NonNull AudioSharingProgressDialogFragment fragment,
            @NonNull String expectedMessage) {
        TextView progressMessage = fragment.getDialog() == null ? null
                : fragment.getDialog().findViewById(R.id.message);
        assertThat(progressMessage).isNotNull();
        assertThat(progressMessage.getText().toString()).isEqualTo(expectedMessage);
    }
}
