/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.speech.tts.TtsEngines;

import com.android.settings.voice.VoiceInputHelper;

/**
 * Settings screen for voice input/output.
 */
public class VoiceInputOutputSettings {

    private static final String TAG = "VoiceInputOutputSettings";

    private static final String KEY_VOICE_CATEGORY = "voice_category";
    private static final String KEY_VOICE_INPUT_SETTINGS = "voice_input_settings";
    private static final String KEY_TTS_SETTINGS = "tts_settings";

    private PreferenceGroup mParent;
    private PreferenceCategory mVoiceCategory;
    private Preference mVoiceInputSettingsPref;
    private Preference mTtsSettingsPref;
    private final SettingsPreferenceFragment mFragment;
    private final TtsEngines mTtsEngines;

    public VoiceInputOutputSettings(SettingsPreferenceFragment fragment) {
        mFragment = fragment;
        mTtsEngines = new TtsEngines(fragment.getPreferenceScreen().getContext());
    }

    public void onCreate() {

        mParent = mFragment.getPreferenceScreen();
        mVoiceCategory = (PreferenceCategory) mParent.findPreference(KEY_VOICE_CATEGORY);
        mVoiceInputSettingsPref = mVoiceCategory.findPreference(KEY_VOICE_INPUT_SETTINGS);
        mTtsSettingsPref = mVoiceCategory.findPreference(KEY_TTS_SETTINGS);

        populateOrRemovePreferences();
    }

    private void populateOrRemovePreferences() {
        boolean hasVoiceInputPrefs = populateOrRemoveVoiceInputPrefs();
        boolean hasTtsPrefs = populateOrRemoveTtsPrefs();
        if (!hasVoiceInputPrefs && !hasTtsPrefs) {
            // There were no TTS settings and no recognizer settings,
            // so it should be safe to hide the preference category
            // entirely.
            mFragment.getPreferenceScreen().removePreference(mVoiceCategory);
        }
    }

    private boolean populateOrRemoveVoiceInputPrefs() {
        VoiceInputHelper helper = new VoiceInputHelper(mFragment.getActivity());
        if (!helper.hasItems()) {
            mVoiceCategory.removePreference(mVoiceInputSettingsPref);
            return false;
        }

        return true;
    }

    private boolean populateOrRemoveTtsPrefs() {
        if (mTtsEngines.getEngines().isEmpty()) {
            mVoiceCategory.removePreference(mTtsSettingsPref);
            return false;
        }

        return true;
    }
}
