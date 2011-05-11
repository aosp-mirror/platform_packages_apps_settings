/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.VoiceInputOutputSettings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.provider.UserDictionary;

import java.util.Locale;
import java.util.TreeMap;

public class InputMethodAndLanguageSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener{

    private static final String KEY_PHONE_LANGUAGE = "phone_language";
    private static final String KEY_CURRENT_INPUT_METHOD = "current_input_method";
    private static final String KEY_INPUT_METHOD_SELECTOR = "input_method_selector";
    private static final String KEY_LANGUAGE_SETTINGS_CATEGORY = "language_settings_category";
    private static final String USER_DICTIONARY_SETTINGS_INTENT_ACTION =
            "android.settings.USER_DICTIONARY_SETTINGS";

    private int mDefaultInputMethodSelectorVisibility = 0;
    private ListPreference mShowInputMethodSelectorPref;
    private Preference mLanguagePref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.language_settings);

        try {
            mDefaultInputMethodSelectorVisibility = Integer.valueOf(
                    getString(R.string.input_method_selector_visibility_default_value));
        } catch (NumberFormatException e) {
        }

        if (getActivity().getAssets().getLocales().length == 1) {
            // No "Select language" pref if there's only one system locale available.
            getPreferenceScreen().removePreference(findPreference(KEY_PHONE_LANGUAGE));
        } else {
            mLanguagePref = findPreference(KEY_PHONE_LANGUAGE);
        }

        createUserDictSettings((PreferenceGroup) findPreference(KEY_LANGUAGE_SETTINGS_CATEGORY));

        mShowInputMethodSelectorPref = (ListPreference)findPreference(
                KEY_INPUT_METHOD_SELECTOR);
        mShowInputMethodSelectorPref.setOnPreferenceChangeListener(this);
        // TODO: Update current input method name on summary
        updateInputMethodSelectorSummary(loadInputMethodSelectorVisibility());

        new VoiceInputOutputSettings(this).onCreate();
    }

    private void updateInputMethodSelectorSummary(int value) {
        String[] inputMethodSelectorTitles = getResources().getStringArray(
                R.array.input_method_selector_titles);
        if (inputMethodSelectorTitles.length > value) {
            mShowInputMethodSelectorPref.setSummary(inputMethodSelectorTitles[value]);
            mShowInputMethodSelectorPref.setValue(String.valueOf(value));
        }
    }

    /**
     * Creates the entries that allow the user to go into the user dictionary for each locale.
     * @param userDictGroup The group to put the settings in.
     */
    protected void createUserDictSettings(PreferenceGroup userDictGroup) {
        final Activity activity = getActivity();
        final Cursor locales = activity.managedQuery(UserDictionary.Words.CONTENT_URI,
                new String[] { UserDictionary.Words.LOCALE },
                null, null, null);
        final TreeMap<String, Preference> prefs = new TreeMap<String, Preference>();
        int order = findPreference(KEY_PHONE_LANGUAGE).getOrder();
        if (!locales.moveToFirst()) {
            prefs.put("", createUserDictionaryPreference(null, activity, ++order));
        } else {
            final int columnIndex = locales.getColumnIndex(UserDictionary.Words.LOCALE);
            do {
                final String locale = locales.getString(columnIndex);
                if (!prefs.containsKey(locale))
                    prefs.put(locale, createUserDictionaryPreference(locale, activity, ++order));
            } while (locales.moveToNext());
        }
        for (final Preference p : prefs.values()) {
            userDictGroup.addPreference(p);
        }
    }

    /**
     * Create a single User Dictionary Preference object, with its parameters set.
     * @param locale The locale for which this user dictionary is for.
     * @return The corresponding preference.
     */
    protected Preference createUserDictionaryPreference(String locale, Activity activity,
            int order) {
        final Preference newPref = new Preference(getActivity());
        newPref.setOrder(order);
        newPref.setTitle(activity.getString(R.string.user_dict_settings_title));
        final Intent intent = new Intent(USER_DICTIONARY_SETTINGS_INTENT_ACTION);
        if (null != locale) {
            newPref.setSummary(new Locale(locale).getDisplayName());
            intent.putExtra("locale", locale);
        }
        newPref.setIntent(intent);
        return newPref;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLanguagePref != null) {
            Configuration conf = getResources().getConfiguration();
            String locale = conf.locale.getDisplayName(conf.locale);
            if (locale != null && locale.length() > 1) {
                locale = Character.toUpperCase(locale.charAt(0)) + locale.substring(1);
                mLanguagePref.setSummary(locale);
            }
        }

        mShowInputMethodSelectorPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mShowInputMethodSelectorPref.setOnPreferenceChangeListener(null);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // Input Method stuff
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        if (preference instanceof PreferenceScreen) {
            if (preference.getFragment() != null) {
                // Fragment will be handled correctly by the super class.
            } else if (KEY_CURRENT_INPUT_METHOD.equals(preference.getKey())) {
                final InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void saveInputMethodSelectorVisibility(String value) {
        try {
            int intValue = Integer.valueOf(value);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.INPUT_METHOD_SELECTOR_VISIBILITY, intValue);
            updateInputMethodSelectorSummary(intValue);
        } catch(NumberFormatException e) {
        }
    }

    private int loadInputMethodSelectorVisibility() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.INPUT_METHOD_SELECTOR_VISIBILITY,
                mDefaultInputMethodSelectorVisibility);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mShowInputMethodSelectorPref) {
            if (value instanceof String) {
                saveInputMethodSelectorVisibility((String)value);
            }
        }
        return false;
    }

}
