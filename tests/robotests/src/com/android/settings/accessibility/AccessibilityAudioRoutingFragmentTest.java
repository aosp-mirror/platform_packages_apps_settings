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
package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.FeatureFlagUtils;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

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

/** Tests for {@link AccessibilityAudioRoutingFragment}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class AccessibilityAudioRoutingFragmentTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private BluetoothAdapter mBluetoothAdapter;

    @Before
    public void setUp() {
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mShadowBluetoothAdapter = Shadow.extract(mBluetoothAdapter);
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void deviceSupportsHearingAidAndPageEnabled_isPageSearchEnabled_returnTrue() {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING, true);
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);

        assertThat(AccessibilityAudioRoutingFragment.isPageSearchEnabled(mContext)).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void deviceDoesNotSupportHearingAidAndPageEnabled_isPageSearchEnabled_returnFalse() {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING, true);
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEADSET);

        assertThat(AccessibilityAudioRoutingFragment.isPageSearchEnabled(mContext)).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void deviceSupportsHearingAidAndPageDisabled_isPageSearchEnabled_returnFalse() {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING, false);
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);

        assertThat(AccessibilityAudioRoutingFragment.isPageSearchEnabled(mContext)).isFalse();
    }
}
