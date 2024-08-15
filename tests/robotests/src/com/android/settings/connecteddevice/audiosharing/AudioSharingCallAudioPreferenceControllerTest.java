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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckedTextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.BluetoothUtils;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

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

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowThreadUtils.class,
            ShadowAlertDialogCompat.class,
        })
public class AudioSharingCallAudioPreferenceControllerTest {
    private static final String PREF_KEY = "calls_and_alarms";
    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final int TEST_DEVICE_GROUP_ID1 = 1;
    private static final int TEST_DEVICE_GROUP_ID2 = 2;

    private static final String TEST_SETTINGS_KEY =
            "bluetooth_le_broadcast_fallback_active_group_id";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private PreferenceScreen mScreen;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mBtEventManager;
    @Mock private LocalBluetoothProfileManager mBtProfileManager;
    @Mock private CachedBluetoothDeviceManager mCacheManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private BluetoothDevice mDevice1;
    @Mock private BluetoothDevice mDevice2;
    @Mock private BluetoothDevice mDevice3;
    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    @Mock private CachedBluetoothDevice mCachedDevice3;
    @Mock private BluetoothLeBroadcastReceiveState mState;
    @Mock private BluetoothLeBroadcastMetadata mSource;
    @Mock private ContentResolver mContentResolver;
    private AudioSharingCallAudioPreferenceController mController;
    @Spy private ContentObserver mContentObserver;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private FakeFeatureFactory mFeatureFactory;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private Preference mPreference;
    private Fragment mParentFragment;

