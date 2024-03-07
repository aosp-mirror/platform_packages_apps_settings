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

package com.android.settings.development.qstile;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SearchIndexable
public class DevelopmentTileConfigFragment extends DashboardFragment {
    private static final String TAG = "DevelopmentTileConfig";
    private static final String QS_TILE_PERF = "develop_qs_tile";

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.development_tile_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVELOPMENT_QS_TILE_CONFIG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.development_tile_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
                }

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    List<SearchIndexableRaw> result = new ArrayList<>();
                    // Save the query system property for getNonIndexableKeys to avoid
                    // getTitleServiceList multiple times
                    SharedPreferences sharedPref = context.getSharedPreferences(QS_TILE_PERF,
                            Context.MODE_PRIVATE);

                    List<ServiceInfo> services =
                            DevelopmentTilePreferenceController.getTileServiceList(context);
                    PackageManager pm = context.getPackageManager();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    for (ServiceInfo sInfo : services) {
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = sInfo.loadLabel(pm).toString();
                        data.key = sInfo.name;
                        result.add(data);

                        if (sInfo.metaData == null) {
                            continue;
                        }
                        String flag = sInfo.metaData.getString(
                                DevelopmentTiles.META_DATA_REQUIRES_SYSTEM_PROPERTY);
                        if (flag == null) {
                            continue;
                        }
                        editor.putString(sInfo.name, flag);
                    }
                    editor.apply();

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    SharedPreferences sharedPref = context.getSharedPreferences(QS_TILE_PERF,
                            Context.MODE_PRIVATE);
                    Map<String, ?> map = sharedPref.getAll();
                    for (Map.Entry<String, ?> entry : map.entrySet()) {
                        if (entry.getValue() == null) {
                            continue;
                        }
                        String key = entry.getKey();
                        String flag = entry.getValue().toString();

                        if (!SystemProperties.getBoolean(flag, false)) {
                            keys.add(key);
                        }
                    }

                    return keys;
                }
            };
}
