/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.annotation.NonNull;

import com.android.internal.app.LocaleHelper;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Provides options of numbering system to each language. */
public class NumberingSystemFormatSelectionFragment extends DashboardFragment {

    @Override
    public void onCreate(@NonNull Bundle icicle) {
        super.onCreate(icicle);
        getActivity().setTitle(initTitle());
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

    @Override
    protected String getLogTag() {
        return NumberingSystemFormatSelectionFragment.class.getSimpleName();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NUMBERING_SYSTEM_NUMBER_FORMAT_SELECTION_PREFERENCE;
    }

    private String initTitle() {
        String selectedLanguage = getArguments().getString(
                NumberingSystemItemController.KEY_SELECTED_LANGUAGE, "");
        if (selectedLanguage.isEmpty()) {
            Log.w(getLogTag(), "No selected language.");
            return "";
        }
        Locale locale = Locale.forLanguageTag(selectedLanguage);
        return LocaleHelper.getDisplayName(locale.stripExtensions(), locale, true);
    }
}