    @Before
    public void setUp() {
        ShadowAlertDialogCompat.reset();
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mParentFragment = new Fragment();
        FragmentController.setupFragment(
                mParentFragment,
                FragmentActivity.class,
                0 /* containerViewId */,
                null /* bundle */);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        LocalBluetoothManager btManager = Utils.getLocalBtManager(mContext);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(btManager.getEventManager()).thenReturn(mBtEventManager);
        when(btManager.getProfileManager()).thenReturn(mBtProfileManager);
        when(btManager.getCachedDeviceManager()).thenReturn(mCacheManager);
        when(mBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        when(mBtProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        when(mAssistant.isProfileReady()).thenReturn(true);
        when(mVolumeControl.isProfileReady()).thenReturn(true);
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(mState.getBisSyncState()).thenReturn(bisSyncState);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        mController = new AudioSharingCallAudioPreferenceController(mContext);
        mController.init(null);
        mContentObserver = mController.getSettingsObserver();
        mPreference = new Preference(mContext);
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);
    }

    @After
    public void tearDown() {
        ShadowAlertDialogCompat.reset();
        ShadowThreadUtils.reset();
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void onStart_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mBtEventManager, never()).registerCallback(mController);
        verify(mContentResolver, never())
                .registerContentObserver(
                        Settings.Secure.getUriFor(
                                BluetoothUtils.getPrimaryGroupIdUriForBroadcast()),
                        false,
                        mContentObserver);
        verify(mAssistant, never())
                .registerServiceCallBack(any(), any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void onStart_flagOn_registerCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mBtEventManager).registerCallback(mController);
        verify(mContentResolver)
                .registerContentObserver(
                        Settings.Secure.getUriFor(
                                BluetoothUtils.getPrimaryGroupIdUriForBroadcast()),
                        false,
                        mContentObserver);
        verify(mAssistant)
                .registerServiceCallBack(any(), any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void onStop_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.setCallbacksRegistered(true);
        mController.onStop(mLifecycleOwner);
        verify(mBtEventManager, never()).unregisterCallback(mController);
        verify(mContentResolver, never()).unregisterContentObserver(mContentObserver);
        verify(mAssistant, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void onStop_flagOn_notRegistered_doNothing() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.setCallbacksRegistered(false);
        mController.onStop(mLifecycleOwner);
        verify(mBtEventManager, never()).unregisterCallback(mController);
        verify(mContentResolver, never()).unregisterContentObserver(mContentObserver);
        verify(mAssistant, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void onStop_flagOn_registered_unregisterCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.setCallbacksRegistered(true);
        mController.onStop(mLifecycleOwner);
        verify(mBtEventManager).unregisterCallback(mController);
        verify(mContentResolver).unregisterContentObserver(mContentObserver);
        verify(mAssistant)
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
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
    public void updateVisibility_flagOff_invisible() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_broadcastOffBluetoothOff_invisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mShadowBluetoothAdapter.setEnabled(false);
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_broadcastOnBluetoothOff_invisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mShadowBluetoothAdapter.setEnabled(false);
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_broadcastOffBluetoothOn_invisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_broadcastOnBluetoothOn_visible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void onProfileConnectionStateChanged_noDeviceInSharing_updateSummary() {
        Settings.Secure.putInt(mContentResolver, TEST_SETTINGS_KEY, TEST_DEVICE_GROUP_ID1);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of());
        mController.displayPreference(mScreen);
        mPreference.setSummary("test");
        mController.onProfileConnectionStateChanged(
                mCachedDevice1,
                BluetoothAdapter.STATE_DISCONNECTED,
                BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString()).isEmpty();
    }

    @Test
    public void onFallbackDeviceChanged_updateSummary() {
        Settings.Secure.putInt(mContentResolver, TEST_SETTINGS_KEY, TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice1));
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mController.displayPreference(mScreen);
        mContentObserver.onChange(true);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString())
                .isEqualTo(
                        mContext.getString(
                                R.string.audio_sharing_call_audio_description, TEST_DEVICE_NAME1));
    }

    @Test
    public void displayPreference_showCorrectSummary() {
        Settings.Secure.putInt(mContentResolver, TEST_SETTINGS_KEY, TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice2.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice2.getDevice()).thenReturn(mDevice2);
        when(mCachedDevice1.getMemberDevice()).thenReturn(ImmutableSet.of(mCachedDevice2));
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice3.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID2);
        when(mCachedDevice3.getDevice()).thenReturn(mDevice3);
        when(mCachedDevice3.getName()).thenReturn(TEST_DEVICE_NAME2);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mCacheManager.findDevice(mDevice2)).thenReturn(mCachedDevice2);
        when(mCacheManager.findDevice(mDevice3)).thenReturn(mCachedDevice3);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        when(mAssistant.getAllConnectedDevices())
                .thenReturn(ImmutableList.of(mDevice1, mDevice2, mDevice3));
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString())
                .isEqualTo(
                        mContext.getString(
                                R.string.audio_sharing_call_audio_description, TEST_DEVICE_NAME1));
    }

    @Test
    public void displayPreference_noFallbackDeviceInSharing_showEmptySummary() {
        Settings.Secure.putInt(mContentResolver, TEST_SETTINGS_KEY, TEST_DEVICE_GROUP_ID2);
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice1));
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString()).isEmpty();
    }

    @Test
    public void displayPreference_noFallbackDevice_showEmptySummary() {
        Settings.Secure.putInt(
                mContentResolver, TEST_SETTINGS_KEY, BluetoothCsipSetCoordinator.GROUP_ID_INVALID);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of());
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString()).isEmpty();
    }

    @Test
    public void displayPreference_clickToShowCorrectDialog() {
        AlertDialog latestAlertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        if (latestAlertDialog != null) {
            latestAlertDialog.dismiss();
            ShadowAlertDialogCompat.reset();
        }
        Settings.Secure.putInt(mContentResolver, TEST_SETTINGS_KEY, TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice2.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID2);
        when(mCachedDevice2.getName()).thenReturn(TEST_DEVICE_NAME2);
        when(mCachedDevice2.getDevice()).thenReturn(mDevice2);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mCacheManager.findDevice(mDevice2)).thenReturn(mCachedDevice2);
        mShadowBluetoothAdapter.setMostRecentlyConnectedDevices(List.of(mDevice1, mDevice2));
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice1, mDevice2));
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mController.init(mParentFragment);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        mPreference.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
        assertThat(dialog.getListView().getCount()).isEqualTo(2);
        ArrayList<View> outViews = new ArrayList<>();
        dialog.getListView()
                .findViewsWithText(outViews, TEST_DEVICE_NAME1, View.FIND_VIEWS_WITH_TEXT);
        assertThat(outViews.size()).isEqualTo(1);
        View view = Iterables.getOnlyElement(outViews);
        assertThat(view instanceof CheckedTextView).isTrue();
        assertThat(((CheckedTextView) view).isChecked()).isTrue();
        verify(mFeatureFactory.metricsFeatureProvider)
                .visible(
                        /* context= */ eq(null),
                        /* source= */ anyInt(),
                        eq(SettingsEnums.DIALOG_AUDIO_SHARING_CALL_AUDIO),
                        /* latency= */ anyInt());
    }

    @Test
    public void logCallAudioDeviceChange_changeCallAudioToEarlierConnectedDevice() {
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice2.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID2);
        when(mCachedDevice2.getDevice()).thenReturn(mDevice2);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mCacheManager.findDevice(mDevice2)).thenReturn(mCachedDevice2);
        mShadowBluetoothAdapter.setMostRecentlyConnectedDevices(List.of(mDevice1, mDevice2));
        mController.logCallAudioDeviceChange(TEST_DEVICE_GROUP_ID1, mCachedDevice2);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_CALL_AUDIO,
                        AudioSharingCallAudioPreferenceController.ChangeCallAudioType
                                .CONNECTED_EARLIER
                                .ordinal());
    }

    @Test
    public void logCallAudioDeviceChange_changeCallAudioToLaterConnectedDevice() {
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice2.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID2);
        when(mCachedDevice2.getDevice()).thenReturn(mDevice2);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mCacheManager.findDevice(mDevice2)).thenReturn(mCachedDevice2);
        mShadowBluetoothAdapter.setMostRecentlyConnectedDevices(List.of(mDevice1, mDevice2));
        mController.logCallAudioDeviceChange(TEST_DEVICE_GROUP_ID2, mCachedDevice1);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_CALL_AUDIO,
                        AudioSharingCallAudioPreferenceController.ChangeCallAudioType
                                .CONNECTED_LATER
                                .ordinal());
    }

    @Test
    public void logCallAudioDeviceChange_deviceNotFoundInRecentList_unknownChangeType() {
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice2.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID2);
        when(mCachedDevice2.getDevice()).thenReturn(mDevice2);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mCacheManager.findDevice(mDevice2)).thenReturn(mCachedDevice2);
        mShadowBluetoothAdapter.setMostRecentlyConnectedDevices(List.of(mDevice1));
        mController.logCallAudioDeviceChange(TEST_DEVICE_GROUP_ID1, mCachedDevice2);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_CALL_AUDIO,
                        AudioSharingCallAudioPreferenceController.ChangeCallAudioType.UNKNOWN
                                .ordinal());
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_updateSummary() {
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        Settings.Secure.putInt(
                mContentResolver, TEST_SETTINGS_KEY, BluetoothCsipSetCoordinator.GROUP_ID_INVALID);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of());
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString()).isEmpty();

        // onReceiveStateChanged will update summary
        Settings.Secure.putInt(mContentResolver, TEST_SETTINGS_KEY, TEST_DEVICE_GROUP_ID1);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice1));
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(
                mDevice1, /* sourceId= */ 1, mState);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString())
                .isEqualTo(
                        mContext.getString(
                                R.string.audio_sharing_call_audio_description, TEST_DEVICE_NAME1));
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_doNothing() {
        when(mCachedDevice1.getGroupId()).thenReturn(TEST_DEVICE_GROUP_ID1);
        when(mCachedDevice1.getDevice()).thenReturn(mDevice1);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCacheManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        Settings.Secure.putInt(
                mContentResolver, TEST_SETTINGS_KEY, BluetoothCsipSetCoordinator.GROUP_ID_INVALID);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of());
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString()).isEmpty();

        Settings.Secure.putInt(mContentResolver, TEST_SETTINGS_KEY, TEST_DEVICE_GROUP_ID1);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice1));
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(mState));
        mController.mBroadcastAssistantCallback.onSearchStarted(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStartFailed(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStopped(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStopFailed(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceAdded(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceAddFailed(
                mDevice1, mSource, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceRemoved(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceRemoveFailed(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceModified(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceModifyFailed(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceFound(mSource);
        mController.mBroadcastAssistantCallback.onSourceLost(/* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();

        // Above callbacks won't update summary.
        assertThat(mPreference.getSummary().toString()).isEmpty();
    }
}
