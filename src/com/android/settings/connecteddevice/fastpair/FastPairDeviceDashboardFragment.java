/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.fastpair;

import android.app.settings.SettingsEnums;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** This fragment contains list of available FastPair device */
@SearchIndexable(forTarget = SearchIndexable.MOBILE)
public class FastPairDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "FastPairDeviceFrag";

    @Override
    public int getHelpResource() {
        return R.string.help_url_connected_devices_fast_pair_devices;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.fast_pair_devices;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FAST_PAIR_DEVICES;
    }

    /** For Search. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.fast_pair_devices);
}
