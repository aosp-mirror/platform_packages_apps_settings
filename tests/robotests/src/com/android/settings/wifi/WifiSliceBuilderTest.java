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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.SliceTester;

import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import androidx.core.graphics.drawable.IconCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

@RunWith(SettingsRobolectricTestRunner.class)
public class WifiSliceBuilderTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);

        // Prevent crash in SliceMetadata.
        Resources resources = spy(mContext.getResources());
        doReturn(60).when(resources).getDimensionPixelSize(anyInt());
        doReturn(resources).when(mContext).getResources();

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void getWifiSlice_correctSliceContent() {
        final Slice wifiSlice = WifiSliceBuilder.getSlice(mContext);
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
    public void handleUriChange_updatesWifi() {
        final Intent intent = new Intent(WifiSliceBuilder.ACTION_WIFI_SLICE_CHANGED);
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, true);
        final WifiManager wifiManager = mContext.getSystemService(WifiManager.class);

        WifiSliceBuilder.handleUriChange(mContext, intent);

        assertThat(wifiManager.getWifiState()).isEqualTo(WifiManager.WIFI_STATE_ENABLED);
    }
}
