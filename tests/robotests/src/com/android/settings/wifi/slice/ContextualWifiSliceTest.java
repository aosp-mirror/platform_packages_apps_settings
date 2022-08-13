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

import static com.android.settings.wifi.slice.ContextualWifiSlice.COLLAPSED_ROW_COUNT;
import static com.android.settings.wifi.slice.WifiSlice.DEFAULT_EXPANDED_ROW_COUNT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
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
import com.android.settings.testutils.shadow.ShadowWifiSlice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBinder;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowConnectivityManager.class, ShadowWifiSlice.class})
public class ContextualWifiSliceTest {
    private static final String SSID = "123";

    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private WifiInfo mWifiInfo;
    @Mock
    private Network mNetwork;

    private Context mContext;
    private ContentResolver mResolver;
    private ContextualWifiSlice mWifiSlice;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mResolver = mock(ContentResolver.class);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFeatureFactory.slicesFeatureProvider = new SlicesFeatureProviderImpl();
        mFeatureFactory.slicesFeatureProvider.newUiSession();
        doReturn(mResolver).when(mContext).getContentResolver();
        doReturn(mWifiManager).when(mContext).getSystemService(WifiManager.class);
        doReturn(true).when(mWifiManager).isWifiEnabled();
        doReturn(WifiManager.WIFI_STATE_ENABLED).when(mWifiManager).getWifiState();
        doReturn(mWifiInfo).when(mWifiManager).getConnectionInfo();
        doReturn(SSID).when(mWifiInfo).getSSID();
        doReturn(mNetwork).when(mWifiManager).getCurrentNetwork();
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        final String siPackageName =
                mContext.getString(R.string.config_settingsintelligence_package_name);
        ShadowBinder.setCallingUid(1);
        when(mPackageManager.getPackagesForUid(1)).thenReturn(new String[]{siPackageName});
        when(mPackageManager.getNameForUid(1)).thenReturn(siPackageName);
        ShadowWifiSlice.setWifiPermissible(true);
        mWifiSlice = new ContextualWifiSlice(mContext);
    }

    @Test
    public void getWifiSlice_newSession_hasActiveConnection_shouldCollapseSlice() {
        mWifiSlice.sActiveUiSession = ~mFeatureFactory.slicesFeatureProvider.getUiSessionToken();
        connectToWifi(makeValidatedNetworkCapabilities());

        final Slice wifiSlice = mWifiSlice.getSlice();

        assertWifiHeader(wifiSlice);
        assertThat(ContextualWifiSlice.getApRowCount()).isEqualTo(COLLAPSED_ROW_COUNT);
    }

    @Test
    public void getWifiSlice_newSession_noConnection_shouldExpandSlice() {
        mWifiSlice.sActiveUiSession = ~mFeatureFactory.slicesFeatureProvider.getUiSessionToken();

        final Slice wifiSlice = mWifiSlice.getSlice();

        assertWifiHeader(wifiSlice);
        assertThat(ContextualWifiSlice.getApRowCount()).isEqualTo(DEFAULT_EXPANDED_ROW_COUNT);
    }

    @Test
    public void getWifiSlice_previousExpanded_hasActiveConnection_shouldExpandSlice() {
        mWifiSlice.sActiveUiSession = mFeatureFactory.slicesFeatureProvider.getUiSessionToken();
        mWifiSlice.sApRowCollapsed = false;
        connectToWifi(makeValidatedNetworkCapabilities());

        final Slice wifiSlice = mWifiSlice.getSlice();

        assertWifiHeader(wifiSlice);
        assertThat(ContextualWifiSlice.getApRowCount()).isEqualTo(DEFAULT_EXPANDED_ROW_COUNT);
    }

    @Test
    public void getWifiSlice_previousCollapsed_connectionLoss_shouldCollapseSlice() {
        mWifiSlice.sActiveUiSession = mFeatureFactory.slicesFeatureProvider.getUiSessionToken();
        mWifiSlice.sApRowCollapsed = true;
        connectToWifi(makeValidatedNetworkCapabilities());

        doReturn(null).when(mWifiManager).getCurrentNetwork();
        final Slice wifiSlice = mWifiSlice.getSlice();

        assertWifiHeader(wifiSlice);
        assertThat(ContextualWifiSlice.getApRowCount()).isEqualTo(COLLAPSED_ROW_COUNT);
    }

    @Test
    public void getWifiSlice_contextualWifiSlice_shouldReturnContextualWifiSliceUri() {
        mWifiSlice.sActiveUiSession = mFeatureFactory.slicesFeatureProvider.getUiSessionToken();

        final Slice wifiSlice = mWifiSlice.getSlice();

        assertThat(wifiSlice.getUri()).isEqualTo(CustomSliceRegistry.CONTEXTUAL_WIFI_SLICE_URI);
    }

    private void connectToWifi(NetworkCapabilities nc) {
        ShadowConnectivityManager.getShadow().setNetworkCapabilities(mNetwork, nc);
    }

    private NetworkCapabilities makeValidatedNetworkCapabilities() {
        final NetworkCapabilities nc = NetworkCapabilities.Builder.withoutDefaultCapabilities()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();
        return nc;
    }

    private void assertWifiHeader(Slice slice) {
        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getString(R.string.wifi_settings));

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedToggleIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_wireless);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedToggleIcon.toString());

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);
    }
}
