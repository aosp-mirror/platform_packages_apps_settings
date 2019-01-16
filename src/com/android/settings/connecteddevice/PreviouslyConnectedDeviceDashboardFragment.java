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
import android.content.Context;
import android.content.res.Resources;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment contains previously connected device
 */
@SearchIndexable(forTarget = SearchIndexable.MOBILE)
public class PreviouslyConnectedDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "PreConnectedDeviceFrag";
    static final String KEY_PREVIOUSLY_CONNECTED_DEVICES = "saved_device_list";

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
    public void onAttach(Context context) {
        super.onAttach(context);
        use(SavedDeviceGroupController.class).init(this);
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(
                Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            // Add fragment title
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.key = KEY_PREVIOUSLY_CONNECTED_DEVICES;
            data.title = res.getString(
                    R.string.connected_device_previously_connected_title);
            data.screenTitle = res.getString(
                    R.string.connected_device_previously_connected_title);
            result.add(data);
            return result;
        }
    };
}