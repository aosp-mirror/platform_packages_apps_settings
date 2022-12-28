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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/** A fragment to include each kind of regional preferences. */
public class RegionalPreferencesFragment extends SettingsPreferenceFragment {
    private static final String TAG = RegionalPreferencesFragment.class.getSimpleName();

    static final String TYPE_DEFAULT = "default";
    static final String TYPE_TEMPERATURE = "mu";
    static final String TYPE_CALENDAR = "ca";
    static final String TYPE_FIRST_DAY_OF_WEEK = "fw";
    static final String TYPE_NUMBERING_SYSTEM = "nu";
    static final String TYPE_OF_REGIONAL_PREFERENCE = "type_of_regional_preference";

    private PreferenceScreen mPreferenceScreen;
    private String mTitle = "";
    @VisibleForTesting
    String mType = "";

    private String[] initializeUIdata(String type) {
        switch(type) {
            case TYPE_TEMPERATURE:
                mTitle = getPrefContext().getString(R.string.temperature_preferences_title);
                return getPrefContext().getResources().getStringArray(R.array.temperature_units);
            case TYPE_CALENDAR:
                mTitle = getPrefContext().getString(R.string.calendar_preferences_title);
                return getPrefContext().getResources().getStringArray(R.array.calendar_type);
            case TYPE_FIRST_DAY_OF_WEEK:
                mTitle = getPrefContext().getString(R.string.first_day_of_week_preferences_title);
                return getPrefContext().getResources().getStringArray(R.array.first_day_of_week);
            case TYPE_NUMBERING_SYSTEM:
                mTitle = getPrefContext().getString(R.string.numbers_preferences_title);
                return new String[0];
            default:
                mTitle = getPrefContext().getString(R.string.regional_preferences_title);
                return new String[0];
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        for (int i = 0; i < mPreferenceScreen.getPreferenceCount(); i++) {
            TickButtonPreference pref = (TickButtonPreference) mPreferenceScreen.getPreference(i);
            Log.i(TAG, "[onPreferenceClick] key is " + pref.getKey());
            if (pref.getKey().equals(preference.getKey())) {
                pref.setTickEnable(true);
                RegionalPreferencesDataUtils.savePreference(
                        getPrefContext(),
                        mType,
                        preference.getKey().equals(TYPE_DEFAULT) ? null : preference.getKey());
                continue;
            }
            pref.setTickEnable(false);
        }
        return true;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle bundle = getArguments();
        String type = bundle.getString(TYPE_OF_REGIONAL_PREFERENCE, "");
        if (type.isEmpty()) {
            Log.w(TAG, "There is no type name.");
            finish();
        }
        mType = type;
        addPreferencesFromResource(R.xml.regional_preference_content_page);
        mPreferenceScreen = getPreferenceScreen();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        String[] uiData = initializeUIdata(mType);

        for (String item : uiData) {
            TickButtonPreference pref = new TickButtonPreference(getPrefContext());
            if (mType.equals(TYPE_FIRST_DAY_OF_WEEK)) {
                pref.setTitle(RegionalPreferencesDataUtils.dayConverter(
                        getPrefContext(), item));
            } else if (mType.equals(TYPE_TEMPERATURE)) {
                pref.setTitle(RegionalPreferencesDataUtils.temperatureUnitsConverter(
                        getPrefContext(), item));
            } else if (mType.equals(TYPE_CALENDAR)) {
                pref.setTitle(RegionalPreferencesDataUtils.calendarConverter(
                        getPrefContext(), item));
            } else {
                Log.d(TAG, "Finish this page due to no suitable type.");
                finish();
            }

            String value = RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                    getPrefContext(), mType);
            pref.setKey(item);
            pref.setTickEnable(!value.isEmpty() && item.equals(value));
            mPreferenceScreen.addPreference(pref);
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(mTitle);
    }

    @Override
    public int getMetricsCategory() {
        switch(mType) {
            case TYPE_CALENDAR:
                return SettingsEnums.CALENDAR_PREFERENCE;
            case TYPE_FIRST_DAY_OF_WEEK:
                return SettingsEnums.FIRST_DAY_OF_WEEK_PREFERENCE;
            default:
                return SettingsEnums.TEMPERATURE_PREFERENCE;
        }
    }

}
