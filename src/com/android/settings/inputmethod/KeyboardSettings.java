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

package com.android.settings.inputmethod;

import static android.app.admin.DevicePolicyResources.Strings.Settings.PERSONAL_DICTIONARY_FOR_WORK;
import static android.app.admin.DevicePolicyResources.Strings.Settings.SPELL_CHECKER_FOR_WORK;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_KEYBOARDS_AND_TOOLS;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.util.FeatureFlagUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.language.DefaultVoiceInputPreferenceController;
import com.android.settings.language.PointerSpeedController;
import com.android.settings.language.TtsPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class KeyboardSettings extends DashboardFragment {

    private static final String TAG = "KeyboardSettings";

    private static final String KEY_KEYBOARDS_CATEGORY = "keyboards_category";
    private static final String KEY_SPEECH_CATEGORY = "speech_category";
    private static final String KEY_TEXT_TO_SPEECH = "tts_settings_summary";
    private static final String KEY_POINTER_CATEGORY = "pointer_category";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_KEYBOARDS_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        replaceEnterpriseStringTitle("language_and_input_for_work_category",
                WORK_PROFILE_KEYBOARDS_AND_TOOLS,
                R.string.language_and_input_for_work_category_title);
        replaceEnterpriseStringTitle("spellcheckers_settings_for_work_pref",
                SPELL_CHECKER_FOR_WORK,
                R.string.spellcheckers_settings_for_work_title);
        replaceEnterpriseStringTitle("user_dictionary_settings_for_work_pref",
                PERSONAL_DICTIONARY_FOR_WORK,
                R.string.user_dict_settings_for_work_title);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.keyboard_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            @NonNull Context context, @Nullable Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        // Input
        final VirtualKeyboardPreferenceController virtualKeyboardPreferenceController =
                new VirtualKeyboardPreferenceController(context);
        final PhysicalKeyboardPreferenceController physicalKeyboardPreferenceController =
                new PhysicalKeyboardPreferenceController(context, lifecycle);
        controllers.add(virtualKeyboardPreferenceController);
        controllers.add(physicalKeyboardPreferenceController);
        controllers.add(new PreferenceCategoryController(context,
                KEY_KEYBOARDS_CATEGORY).setChildren(
                Arrays.asList(virtualKeyboardPreferenceController,
                        physicalKeyboardPreferenceController)));

        // Speech
        final DefaultVoiceInputPreferenceController defaultVoiceInputPreferenceController =
                new DefaultVoiceInputPreferenceController(context, lifecycle);
        final TtsPreferenceController ttsPreferenceController =
                new TtsPreferenceController(context, KEY_TEXT_TO_SPEECH);
        controllers.add(defaultVoiceInputPreferenceController);
        controllers.add(ttsPreferenceController);
        controllers.add(new PreferenceCategoryController(context,
                KEY_SPEECH_CATEGORY).setChildren(
                Arrays.asList(defaultVoiceInputPreferenceController, ttsPreferenceController)));

        // Pointer
        final PointerSpeedController pointerController = new PointerSpeedController(context);
        controllers.add(pointerController);
        controllers.add(new PreferenceCategoryController(context,
                KEY_POINTER_CATEGORY).setChildren(Arrays.asList(pointerController)));

        // Input Assistance
        controllers.add(new SpellCheckerPreferenceController(context));

        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.keyboard_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return FeatureFlagUtils
                            .isEnabled(context, FeatureFlagUtils.SETTINGS_NEW_KEYBOARD_UI);
                }
            };
}