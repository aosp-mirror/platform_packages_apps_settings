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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TtsEngines;
import android.speech.tts.UtteranceProgressListener;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Checkable;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SeekBarPreference;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.tts.TtsEnginePreference.RadioButtonGroupState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Set;

import static android.provider.Settings.Secure.TTS_DEFAULT_PITCH;
import static android.provider.Settings.Secure.TTS_DEFAULT_RATE;
import static android.provider.Settings.Secure.TTS_DEFAULT_SYNTH;

public class TextToSpeechSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        RadioButtonGroupState {

    private static final String TAG = "TextToSpeechSettings";
    private static final boolean DBG = false;

    /** Preference key for the "play TTS example" preference. */
    private static final String KEY_PLAY_EXAMPLE = "tts_play_example";;

    /** Preference key for the TTS pitch selection slider. */
    private static final String KEY_DEFAULT_PITCH = "tts_default_pitch";

    /** Preference key for the TTS rate selection slider. */
    private static final String KEY_DEFAULT_RATE = "tts_default_rate";

    /** Preference key for the TTS reset speech rate preference. */
    private static final String KEY_RESET_SPEECH_RATE = "reset_speech_rate";

    /** Preference key for the TTS reset speech pitch preference. */
    private static final String KEY_RESET_SPEECH_PITCH = "reset_speech_pitch";

    /** Preference key for the TTS status field. */
    private static final String KEY_STATUS = "tts_status";

    /**
     * Preference key for the engine selection preference.
     */
    private static final String KEY_ENGINE_PREFERENCE_SECTION =
            "tts_engine_preference_section";

    /**
     * These look like birth years, but they aren't mine. I'm much younger than this.
     */
    private static final int GET_SAMPLE_TEXT = 1983;
    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;

    /**
     * Speech rate value.
     * This value should be kept in sync with the max value set in tts_settings xml.
     */
    private static final int MAX_SPEECH_RATE = 600;
    private static final int MIN_SPEECH_RATE = 10;

    /**
     * Speech pitch value.
     * TTS pitch value varies from 25 to 400, where 100 is the value
     * for normal pitch. The max pitch value is set to 400, based on feedback from users
     * and the GoogleTTS pitch variation range. The range for pitch is not set in stone
     * and should be readjusted based on user need.
     * This value should be kept in sync with the max value set in tts_settings xml.
     */
    private static final int MAX_SPEECH_PITCH = 400;
    private static final int MIN_SPEECH_PITCH = 25;

    private PreferenceCategory mEnginePreferenceCategory;
    private SeekBarPreference mDefaultPitchPref;
    private SeekBarPreference mDefaultRatePref;
    private Preference mResetSpeechRate;
    private Preference mResetSpeechPitch;
    private Preference mPlayExample;
    private Preference mEngineStatus;

    private int mDefaultPitch = TextToSpeech.Engine.DEFAULT_PITCH;
    private int mDefaultRate = TextToSpeech.Engine.DEFAULT_RATE;

    /**
     * The currently selected engine.
     */
    private String mCurrentEngine;

    /**
     * The engine checkbox that is currently checked. Saves us a bit of effort
     * in deducing the right one from the currently selected engine.
     */
    private Checkable mCurrentChecked;

    /**
     * The previously selected TTS engine. Useful for rollbacks if the users
     * choice is not loaded or fails a voice integrity check.
     */
    private String mPreviousEngine;

    private TextToSpeech mTts = null;
    private TtsEngines mEnginesHelper = null;

    private String mSampleText = null;

    /**
     * Default locale used by selected TTS engine, null if not connected to any engine.
     */
    private Locale mCurrentDefaultLocale;

    /**
     * List of available locals of selected TTS engine, as returned by
     * {@link TextToSpeech.Engine#ACTION_CHECK_TTS_DATA} activity. If empty, then activity
     * was not yet called.
     */
    private List<String> mAvailableStrLocals;

