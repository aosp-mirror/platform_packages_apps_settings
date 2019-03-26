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

import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import static com.android.settings.slices.CustomSliceRegistry.WIFI_SLICE_URI;
import static com.android.settings.wifi.slice.WifiSlice.DEFAULT_EXPANDED_ROW_COUNT;
import static com.android.settings.wifi.slice.WifiSlice.WifiScanWorker;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.testutils.SliceTester;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WifiSliceTest {

    private static final String AP1_NAME = "ap1";
    private static final String AP2_NAME = "ap2";

    private Context mContext;
    private ContentResolver mResolver;
    private WifiManager mWifiManager;
    private WifiSlice mWifiSlice;
    private WifiScanWorker mWifiScanWorker;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mResolver = mock(ContentResolver.class);
        doReturn(mResolver).when(mContext).getContentResolver();
        mWifiManager = mContext.getSystemService(WifiManager.class);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mWifiManager.setWifiEnabled(true);

        mWifiSlice = new WifiSlice(mContext);
        mWifiScanWorker = new WifiScanWorker(mContext, WIFI_SLICE_URI);
    }

    @Test
    public void getWifiSlice_shouldHaveTitleAndToggle() {
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
    public void getWifiSlice_wifiOff_shouldReturnSingleRow() {
        mWifiManager.setWifiEnabled(false);

        final Slice wifiSlice = mWifiSlice.getSlice();

        final int rows = SliceQuery.findAll(wifiSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();

        // Title row
        assertThat(rows).isEqualTo(1);
    }

    @Test
    public void getWifiSlice_noAp_shouldReturnLoadingRow() {
        final Slice wifiSlice = mWifiSlice.getSlice();

        final int rows = SliceQuery.findAll(wifiSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();
        final List<SliceItem> sliceItems = wifiSlice.getItems();

        // All AP rows + title row
        assertThat(rows).isEqualTo(DEFAULT_EXPANDED_ROW_COUNT + 1);
        // Has scanning text
        SliceTester.assertAnySliceItemContainsSubtitle(sliceItems,
                mContext.getString(R.string.wifi_empty_list_wifi_on));
    }

    private AccessPoint createAccessPoint(String name, boolean active, boolean reachable) {
        final AccessPoint accessPoint = mock(AccessPoint.class);
        doReturn(name).when(accessPoint).getConfigName();
        doReturn(active).when(accessPoint).isActive();
        doReturn(reachable).when(accessPoint).isReachable();
        if (active) {
            final NetworkInfo networkInfo = mock(NetworkInfo.class);
            doReturn(networkInfo).when(accessPoint).getNetworkInfo();
            doReturn(NetworkInfo.State.CONNECTED).when(networkInfo).getState();
        }
        return accessPoint;
    }

    private void setWorkerResults(AccessPoint... accessPoints) {
        final ArrayList<AccessPoint> results = new ArrayList<>();
        for (AccessPoint ap : accessPoints) {
            results.add(ap);
        }
        final SliceBackgroundWorker worker = SliceBackgroundWorker.getInstance(mWifiSlice.getUri());
        doReturn(results).when(worker).getResults();
    }

    @Test
    @Config(shadows = ShadowSliceBackgroundWorker.class)
    @Ignore("b/129293669")
    public void getWifiSlice_noReachableAp_shouldReturnLoadingRow() {
        setWorkerResults(
                createAccessPoint(AP1_NAME, false, false),
                createAccessPoint(AP2_NAME, false, false));
        final Slice wifiSlice = mWifiSlice.getSlice();

        final List<SliceItem> sliceItems = wifiSlice.getItems();

        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP1_NAME);
        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP2_NAME);
        // Has scanning text
        SliceTester.assertAnySliceItemContainsSubtitle(sliceItems,
                mContext.getString(R.string.wifi_empty_list_wifi_on));
    }

    @Test
    @Config(shadows = ShadowSliceBackgroundWorker.class)
    @Ignore("b/129293669")
    public void getWifiSlice_oneActiveAp_shouldReturnLoadingRow() {
        setWorkerResults(createAccessPoint(AP1_NAME, true, true));
        final Slice wifiSlice = mWifiSlice.getSlice();

        final List<SliceItem> sliceItems = wifiSlice.getItems();

        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP1_NAME);
        // Has scanning text
        SliceTester.assertAnySliceItemContainsSubtitle(sliceItems,
                mContext.getString(R.string.wifi_empty_list_wifi_on));
    }

    @Test
    @Config(shadows = ShadowSliceBackgroundWorker.class)
    @Ignore("b/129293669")
    public void getWifiSlice_oneActiveApAndOneUnreachableAp_shouldReturnLoadingRow() {
        setWorkerResults(
                createAccessPoint(AP1_NAME, true, true),
                createAccessPoint(AP2_NAME, false, false));
        final Slice wifiSlice = mWifiSlice.getSlice();

        final List<SliceItem> sliceItems = wifiSlice.getItems();

        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP1_NAME);
        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP2_NAME);
        // Has scanning text
        SliceTester.assertAnySliceItemContainsSubtitle(sliceItems,
                mContext.getString(R.string.wifi_empty_list_wifi_on));
    }

    @Test
    @Config(shadows = ShadowSliceBackgroundWorker.class)
    @Ignore("b/129293669")
    public void getWifiSlice_oneReachableAp_shouldNotReturnLoadingRow() {
        setWorkerResults(createAccessPoint(AP1_NAME, false, true));
        final Slice wifiSlice = mWifiSlice.getSlice();

        final List<SliceItem> sliceItems = wifiSlice.getItems();

        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP1_NAME);
        // No scanning text
        SliceTester.assertNoSliceItemContainsSubtitle(sliceItems,
                mContext.getString(R.string.wifi_empty_list_wifi_on));
    }

    @Test
    @Config(shadows = ShadowSliceBackgroundWorker.class)
    @Ignore("b/129293669")
    public void getWifiSlice_allReachableAps_shouldNotReturnLoadingRow() {
        setWorkerResults(
                createAccessPoint(AP1_NAME, false, true),
                createAccessPoint(AP2_NAME, false, true));
        final Slice wifiSlice = mWifiSlice.getSlice();

        final List<SliceItem> sliceItems = wifiSlice.getItems();

        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP1_NAME);
        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP2_NAME);
        // No scanning text
        SliceTester.assertNoSliceItemContainsSubtitle(sliceItems,
                mContext.getString(R.string.wifi_empty_list_wifi_on));
    }

    @Test
    public void handleUriChange_updatesWifi() {
        final Intent intent = mWifiSlice.getIntent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, true);
        final WifiManager wifiManager = mContext.getSystemService(WifiManager.class);

        mWifiSlice.onNotifyChange(intent);

        assertThat(wifiManager.getWifiState()).isEqualTo(WifiManager.WIFI_STATE_ENABLED);
    }

    @Test
    public void onWifiStateChanged_shouldNotifyChange() {
        mWifiScanWorker.onWifiStateChanged(WifiManager.WIFI_STATE_DISABLED);

        verify(mResolver).notifyChange(WIFI_SLICE_URI, null);
    }

    private AccessPoint createAccessPoint(String name, State state) {
        final NetworkInfo info = mock(NetworkInfo.class);
        doReturn(state).when(info).getState();

        final Bundle savedState = new Bundle();
        savedState.putString("key_ssid", name);
        savedState.putParcelable("key_networkinfo", info);
        return new AccessPoint(mContext, savedState);
    }

    @Test
    public void SliceAccessPoint_sameState_shouldBeTheSame() {
        final AccessPoint ap1 = createAccessPoint(AP1_NAME, State.CONNECTED);
        final AccessPoint ap2 = createAccessPoint(AP1_NAME, State.CONNECTED);

        assertThat(mWifiScanWorker.areListsTheSame(Arrays.asList(ap1), Arrays.asList(ap2)))
                .isTrue();
    }

    @Test
    public void SliceAccessPoint_differentState_shouldBeDifferent() {
        final AccessPoint ap1 = createAccessPoint(AP1_NAME, State.CONNECTING);
        final AccessPoint ap2 = createAccessPoint(AP1_NAME, State.CONNECTED);

        assertThat(mWifiScanWorker.areListsTheSame(Arrays.asList(ap1), Arrays.asList(ap2)))
                .isFalse();
    }
    @Test
    public void SliceAccessPoint_differentLength_shouldBeDifferent() {
        final AccessPoint ap1 = createAccessPoint(AP1_NAME, State.CONNECTED);
        final AccessPoint ap2 = createAccessPoint(AP1_NAME, State.CONNECTED);
        final List<AccessPoint> list = new ArrayList<>();
        list.add(ap1);
        list.add(ap2);

        assertThat(mWifiScanWorker.areListsTheSame(list, Arrays.asList(ap1))).isFalse();
    }

    @Implements(SliceBackgroundWorker.class)
    public static class ShadowSliceBackgroundWorker {
        private static WifiScanWorker mWifiScanWorker = mock(WifiScanWorker.class);

        @Implementation
        public static SliceBackgroundWorker getInstance(Uri uri) {
            return mWifiScanWorker;
        }
    }
}
