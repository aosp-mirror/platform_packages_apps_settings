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

package com.android.settings.bluetooth;

import static android.bluetooth.AudioInputControl.MUTE_DISABLED;
import static android.bluetooth.AudioInputControl.MUTE_NOT_MUTED;
import static android.bluetooth.AudioInputControl.MUTE_MUTED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;

import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME;
import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME_SLIDER;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.preference.PreferenceCategory;

import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.bluetooth.AmbientVolumeController;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingDeviceLocalDataManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/** Tests for {@link BluetoothDetailsAmbientVolumePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        BluetoothDetailsAmbientVolumePreferenceControllerTest.ShadowGlobal.class,
        ShadowThreadUtils.class
})
public class BluetoothDetailsAmbientVolumePreferenceControllerTest extends
        BluetoothDetailsControllerTestBase {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String LEFT_CONTROL_KEY = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_LEFT;
    private static final String RIGHT_CONTROL_KEY = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_RIGHT;
    private static final String TEST_ADDRESS = "00:00:00:00:11";
    private static final String TEST_MEMBER_ADDRESS = "00:00:00:00:22";

    @Mock
    private CachedBluetoothDevice mCachedMemberDevice;
    @Mock
    private BluetoothDevice mDevice;
    @Mock
    private BluetoothDevice mMemberDevice;
    @Mock
    private HearingDeviceLocalDataManager mLocalDataManager;
    @Mock
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private BluetoothEventManager mEventManager;
    @Mock
    private LocalBluetoothProfileManager mProfileManager;
    @Mock
    private VolumeControlProfile mVolumeControlProfile;
    @Mock
    private AmbientVolumeController mVolumeController;
    @Mock
    private Handler mTestHandler;

    private BluetoothDetailsAmbientVolumePreferenceController mController;

    @Before
    public void setUp() {
        super.setUp();

        mContext = spy(mContext);
        PreferenceCategory deviceControls = new PreferenceCategory(mContext);
        deviceControls.setKey(KEY_HEARING_DEVICE_GROUP);
        mScreen.addPreference(deviceControls);
        mController = spy(
                new BluetoothDetailsAmbientVolumePreferenceController(mContext, mBluetoothManager,
                        mFragment, mCachedDevice, mLifecycle, mLocalDataManager,
                        mVolumeController));

        when(mBluetoothManager.getEventManager()).thenReturn(mEventManager);
        when(mBluetoothManager.getProfileManager()).thenReturn(mProfileManager);
        when(mProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControlProfile);
        when(mVolumeControlProfile.getConnectionStatus(mDevice)).thenReturn(
                BluetoothProfile.STATE_CONNECTED);
        when(mVolumeControlProfile.getConnectionStatus(mMemberDevice)).thenReturn(
                BluetoothProfile.STATE_CONNECTED);
        when(mCachedDevice.getProfiles()).thenReturn(List.of(mVolumeControlProfile));
        when(mLocalDataManager.get(any(BluetoothDevice.class))).thenReturn(
                new HearingDeviceLocalDataManager.Data.Builder().build());

        when(mContext.getMainThreadHandler()).thenReturn(mTestHandler);
        when(mTestHandler.postDelayed(any(Runnable.class), anyLong())).thenAnswer(
                invocationOnMock -> {
                    invocationOnMock.getArgument(0, Runnable.class).run();
                    return null;
                });
    }

    @Test
    public void init_deviceWithoutMember_controlNotExpandable() {
        prepareDevice(/* hasMember= */ false);

        mController.init(mScreen);

        AmbientVolumePreference preference = mScreen.findPreference(KEY_AMBIENT_VOLUME);
        assertThat(preference).isNotNull();
        assertThat(preference.isExpandable()).isFalse();
    }

    @Test
    public void init_deviceWithMember_controlExpandable() {
        prepareDevice(/* hasMember= */ true);

        mController.init(mScreen);

        AmbientVolumePreference preference = mScreen.findPreference(KEY_AMBIENT_VOLUME);
        assertThat(preference).isNotNull();
        assertThat(preference.isExpandable()).isTrue();
    }

    @Test
    public void onDeviceLocalDataChange_noMemberAndExpanded_uiCorrectAndDataUpdated() {
        prepareDevice(/* hasMember= */ false);
        mController.init(mScreen);
        HearingDeviceLocalDataManager.Data data = new HearingDeviceLocalDataManager.Data.Builder()
                .ambient(0).groupAmbient(0).ambientControlExpanded(true).build();
        when(mLocalDataManager.get(mDevice)).thenReturn(data);

        mController.onDeviceLocalDataChange(TEST_ADDRESS, data);
        shadowOf(Looper.getMainLooper()).idle();

        AmbientVolumePreference preference = mScreen.findPreference(KEY_AMBIENT_VOLUME);
        assertThat(preference).isNotNull();
        assertThat(preference.isExpanded()).isFalse();
        verifyDeviceDataUpdated(mDevice);
    }

    @Test
    public void onDeviceLocalDataChange_noMemberAndCollapsed_uiCorrectAndDataUpdated() {
        prepareDevice(/* hasMember= */ false);
        mController.init(mScreen);
        HearingDeviceLocalDataManager.Data data = new HearingDeviceLocalDataManager.Data.Builder()
                .ambient(0).groupAmbient(0).ambientControlExpanded(false).build();
        when(mLocalDataManager.get(mDevice)).thenReturn(data);

        mController.onDeviceLocalDataChange(TEST_ADDRESS, data);
        shadowOf(Looper.getMainLooper()).idle();

        AmbientVolumePreference preference = mScreen.findPreference(KEY_AMBIENT_VOLUME);
        assertThat(preference).isNotNull();
        assertThat(preference.isExpanded()).isFalse();
        verifyDeviceDataUpdated(mDevice);
    }

    @Test
    public void onDeviceLocalDataChange_hasMemberAndExpanded_uiCorrectAndDataUpdated() {
        prepareDevice(/* hasMember= */ true);
        mController.init(mScreen);
        HearingDeviceLocalDataManager.Data data = new HearingDeviceLocalDataManager.Data.Builder()
                .ambient(0).groupAmbient(0).ambientControlExpanded(true).build();
        when(mLocalDataManager.get(mDevice)).thenReturn(data);

        mController.onDeviceLocalDataChange(TEST_ADDRESS, data);
        shadowOf(Looper.getMainLooper()).idle();

        AmbientVolumePreference preference = mScreen.findPreference(KEY_AMBIENT_VOLUME);
        assertThat(preference).isNotNull();
        assertThat(preference.isExpanded()).isTrue();
        verifyDeviceDataUpdated(mDevice);
    }

    @Test
    public void onDeviceLocalDataChange_hasMemberAndCollapsed_uiCorrectAndDataUpdated() {
        prepareDevice(/* hasMember= */ true);
        mController.init(mScreen);
        HearingDeviceLocalDataManager.Data data = new HearingDeviceLocalDataManager.Data.Builder()
                .ambient(0).groupAmbient(0).ambientControlExpanded(false).build();
        when(mLocalDataManager.get(mDevice)).thenReturn(data);

        mController.onDeviceLocalDataChange(TEST_ADDRESS, data);
        shadowOf(Looper.getMainLooper()).idle();

        AmbientVolumePreference preference = mScreen.findPreference(KEY_AMBIENT_VOLUME);
        assertThat(preference).isNotNull();
        assertThat(preference.isExpanded()).isFalse();
        verifyDeviceDataUpdated(mDevice);
    }

    @Test
    public void onStart_localDataManagerStartAndCallbackRegistered() {
        prepareDevice(/* hasMember= */ true);
        mController.init(mScreen);

        mController.onStart();

        verify(mLocalDataManager, atLeastOnce()).start();
        verify(mVolumeController).registerCallback(any(Executor.class), eq(mDevice));
        verify(mVolumeController).registerCallback(any(Executor.class), eq(mMemberDevice));
        verify(mCachedDevice).registerCallback(any(Executor.class),
                any(CachedBluetoothDevice.Callback.class));
        verify(mCachedMemberDevice).registerCallback(any(Executor.class),
                any(CachedBluetoothDevice.Callback.class));
    }

    @Test
    public void onStop_localDataManagerStopAndCallbackUnregistered() {
        prepareDevice(/* hasMember= */ true);
        mController.init(mScreen);

        mController.onStop();

        verify(mLocalDataManager).stop();
        verify(mVolumeController).unregisterCallback(mDevice);
        verify(mVolumeController).unregisterCallback(mMemberDevice);
        verify(mCachedDevice).unregisterCallback(any(CachedBluetoothDevice.Callback.class));
        verify(mCachedMemberDevice).unregisterCallback(any(CachedBluetoothDevice.Callback.class));
    }

    @Test
    public void onDeviceAttributesChanged_newDevice_newPreference() {
        prepareDevice(/* hasMember= */ false);
        mController.init(mScreen);

        // check the right control is null before onDeviceAttributesChanged()
        SeekBarPreference leftControl = mScreen.findPreference(LEFT_CONTROL_KEY);
        SeekBarPreference rightControl = mScreen.findPreference(RIGHT_CONTROL_KEY);
        assertThat(leftControl).isNotNull();
        assertThat(rightControl).isNull();

        prepareDevice(/* hasMember= */ true);
        mController.onDeviceAttributesChanged();
        shadowOf(Looper.getMainLooper()).idle();

        // check the right control is created after onDeviceAttributesChanged()
        SeekBarPreference updatedLeftControl = mScreen.findPreference(LEFT_CONTROL_KEY);
        SeekBarPreference updatedRightControl = mScreen.findPreference(RIGHT_CONTROL_KEY);
        assertThat(updatedLeftControl).isEqualTo(leftControl);
        assertThat(updatedRightControl).isNotNull();
    }

    @Test
    public void onAmbientChanged_refreshWhenNotInitiateFromUi() {
        prepareDevice(/* hasMember= */ false);
        mController.init(mScreen);
        final int testAmbient = 10;
        HearingDeviceLocalDataManager.Data data = new HearingDeviceLocalDataManager.Data.Builder()
                .ambient(testAmbient)
                .groupAmbient(testAmbient)
                .ambientControlExpanded(false)
                .build();
        when(mLocalDataManager.get(mDevice)).thenReturn(data);
        getPreference().setExpanded(true);

        mController.onAmbientChanged(mDevice, testAmbient);
        verify(mController, never()).refresh();

        final int updatedTestAmbient = 20;
        mController.onAmbientChanged(mDevice, updatedTestAmbient);
        verify(mController).refresh();
    }

    @Test
    public void onMuteChanged_refreshWhenNotInitiateFromUi() {
        prepareDevice(/* hasMember= */ false);
        mController.init(mScreen);
        final int testMute = MUTE_NOT_MUTED;
        AmbientVolumeController.RemoteAmbientState state =
                new AmbientVolumeController.RemoteAmbientState(testMute, 0);
        when(mVolumeController.refreshAmbientState(mDevice)).thenReturn(state);
        getPreference().setMuted(false);

        mController.onMuteChanged(mDevice, testMute);
        verify(mController, never()).refresh();

        final int updatedTestMute = MUTE_MUTED;
        mController.onMuteChanged(mDevice, updatedTestMute);
        verify(mController).refresh();
    }

    @Test
    public void refresh_leftAndRightDifferentGainSetting_expandControl() {
        prepareDevice(/* hasMember= */ true);
        mController.init(mScreen);
        prepareRemoteData(mDevice, 10, MUTE_NOT_MUTED);
        prepareRemoteData(mMemberDevice, 20, MUTE_NOT_MUTED);
        getPreference().setExpanded(false);

        mController.refresh();

        assertThat(getPreference().isExpanded()).isTrue();
    }

    @Test
    public void refresh_oneSideNotMutable_controlNotMutableAndNotMuted() {
        prepareDevice(/* hasMember= */ true);
        mController.init(mScreen);
        prepareRemoteData(mDevice, 10, MUTE_DISABLED);
        prepareRemoteData(mMemberDevice, 20, MUTE_NOT_MUTED);
        getPreference().setMutable(true);
        getPreference().setMuted(true);

        mController.refresh();

        assertThat(getPreference().isMutable()).isFalse();
        assertThat(getPreference().isMuted()).isFalse();
    }

    @Test
    public void refresh_oneSideNotMuted_controlNotMutedAndSyncToRemote() {
        prepareDevice(/* hasMember= */ true);
        mController.init(mScreen);
        prepareRemoteData(mDevice, 10, MUTE_MUTED);
        prepareRemoteData(mMemberDevice, 20, MUTE_NOT_MUTED);
        getPreference().setMutable(true);
        getPreference().setMuted(true);

        mController.refresh();

        assertThat(getPreference().isMutable()).isTrue();
        assertThat(getPreference().isMuted()).isFalse();
        verify(mVolumeController).setMuted(mDevice, false);
    }

    private void prepareDevice(boolean hasMember) {
        when(mCachedDevice.getDeviceSide()).thenReturn(SIDE_LEFT);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getBondState()).thenReturn(BOND_BONDED);
        when(mDevice.getAddress()).thenReturn(TEST_ADDRESS);
        when(mDevice.getAnonymizedAddress()).thenReturn(TEST_ADDRESS);
        when(mDevice.isConnected()).thenReturn(true);
        if (hasMember) {
            when(mCachedDevice.getMemberDevice()).thenReturn(Set.of(mCachedMemberDevice));
            when(mCachedMemberDevice.getDeviceSide()).thenReturn(SIDE_RIGHT);
            when(mCachedMemberDevice.getDevice()).thenReturn(mMemberDevice);
            when(mCachedMemberDevice.getBondState()).thenReturn(BOND_BONDED);
            when(mMemberDevice.getAddress()).thenReturn(TEST_MEMBER_ADDRESS);
            when(mMemberDevice.getAnonymizedAddress()).thenReturn(TEST_MEMBER_ADDRESS);
            when(mMemberDevice.isConnected()).thenReturn(true);
        }
    }

    private void prepareRemoteData(BluetoothDevice device, int gainSetting, int mute) {
        when(mVolumeController.isAmbientControlAvailable(device)).thenReturn(true);
        when(mVolumeController.refreshAmbientState(device)).thenReturn(
                new AmbientVolumeController.RemoteAmbientState(gainSetting, mute));
    }

    private void verifyDeviceDataUpdated(BluetoothDevice device) {
        verify(mLocalDataManager, atLeastOnce()).updateAmbient(eq(device), anyInt());
        verify(mLocalDataManager, atLeastOnce()).updateGroupAmbient(eq(device), anyInt());
        verify(mLocalDataManager, atLeastOnce()).updateAmbientControlExpanded(eq(device),
                anyBoolean());
    }

    private AmbientVolumePreference getPreference() {
        return mScreen.findPreference(KEY_AMBIENT_VOLUME);
    }

    @Implements(value = Settings.Global.class)
    public static class ShadowGlobal extends ShadowSettings.ShadowGlobal {
        private static final Map<ContentResolver, Map<String, String>> sDataMap = new HashMap<>();

        @Implementation
        protected static boolean putStringForUser(
                ContentResolver cr, String name, String value, int userHandle) {
            get(cr).put(name, value);
            return true;
        }

        @Implementation
        protected static String getStringForUser(ContentResolver cr, String name, int userHandle) {
            return get(cr).get(name);
        }

        private static Map<String, String> get(ContentResolver cr) {
            return sDataMap.computeIfAbsent(cr, k -> new HashMap<>());
        }
    }
}
