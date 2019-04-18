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

import android.app.Activity;
import android.app.WallpaperManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.display.WallpaperPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.search.SearchIndexable;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class WallpaperSuggestionActivity extends Activity implements Indexable {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PackageManager pm = getPackageManager();
        final Intent intent = new Intent()
                .setComponent(new WallpaperPreferenceController(this, "dummy key")
                        .getComponentName())
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // passing the necessary extra to next page
        WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);

        if (pm.resolveActivity(intent, 0) != null) {
            startActivity(intent);
        } else {
            startFallbackSuggestion();
        }

        finish();
    }

    @VisibleForTesting
    void startFallbackSuggestion() {
        // fall back to default wallpaper picker
        new SubSettingLauncher(this)
                .setDestination(WallpaperTypeSettings.class.getName())
                .setTitleRes(R.string.wallpaper_suggestion_title)
                .setSourceMetricsCategory(SettingsEnums.DASHBOARD_SUMMARY)
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                .launch();
    }

    @VisibleForTesting
    public static boolean isSuggestionComplete(Context context) {
        if (!isWallpaperServiceEnabled(context)) {
            return true;
        }
        final WallpaperManager manager = (WallpaperManager) context.getSystemService(
                WALLPAPER_SERVICE);
        return manager.getWallpaperId(WallpaperManager.FLAG_SYSTEM) > 0;
    }

    private static boolean isWallpaperServiceEnabled(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_enableWallpaperService);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                private static final String SUPPORT_SEARCH_INDEX_KEY = "wallpaper_type";

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<>();
                    WallpaperPreferenceController controller =
                            new WallpaperPreferenceController(context, "dummy key");
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = controller.getTitle();
                    data.screenTitle = data.title;
                    ComponentName component = controller.getComponentName();
                    data.intentTargetPackage = component.getPackageName();
                    data.intentTargetClass = component.getClassName();
                    data.intentAction = Intent.ACTION_MAIN;
                    data.key = SUPPORT_SEARCH_INDEX_KEY;
                    data.keywords = controller.getKeywords();
                    result.add(data);
                    return result;
                }
            };
}
