/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.wallpaper;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.display.WallpaperPreferenceController;

import com.google.android.setupcompat.util.WizardManagerHelper;

public abstract class StyleSuggestionActivityBase extends Activity {

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

        addExtras(intent);

        if (pm.resolveActivity(intent, 0) != null) {
            startActivity(intent);
        } else {
            startFallbackSuggestion();
        }

        finish();
    }

    /**
     * Add any extras to the intent before launching the wallpaper activity
     * @param intent
     */
    protected void addExtras(Intent intent) { }

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

    protected static boolean isWallpaperServiceEnabled(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_enableWallpaperService);
    }
}
