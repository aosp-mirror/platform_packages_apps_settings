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

import static com.android.settings.wifi.WifiSlice.DEFAULT_EXPANDED_ROW_COUNT;

import static com.google.common.truth.Truth.assertThat;

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
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.SliceTester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class WifiSliceTest {

    private Context mContext;

    private WifiSlice mWifiSlice;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mWifiSlice = new WifiSlice(mContext);
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
    public void getWifiSlice_noAp_shouldReturnPlaceholder() {
        final Slice wifiSlice = mWifiSlice.getSlice();

        int rows = SliceQuery.findAll(wifiSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();
        // All AP rows + title row
        assertThat(rows).isEqualTo(DEFAULT_EXPANDED_ROW_COUNT + 1);
    }

    @Test
    public void handleUriChange_updatesWifi() {
        final Intent intent = mWifiSlice.getIntent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, true);
        final WifiManager wifiManager = mContext.getSystemService(WifiManager.class);

        mWifiSlice.onNotifyChange(intent);

        assertThat(wifiManager.getWifiState()).isEqualTo(WifiManager.WIFI_STATE_ENABLED);
    }
}
