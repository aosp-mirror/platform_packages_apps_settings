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
package com.android.settings.connecteddevice;

import static com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment.KEY_AVAILABLE_DEVICES;
import static com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment.KEY_CONNECTED_DEVICES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerListHelper;
import com.android.settings.slices.SlicePreferenceController;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class,
        ShadowConnectivityManager.class, ShadowBluetoothAdapter.class})
public class ConnectedDeviceDashboardFragmentTest {
    private static final String KEY_NEARBY_DEVICES = "bt_nearby_slice";
    private static final String KEY_DISCOVERABLE_FOOTER = "discoverable_footer";
    private static final String KEY_SEE_ALL = "previously_connected_devices_see_all";

    @Mock
    private PackageManager mPackageManager;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                ConnectedDeviceDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(mContext, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(R.xml.connected_devices);
    }

    @Test
    public void nonIndexableKeys_existInXmlLayout() {
        final List<String> niks = ConnectedDeviceDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        assertThat(niks).containsExactly(KEY_CONNECTED_DEVICES, KEY_AVAILABLE_DEVICES,
                KEY_NEARBY_DEVICES, KEY_DISCOVERABLE_FOOTER, KEY_SEE_ALL);
    }

    @Test
    public void getPreferenceControllers_containSlicePrefController() {
        final List<BasePreferenceController> controllers =
                PreferenceControllerListHelper.getPreferenceControllersFromXml(mContext,
                        R.xml.connected_devices);

        assertThat(controllers
                .stream()
                .filter(controller -> controller instanceof SlicePreferenceController)
                .count())
                .isEqualTo(1);
    }
}
