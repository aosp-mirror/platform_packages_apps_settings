/*
 * Copyright 2018 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.AvailableMediaBluetoothDeviceUpdater;
import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingDialogHandler;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.concurrent.Executor;

/** Tests for {@link AvailableMediaDeviceGroupController}. */
@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAudioManager.class,
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowAlertDialogCompat.class,
        })
public class AvailableMediaDeviceGroupControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";
    private static final String PREFERENCE_KEY_1 = "pref_key_1";
    private static final String TEST_DEVICE_NAME = "test";

    @Mock private AvailableMediaBluetoothDeviceUpdater mAvailableMediaBluetoothDeviceUpdater;
    @Mock private PreferenceScreen mPreferenceScreen;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    @Mock private PackageManager mPackageManager;
    @Mock private BluetoothEventManager mEventManager;
    @Mock private LocalBluetoothManager mLocalBluetoothManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock private BluetoothDevice mDevice;
    @Mock private Drawable mDrawable;
    @Mock private AudioSharingDialogHandler mDialogHandler;

    private PreferenceGroup mPreferenceGroup;
    private Context mContext;
    private FragmentManager mFragManager;
    private FakeFeatureFactory mFeatureFactory;
    private Preference mPreference;
    private AvailableMediaDeviceGroupController mAvailableMediaDeviceGroupController;
    private AudioManager mAudioManager;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mPreference = new Preference(mContext);
        mPreference.setKey(PREFERENCE_KEY_1);
        mPreferenceGroup = spy(new PreferenceScreen(mContext, null));
        mFragManager =
                Robolectric.setupActivity(FragmentActivity.class).getSupportFragmentManager();
        when(mPreferenceGroup.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mAudioManager = mContext.getSystemService(AudioManager.class);
        doReturn(mEventManager).when(mLocalBluetoothManager).getEventManager();
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mCachedDeviceManager.findDevice(any(BluetoothDevice.class)))
                .thenReturn(mCachedBluetoothDevice);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);

        mAvailableMediaDeviceGroupController =
                spy(new AvailableMediaDeviceGroupController(mContext));
        mAvailableMediaDeviceGroupController.setBluetoothDeviceUpdater(
                mAvailableMediaBluetoothDeviceUpdater);
        mAvailableMediaDeviceGroupController.setDialogHandler(mDialogHandler);
        mAvailableMediaDeviceGroupController.setFragmentManager(mFragManager);
        mAvailableMediaDeviceGroupController.mPreferenceGroup = mPreferenceGroup;
    }

    @Test
    public void onDeviceAdded_firstAdd_becomeVisibleAndPreferenceAdded() {
        mAvailableMediaDeviceGroupController.onDeviceAdded(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isTrue();
        assertThat((Preference) mPreferenceGroup.findPreference(PREFERENCE_KEY_1))
                .isEqualTo(mPreference);
    }

    @Test
    public void onDeviceRemoved_lastRemove_becomeInvisibleAndPreferenceRemoved() {
        mPreferenceGroup.addPreference(mPreference);

        mAvailableMediaDeviceGroupController.onDeviceRemoved(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void onDeviceRemoved_notLastRemove_stillVisible() {
        mPreferenceGroup.setVisible(true);
        mPreferenceGroup.addPreference(mPreference);
        mPreferenceGroup.addPreference(new Preference(mContext));

        mAvailableMediaDeviceGroupController.onDeviceRemoved(mPreference);

        assertThat(mPreferenceGroup.isVisible()).isTrue();
    }

    @Test
    public void displayPreference_becomeInvisible() {
        doReturn(mPreferenceGroup).when(mPreferenceScreen).findPreference(anyString());

        mAvailableMediaDeviceGroupController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    public void testRegister_audioSharingFlagOff() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        setUpBroadcast();
        // register the callback in onStart()
        mAvailableMediaDeviceGroupController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mAvailableMediaBluetoothDeviceUpdater).registerCallback();
        verify(mEventManager).registerCallback(any(BluetoothCallback.class));
        verify(mAvailableMediaBluetoothDeviceUpdater).refreshPreference();
        verify(mBroadcast, times(0))
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant, times(0))
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDialogHandler, times(0)).registerCallbacks(any(Executor.class));
    }

    @Test
    public void testRegister_audioSharingFlagOn() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        setUpBroadcast();
        // register the callback in onStart()
        mAvailableMediaDeviceGroupController.onStart(mLifecycleOwner);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mAvailableMediaBluetoothDeviceUpdater).registerCallback();
        verify(mEventManager).registerCallback(any(BluetoothCallback.class));
        verify(mAvailableMediaBluetoothDeviceUpdater).refreshPreference();
        verify(mBroadcast)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDialogHandler).registerCallbacks(any(Executor.class));
    }

    @Test
    public void testUnregister_audioSharingFlagOff() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        setUpBroadcast();
        // unregister the callback in onStop()
        mAvailableMediaDeviceGroupController.onStop(mLifecycleOwner);
        verify(mAvailableMediaBluetoothDeviceUpdater).unregisterCallback();
        verify(mEventManager).unregisterCallback(any(BluetoothCallback.class));
        verify(mBroadcast, times(0))
                .unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant, times(0))
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDialogHandler, times(0)).unregisterCallbacks();
    }

    @Test
    public void testUnregister_audioSharingFlagOn() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        setUpBroadcast();
        // unregister the callback in onStop()
        mAvailableMediaDeviceGroupController.onStop(mLifecycleOwner);
        verify(mAvailableMediaBluetoothDeviceUpdater).unregisterCallback();
        verify(mEventManager).unregisterCallback(any(BluetoothCallback.class));
        verify(mBroadcast).unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
        verify(mAssistant)
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDialogHandler).unregisterCallbacks();
    }

    @Test
    public void testGetAvailabilityStatus_noBluetoothFeature_returnUnSupported() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mAvailableMediaDeviceGroupController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void testGetAvailabilityStatus_BluetoothFeature_returnSupported() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mAvailableMediaDeviceGroupController.getAvailabilityStatus())
                .isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void setTitle_inCallState_showCallStateTitle() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        setUpBroadcast();
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        mAvailableMediaDeviceGroupController.onAudioModeChanged();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreferenceGroup.getTitle().toString())
                .isEqualTo(mContext.getString(R.string.connected_device_call_device_title));
    }

    @Test
    public void setTitle_notInCallState_notInAudioSharing_showMediaStateTitle() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        setUpBroadcast();
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        mAvailableMediaDeviceGroupController.onAudioModeChanged();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreferenceGroup.getTitle().toString())
                .isEqualTo(mContext.getString(R.string.connected_device_media_device_title));
    }

    @Test
    public void setTitle_notInCallState_audioSharingFlagOff_showMediaStateTitle() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        setUpBroadcast();
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        mAvailableMediaDeviceGroupController.onAudioModeChanged();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreferenceGroup.getTitle().toString())
                .isEqualTo(mContext.getString(R.string.connected_device_media_device_title));
    }

    @Test
    public void setTitle_notInCallState_inAudioSharing_showAudioSharingMediaStateTitle() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        setUpBroadcast();
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        mAvailableMediaDeviceGroupController.onAudioModeChanged();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreferenceGroup.getTitle().toString())
                .isEqualTo(mContext.getString(R.string.audio_sharing_media_device_group_title));
    }

    @Test
    public void onStart_localBluetoothManagerNull_shouldNotCrash() {
        mAvailableMediaDeviceGroupController.mBtManager = null;

        // Shouldn't crash
        mAvailableMediaDeviceGroupController.onStart(mLifecycleOwner);
    }

    @Test
    public void onStop_localBluetoothManagerNull_shouldNotCrash() {
        mAvailableMediaDeviceGroupController.mBtManager = null;

        // Shouldn't crash
        mAvailableMediaDeviceGroupController.onStop(mLifecycleOwner);
    }

    @Test
    public void onActiveDeviceChanged_hearingAidProfile_launchHearingAidPairingDialog() {
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode())
                .thenReturn(HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mCachedBluetoothDevice.getDeviceSide())
                .thenReturn(HearingAidInfo.DeviceSide.SIDE_LEFT);

        mAvailableMediaDeviceGroupController.onActiveDeviceChanged(
                mCachedBluetoothDevice, BluetoothProfile.HEARING_AID);
        shadowMainLooper().idle();

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
    }

    @Test
    public void onDeviceClick_audioSharingOff_setActive() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mDevice);
        Pair<Drawable, String> pair = new Pair<>(mDrawable, TEST_DEVICE_NAME);
        when(mCachedBluetoothDevice.getDrawableWithDescription()).thenReturn(pair);
        BluetoothDevicePreference preference =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedBluetoothDevice,
                        true,
                        BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        mAvailableMediaDeviceGroupController.onDeviceClick(preference);
        verify(mCachedBluetoothDevice).setActive();
    }

    @Test
    public void onDeviceClick_audioSharingOn_dialogHandler() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        setUpBroadcast();
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mDevice);
        Pair<Drawable, String> pair = new Pair<>(mDrawable, TEST_DEVICE_NAME);
        when(mCachedBluetoothDevice.getDrawableWithDescription()).thenReturn(pair);
        BluetoothDevicePreference preference =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedBluetoothDevice,
                        true,
                        BluetoothDevicePreference.SortType.TYPE_NO_SORT);
        mAvailableMediaDeviceGroupController.onDeviceClick(preference);
        verify(mDialogHandler)
                .handleDeviceConnected(mCachedBluetoothDevice, /* userTriggered= */ true);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_MEDIA_DEVICE_CLICK);
    }

    private void setUpBroadcast() {
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        when(mLocalBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mLocalBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        mAvailableMediaDeviceGroupController =
                spy(new AvailableMediaDeviceGroupController(mContext));
        mAvailableMediaDeviceGroupController.setBluetoothDeviceUpdater(
                mAvailableMediaBluetoothDeviceUpdater);
        mAvailableMediaDeviceGroupController.setDialogHandler(mDialogHandler);
        mAvailableMediaDeviceGroupController.setFragmentManager(mFragManager);
        mAvailableMediaDeviceGroupController.mPreferenceGroup = mPreferenceGroup;
    }
}
