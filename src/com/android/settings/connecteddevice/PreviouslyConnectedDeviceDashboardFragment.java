/*
 * Copyright 2018 The Android Open Source Project
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
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * This fragment contains previously connected device
 */
@SearchIndexable(forTarget = SearchIndexable.MOBILE)
public class PreviouslyConnectedDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "PreConnectedDeviceFrag";
    static final String KEY_PREVIOUSLY_CONNECTED_DEVICES = "saved_device_list";

    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;

    @Override
    public int getHelpResource() {
        return R.string.help_url_previously_connected_devices;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.previously_connected_devices;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PREVIOUSLY_CONNECTED_DEVICES;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(SavedDeviceGroupController.class).init(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        enableBluetoothIfNecessary();
    }

    @VisibleForTesting
    void enableBluetoothIfNecessary() {
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.previously_connected_devices);
}