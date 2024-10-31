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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.os.Looper;
import android.provider.Settings;

import androidx.preference.PreferenceCategory;

import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingDeviceLocalDataManager;

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

    private BluetoothDetailsAmbientVolumePreferenceController mController;

    @Before
    public void setUp() {
        super.setUp();

        PreferenceCategory deviceControls = new PreferenceCategory(mContext);
        deviceControls.setKey(KEY_HEARING_DEVICE_GROUP);
        mScreen.addPreference(deviceControls);
        mController = new BluetoothDetailsAmbientVolumePreferenceController(mContext, mFragment,
                mCachedDevice, mLifecycle, mLocalDataManager);
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
        prepareDevice(/* hasMember= */ false, /* controlExpanded= */ true);

        mController.init(mScreen);
        mController.onDeviceLocalDataChange(TEST_ADDRESS, prepareEmptyData());
        shadowOf(Looper.getMainLooper()).idle();

        AmbientVolumePreference preference = mScreen.findPreference(KEY_AMBIENT_VOLUME);
        assertThat(preference).isNotNull();
        assertThat(preference.isExpanded()).isFalse();
        verifyDeviceDataUpdated(mDevice);
    }

    @Test
    public void onDeviceLocalDataChange_noMemberAndCollapsed_uiCorrectAndDataUpdated() {
        prepareDevice(/* hasMember= */ false, /* controlExpanded= */ false);

        mController.init(mScreen);
        mController.onDeviceLocalDataChange(TEST_ADDRESS, prepareEmptyData());
        shadowOf(Looper.getMainLooper()).idle();

        AmbientVolumePreference preference = mScreen.findPreference(KEY_AMBIENT_VOLUME);
        assertThat(preference).isNotNull();
        assertThat(preference.isExpanded()).isFalse();
        verifyDeviceDataUpdated(mDevice);
    }

    @Test
    public void onDeviceLocalDataChange_hasMemberAndExpanded_uiCorrectAndDataUpdated() {
        prepareDevice(/* hasMember= */ true, /* controlExpanded= */ true);

        mController.init(mScreen);
        mController.onDeviceLocalDataChange(TEST_ADDRESS, prepareEmptyData());
        shadowOf(Looper.getMainLooper()).idle();

        AmbientVolumePreference preference = mScreen.findPreference(KEY_AMBIENT_VOLUME);
        assertThat(preference).isNotNull();
        assertThat(preference.isExpanded()).isTrue();
        verifyDeviceDataUpdated(mDevice);
    }

    @Test
    public void onDeviceLocalDataChange_hasMemberAndCollapsed_uiCorrectAndDataUpdated() {
        prepareDevice(/* hasMember= */ true, /* controlExpanded= */ false);

        mController.init(mScreen);
        mController.onDeviceLocalDataChange(TEST_ADDRESS, prepareEmptyData());
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

    private void prepareDevice(boolean hasMember) {
        prepareDevice(hasMember, false);
    }

    private void prepareDevice(boolean hasMember, boolean controlExpanded) {
        when(mCachedDevice.getDeviceSide()).thenReturn(SIDE_LEFT);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getBondState()).thenReturn(BOND_BONDED);
        when(mDevice.getAddress()).thenReturn(TEST_ADDRESS);
        when(mDevice.getAnonymizedAddress()).thenReturn(TEST_ADDRESS);
        if (hasMember) {
            when(mCachedDevice.getMemberDevice()).thenReturn(Set.of(mCachedMemberDevice));
            when(mCachedMemberDevice.getDeviceSide()).thenReturn(SIDE_RIGHT);
            when(mCachedMemberDevice.getDevice()).thenReturn(mMemberDevice);
            when(mCachedMemberDevice.getBondState()).thenReturn(BOND_BONDED);
            when(mMemberDevice.getAddress()).thenReturn(TEST_MEMBER_ADDRESS);
            when(mMemberDevice.getAnonymizedAddress()).thenReturn(TEST_MEMBER_ADDRESS);
        }
        HearingDeviceLocalDataManager.Data data = new HearingDeviceLocalDataManager.Data.Builder()
                .ambient(0).groupAmbient(0).ambientControlExpanded(controlExpanded).build();
        when(mLocalDataManager.get(any(BluetoothDevice.class))).thenReturn(data);
    }

    private HearingDeviceLocalDataManager.Data prepareEmptyData() {
        return new HearingDeviceLocalDataManager.Data.Builder().build();
    }

    private void verifyDeviceDataUpdated(BluetoothDevice device) {
        verify(mLocalDataManager, atLeastOnce()).updateAmbient(eq(device), anyInt());
        verify(mLocalDataManager, atLeastOnce()).updateGroupAmbient(eq(device), anyInt());
        verify(mLocalDataManager, atLeastOnce()).updateAmbientControlExpanded(eq(device),
                anyBoolean());
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
