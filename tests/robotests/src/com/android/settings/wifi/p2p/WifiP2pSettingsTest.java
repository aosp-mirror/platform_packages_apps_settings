/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.wifi.p2p;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.XmlTestUtils;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WifiP2pSettingsTest {

    private Context mContext;
    private FragmentActivity mActivity;
    private WifiP2pSettings mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mFragment = spy(new WifiP2pSettings());
    }

    @Test
    public void preferenceScreenKey_shouldContainsAllControllerKeys() {
        final List<String> preferenceScreenKeys = XmlTestUtils.getKeysFromPreferenceXml(mContext,
                mFragment.getPreferenceScreenResId());
        final List<String> preferenceKeys = new ArrayList<>();

        for (AbstractPreferenceController controller : mFragment.createPreferenceControllers(
                mContext)) {
            preferenceKeys.add(controller.getPreferenceKey());
        }

        assertThat(preferenceScreenKeys).containsAllIn(preferenceKeys);
    }

    @Test
    public void onActivityCreate_withNullBundle_canNotGetValue() {
        when(mFragment.getActivity()).thenReturn(mActivity);

        mFragment.onActivityCreated(null);

        assertThat(mFragment.mSelectedWifiPeer).isNull();
    }

    @Test
    public void onActivityCreate_withDeviceName_shouldGetDeviceName() {
        when(mFragment.getActivity()).thenReturn(mActivity);
        final String fakeDeviceName = "fakename";
        final Bundle bundle = new Bundle();
        bundle.putString(WifiP2pSettings.SAVE_DEVICE_NAME, fakeDeviceName);

        mFragment.onActivityCreated(bundle);

        assertThat(mFragment.mSavedDeviceName).isEqualTo(fakeDeviceName);
    }

    @Test
    public void onActivityCreate_withGroupName_shouldGetGroupName() {
        when(mFragment.getActivity()).thenReturn(mActivity);
        final String fakeGroupName = "fakegroup";
        final Bundle bundle = new Bundle();
        bundle.putString(WifiP2pSettings.SAVE_SELECTED_GROUP, fakeGroupName);

        mFragment.onActivityCreated(bundle);

        assertThat(mFragment.mSelectedGroupName).isEqualTo(fakeGroupName);
        assertThat(mFragment.mSavedDeviceName).isNull();
    }
}
