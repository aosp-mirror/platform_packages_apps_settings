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

    private static final String STATE_KEY_LOCALE_ENTRIES = "locale_entries";
    private static final String STATE_KEY_LOCALE_ENTRY_VALUES= "locale_entry_values";
    private static final String STATE_KEY_LOCALE_VALUE = "locale_value";

    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;

    private TtsEngines mEnginesHelper;
    private ListPreference mLocalePreference;
    private Preference mEngineSettingsPreference;
    private Preference mInstallVoicesPreference;
    private Intent mEngineSettingsIntent;
    private Intent mVoiceDataDetails;

    private TextToSpeech mTts;

    private int mSelectedLocaleIndex = -1;

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

        root.setTitle(getEngineLabel());
        root.setKey(getEngineName());
        mEngineSettingsPreference.setTitle(getResources().getString(
                R.string.tts_engine_settings_title, getEngineLabel()));

        mEngineSettingsIntent = mEnginesHelper.getSettingsIntent(getEngineName());
        if (mEngineSettingsIntent == null) {
            mEngineSettingsPreference.setEnabled(false);
        }
        mInstallVoicesPreference.setEnabled(false);

        if (savedInstanceState == null) {
            mLocalePreference.setEnabled(false);
            mLocalePreference.setEntries(new CharSequence[0]);
            mLocalePreference.setEntryValues(new CharSequence[0]);
        } else {
            // Repopulate mLocalePreference with saved state. Will be updated later with
            // up-to-date values when checkTtsData() calls back with results.
            final CharSequence[] entries =
                    savedInstanceState.getCharSequenceArray(STATE_KEY_LOCALE_ENTRIES);
            final CharSequence[] entryValues =
                    savedInstanceState.getCharSequenceArray(STATE_KEY_LOCALE_ENTRY_VALUES);
            final CharSequence value =
                    savedInstanceState.getCharSequence(STATE_KEY_LOCALE_VALUE);

            mLocalePreference.setEntries(entries);
            mLocalePreference.setEntryValues(entryValues);
            mLocalePreference.setValue(value != null ? value.toString() : null);
            mLocalePreference.setEnabled(entries.length > 0);
        }

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the mLocalePreference values, so we can repopulate it with entries.
        outState.putCharSequenceArray(STATE_KEY_LOCALE_ENTRIES,
                mLocalePreference.getEntries());
        outState.putCharSequenceArray(STATE_KEY_LOCALE_ENTRY_VALUES,
                mLocalePreference.getEntryValues());
        outState.putCharSequence(STATE_KEY_LOCALE_VALUE,
                mLocalePreference.getValue());
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
            if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL) {
                updateVoiceDetails(data);
            } else {
                Log.e(TAG, "CheckVoiceData activity failed");
            }
        }
    }

    private void updateVoiceDetails(Intent data) {
        if (data == null){
            Log.e(TAG, "Engine failed voice data integrity check (null return)" +
                    mTts.getCurrentEngine());
            return;
        }
        mVoiceDataDetails = data;

        if (DBG) Log.d(TAG, "Parsing voice data details, data: " + mVoiceDataDetails.toUri(0));

        final ArrayList<String> available = mVoiceDataDetails.getStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
        final ArrayList<String> unavailable = mVoiceDataDetails.getStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES);

        if (unavailable != null && unavailable.size() > 0) {
            mInstallVoicesPreference.setEnabled(true);
        } else {
            mInstallVoicesPreference.setEnabled(false);
        }

        if (available == null){
            Log.e(TAG, "TTS data check failed (available == null).");
            mLocalePreference.setEnabled(false);
            return;
        } else {
            updateDefaultLocalePref(available);
        }
    }

    private void updateDefaultLocalePref(ArrayList<String> availableLangs) {
        if (availableLangs == null || availableLangs.size() == 0) {
            mLocalePreference.setEnabled(false);
            return;
        }
        Locale currentLocale = null;
        if (!mEnginesHelper.isLocaleSetToDefaultForEngine(getEngineName())) {
            currentLocale = mEnginesHelper.getLocalePrefForEngine(getEngineName());
        }

        ArrayList<Pair<String, Locale>> entryPairs =
                new ArrayList<Pair<String, Locale>>(availableLangs.size());
        for (int i = 0; i < availableLangs.size(); i++) {
            Locale locale = mEnginesHelper.parseLocaleString(availableLangs.get(i));
            if (locale != null){
                entryPairs.add(new Pair<String, Locale>(
                        locale.getDisplayName(), locale));
            }
        }

        // Sort it
        Collections.sort(entryPairs, new Comparator<Pair<String, Locale>>() {
            @Override
            public int compare(Pair<String, Locale> lhs, Pair<String, Locale> rhs) {
                return lhs.first.compareToIgnoreCase(rhs.first);
            }
        });

        // Get two arrays out of one of pairs
        mSelectedLocaleIndex = 0; // Will point to the R.string.tts_lang_use_system value
        CharSequence[] entries = new CharSequence[availableLangs.size()+1];
        CharSequence[] entryValues = new CharSequence[availableLangs.size()+1];

        entries[0] = getActivity().getString(R.string.tts_lang_use_system);
        entryValues[0] = "";

        int i = 1;
        for (Pair<String, Locale> entry : entryPairs) {
            if (entry.second.equals(currentLocale)) {
                mSelectedLocaleIndex = i;
            }
            entries[i] = entry.first;
            entryValues[i++] = entry.second.toString();
        }

        mLocalePreference.setEntries(entries);
        mLocalePreference.setEntryValues(entryValues);
        mLocalePreference.setEnabled(true);
        setLocalePreference(mSelectedLocaleIndex);
    }

    /** Set entry from entry table in mLocalePreference */
    private void setLocalePreference(int index) {
        if (index < 0) {
            mLocalePreference.setValue("");
            mLocalePreference.setSummary(R.string.tts_lang_not_selected);
        } else {
            mLocalePreference.setValueIndex(index);
            mLocalePreference.setSummary(mLocalePreference.getEntries()[index]);
        }
    }

    /**
     * Ask the current default engine to launch the matching INSTALL_TTS_DATA activity
     * so the required TTS files are properly installed.
     */
    private void installVoiceData() {
        if (TextUtils.isEmpty(getEngineName())) return;
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
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
            String localeString = (String) newValue;
            updateLanguageTo((!TextUtils.isEmpty(localeString) ?
                    mEnginesHelper.parseLocaleString(localeString) : null));
            return true;
        }
        return false;
    }

    private void updateLanguageTo(Locale locale) {
        int selectedLocaleIndex = -1;
        String localeString = (locale != null) ? locale.toString() : "";
        for (int i=0; i < mLocalePreference.getEntryValues().length; i++) {
            if (localeString.equalsIgnoreCase(mLocalePreference.getEntryValues()[i].toString())) {
                selectedLocaleIndex = i;
                break;
            }
        }

        if (selectedLocaleIndex == -1) {
            Log.w(TAG, "updateLanguageTo called with unknown locale argument");
            return;
        }
        mLocalePreference.setSummary(mLocalePreference.getEntries()[selectedLocaleIndex]);
        mSelectedLocaleIndex = selectedLocaleIndex;

        mEnginesHelper.updateLocalePrefForEngine(getEngineName(), locale);

        if (getEngineName().equals(mTts.getCurrentEngine())) {
            // Null locale means "use system default"
            mTts.setLanguage((locale != null) ? locale : Locale.getDefault());
        }
    }

    private String getEngineName() {
        return getArguments().getString(TtsEnginePreference.FRAGMENT_ARGS_NAME);
    }

    private String getEngineLabel() {
        return getArguments().getString(TtsEnginePreference.FRAGMENT_ARGS_LABEL);
    }
}
