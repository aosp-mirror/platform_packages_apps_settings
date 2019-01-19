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

package com.android.settings.wallpaper;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class WallpaperTypeSettings extends DashboardFragment {
    private static final String TAG = "WallpaperTypeSettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WALLPAPER_TYPE;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_wallpaper;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wallpaper_settings;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<>();

                final Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                final PackageManager pm = context.getPackageManager();
                final List<ResolveInfo> rList = pm.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);

                // Add indexable data for package that is in config_wallpaper_picker_package
                final String wallpaperPickerPackage =
                        context.getString(R.string.config_wallpaper_picker_package);
                for (ResolveInfo info : rList) {
                    if (!wallpaperPickerPackage.equals(info.activityInfo.packageName)) {
                        continue;
                    }
                    CharSequence label = info.loadLabel(pm);
                    if (label == null) {
                        label = info.activityInfo.packageName;
                    }
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = label.toString();
                    data.key = "wallpaper_type_settings";
                    data.screenTitle = context.getResources().getString(
                            R.string.wallpaper_settings_fragment_title);
                    data.intentAction = Intent.ACTION_SET_WALLPAPER;
                    data.intentTargetPackage = info.activityInfo.packageName;
                    data.intentTargetClass = info.activityInfo.name;
                    data.keywords = context.getString(R.string.keywords_wallpaper);
                    result.add(data);
                }

                return result;
            }
        };
}
