/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settings.bluetooth.BluetoothDetailsAudioRoutingController.KEY_AUDIO_ROUTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.util.FeatureFlagUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link BluetoothDetailsAudioRoutingController}. */
@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsAudioRoutingControllerTest extends
        BluetoothDetailsControllerTestBase {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private static final String TEST_ADDRESS = "55:66:77:88:99:AA";

    private BluetoothDetailsAudioRoutingController mController;

    @Override
    public void setUp() {
        super.setUp();

        mController = new BluetoothDetailsAudioRoutingController(mContext, mFragment, mCachedDevice,
                mLifecycle);
        final PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(mController.getPreferenceKey());
        mScreen.addPreference(preferenceCategory);
    }

    @Test
    public void isAvailable_isHearingAidDevice_available() {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING, true);
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_isNotHearingAidDevice_notAvailable() {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING, true);
        when(mCachedDevice.isHearingAidDevice()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void init_isHearingAidDevice_expectedAudioRoutingPreference() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);
        when(mCachedDevice.getAddress()).thenReturn(TEST_ADDRESS);

        mController.init(mScreen);
        final Preference preference = mScreen.findPreference(KEY_AUDIO_ROUTING);
        final String address = preference.getExtras().getString(
                BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS);
        final String fragment = preference.getFragment();

        assertThat(address).isEqualTo(TEST_ADDRESS);
        assertThat(fragment).isEqualTo(BluetoothDetailsAudioRoutingFragment.class.getName());

    }
}
