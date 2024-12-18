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

package com.android.settings.language;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class LanguageSettings extends DashboardFragment {

    private static final String KEY_SPEECH_CATEGORY = "speech_category";
    private static final String KEY_ON_DEVICE_RECOGNITION = "on_device_recognition_settings";
    private static final String KEY_TEXT_TO_SPEECH = "tts_settings_summary";

    private static final String TAG = "LanguageSettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_LANGUAGES_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hack to update action bar title. It's necessary to refresh title because this page user
        // can change locale from here and fragment won't relaunch. Once language changes, title
        // must display in the new language.
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.setTitle(R.string.languages_settings);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.language_settings;
    }

    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            @NonNull Context context, @Nullable Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        final DefaultVoiceInputPreferenceController defaultVoiceInputPreferenceController =
                new DefaultVoiceInputPreferenceController(context, lifecycle);
        final TtsPreferenceController ttsPreferenceController =
                new TtsPreferenceController(context, KEY_TEXT_TO_SPEECH);
        final OnDeviceRecognitionPreferenceController onDeviceRecognitionPreferenceController =
                new OnDeviceRecognitionPreferenceController(context, KEY_ON_DEVICE_RECOGNITION);

        controllers.add(defaultVoiceInputPreferenceController);
        controllers.add(ttsPreferenceController);
        List<AbstractPreferenceController> speechCategoryChildren = new ArrayList<>(
                List.of(defaultVoiceInputPreferenceController, ttsPreferenceController));

        if (onDeviceRecognitionPreferenceController.isAvailable()) {
            controllers.add(onDeviceRecognitionPreferenceController);
            speechCategoryChildren.add(onDeviceRecognitionPreferenceController);
        }

        controllers.add(new PreferenceCategoryController(context, KEY_SPEECH_CATEGORY)
                .setChildren(speechCategoryChildren));

        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.language_settings) {
                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null);
                }
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return true;
                }
            };
}