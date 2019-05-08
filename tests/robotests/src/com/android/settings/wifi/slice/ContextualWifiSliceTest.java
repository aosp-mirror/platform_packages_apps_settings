/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.slice;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SlicesFeatureProviderImpl;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowConnectivityManager.class)
public class ContextualWifiSliceTest {

    private Context mContext;
    private ContentResolver mResolver;
    private WifiManager mWifiManager;
    private ContextualWifiSlice mWifiSlice;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mResolver = mock(ContentResolver.class);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFeatureFactory.slicesFeatureProvider = new SlicesFeatureProviderImpl();
        mFeatureFactory.slicesFeatureProvider.newUiSession();
        doReturn(mResolver).when(mContext).getContentResolver();
        mWifiManager = mContext.getSystemService(WifiManager.class);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mWifiManager.setWifiEnabled(true);

        mWifiSlice = new ContextualWifiSlice(mContext);
        mWifiSlice.sPreviouslyDisplayed = false;
    }

    @Test
    public void getWifiSlice_hasActiveConnection_shouldReturnNull() {
        mWifiSlice.sPreviouslyDisplayed = false;
        connectToWifi(makeValidatedNetworkCapabilities());

        final Slice wifiSlice = mWifiSlice.getSlice();

        assertThat(wifiSlice).isNull();
    }

    @Test
    public void getWifiSlice_newSession_hasActiveConnection_shouldReturnNull() {
        // Session: use a non-active value
        // previous displayed: yes
        mWifiSlice.sPreviouslyDisplayed = true;
        mWifiSlice.sActiveUiSession = ~mFeatureFactory.slicesFeatureProvider.getUiSessionToken();
        connectToWifi(makeValidatedNetworkCapabilities());

        final Slice wifiSlice = mWifiSlice.getSlice();

        assertThat(wifiSlice).isNull();
    }

    @Test
    public void getWifiSlice_previousDisplayed_hasActiveConnection_shouldHaveTitleAndToggle() {
        mWifiSlice.sActiveUiSession = mFeatureFactory.slicesFeatureProvider.getUiSessionToken();
        mWifiSlice.sPreviouslyDisplayed = true;
        connectToWifi(makeValidatedNetworkCapabilities());

        final Slice wifiSlice = mWifiSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, wifiSlice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.wifi_settings));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedToggleIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_wireless);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedToggleIcon.toString());
    }

    @Test
    public void getWifiSlice_isCaptivePortal_shouldHaveTitleAndToggle() {
        mWifiSlice.sPreviouslyDisplayed = false;
        connectToWifi(WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        final Slice wifiSlice = mWifiSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, wifiSlice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.wifi_settings));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedToggleIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_wireless);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedToggleIcon.toString());
    }

    @Test
    public void getWifiSlice_contextualWifiSlice_shouldReturnContextualWifiSliceUri() {
        mWifiSlice.sActiveUiSession = mFeatureFactory.slicesFeatureProvider.getUiSessionToken();
        mWifiSlice.sPreviouslyDisplayed = true;

        final Slice wifiSlice = mWifiSlice.getSlice();

        assertThat(wifiSlice.getUri()).isEqualTo(CustomSliceRegistry.CONTEXTUAL_WIFI_SLICE_URI);
    }

    private void connectToWifi(NetworkCapabilities nc) {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "123";
        mWifiManager.connect(config, null /* listener */);
        ShadowConnectivityManager.getShadow().setNetworkCapabilities(
                mWifiManager.getCurrentNetwork(), nc);
    }

    private NetworkCapabilities makeValidatedNetworkCapabilities() {
        final NetworkCapabilities nc = new NetworkCapabilities();
        nc.clearAll();
        nc.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        nc.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        return nc;
    }
}