    /**
     * The initialization listener used when we are initalizing the settings
     * screen for the first time (as opposed to when a user changes his choice
     * of engine).
     */
    private final TextToSpeech.OnInitListener mInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            onInitEngine(status);
        }
    };

    /**
     * The initialization listener used when the user changes his choice of
     * engine (as opposed to when then screen is being initialized for the first
     * time).
     */
    private final TextToSpeech.OnInitListener mUpdateListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            onUpdateEngine(status);
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.TTS_TEXT_TO_SPEECH;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tts_settings);

        getActivity().setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);

        mPlayExample = findPreference(KEY_PLAY_EXAMPLE);
        mPlayExample.setOnPreferenceClickListener(this);
        mPlayExample.setEnabled(false);

        mResetSpeechRate = findPreference(KEY_RESET_SPEECH_RATE);
        mResetSpeechRate.setOnPreferenceClickListener(this);
        mResetSpeechPitch = findPreference(KEY_RESET_SPEECH_PITCH);
        mResetSpeechPitch.setOnPreferenceClickListener(this);

        mEnginePreferenceCategory = (PreferenceCategory) findPreference(
                KEY_ENGINE_PREFERENCE_SECTION);
        mDefaultPitchPref = (SeekBarPreference) findPreference(KEY_DEFAULT_PITCH);
        mDefaultRatePref = (SeekBarPreference) findPreference(KEY_DEFAULT_RATE);

        mEngineStatus = findPreference(KEY_STATUS);
        updateEngineStatus(R.string.tts_status_checking);

        mTts = new TextToSpeech(getActivity().getApplicationContext(), mInitListener);
        mEnginesHelper = new TtsEngines(getActivity().getApplicationContext());

        setTtsUtteranceProgressListener();
        initSettings();

        // Prevent restarting the TTS connection on rotation
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mTts == null || mCurrentDefaultLocale == null) {
            return;
        }
        Locale ttsDefaultLocale = mTts.getDefaultLanguage();
        if (mCurrentDefaultLocale != null && !mCurrentDefaultLocale.equals(ttsDefaultLocale)) {
            updateWidgetState(false);
            checkDefaultLocale();
        }
    }

    private void setTtsUtteranceProgressListener() {
        if (mTts == null) {
            return;
        }
        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {}

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "Error while trying to synthesize sample text");
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
    }

    private void initSettings() {
        final ContentResolver resolver = getContentResolver();

        // Set up the default rate and pitch.
        mDefaultRate = android.provider.Settings.Secure.getInt(
            resolver, TTS_DEFAULT_RATE, TextToSpeech.Engine.DEFAULT_RATE);
        mDefaultPitch = android.provider.Settings.Secure.getInt(
            resolver, TTS_DEFAULT_PITCH, TextToSpeech.Engine.DEFAULT_PITCH);

        mDefaultRatePref.setProgress(getSeekBarProgressFromValue(KEY_DEFAULT_RATE, mDefaultRate));
        mDefaultRatePref.setOnPreferenceChangeListener(this);
        mDefaultRatePref.setMax(getSeekBarProgressFromValue(KEY_DEFAULT_RATE, MAX_SPEECH_RATE));

        mDefaultPitchPref.setProgress(getSeekBarProgressFromValue(KEY_DEFAULT_PITCH,
              mDefaultPitch));
        mDefaultPitchPref.setOnPreferenceChangeListener(this);
        mDefaultPitchPref.setMax(getSeekBarProgressFromValue(KEY_DEFAULT_PITCH,
              MAX_SPEECH_PITCH));

        if (mTts != null) {
            mCurrentEngine = mTts.getCurrentEngine();
            mTts.setSpeechRate(mDefaultRate/100.0f);
            mTts.setPitch(mDefaultPitch/100.0f);
        }

        SettingsActivity activity = null;
        if (getActivity() instanceof SettingsActivity) {
            activity = (SettingsActivity) getActivity();
        } else {
            throw new IllegalStateException("TextToSpeechSettings used outside a " +
                    "Settings");
        }

        mEnginePreferenceCategory.removeAll();

        List<EngineInfo> engines = mEnginesHelper.getEngines();
        for (EngineInfo engine : engines) {
            TtsEnginePreference enginePref = new TtsEnginePreference(getPrefContext(), engine,
                    this, activity);
            mEnginePreferenceCategory.addPreference(enginePref);
        }

        checkVoiceData(mCurrentEngine);
    }

    /**
     * The minimum speech pitch/rate value should be > 0 but the minimum value of a seekbar in
     * android is fixed at 0. Therefore, we increment the seekbar progress with MIN_SPEECH_VALUE
     * so that the minimum seekbar progress value is MIN_SPEECH_PITCH/RATE.
     *     SPEECH_VALUE = MIN_SPEECH_VALUE + SEEKBAR_PROGRESS
     */
    private int getValueFromSeekBarProgress(String preferenceKey, int progress) {
        if (preferenceKey.equals(KEY_DEFAULT_RATE)) {
            return MIN_SPEECH_RATE + progress;
        } else if (preferenceKey.equals(KEY_DEFAULT_PITCH)) {
            return MIN_SPEECH_PITCH + progress;
        }
        return progress;
    }

    /**
     * Since we are appending the MIN_SPEECH value to the speech seekbar progress, the
     * speech seekbar progress should be set to (speechValue - MIN_SPEECH value).
     */
    private int getSeekBarProgressFromValue(String preferenceKey, int value) {
        if (preferenceKey.equals(KEY_DEFAULT_RATE)) {
            return value - MIN_SPEECH_RATE;
        } else if (preferenceKey.equals(KEY_DEFAULT_PITCH)) {
            return value - MIN_SPEECH_PITCH;
        }
        return value;
    }

    /**
     * Called when the TTS engine is initialized.
     */
    public void onInitEngine(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (DBG) Log.d(TAG, "TTS engine for settings screen initialized.");
            checkDefaultLocale();
        } else {
            if (DBG) Log.d(TAG, "TTS engine for settings screen failed to initialize successfully.");
            updateWidgetState(false);
        }
    }

    private void checkDefaultLocale() {
        Locale defaultLocale = mTts.getDefaultLanguage();
        if (defaultLocale == null) {
            Log.e(TAG, "Failed to get default language from engine " + mCurrentEngine);
            updateWidgetState(false);
            updateEngineStatus(R.string.tts_status_not_supported);
            return;
        }

        // ISO-3166 alpha 3 country codes are out of spec. If we won't normalize,
        // we may end up with English (USA)and German (DEU).
        final Locale oldDefaultLocale = mCurrentDefaultLocale;
        mCurrentDefaultLocale = mEnginesHelper.parseLocaleString(defaultLocale.toString());
        if (!Objects.equals(oldDefaultLocale, mCurrentDefaultLocale)) {
            mSampleText = null;
        }

        int defaultAvailable = mTts.setLanguage(defaultLocale);
        if (evaluateDefaultLocale() && mSampleText == null) {
            getSampleText();
        }
    }

    private boolean evaluateDefaultLocale() {
        // Check if we are connected to the engine, and CHECK_VOICE_DATA returned list
        // of available languages.
        if (mCurrentDefaultLocale == null || mAvailableStrLocals == null) {
            return false;
        }

        boolean notInAvailableLangauges = true;
        try {
            // Check if language is listed in CheckVoices Action result as available voice.
            String defaultLocaleStr = mCurrentDefaultLocale.getISO3Language();
            if (!TextUtils.isEmpty(mCurrentDefaultLocale.getISO3Country())) {
                defaultLocaleStr += "-" + mCurrentDefaultLocale.getISO3Country();
            }
            if (!TextUtils.isEmpty(mCurrentDefaultLocale.getVariant())) {
                defaultLocaleStr += "-" + mCurrentDefaultLocale.getVariant();
            }

            for (String loc : mAvailableStrLocals) {
                if (loc.equalsIgnoreCase(defaultLocaleStr)) {
                  notInAvailableLangauges = false;
                  break;
                }
            }
        } catch (MissingResourceException e) {
            if (DBG) Log.wtf(TAG, "MissingResourceException", e);
            updateEngineStatus(R.string.tts_status_not_supported);
            updateWidgetState(false);
            return false;
        }

        int defaultAvailable = mTts.setLanguage(mCurrentDefaultLocale);
        if (defaultAvailable == TextToSpeech.LANG_NOT_SUPPORTED ||
                defaultAvailable == TextToSpeech.LANG_MISSING_DATA ||
                notInAvailableLangauges) {
            if (DBG) Log.d(TAG, "Default locale for this TTS engine is not supported.");
            updateEngineStatus(R.string.tts_status_not_supported);
            updateWidgetState(false);
            return false;
        } else {
            if (isNetworkRequiredForSynthesis()) {
                updateEngineStatus(R.string.tts_status_requires_network);
            } else {
                updateEngineStatus(R.string.tts_status_ok);
            }
            updateWidgetState(true);
            return true;
        }
    }

    /**
     * Ask the current default engine to return a string of sample text to be
     * spoken to the user.
     */
    private void getSampleText() {
        String currentEngine = mTts.getCurrentEngine();

        if (TextUtils.isEmpty(currentEngine)) currentEngine = mTts.getDefaultEngine();

        // TODO: This is currently a hidden private API. The intent extras
        // and the intent action should be made public if we intend to make this
        // a public API. We fall back to using a canned set of strings if this
        // doesn't work.
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_GET_SAMPLE_TEXT);

        intent.putExtra("language", mCurrentDefaultLocale.getLanguage());
        intent.putExtra("country", mCurrentDefaultLocale.getCountry());
        intent.putExtra("variant", mCurrentDefaultLocale.getVariant());
        intent.setPackage(currentEngine);

        try {
            if (DBG) Log.d(TAG, "Getting sample text: " + intent.toUri(0));
            startActivityForResult(intent, GET_SAMPLE_TEXT);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Failed to get sample text, no activity found for " + intent + ")");
        }
    }

    /**
     * Called when voice data integrity check returns
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_SAMPLE_TEXT) {
            onSampleTextReceived(resultCode, data);
        } else if (requestCode == VOICE_DATA_INTEGRITY_CHECK) {
            onVoiceDataIntegrityCheckDone(data);
        }
    }

    private String getDefaultSampleString() {
        if (mTts != null && mTts.getLanguage() != null) {
            try {
                final String currentLang = mTts.getLanguage().getISO3Language();
                String[] strings = getActivity().getResources().getStringArray(
                        R.array.tts_demo_strings);
                String[] langs = getActivity().getResources().getStringArray(
                        R.array.tts_demo_string_langs);

                for (int i = 0; i < strings.length; ++i) {
                    if (langs[i].equals(currentLang)) {
                        return strings[i];
                    }
                }
            } catch (MissingResourceException e) {
                if (DBG) Log.wtf(TAG, "MissingResourceException", e);
                // Ignore and fall back to default sample string
            }
        }
        return getString(R.string.tts_default_sample_string);
    }

    private boolean isNetworkRequiredForSynthesis() {
        Set<String> features = mTts.getFeatures(mCurrentDefaultLocale);
        if (features == null) {
          return false;
        }
        return features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) &&
                !features.contains(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
    }

    private void onSampleTextReceived(int resultCode, Intent data) {
        String sample = getDefaultSampleString();

        if (resultCode == TextToSpeech.LANG_AVAILABLE && data != null) {
            if (data != null && data.getStringExtra("sampleText") != null) {
                sample = data.getStringExtra("sampleText");
            }
            if (DBG) Log.d(TAG, "Got sample text: " + sample);
        } else {
            if (DBG) Log.d(TAG, "Using default sample text :" + sample);
        }

        mSampleText = sample;
        if (mSampleText != null) {
            updateWidgetState(true);
        } else {
            Log.e(TAG, "Did not have a sample string for the requested language. Using default");
        }
    }

    private void speakSampleText() {
        final boolean networkRequired = isNetworkRequiredForSynthesis();
        if (!networkRequired || networkRequired &&
                (mTts.isLanguageAvailable(mCurrentDefaultLocale) >= TextToSpeech.LANG_AVAILABLE)) {
            HashMap<String, String> params = new HashMap<String, String>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Sample");

            mTts.speak(mSampleText, TextToSpeech.QUEUE_FLUSH, params);
        } else {
            Log.w(TAG, "Network required for sample synthesis for requested language");
            displayNetworkAlert();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (KEY_DEFAULT_RATE.equals(preference.getKey())) {
            updateSpeechRate((Integer) objValue);
        } else if (KEY_DEFAULT_PITCH.equals(preference.getKey())) {
            updateSpeechPitchValue((Integer) objValue);
        }
        return true;
    }

    /**
     * Called when mPlayExample, mResetSpeechRate or mResetSpeechPitch is
     * clicked.
     */
    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mPlayExample) {
            // Get the sample text from the TTS engine; onActivityResult will do
            // the actual speaking
            speakSampleText();
            return true;
        } else if (preference == mResetSpeechRate) {
          int speechRateSeekbarProgress = getSeekBarProgressFromValue(
              KEY_DEFAULT_RATE, TextToSpeech.Engine.DEFAULT_RATE);
          mDefaultRatePref.setProgress(speechRateSeekbarProgress);
          updateSpeechRate(speechRateSeekbarProgress);
          return true;
        } else if (preference == mResetSpeechPitch) {
          int pitchSeekbarProgress = getSeekBarProgressFromValue(
              KEY_DEFAULT_PITCH, TextToSpeech.Engine.DEFAULT_PITCH);
          mDefaultPitchPref.setProgress(pitchSeekbarProgress);
          updateSpeechPitchValue(pitchSeekbarProgress);
          return true;
        }
        return false;
    }

    private void updateSpeechRate(int speechRateSeekBarProgress) {
        mDefaultRate = getValueFromSeekBarProgress(KEY_DEFAULT_RATE,
            speechRateSeekBarProgress);
        try {
            android.provider.Settings.Secure.putInt(getContentResolver(),
                    TTS_DEFAULT_RATE, mDefaultRate);
            if (mTts != null) {
                mTts.setSpeechRate(mDefaultRate / 100.0f);
            }
            if (DBG) Log.d(TAG, "TTS default rate changed, now " + mDefaultRate);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist default TTS rate setting", e);
        }
        return;
    }

    private void updateSpeechPitchValue(int speechPitchSeekBarProgress) {
        mDefaultPitch = getValueFromSeekBarProgress(KEY_DEFAULT_PITCH,
            speechPitchSeekBarProgress);
        try {
            android.provider.Settings.Secure.putInt(getContentResolver(),
                    TTS_DEFAULT_PITCH, mDefaultPitch);
            if (mTts != null) {
                mTts.setPitch(mDefaultPitch / 100.0f);
            }
            if (DBG) Log.d(TAG, "TTS default pitch changed, now" + mDefaultPitch);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist default TTS pitch setting", e);
        }
        return;
    }

    private void updateWidgetState(boolean enable) {
        mPlayExample.setEnabled(enable);
        mDefaultRatePref.setEnabled(enable);
        mEngineStatus.setEnabled(enable);
    }

    private void updateEngineStatus(int resourceId) {
        Locale locale = mCurrentDefaultLocale;
        if (locale == null) {
            locale = Locale.getDefault();
        }
        mEngineStatus.setSummary(getString(resourceId, locale.getDisplayName()));
    }

    private void displayNetworkAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(android.R.string.dialog_alert_title)
                .setMessage(getActivity().getString(R.string.tts_engine_network_required))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateDefaultEngine(String engine) {
        if (DBG) Log.d(TAG, "Updating default synth to : " + engine);

        // Disable the "play sample text" preference and the speech
        // rate preference while the engine is being swapped.
        updateWidgetState(false);
        updateEngineStatus(R.string.tts_status_checking);

        // Keep track of the previous engine that was being used. So that
        // we can reuse the previous engine.
        //
        // Note that if TextToSpeech#getCurrentEngine is not null, it means at
        // the very least that we successfully bound to the engine service.
        mPreviousEngine = mTts.getCurrentEngine();

        // Step 1: Shut down the existing TTS engine.
        if (mTts != null) {
            try {
                mTts.shutdown();
                mTts = null;
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down TTS engine" + e);
            }
        }

        // Step 2: Connect to the new TTS engine.
        // Step 3 is continued on #onUpdateEngine (below) which is called when
        // the app binds successfully to the engine.
        if (DBG) Log.d(TAG, "Updating engine : Attempting to connect to engine: " + engine);
        mTts = new TextToSpeech(getActivity().getApplicationContext(), mUpdateListener, engine);
        setTtsUtteranceProgressListener();
    }

    /*
     * Step 3: We have now bound to the TTS engine the user requested. We will
     * attempt to check voice data for the engine if we successfully bound to it,
     * or revert to the previous engine if we didn't.
     */
    public void onUpdateEngine(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (DBG) {
                Log.d(TAG, "Updating engine: Successfully bound to the engine: " +
                        mTts.getCurrentEngine());
            }
            checkVoiceData(mTts.getCurrentEngine());
        } else {
            if (DBG) Log.d(TAG, "Updating engine: Failed to bind to engine, reverting.");
            if (mPreviousEngine != null) {
                // This is guaranteed to at least bind, since mPreviousEngine would be
                // null if the previous bind to this engine failed.
                mTts = new TextToSpeech(getActivity().getApplicationContext(), mInitListener,
                        mPreviousEngine);
                setTtsUtteranceProgressListener();
            }
            mPreviousEngine = null;
        }
    }

    /*
     * Step 4: Check whether the voice data for the engine is ok.
     */
    private void checkVoiceData(String engine) {
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        intent.setPackage(engine);
        try {
            if (DBG) Log.d(TAG, "Updating engine: Checking voice data: " + intent.toUri(0));
            startActivityForResult(intent, VOICE_DATA_INTEGRITY_CHECK);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Failed to check TTS data, no activity found for " + intent + ")");
        }
    }

    /*
     * Step 5: The voice data check is complete.
     */
    private void onVoiceDataIntegrityCheckDone(Intent data) {
        final String engine = mTts.getCurrentEngine();

        if (engine == null) {
            Log.e(TAG, "Voice data check complete, but no engine bound");
            return;
        }

        if (data == null){
            Log.e(TAG, "Engine failed voice data integrity check (null return)" +
                    mTts.getCurrentEngine());
            return;
        }

        android.provider.Settings.Secure.putString(getContentResolver(), TTS_DEFAULT_SYNTH, engine);

        mAvailableStrLocals = data.getStringArrayListExtra(
            TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
        if (mAvailableStrLocals == null) {
            Log.e(TAG, "Voice data check complete, but no available voices found");
            // Set mAvailableStrLocals to empty list
            mAvailableStrLocals = new ArrayList<String>();
        }
        if (evaluateDefaultLocale()) {
            getSampleText();
        }

        final int engineCount = mEnginePreferenceCategory.getPreferenceCount();
        for (int i = 0; i < engineCount; ++i) {
            final Preference p = mEnginePreferenceCategory.getPreference(i);
            if (p instanceof TtsEnginePreference) {
                TtsEnginePreference enginePref = (TtsEnginePreference) p;
                if (enginePref.getKey().equals(engine)) {
                    enginePref.setVoiceDataDetails(data);
                    break;
                }
            }
        }
    }

    @Override
    public Checkable getCurrentChecked() {
        return mCurrentChecked;
    }

    @Override
    public String getCurrentKey() {
        return mCurrentEngine;
    }

    @Override
    public void setCurrentChecked(Checkable current) {
        mCurrentChecked = current;
    }

    @Override
    public void setCurrentKey(String key) {
        mCurrentEngine = key;
        updateDefaultEngine(mCurrentEngine);
    }

}
