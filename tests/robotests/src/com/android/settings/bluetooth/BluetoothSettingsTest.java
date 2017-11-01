/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothSettingsTest {
    private static final String FOOTAGE_MAC_STRING = "Bluetooth mac: xxxx";

    @Mock
    private UserManager mUserManager;
    @Mock
    private Resources mResource;
    @Mock
    private LocalBluetoothAdapter mLocalAdapter;
    @Mock
    private Activity mActivity;
    @Mock
    private PreferenceGroup mPairedDevicesCategory;
    @Mock
    private BluetoothPairingPreferenceController mPairingPreferenceController;
    private Context mContext;
    private BluetoothSettings mFragment;
    private FakeFeatureFactory mFeatureFactory;
    private Preference mFooterPreference;
    private TextView mEmptyMessage;
    private View mContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        mFragment = spy(new BluetoothSettings());

        doReturn(mContext).when(mFragment).getContext();
        doReturn(mResource).when(mFragment).getResources();
        doReturn(mActivity).when(mFragment).getActivity();

        mContainer = new View(mContext);
        mEmptyMessage = new TextView(mContext);
        doReturn(mContainer).when(mActivity).findViewById(android.R.id.list_container);
        doReturn(mEmptyMessage).when(mActivity).findViewById(android.R.id.empty);

        mFooterPreference = new FooterPreference(RuntimeEnvironment.application);
        mFragment.setLocalBluetoothAdapter(mLocalAdapter);
        mFragment.mPairingPrefController = mPairingPreferenceController;
    }

    @Test
    public void setTextSpan_notSpannable_shouldNotCrash() {
        final String str = "test";
        mFragment.setTextSpan(str, "hello");
    }

    @Test
    public void setUpdateMyDevicePreference_setTitleCorrectly() {
        doReturn(FOOTAGE_MAC_STRING).when(mFragment).getString(
                eq(R.string.bluetooth_footer_mac_message), any());

        mFragment.updateFooterPreference(mFooterPreference);

        assertThat(mFooterPreference.getTitle()).isEqualTo(FOOTAGE_MAC_STRING);
    }

    @Test
    public void testDisplayEmptyMessage_showEmptyMessage() {
        mFragment.displayEmptyMessage(true);

        assertThat(mContainer.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(mEmptyMessage.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testDisplayEmptyMessage_hideEmptyMessage() {
        mFragment.displayEmptyMessage(false);

        assertThat(mContainer.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mEmptyMessage.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testInitPreferencesFromPreferenceScreen() {
        doReturn(mPairedDevicesCategory).when(mFragment).findPreference(
                BluetoothSettings.KEY_PAIRED_DEVICES);
        doReturn(mFooterPreference).when(mFragment).findPreference(
                BluetoothSettings.KEY_FOOTER_PREF);

        mFragment.initPreferencesFromPreferenceScreen();

        assertThat(mFragment.mPairedDevicesCategory).isEqualTo(mPairedDevicesCategory);
        assertThat(mFragment.mFooterPreference).isEqualTo(mFooterPreference);
    }

    @Test
    public void testSearchIndexProvider_pairPageEnabled_keyNotAdded() {
        doReturn(true).when(mFeatureFactory.bluetoothFeatureProvider).isPairingPageEnabled();

        final List<String> keys = mFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                mContext);

        assertThat(keys).doesNotContain(BluetoothSettings.DATA_KEY_REFERENCE);
    }

    @Test
    public void testSearchIndexProvider_pairPageDisabled_keyAdded() {
        final List<String> keys = mFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                mContext);

        assertThat(keys).contains(BluetoothSettings.DATA_KEY_REFERENCE);
    }

}
