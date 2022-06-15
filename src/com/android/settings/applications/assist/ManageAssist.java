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

package com.android.settings.applications.assist;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.gestures.AssistGestureSettingsPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen to manage everything about assist.
 */
@SearchIndexable
public class ManageAssist extends DashboardFragment {

    private static final String TAG = "ManageAssist";
    private static final String KEY_ASSIST = "gesture_assist_application";

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.manage_assist;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APPLICATIONS_MANAGE_ASSIST;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(AssistGestureSettingsPreferenceController.class).setAssistOnly(true);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new DefaultAssistPreferenceController(context, "default_assist",
                true /* showSetting */));
        controllers.add(new AssistContextPreferenceController(context, lifecycle));
        controllers.add(new AssistScreenshotPreferenceController(context, lifecycle));
        controllers.add(new AssistFlashScreenPreferenceController(context, lifecycle));
        controllers.add(new DefaultVoiceInputPreferenceController(context, lifecycle));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.manage_assist) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* lifecycle */);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(KEY_ASSIST);
                    return keys;
                }
            };
}
