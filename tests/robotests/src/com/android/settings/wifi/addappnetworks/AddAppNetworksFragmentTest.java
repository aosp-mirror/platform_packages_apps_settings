/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.addappnetworks;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.widget.TextView;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AddAppNetworksFragmentTest {
    private static final String FAKE_APP_NAME = "fake_app_name";
    private static final String FAKE_SSID = "fake_ssid";
    private AddAppNetworksFragment mAddAppNetworksFragment;
    private Context mContext;

    @Before
    public void setUp() {
        mAddAppNetworksFragment = spy(new AddAppNetworksFragment());
    }

    @Test
    public void callingPackageName_onCreateView_shouldBeCorrect() {
        setUpOneNetworkBundle();
        setupFragment();

        assertThat(mAddAppNetworksFragment.mCallingPackageName).isEqualTo(FAKE_APP_NAME);
    }

    @Test
    public void launchFragment_shouldShowSaveButton() {
        setUpOneNetworkBundle();
        setupFragment();

        assertThat(mAddAppNetworksFragment.mSaveButton).isNotNull();
    }

    @Test
    public void launchFragment_shouldShowCancelButton() {
        setUpOneNetworkBundle();
        setupFragment();

        assertThat(mAddAppNetworksFragment.mCancelButton).isNotNull();
    }

    @Test
    public void requestOneNetwork_shouldShowCorrectSSID() {
        setUpOneNetworkBundle();
        setupFragment();
        TextView ssidView = (TextView) mAddAppNetworksFragment.mLayoutView.findViewById(
                R.id.single_ssid);

        assertThat(ssidView.getText()).isEqualTo(FAKE_SSID);
    }

    @Test
    public void withNoExtra_requestNetwork_shouldFinished() {
        setUpNoNetworkBundle();
        setupFragment();

        assertThat(mAddAppNetworksFragment.mActivity.isFinishing()).isTrue();
    }

    private void setUpOneNetworkBundle() {
        // Setup one network.
        List<WifiConfiguration> wifiConfigurationList = new ArrayList<>();
        wifiConfigurationList.add(
                generateWifiConfig(FAKE_SSID, WifiConfiguration.KeyMgmt.WPA_PSK, "\"1234567890\""));
        // Set up bundle.
        final Bundle bundle = new Bundle();
        bundle.putString(AddAppNetworksActivity.KEY_CALLING_PACKAGE_NAME, FAKE_APP_NAME);
        bundle.putParcelableArrayList(Settings.EXTRA_WIFI_CONFIGURATION_LIST,
                (ArrayList<? extends Parcelable>) wifiConfigurationList);
        doReturn(bundle).when(mAddAppNetworksFragment).getArguments();
    }

    private void setUpNoNetworkBundle() {
        // Set up bundle.
        final Bundle bundle = new Bundle();
        bundle.putString(AddAppNetworksActivity.KEY_CALLING_PACKAGE_NAME, FAKE_APP_NAME);
        bundle.putParcelableArrayList(Settings.EXTRA_WIFI_CONFIGURATION_LIST, null);
        doReturn(bundle).when(mAddAppNetworksFragment).getArguments();
    }

    private void setupFragment() {
        FragmentController.setupFragment(mAddAppNetworksFragment);
    }

    private static WifiConfiguration generateWifiConfig(String ssid, int securityType,
            String password) {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = ssid;
        config.allowedKeyManagement.set(securityType);

        if (password != null) {
            config.preSharedKey = password;
        }
        return config;
    }

}
