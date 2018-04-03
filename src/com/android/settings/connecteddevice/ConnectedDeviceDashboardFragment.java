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

import android.app.Activity;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothSwitchPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.nfc.NfcPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ConnectedDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "ConnectedDeviceFrag";

    @VisibleForTesting
    static final String KEY_CONNECTED_DEVICES = "connected_device_list";
    @VisibleForTesting
    static final String KEY_SAVED_DEVICES = "saved_device_list";

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_CONNECTED_DEVICE_CATEGORY;
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
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle(), this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, DashboardFragment dashboardFragment) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ConnectedDeviceGroupController(context, dashboardFragment, lifecycle));
        controllers.add(new SavedDeviceGroupController(context, dashboardFragment, lifecycle));

        final NfcPreferenceController nfcPreferenceController =
                new NfcPreferenceController(context);
        controllers.add(nfcPreferenceController);

        final BluetoothSwitchPreferenceController bluetoothPreferenceController =
                new BluetoothSwitchPreferenceController(context);
        controllers.add(bluetoothPreferenceController);

        if (lifecycle != null) {
            lifecycle.addObserver(nfcPreferenceController);
            lifecycle.addObserver(bluetoothPreferenceController);
        }

        return controllers;
    }

    @VisibleForTesting
    static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private final NfcPreferenceController mNfcPreferenceController;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mNfcPreferenceController = new NfcPreferenceController(context);
        }


        @Override
        public void setListening(boolean listening) {
            if (listening) {
                if (mNfcPreferenceController.isAvailable()) {
                    mSummaryLoader.setSummary(this,
                            mContext.getString(R.string.connected_devices_dashboard_summary));
                } else {
                    mSummaryLoader.setSummary(this, mContext.getString(
                            R.string.connected_devices_dashboard_no_nfc_summary));
                }
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.connected_devices;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* lifecycle */,
                            null /* dashboardFragment */);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    // Disable because they show dynamic data
                    keys.add(KEY_CONNECTED_DEVICES);
                    keys.add(KEY_SAVED_DEVICES);
                    return keys;
                }
            };
}
