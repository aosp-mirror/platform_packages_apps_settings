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

import static com.android.settings.wifi.slice.WifiSlice.DEFAULT_EXPANDED_ROW_COUNT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.ListContent;
import androidx.slice.widget.SliceLiveData;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.testutils.SliceTester;
import com.android.settings.testutils.shadow.ShadowWifiSlice;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiEntry.ConnectedState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBinder;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        WifiSliceTest.ShadowSliceBackgroundWorker.class,
        ShadowWifiSlice.class})
public class WifiSliceTest {

    private static final String AP1_NAME = "ap1";
    private static final String AP2_NAME = "ap2";
    private static final String AP3_NAME = "ap3";
    private static final int USER_ID = 1;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ContentResolver mResolver;
    @Mock
    private WifiSlice.WifiRestriction mWifiRestriction;

    private WifiSlice mWifiSlice;
    private String mSIPackageName;

    @Before
    public void setUp() {
        doReturn(mResolver).when(mContext).getContentResolver();
        doReturn(mWifiManager).when(mContext).getSystemService(WifiManager.class);
        doReturn(WifiManager.WIFI_STATE_ENABLED).when(mWifiManager).getWifiState();
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mWifiRestriction.isChangeWifiStateAllowed(mContext)).thenReturn(true);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mSIPackageName = mContext.getString(R.string.config_settingsintelligence_package_name);
        ShadowBinder.setCallingUid(USER_ID);
        when(mPackageManager.getPackagesForUid(USER_ID)).thenReturn(new String[]{mSIPackageName});
        when(mPackageManager.getNameForUid(USER_ID)).thenReturn(mSIPackageName);
        ShadowWifiSlice.setWifiPermissible(true);
        mWifiSlice = new WifiSlice(mContext, mWifiRestriction);
    }

    @Test
    public void getWifiSlice_fromSIPackage_shouldHaveTitleAndToggle() {
        when(mPackageManager.getPackagesForUid(USER_ID)).thenReturn(new String[]{mSIPackageName});
        when(mPackageManager.getNameForUid(USER_ID)).thenReturn(mSIPackageName);
        ShadowWifiSlice.setWifiPermissible(false);

        final Slice wifiSlice = mWifiSlice.getSlice();

        assertThat(wifiSlice).isNotNull();

        final SliceMetadata metadata = SliceMetadata.from(mContext, wifiSlice);
        final List<SliceAction> toggles = metadata.getToggles();

        assertThat(toggles).hasSize(1);
    }

    @Test
    public void getWifiSlice_notFromSIPackageAndWithWifiPermission_shouldHaveTitleAndToggle() {
        when(mPackageManager.getPackagesForUid(USER_ID)).thenReturn(new String[]{"com.test"});
        when(mPackageManager.getNameForUid(USER_ID)).thenReturn("com.test");
        ShadowWifiSlice.setWifiPermissible(true);

        final Slice wifiSlice = mWifiSlice.getSlice();
        assertThat(wifiSlice).isNotNull();

        final SliceMetadata metadata = SliceMetadata.from(mContext, wifiSlice);
        final List<SliceAction> toggles = metadata.getToggles();

        assertThat(toggles).hasSize(1);
    }

    @Test
    public void getWifiSlice_notFromSIPackageAndWithoutWifiPermission_shouldReturnNoToggle() {
        when(mPackageManager.getPackagesForUid(USER_ID)).thenReturn(new String[]{"com.test"});
        when(mPackageManager.getNameForUid(USER_ID)).thenReturn("com.test");
        ShadowWifiSlice.setWifiPermissible(false);

        final Slice wifiSlice = mWifiSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, wifiSlice);
        final List<SliceAction> toggles = metadata.getToggles();

        assertThat(toggles).hasSize(0);

        final int rows = SliceQuery.findAll(wifiSlice, FORMAT_SLICE, HINT_LIST_ITEM,
                null /* nonHints */).size();
        // Title row
        assertThat(rows).isEqualTo(1);
    }

    @Test
    public void getWifiSlice_wifiOff_shouldReturnSingleRow() {
        doReturn(WifiManager.WIFI_STATE_DISABLED).when(mWifiManager).getWifiState();

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

    private WifiSliceItem createWifiSliceItem(String title, @ConnectedState int connectedState) {
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(wifiEntry.getTitle()).thenReturn(title);
        when(wifiEntry.getKey()).thenReturn("key");
        when(wifiEntry.getConnectedState()).thenReturn(connectedState);
        when(wifiEntry.getLevel()).thenReturn(WifiEntry.WIFI_LEVEL_MAX);
        return new WifiSliceItem(mContext, wifiEntry);
    }

    private void setWorkerResults(WifiSliceItem... wifiSliceItems) {
        final ArrayList<WifiSliceItem> results = new ArrayList<>();
        for (WifiSliceItem wifiSliceItem : wifiSliceItems) {
            results.add(wifiSliceItem);
        }
        final SliceBackgroundWorker worker = SliceBackgroundWorker.getInstance(mWifiSlice.getUri());
        doReturn(results).when(worker).getResults();
    }

    @Test
    public void getWifiSlice_oneConnectedAp_shouldReturnLoadingRow() {
        setWorkerResults(createWifiSliceItem(AP1_NAME, WifiEntry.CONNECTED_STATE_CONNECTED));

        final Slice wifiSlice = mWifiSlice.getSlice();
        final List<SliceItem> sliceItems = wifiSlice.getItems();

        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP1_NAME);
        // Has scanning text
        SliceTester.assertAnySliceItemContainsSubtitle(sliceItems,
                mContext.getString(R.string.wifi_empty_list_wifi_on));
    }

    @Test
    public void getWifiSlice_oneConnectedApAndOneDisconnectedAp_shouldReturnLoadingRow() {
        setWorkerResults(
                createWifiSliceItem(AP1_NAME, WifiEntry.CONNECTED_STATE_CONNECTED),
                createWifiSliceItem(AP2_NAME, WifiEntry.CONNECTED_STATE_DISCONNECTED));

        final Slice wifiSlice = mWifiSlice.getSlice();
        final List<SliceItem> sliceItems = wifiSlice.getItems();

        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP1_NAME);
        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP2_NAME);
        // Has scanning text
        SliceTester.assertAnySliceItemContainsSubtitle(sliceItems,
                mContext.getString(R.string.wifi_empty_list_wifi_on));
    }

    @Test
    public void getWifiSlice_oneDisconnectedAp_shouldReturnLoadingRow() {
        setWorkerResults(createWifiSliceItem(AP1_NAME, WifiEntry.CONNECTED_STATE_DISCONNECTED));

        final Slice wifiSlice = mWifiSlice.getSlice();
        final List<SliceItem> sliceItems = wifiSlice.getItems();

        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP1_NAME);
        // Has scanning text
        SliceTester.assertAnySliceItemContainsSubtitle(sliceItems,
                mContext.getString(R.string.wifi_empty_list_wifi_on));
    }

    @Test
    public void getWifiSlice_apReachExpandedCount_shouldNotReturnLoadingRow() {
        setWorkerResults(
                createWifiSliceItem(AP1_NAME, WifiEntry.CONNECTED_STATE_DISCONNECTED),
                createWifiSliceItem(AP2_NAME, WifiEntry.CONNECTED_STATE_DISCONNECTED),
                createWifiSliceItem(AP3_NAME, WifiEntry.CONNECTED_STATE_DISCONNECTED));

        final Slice wifiSlice = mWifiSlice.getSlice();
        final List<SliceItem> sliceItems = wifiSlice.getItems();

        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP1_NAME);
        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP2_NAME);
        SliceTester.assertAnySliceItemContainsTitle(sliceItems, AP3_NAME);
        // No scanning text
        SliceTester.assertNoSliceItemContainsSubtitle(sliceItems,
                mContext.getString(R.string.wifi_empty_list_wifi_on));
    }

    @Test
    public void getWifiSlice_disallowedChangeWifiState_addSubtitleAndNoToggle() {
        when(mWifiRestriction.isChangeWifiStateAllowed(mContext)).thenReturn(false);

        final Slice slice = mWifiSlice.getSlice();

        final ListContent listContent = SliceMetadata.from(mContext, slice).getListContent();
        assertThat(slice).isNotNull();
        assertThat(listContent.getHeader().getSubtitleItem()).isNotNull();
        assertThat(listContent.getSliceActions()).isNull();
    }

    @Test
    public void getWifiSlice_allowedChangeWifiState_noSubtitleAndAddToggle() {
        when(mWifiRestriction.isChangeWifiStateAllowed(mContext)).thenReturn(true);

        final Slice slice = mWifiSlice.getSlice();

        final ListContent listContent = SliceMetadata.from(mContext, slice).getListContent();
        assertThat(slice).isNotNull();
        assertThat(listContent.getHeader().getSubtitleItem()).isNull();
        assertThat(listContent.getSliceActions()).isNotNull();
    }

    @Test
    public void handleUriChange_updatesWifi() {
        final Intent intent = mWifiSlice.getIntent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, true);
        final WifiManager wifiManager = mContext.getSystemService(WifiManager.class);

        mWifiSlice.onNotifyChange(intent);

        assertThat(wifiManager.getWifiState()).isEqualTo(WifiManager.WIFI_STATE_ENABLED);
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
