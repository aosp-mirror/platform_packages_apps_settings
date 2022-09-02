/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings;

import android.app.Application;

import com.android.settings.activityembedding.ActivityEmbeddingRulesController;
import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settingslib.applications.AppIconCacheManager;

import java.lang.ref.WeakReference;

/** Settings application which sets up activity embedding rules for the large screen device. */
public class SettingsApplication extends Application {

    private WeakReference<SettingsHomepageActivity> mHomeActivity = new WeakReference<>(null);

    @Override
    public void onCreate() {
        super.onCreate();

        final ActivityEmbeddingRulesController controller =
                new ActivityEmbeddingRulesController(this);
        controller.initRules();
    }

    public void setHomeActivity(SettingsHomepageActivity homeActivity) {
        mHomeActivity = new WeakReference<>(homeActivity);
    }

    public SettingsHomepageActivity getHomeActivity() {
        return mHomeActivity.get();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        AppIconCacheManager.getInstance().release();
    }
}
