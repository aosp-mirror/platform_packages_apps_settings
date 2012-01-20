/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.tts;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TtsEngines;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;


public class TtsEngineSettingsFragment extends SettingsPreferenceFragment implements
        OnPreferenceClickListener, OnPreferenceChangeListener {
    private static final String TAG = "TtsEngineSettings";
    private static final boolean DBG = false;

    private static final String KEY_ENGINE_LOCALE = "tts_default_lang";
    private static final String KEY_ENGINE_SETTINGS = "tts_engine_settings";
    private static final String KEY_INSTALL_DATA = "tts_install_data";

    private TtsEngines mEnginesHelper;
    private ListPreference mLocalePreference;
    private Preference mEngineSettingsPreference;
    private Preference mInstallVoicesPreference;
    private Intent mEngineSettingsIntent;

    public TtsEngineSettingsFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tts_engine_settings);
        mEnginesHelper = new TtsEngines(getActivity());

        final PreferenceScreen root = getPreferenceScreen();
        mLocalePreference = (ListPreference) root.findPreference(KEY_ENGINE_LOCALE);
        mLocalePreference.setOnPreferenceChangeListener(this);
        mEngineSettingsPreference = root.findPreference(KEY_ENGINE_SETTINGS);
        mEngineSettingsPreference.setOnPreferenceClickListener(this);
        mInstallVoicesPreference = root.findPreference(KEY_INSTALL_DATA);
        mInstallVoicesPreference.setOnPreferenceClickListener(this);
        // Remove this preference unless voices are indeed available to install.
        root.removePreference(mInstallVoicesPreference);


        root.setTitle(getEngineLabel());
        root.setKey(getEngineName());
        mEngineSettingsPreference.setTitle(getResources().getString(
                R.string.tts_engine_settings_title, getEngineLabel()));

        mEngineSettingsIntent = mEnginesHelper.getSettingsIntent(getEngineName());
        if (mEngineSettingsIntent == null) {
            mEngineSettingsPreference.setEnabled(false);
        }
        mInstallVoicesPreference.setEnabled(false);

        updateVoiceDetails();
    }

    private void updateVoiceDetails() {
        final Intent voiceDataDetails = getArguments().getParcelable(
                TtsEnginePreference.FRAGMENT_ARGS_VOICES);
        if (DBG) Log.d(TAG, "Parsing voice data details, data: " + voiceDataDetails.toUri(0));
        ArrayList<String> available = voiceDataDetails.getStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
        ArrayList<String> unavailable = voiceDataDetails.getStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES);

        if (available == null){
            Log.e(TAG, "TTS data check failed (available == null).");
            final CharSequence[] empty = new CharSequence[0];
            mLocalePreference.setEntries(empty);
            mLocalePreference.setEntryValues(empty);
            return;
        }

        if (unavailable != null && unavailable.size() > 0) {
            mInstallVoicesPreference.setEnabled(true);
            getPreferenceScreen().addPreference(mInstallVoicesPreference);
        } else {
            getPreferenceScreen().removePreference(mInstallVoicesPreference);
        }

        if (available.size() > 0) {
            updateDefaultLocalePref(available);
        } else {
            final CharSequence[] empty = new CharSequence[0];
            mLocalePreference.setEntries(empty);
            mLocalePreference.setEntryValues(empty);
        }
    }

    private void updateDefaultLocalePref(ArrayList<String> availableLangs) {
        String currentLocale = mEnginesHelper.getLocalePrefForEngine(
                getEngineName());

        CharSequence[] entries = new CharSequence[availableLangs.size()];
        CharSequence[] entryValues = new CharSequence[availableLangs.size()];

        int selectedLanguageIndex = -1;
        for (int i = 0; i < availableLangs.size(); i++) {
            String[] langCountryVariant = availableLangs.get(i).split("-");
            Locale loc = null;
            if (langCountryVariant.length == 1){
                loc = new Locale(langCountryVariant[0]);
            } else if (langCountryVariant.length == 2){
                loc = new Locale(langCountryVariant[0], langCountryVariant[1]);
            } else if (langCountryVariant.length == 3){
                loc = new Locale(langCountryVariant[0], langCountryVariant[1],
                                 langCountryVariant[2]);
            }
            if (loc != null){
                entries[i] = loc.getDisplayName();
                entryValues[i] = availableLangs.get(i);
                if (availableLangs.get(i).equalsIgnoreCase(currentLocale)) {
                    selectedLanguageIndex = i;
                }
            }
        }

        mLocalePreference.setEntries(entries);
        mLocalePreference.setEntryValues(entryValues);
        if (selectedLanguageIndex > -1) {
            mLocalePreference.setValueIndex(selectedLanguageIndex);
        } else {
            mLocalePreference.setValueIndex(0);
            mEnginesHelper.updateLocalePrefForEngine(getEngineName(),
                    availableLangs.get(0));
        }
    }

    /**
     * Ask the current default engine to launch the matching INSTALL_TTS_DATA activity
     * so the required TTS files are properly installed.
     */
    private void installVoiceData() {
        if (TextUtils.isEmpty(getEngineName())) return;
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(getEngineName());
        try {
            Log.v(TAG, "Installing voice data: " + intent.toUri(0));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Failed to install TTS data, no acitivty found for " + intent + ")");
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mInstallVoicesPreference) {
            installVoiceData();
            return true;
        } else if (preference == mEngineSettingsPreference) {
            startActivity(mEngineSettingsIntent);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLocalePreference) {
            mEnginesHelper.updateLocalePrefForEngine(getEngineName(), (String) newValue);
            return true;
        }

        return false;
    }

    private String getEngineName() {
        return getArguments().getString(TtsEnginePreference.FRAGMENT_ARGS_NAME);
    }

    private String getEngineLabel() {
        return getArguments().getString(TtsEnginePreference.FRAGMENT_ARGS_LABEL);
    }

}
