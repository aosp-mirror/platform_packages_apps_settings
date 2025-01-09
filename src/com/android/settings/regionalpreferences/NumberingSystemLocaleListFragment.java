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

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.annotation.NonNull;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Provides locale list for numbering system settings. */
public class NumberingSystemLocaleListFragment extends DashboardFragment {

    @Override
    public void onCreate(@NonNull Bundle icicle) {
        super.onCreate(icicle);

        if (isEmptyNumberingSystemLocale()) {
            getActivity().setResult(Activity.RESULT_CANCELED);
            finish();
        }

        getActivity().setTitle(R.string.numbers_preferences_title);
        getActivity().setResult(Activity.RESULT_OK);
    }

    /**
     * Get a list of {@link AbstractPreferenceController} for this fragment.
     */
    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        NumberingSystemItemController controller =
                new NumberingSystemItemController(context, getExtraData());
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
        return NumberingSystemLocaleListFragment.class.getSimpleName();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NUMBERING_SYSTEM_LANGUAGE_SELECTION_PREFERENCE;
    }

    private static boolean isEmptyNumberingSystemLocale() {
        LocaleList localeList = LocaleList.getDefault();
        Set<Locale> localesHasNumberingSystems = new HashSet<>();
        for (int i = 0; i < localeList.size(); i++) {
            Locale locale = localeList.get(i);
            LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(locale);
            if (localeInfo.hasNumberingSystems()) {
                localesHasNumberingSystems.add(locale);
            }
        }
        return localesHasNumberingSystems.isEmpty();
    }

    private static Bundle getExtraData() {
        Bundle extra = new Bundle();
        extra.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                NumberingSystemItemController.ARG_VALUE_LANGUAGE_SELECT);
        return extra;
    }
}
