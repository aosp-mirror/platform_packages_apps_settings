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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.XmlTestUtils;
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

import java.util.List;
import java.util.Objects;

/** Tests for {@link AccessibilityHearingAidsFragment}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class AccessibilityHearingAidsFragmentTest {

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
    private TelephonyManager mTelephonyManager;

    @Before
    public void setUp() {
        mTelephonyManager = spy(mContext.getSystemService(TelephonyManager.class));
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        doReturn(true).when(mTelephonyManager).isHearingAidCompatibilitySupported();

        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mShadowBluetoothAdapter = Shadow.extract(mBluetoothAdapter);
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);

        final List<String> niks = AccessibilityHearingAidsFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext).stream()
                .filter(Objects::nonNull)
                .toList();
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext, R.xml.accessibility_hearing_aids);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void deviceSupportsHearingAid_isPageSearchEnabled_returnTrue() {
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);

        assertThat(AccessibilityHearingAidsFragment.isPageSearchEnabled(mContext)).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void deviceDoesNotSupportHearingAid_isPageSearchEnabled_returnFalse() {
        mShadowBluetoothAdapter.clearSupportedProfiles();
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEADSET);

        assertThat(AccessibilityHearingAidsFragment.isPageSearchEnabled(mContext)).isFalse();
    }
}
