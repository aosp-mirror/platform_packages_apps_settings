/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.regionalpreferences;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Provides options of numbering system to each language. */
public class NumberingPreferencesFragment  extends DashboardFragment {
    /** Initializes variables. */
    @VisibleForTesting
    String initTitle() {
        String option = getArguments().getString(
                RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE, "");
        if (option.isEmpty()) {
            Log.d(getLogTag(), "Option is empty.");
            return "";
        }
        Log.i(getLogTag(), "[NumberingPreferencesFragment] option is " + option);

        if (option.equals(NumberingSystemItemController.ARG_VALUE_LANGUAGE_SELECT)) {
            return getContext().getString(R.string.numbers_preferences_title);
        } else if (option.equals(NumberingSystemItemController.ARG_VALUE_NUMBERING_SYSTEM_SELECT)) {
            String selectedLanguage = getArguments().getString(
                    NumberingSystemItemController.KEY_SELECTED_LANGUAGE, "");
            if (selectedLanguage.isEmpty()) {
                Log.w(getLogTag(), "No selected language.");
                return "";
            }
            return Locale.forLanguageTag(selectedLanguage)
                    .stripExtensions()
                    .getDisplayName(Locale.forLanguageTag(selectedLanguage));
        }
        Log.w(getLogTag(), "Incorrect option : " + option);
        return "";
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        String title = initTitle();
        if (initTitle().isEmpty()) {
            finish();
        } else {
            getActivity().setTitle(title);
        }
    }

    /**
     * Get a list of {@link AbstractPreferenceController} for this fragment.
     */
    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        NumberingSystemItemController controller =
                new NumberingSystemItemController(context, getArguments());
        controller.setParentFragment(this);
        List<AbstractPreferenceController> listControllers = new ArrayList<>();
        listControllers.add(controller);
        return listControllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.regional_preference_numbering_system_page;
    }

    /**
     * Get the tag string for logging.
     */
    @Override
    protected String getLogTag() {
        return NumberingPreferencesFragment.class.getSimpleName();
    }

    @Override
    public int getMetricsCategory() {
        String option = getArguments().getString(
                RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE, "");
        if (option.equals(NumberingSystemItemController.ARG_VALUE_LANGUAGE_SELECT)) {
            return SettingsEnums.NUMBERING_SYSTEM_LANGUAGE_SELECTION_PREFERENCE;
        } else {
            return SettingsEnums.NUMBERING_SYSTEM_NUMBER_FORMAT_SELECTION_PREFERENCE;
        }
    }
}
