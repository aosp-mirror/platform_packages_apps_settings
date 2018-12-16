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
 *
 */

package com.android.settings.wifi;

import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import static com.android.settings.slices.CustomSliceRegistry.WIFI_SLICE_URI;
import static com.android.settings.wifi.WifiSlice.DEFAULT_EXPANDED_ROW_COUNT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.SliceTester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WifiSliceTest {

    private Context mContext;
    private ContentResolver mResolver;
    private WifiManager mWifiManager;
    private WifiSlice mWifiSlice;
    private WifiSlice.WifiScanWorker mWifiScanWorker;

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
        mWifiScanWorker = new WifiSlice.WifiScanWorker(mContext, WIFI_SLICE_URI);
    }

    @Test
    public void getWifiSlice_shouldHaveTitleAndToggle() {
        final Slice wifiSlice = mWifiSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, wifiSlice);

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedToggleIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_wireless);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedToggleIcon.toString());

        final List<SliceItem> sliceItems = wifiSlice.getItems();
        SliceTester.assertTitle(sliceItems, mContext.getString(R.string.wifi_settings));
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
        SliceTester.assertTitle(sliceItems, mContext.getString(R.string.wifi_empty_list_wifi_on));
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
}
