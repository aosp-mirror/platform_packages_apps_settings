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
import com.android.settings.UserDictionarySettings;
import com.android.settings.Utils;
import com.android.settings.VoiceInputOutputSettings;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;

import java.util.Set;

public class InputMethodAndLanguageSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener{

    private static final String KEY_PHONE_LANGUAGE = "phone_language";
    private static final String KEY_CURRENT_INPUT_METHOD = "current_input_method";
    private static final String KEY_INPUT_METHOD_SELECTOR = "input_method_selector";
    private static final String KEY_LANGUAGE_SETTINGS_CATEGORY = "language_settings_category";
    private static final String KEY_USER_DICTIONARY_SETTINGS = "key_user_dictionary_settings";


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

    private void updateUserDictionaryPreference(Preference userDictionaryPreference) {
        final Activity activity = getActivity();
        final Set<String> localeList = UserDictionaryList.getUserDictionaryLocalesList(activity);
        if (localeList.size() <= 1) {
            userDictionaryPreference.setTitle(R.string.user_dict_single_settings_title);
            userDictionaryPreference.setFragment(UserDictionarySettings.class.getName());
            // If the size of localeList is 0, we don't set the locale parameter in the
            // extras. This will be interpreted by the UserDictionarySettings class as
            // meaning "the current locale".
            // Note that with the current code for UserDictionaryList#getUserDictionaryLocalesList()
            // the locale list always has at least one element, since it always includes the current
            // locale explicitly. @see UserDictionaryList.getUserDictionaryLocalesList().
            if (localeList.size() == 1) {
                final String locale = (String)localeList.toArray()[0];
                userDictionaryPreference.getExtras().putString("locale", locale);
            }
        } else {
            userDictionaryPreference.setTitle(R.string.user_dict_multiple_settings_title);
            userDictionaryPreference.setFragment(UserDictionaryList.class.getName());
        }
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

        updateUserDictionaryPreference(findPreference(KEY_USER_DICTIONARY_SETTINGS));
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
