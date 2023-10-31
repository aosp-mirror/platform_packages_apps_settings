/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.print.PrintSettingPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.uwb.UwbPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This fragment contains all the advanced connection preferences(i.e, Bluetooth, NFC, USB..)
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AdvancedConnectedDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "AdvancedConnectedDeviceFrag";

    static final String KEY_BLUETOOTH = "bluetooth_settings";
    static final String KEY_UWB = "uwb_settings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONNECTION_DEVICE_ADVANCED;
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
        return R.xml.connected_devices_advanced;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        UwbPreferenceController uwbPreferenceController = use(UwbPreferenceController.class);
        // We only need the observer listen to the broadcast in the background for refreshing
        // UI if the device supports UWB.
        if (uwbPreferenceController != null && uwbPreferenceController.isUwbSupportedOnDevice()) {
            if (getSettingsLifecycle() != null) {
                getSettingsLifecycle().addObserver(uwbPreferenceController);
            }
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        final PrintSettingPreferenceController printerController =
                new PrintSettingPreferenceController(context);

        if (lifecycle != null) {
            lifecycle.addObserver(printerController);
        }
        controllers.add(printerController);

        return controllers;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.connected_devices_advanced;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildControllers(context, null /* lifecycle */);
                }
            };
}
