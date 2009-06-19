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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.List;

public class TextToSpeechSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        TextToSpeech.OnInitListener {

    private static final String TAG = "TextToSpeechSettings";

    private static final String KEY_TTS_PLAY_EXAMPLE = "tts_play_example";
    private static final String KEY_TTS_USE_DEFAULT = "toggle_use_default_tts_settings";
    private static final String KEY_TTS_DEFAULT_RATE = "tts_default_rate";
    private static final String KEY_TTS_DEFAULT_PITCH = "tts_default_pitch";
    private static final String KEY_TTS_DEFAULT_LANG = "tts_default_lang";

    // TODO move this to android.speech.tts.TextToSpeech.Engine
    private static final String FALLBACK_TTS_DEFAULT_SYNTH = "com.svox.pico";

    private Preference         mPlayExample = null;
    private CheckBoxPreference mUseDefaultPref = null;
    private ListPreference     mDefaultRatePref = null;
    private ListPreference     mDefaultPitchPref = null;
    private ListPreference     mDefaultLangPref = null;
    private String             mDefaultEng = "";

    private boolean mEnableDemo = false;

    private TextToSpeech mTts = null;

    /**
     * Request code (arbitrary value) for voice data check through
     * startActivityForResult.
     */
    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.tts_settings);

        initDemo();
        initDefaultSettings();

        checkVoiceData();
    }
    
    
    @Override
    protected void onResume() {
        super.onResume();
        // whenever we return to this screen, we don't know the state of the
        // system, so we have to recheck that we can play the demo, or it must be disabled.
        mEnableDemo = false;
        initDemo();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTts != null) {
            mTts.shutdown();
        }
    }


    private void initDemo() {
        mPlayExample = findPreference(KEY_TTS_PLAY_EXAMPLE);
        mPlayExample.setEnabled(mEnableDemo);
        mPlayExample.setOnPreferenceClickListener(this);
    }


    private void initDefaultSettings() {
        ContentResolver resolver = getContentResolver();

        // "Use Defaults"
        mUseDefaultPref = 
            (CheckBoxPreference) findPreference(KEY_TTS_USE_DEFAULT);
        mUseDefaultPref.setChecked(Settings.Secure.getInt(resolver,
                TTS_USE_DEFAULTS,
                TextToSpeech.Engine.FALLBACK_TTS_USE_DEFAULTS) == 1 ? true : false);
        mUseDefaultPref.setOnPreferenceChangeListener(this);

        // Default engine
        mDefaultEng = FALLBACK_TTS_DEFAULT_SYNTH;

        // Default rate
        mDefaultRatePref =
            (ListPreference) findPreference(KEY_TTS_DEFAULT_RATE);
        mDefaultRatePref.setValue(String.valueOf(Settings.Secure.getInt(
                resolver, TTS_DEFAULT_RATE, TextToSpeech.Engine.FALLBACK_TTS_DEFAULT_RATE)));
        mDefaultRatePref.setOnPreferenceChangeListener(this);

        // Default pitch
        mDefaultPitchPref =
            (ListPreference) findPreference(KEY_TTS_DEFAULT_PITCH);
        mDefaultPitchPref.setValue(String.valueOf(Settings.Secure.getInt(
                resolver, TTS_DEFAULT_PITCH, TextToSpeech.Engine.FALLBACK_TTS_DEFAULT_PITCH)));
        mDefaultPitchPref.setOnPreferenceChangeListener(this);

        // Default language
        mDefaultLangPref =
                (ListPreference) findPreference(KEY_TTS_DEFAULT_LANG);
        String defaultLang = String.valueOf(Settings.Secure.getString(resolver, 
                TTS_DEFAULT_LANG));
        if (defaultLang.compareTo("null") == 0) {
            mDefaultLangPref.setValue(TextToSpeech.Engine.FALLBACK_TTS_DEFAULT_LANG);
            Log.i(TAG, "TTS initDefaultSettings() default lang null ");
        } else {
            mDefaultLangPref.setValue(defaultLang);
            Log.i(TAG, "TTS initDefaultSettings() default lang is "+defaultLang);
        }
        mDefaultLangPref.setOnPreferenceChangeListener(this);
    }


    private void checkVoiceData() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent();
        intent.setAction("android.intent.action.CHECK_TTS_DATA");
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        // query only the package that matches that of the default engine
        for (int i = 0; i < resolveInfos.size(); i++) {
            ActivityInfo currentActivityInfo = resolveInfos.get(i).activityInfo;
            if (mDefaultEng.equals(currentActivityInfo.packageName)) {
                intent.setClassName(mDefaultEng, currentActivityInfo.name);
                this.startActivityForResult(intent, VOICE_DATA_INTEGRITY_CHECK);
            }
        }
    }


    /**
     * Called when the TTS engine is initialized.
     */
    public void onInit(int status) {
        if (status == TextToSpeech.TTS_SUCCESS) {
            Log.v(TAG, "TTS engine for settings screen initialized.");
            mEnableDemo = true;
        } else {
            Log.v(TAG, "TTS engine for settings screen failed to initialize successfully.");
            mEnableDemo = false;
        }
        mPlayExample.setEnabled(mEnableDemo);
    }


    /**
     * Called when voice data integrity check returns
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_DATA_INTEGRITY_CHECK) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                Log.v(TAG, "Voice data check passed");
                if (mTts == null) {
                    mTts = new TextToSpeech(this, this);
                }
            } else {
                Log.v(TAG, "Voice data check failed");

            }
        }
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
        } else if (KEY_TTS_DEFAULT_LANG.equals(preference.getKey())) {
            // Default language
            String value = (String) objValue;
            Settings.Secure.putString(getContentResolver(),
                        TTS_DEFAULT_LANG, value); 
            Log.i(TAG, "TTS default lang is "+value);
        }
        
        return true;
    }
    

    public boolean onPreferenceClick(Preference preference) {
        if (preference == mPlayExample) {
            if (mTts != null) {
                mTts.speak(getResources().getString(R.string.tts_demo),
                        TextToSpeech.TTS_QUEUE_FLUSH, null);
            }
            return true;
        }
        return false;
    }

}
