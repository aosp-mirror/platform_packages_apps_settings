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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
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
public class ContextualWifiSliceTest {

    private Context mContext;
    private ContentResolver mResolver;
    private WifiManager mWifiManager;
    private ContextualWifiSlice mWifiSlice;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mResolver = mock(ContentResolver.class);
        doReturn(mResolver).when(mContext).getContentResolver();
        mWifiManager = mContext.getSystemService(WifiManager.class);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mWifiManager.setWifiEnabled(true);

        mWifiSlice = new ContextualWifiSlice(mContext);
    }

    @Test
    public void getWifiSlice_hasActiveConnection_shouldReturnNull() {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "123";
        mWifiManager.connect(config, null /* listener */);

        final Slice wifiSlice = mWifiSlice.getSlice();

        assertThat(wifiSlice).isNull();
    }

    @Test
    public void getWifiSlice_previousDisplayed_hasActiveConnection_shouldHaveTitleAndToggle() {
        mWifiSlice.mPreviouslyDisplayed = true;
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "123";
        mWifiManager.connect(config, null /* listener */);

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
}
