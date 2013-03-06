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

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.util.Pair;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;


public class TtsEngineSettingsFragment extends SettingsPreferenceFragment implements
        OnPreferenceClickListener, OnPreferenceChangeListener {
    private static final String TAG = "TtsEngineSettings";
    private static final boolean DBG = false;

    private static final String KEY_ENGINE_LOCALE = "tts_default_lang";
    private static final String KEY_ENGINE_SETTINGS = "tts_engine_settings";
    private static final String KEY_INSTALL_DATA = "tts_install_data";

    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;

    private TtsEngines mEnginesHelper;
    private ListPreference mLocalePreference;
    private Preference mEngineSettingsPreference;
    private Preference mInstallVoicesPreference;
    private Intent mEngineSettingsIntent;
    private Intent mVoiceDataDetails;

    private TextToSpeech mTts;

    private final TextToSpeech.OnInitListener mTtsInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if (status != TextToSpeech.SUCCESS) {
                finishFragment();
            } else {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLocalePreference.setEnabled(true);
                    }
                });
            }
        }
    };

    private final BroadcastReceiver mLanguagesChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Installed or uninstalled some data packs
            if (TextToSpeech.Engine.ACTION_TTS_DATA_INSTALLED.equals(intent.getAction())) {
                checkTtsData();
            }
        }
    };

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
        // Remove this preference unless locales are indeed available.
        root.removePreference(mLocalePreference);

        root.setTitle(getEngineLabel());
        root.setKey(getEngineName());
        mEngineSettingsPreference.setTitle(getResources().getString(
                R.string.tts_engine_settings_title, getEngineLabel()));

        mEngineSettingsIntent = mEnginesHelper.getSettingsIntent(getEngineName());
        if (mEngineSettingsIntent == null) {
            mEngineSettingsPreference.setEnabled(false);
        }
        mInstallVoicesPreference.setEnabled(false);

        mLocalePreference.setEnabled(false);
        mLocalePreference.setEntries(new CharSequence[0]);
        mLocalePreference.setEntryValues(new CharSequence[0]);

        mVoiceDataDetails = getArguments().getParcelable(TtsEnginePreference.FRAGMENT_ARGS_VOICES);

        mTts = new TextToSpeech(getActivity().getApplicationContext(), mTtsInitListener,
                getEngineName());


        // Check if data packs changed
        checkTtsData();

        getActivity().registerReceiver(mLanguagesChangedReceiver,
                new IntentFilter(TextToSpeech.Engine.ACTION_TTS_DATA_INSTALLED));
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mLanguagesChangedReceiver);
        mTts.shutdown();
        super.onDestroy();
    }

    private final void checkTtsData() {
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        intent.setPackage(getEngineName());
        try {
            if (DBG) Log.d(TAG, "Updating engine: Checking voice data: " + intent.toUri(0));
            startActivityForResult(intent, VOICE_DATA_INTEGRITY_CHECK);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Failed to check TTS data, no activity found for " + intent + ")");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_DATA_INTEGRITY_CHECK) {
            mVoiceDataDetails = data;
            updateVoiceDetails();
        }
    }

    private void updateVoiceDetails() {
        if (DBG) Log.d(TAG, "Parsing voice data details, data: " + mVoiceDataDetails.toUri(0));

        final ArrayList<String> available = mVoiceDataDetails.getStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
        final ArrayList<String> unavailable = mVoiceDataDetails.getStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES);

        if (available == null){
            Log.e(TAG, "TTS data check failed (available == null).");
            mLocalePreference.setEnabled(false);
            getPreferenceScreen().removePreference(mLocalePreference);
            return;
        }

        if (unavailable != null && unavailable.size() > 0) {
            mInstallVoicesPreference.setEnabled(true);
            getPreferenceScreen().addPreference(mInstallVoicesPreference);
        } else {
            getPreferenceScreen().removePreference(mInstallVoicesPreference);
        }

        if (available.size() > 0) {
            mLocalePreference.setEnabled(true);
            getPreferenceScreen().addPreference(mLocalePreference);
            updateDefaultLocalePref(available);
        } else {
            mLocalePreference.setEnabled(false);
            getPreferenceScreen().removePreference(mLocalePreference);
        }
    }

    private void updateDefaultLocalePref(ArrayList<String> availableLangs) {
        String currentLocale = mEnginesHelper.getLocalePrefForEngine(
                getEngineName());

        ArrayList<Pair<String, String>> entryPairs =
                new ArrayList<Pair<String, String>>(availableLangs.size());
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
                entryPairs.add(new Pair<String, String>(
                        loc.getDisplayName(), availableLangs.get(i)));
            }
        }

        // Sort it
        Collections.sort(entryPairs, new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> lhs, Pair<String, String> rhs) {
                return lhs.first.compareToIgnoreCase(rhs.first);
            }
        });

        // Get two arrays out of one of pairs
        int selectedLanguageIndex = -1;
        CharSequence[] entries = new CharSequence[availableLangs.size()];
        CharSequence[] entryValues = new CharSequence[availableLangs.size()];
        int i = 0;
        for (Pair<String, String> entry : entryPairs) {
            if (entry.second.equalsIgnoreCase(currentLocale)) {
                selectedLanguageIndex = i;
            }
            entries[i] = entry.first;
            entryValues[i++] = entry.second;
        }

        mLocalePreference.setEntries(entries);
        mLocalePreference.setEntryValues(entryValues);
        if (selectedLanguageIndex > -1) {
            mLocalePreference.setValueIndex(selectedLanguageIndex);
        } else {
            mLocalePreference.setValueIndex(0);
            updateLanguageTo(availableLangs.get(0));
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
            updateLanguageTo((String) newValue);
            return true;
        }

        return false;
    }

    private void updateLanguageTo(String locale) {
        mEnginesHelper.updateLocalePrefForEngine(getEngineName(), locale);
        if (getEngineName().equals(mTts.getCurrentEngine())) {
            String[] localeArray = TtsEngines.parseLocalePref(locale);
            if (localeArray != null) {
                mTts.setLanguage(new Locale(localeArray[0], localeArray[1], localeArray[2]));
            }
        }
    }

    private String getEngineName() {
        return getArguments().getString(TtsEnginePreference.FRAGMENT_ARGS_NAME);
    }

    private String getEngineLabel() {
        return getArguments().getString(TtsEnginePreference.FRAGMENT_ARGS_LABEL);
    }

}
