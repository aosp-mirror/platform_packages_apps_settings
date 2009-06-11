/*
 * Copyright (C) 2009 The Android Open Source Project
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

import static android.provider.Settings.Secure.TTS_USE_DEFAULTS;
import static android.provider.Settings.Secure.TTS_DEFAULT_RATE;
import static android.provider.Settings.Secure.TTS_DEFAULT_PITCH;
import static android.provider.Settings.Secure.TTS_DEFAULT_LANG;

import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.util.Log;

public class TextToSpeechSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "TextToSpeechSettings";
    
    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_TTS_DEFAULT_RATE = 100; // 1x
    private static final int FALLBACK_TTS_DEFAULT_PITCH = 100;// 1x
    private static final int FALLBACK_TTS_USE_DEFAULTS = 1;
    private static final String FALLBACK_TTS_DEFAULT_LANG = "en-rUS";

    private static final String KEY_TTS_USE_DEFAULT =
            "toggle_use_default_tts_settings";
    private static final String KEY_TTS_DEFAULT_RATE = "tts_default_rate";
    private static final String KEY_TTS_DEFAULT_PITCH = "tts_default_pitch";
    private static final String KEY_TTS_DEFAULT_LANG = "tts_default_lang";
    
    private CheckBoxPreference mUseDefaultPref = null;
    private ListPreference     mDefaultRatePref = null;
    private ListPreference     mDefaultPitchPref = null;
    private ListPreference     mDefaultLangPref = null;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.tts_settings);
        
        initDefaultSettings();
    }


    private void initDefaultSettings() {
        ContentResolver resolver = getContentResolver();

        // "Use Defaults"
        mUseDefaultPref = 
            (CheckBoxPreference) findPreference(KEY_TTS_USE_DEFAULT);
        mUseDefaultPref.setChecked(Settings.Secure.getInt(resolver,
                TTS_USE_DEFAULTS,
                FALLBACK_TTS_USE_DEFAULTS) == 1 ? true : false);
        mUseDefaultPref.setOnPreferenceChangeListener(this);

        // Default rate
        mDefaultRatePref =
            (ListPreference) findPreference(KEY_TTS_DEFAULT_RATE);
        mDefaultRatePref.setValue(String.valueOf(Settings.Secure.getInt(
                resolver, TTS_DEFAULT_RATE, FALLBACK_TTS_DEFAULT_RATE)));
        mDefaultRatePref.setOnPreferenceChangeListener(this);

        // Default pitch
        mDefaultPitchPref =
            (ListPreference) findPreference(KEY_TTS_DEFAULT_PITCH);
        mDefaultPitchPref.setValue(String.valueOf(Settings.Secure.getInt(
                resolver, TTS_DEFAULT_PITCH, FALLBACK_TTS_DEFAULT_PITCH)));
        mDefaultPitchPref.setOnPreferenceChangeListener(this);
        
        // Default language
        mDefaultLangPref =
                (ListPreference) findPreference(KEY_TTS_DEFAULT_LANG);
        String defaultLang = String.valueOf(Settings.Secure.getString(resolver, 
                TTS_DEFAULT_LANG));
        if (defaultLang.compareTo("null") == 0) {
            mDefaultLangPref.setValue(FALLBACK_TTS_DEFAULT_LANG);
            Log.i(TAG, "TTS initDefaultSettings() default lang null ");
        } else {
            mDefaultLangPref.setValue(defaultLang);
            Log.i(TAG, "TTS initDefaultSettings() default lang is "+defaultLang);
        }
        mDefaultLangPref.setOnPreferenceChangeListener(this);
    }


    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (KEY_TTS_USE_DEFAULT.equals(preference.getKey())) {
            // "Use Defaults"
            int value = (Boolean)objValue ? 1 : 0;
            Settings.Secure.putInt(getContentResolver(), TTS_USE_DEFAULTS,
                    value);
            Log.i(TAG, "TTS use default settings is "+objValue.toString());
        } else if (KEY_TTS_DEFAULT_RATE.equals(preference.getKey())) {
            // Default rate
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.Secure.putInt(getContentResolver(), 
                        TTS_DEFAULT_RATE, value);
                Log.i(TAG, "TTS default rate is "+value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist default TTS rate setting", e);
            }
        } else if (KEY_TTS_DEFAULT_PITCH.equals(preference.getKey())) {
            // Default pitch
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.Secure.putInt(getContentResolver(), 
                        TTS_DEFAULT_PITCH, value);
                Log.i(TAG, "TTS default pitch is "+value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist default TTS pitch setting", e);
            }
        }else if (KEY_TTS_DEFAULT_LANG.equals(preference.getKey())) {
            // Default language
            String value = (String) objValue;
            Settings.Secure.putString(getContentResolver(),
                        TTS_DEFAULT_LANG, value); 
            Log.i(TAG, "TTS default lang is "+value);
        }
        
        return true;
    }
    
}
