/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.Uri;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.slices.SlicePreferenceController;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ConnectedDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "ConnectedDeviceFrag";
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final String SLICE_ACTION = "com.android.settings.SEARCH_RESULT_TRAMPOLINE";

    @VisibleForTesting
    static final String KEY_CONNECTED_DEVICES = "connected_device_list";
    @VisibleForTesting
    static final String KEY_AVAILABLE_DEVICES = "available_device_list";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_connected_devices;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.connected_devices;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final boolean nearbyEnabled = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_NEAR_BY_SUGGESTION_ENABLED, true);
        String callingAppPackageName = ((SettingsActivity) getActivity())
                .getInitialCallingPackage();
        String action = getIntent() != null ? getIntent().getAction() : "";
        if (DEBUG) {
            Log.d(TAG, "onAttach() calling package name is : " + callingAppPackageName
                    + ", action : " + action);
        }
        use(AvailableMediaDeviceGroupController.class).init(this);
        use(ConnectedDeviceGroupController.class).init(this);
        use(PreviouslyConnectedDevicePreferenceController.class).init(this);
        use(SlicePreferenceController.class).setSliceUri(nearbyEnabled
                ? Uri.parse(getString(R.string.config_nearby_devices_slice_uri))
                : null);
        use(DiscoverableFooterPreferenceController.class)
                .setAlwaysDiscoverable(isAlwaysDiscoverable(callingAppPackageName, action));
    }

    @VisibleForTesting
    boolean isAlwaysDiscoverable(String callingAppPackageName, String action) {
        return TextUtils.equals(SLICE_ACTION, action) ? false
                : TextUtils.equals(SETTINGS_PACKAGE_NAME, callingAppPackageName)
                || TextUtils.equals(SYSTEMUI_PACKAGE_NAME, callingAppPackageName);
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.connected_devices);
}
