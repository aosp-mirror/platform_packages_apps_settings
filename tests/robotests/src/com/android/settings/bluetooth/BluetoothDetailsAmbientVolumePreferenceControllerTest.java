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

import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME;
import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME_SLIDER;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothDevice;
import android.os.Looper;

import androidx.preference.PreferenceCategory;

import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Set;

/** Tests for {@link BluetoothDetailsAmbientVolumePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
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

    private BluetoothDetailsAmbientVolumePreferenceController mController;

    @Before
    public void setUp() {
        super.setUp();

        PreferenceCategory deviceControls = new PreferenceCategory(mContext);
        deviceControls.setKey(KEY_HEARING_DEVICE_GROUP);
        mScreen.addPreference(deviceControls);
        mController = new BluetoothDetailsAmbientVolumePreferenceController(mContext, mFragment,
                mCachedDevice, mLifecycle);
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
        when(mCachedDevice.getDeviceSide()).thenReturn(SIDE_LEFT);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mDevice.getAddress()).thenReturn(TEST_ADDRESS);
        if (hasMember) {
            when(mCachedDevice.getMemberDevice()).thenReturn(Set.of(mCachedMemberDevice));
            when(mCachedMemberDevice.getDeviceSide()).thenReturn(SIDE_RIGHT);
            when(mCachedMemberDevice.getDevice()).thenReturn(mMemberDevice);
            when(mMemberDevice.getAddress()).thenReturn(TEST_MEMBER_ADDRESS);
        }
    }
}
