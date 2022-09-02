/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.display;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.List;

/** Fragment that shows all the available device state based auto-rotation preferences. */
@SearchIndexable
public class DeviceStateAutoRotateDetailsFragment extends DashboardFragment {

    private static final String TAG = "DeviceStateAutoRotateDetailsFragment";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DISPLAY_AUTO_ROTATE_SETTINGS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.device_state_auto_rotate_settings;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        DeviceStateAutoRotationHelper.initControllers(
                getLifecycle(),
                useAll(DeviceStateAutoRotateSettingController.class)
        );
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return DeviceStateAutoRotationHelper.createPreferenceControllers(context);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.device_state_auto_rotate_settings) {

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    return DeviceStateAutoRotationHelper.getRawDataToIndex(context, enabled);
                }
            };
}
