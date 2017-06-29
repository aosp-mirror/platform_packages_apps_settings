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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;

public class WallpaperSuggestionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PackageManager pm = getPackageManager();
        final Intent intent = new Intent()
                .setClassName(getString(R.string.config_wallpaper_picker_package),
                        getString(R.string.config_wallpaper_picker_class))
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
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
        Utils.startWithFragment(this, WallpaperTypeSettings.class.getName(), null, null, 0,
                R.string.wallpaper_suggestion_title, null,
                MetricsProto.MetricsEvent.DASHBOARD_SUMMARY);
    }

}
