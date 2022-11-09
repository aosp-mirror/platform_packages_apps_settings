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

import static android.provider.Settings.Secure.TTS_DEFAULT_PITCH;
import static android.provider.Settings.Secure.TTS_DEFAULT_RATE;
import static android.provider.Settings.Secure.TTS_DEFAULT_SYNTH;

import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TtsEngines;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.GearPreference;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.ActionButtonsPreference;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Set;

@SearchIndexable
public class TextToSpeechSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener,
        GearPreference.OnGearClickListener {

    private static final String STATE_KEY_LOCALE_ENTRIES = "locale_entries";
    private static final String STATE_KEY_LOCALE_ENTRY_VALUES = "locale_entry_values";
    private static final String STATE_KEY_LOCALE_VALUE = "locale_value";

    private static final String TAG = "TextToSpeechSettings";
    private static final boolean DBG = false;

    /** Preference key for the TTS pitch selection slider. */
    private static final String KEY_DEFAULT_PITCH = "tts_default_pitch";

    /** Preference key for the TTS rate selection slider. */
    private static final String KEY_DEFAULT_RATE = "tts_default_rate";

    /** Engine picker. */
    private static final String KEY_TTS_ENGINE_PREFERENCE = "tts_engine_preference";

    /** Locale picker. */
    private static final String KEY_ENGINE_LOCALE = "tts_default_lang";

    /** Play/Reset buttons container. */
    private static final String KEY_ACTION_BUTTONS = "action_buttons";

    /**
     * These look like birth years, but they aren't mine. I'm much younger than this.
     */
    private static final int GET_SAMPLE_TEXT = 1983;
    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;

    /**
     * Speech rate value. This value should be kept in sync with the max value set in tts_settings
     * xml.
     */
    private static final int MAX_SPEECH_RATE = 600;

    private static final int MIN_SPEECH_RATE = 10;

    /**
     * Speech pitch value. TTS pitch value varies from 25 to 400, where 100 is the value for normal
     * pitch. The max pitch value is set to 400, based on feedback from users and the GoogleTTS
     * pitch variation range. The range for pitch is not set in stone and should be readjusted based
     * on user need. This value should be kept in sync with the max value set in tts_settings xml.
     */
    private static final int MAX_SPEECH_PITCH = 400;

    private static final int MIN_SPEECH_PITCH = 25;

    private SeekBarPreference mDefaultPitchPref;
    private SeekBarPreference mDefaultRatePref;
    private ActionButtonsPreference mActionButtons;

    private int mDefaultPitch = TextToSpeech.Engine.DEFAULT_PITCH;
    private int mDefaultRate = TextToSpeech.Engine.DEFAULT_RATE;

    private int mSelectedLocaleIndex = -1;

    /** The currently selected engine. */
    private String mCurrentEngine;

    private TextToSpeech mTts = null;
    private TtsEngines mEnginesHelper = null;

    private String mSampleText = null;

    private ListPreference mLocalePreference;

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
    private final TextToSpeech.OnInitListener mInitListener = this::onInitEngine;

    /**
     * A UserManager used to set settings for both person and work profiles for a user
     */
    private UserManager mUserManager;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TTS_TEXT_TO_SPEECH;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tts_settings);

        getActivity().setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);

        mEnginesHelper = new TtsEngines(getActivity().getApplicationContext());

        mLocalePreference = (ListPreference) findPreference(KEY_ENGINE_LOCALE);
        mLocalePreference.setOnPreferenceChangeListener(this);

        mDefaultPitchPref = (SeekBarPreference) findPreference(KEY_DEFAULT_PITCH);
        mDefaultRatePref = (SeekBarPreference) findPreference(KEY_DEFAULT_RATE);

        mActionButtons = ((ActionButtonsPreference) findPreference(KEY_ACTION_BUTTONS))
                .setButton1Text(R.string.tts_play)
                .setButton1OnClickListener(v -> speakSampleText())
                .setButton1Enabled(false)
                .setButton2Text(R.string.tts_reset)
                .setButton2OnClickListener(v -> resetTts())
                .setButton1Enabled(true);

        mUserManager = (UserManager) getActivity()
                .getApplicationContext().getSystemService(Context.USER_SERVICE);

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
            final CharSequence value = savedInstanceState.getCharSequence(STATE_KEY_LOCALE_VALUE);

            mLocalePreference.setEntries(entries);
            mLocalePreference.setEntryValues(entryValues);
            mLocalePreference.setValue(value != null ? value.toString() : null);
            mLocalePreference.setEnabled(entries.length > 0);
        }

        final TextToSpeechViewModel ttsViewModel =
                ViewModelProviders.of(this).get(TextToSpeechViewModel.class);
        Pair<TextToSpeech, Boolean> ttsAndNew = ttsViewModel.getTtsAndWhetherNew(mInitListener);
        mTts = ttsAndNew.first;
        // If the TTS object is not newly created, we need to run the setup on the settings side to
        // ensure that we can use the TTS object.
        if (!ttsAndNew.second) {
            successSetup();
        }

        setTtsUtteranceProgressListener();
        initSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        // We tend to change the summary contents of our widgets, which at higher text sizes causes
        // them to resize, which results in the recyclerview smoothly animating them at inopportune
        // times. Disable the animation so widgets snap to their positions rather than sliding
        // around while the user is interacting with it.
        if (getListView().getItemAnimator() != null) {
            getListView().getItemAnimator().setMoveDuration(0);
        }

        if (mTts == null || mCurrentDefaultLocale == null) {
            return;
        }
        if (!mTts.getDefaultEngine().equals(mTts.getCurrentEngine())) {
            final TextToSpeechViewModel ttsViewModel =
                    ViewModelProviders.of(this).get(TextToSpeechViewModel.class);
            try {
                // If the current engine isn't the default engine shut down the current engine in
                // preparation for creating the new engine.
                ttsViewModel.shutdownTts();
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down TTS engine" + e);
            }
            final Pair<TextToSpeech, Boolean> ttsAndNew =
                    ttsViewModel.getTtsAndWhetherNew(mInitListener);
            mTts = ttsAndNew.first;
            if (!ttsAndNew.second) {
                successSetup();
            }
            setTtsUtteranceProgressListener();
            initSettings();
        } else {
            // Do set pitch correctly after it may have changed, and unlike speed, it doesn't change
            // immediately.
            final ContentResolver resolver = getContentResolver();
            mTts.setPitch(android.provider.Settings.Secure.getInt(resolver, TTS_DEFAULT_PITCH,
                    TextToSpeech.Engine.DEFAULT_PITCH) / 100.0f);
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
            public void onStart(String utteranceId) {
                updateWidgetState(false);
            }

            @Override
            public void onDone(String utteranceId) {
                updateWidgetState(true);
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "Error while trying to synthesize sample text");
                // Re-enable just in case, although there isn't much hope that following synthesis
                // requests are going to succeed.
                updateWidgetState(true);
            }
        });
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

    private void initSettings() {
        final ContentResolver resolver = getContentResolver();

        // Set up the default rate and pitch.
        mDefaultRate =
                android.provider.Settings.Secure.getInt(
                        resolver, TTS_DEFAULT_RATE, TextToSpeech.Engine.DEFAULT_RATE);
        mDefaultPitch =
                android.provider.Settings.Secure.getInt(
                        resolver, TTS_DEFAULT_PITCH, TextToSpeech.Engine.DEFAULT_PITCH);

        mDefaultRatePref.setProgress(getSeekBarProgressFromValue(KEY_DEFAULT_RATE, mDefaultRate));
        mDefaultRatePref.setOnPreferenceChangeListener(this);
        mDefaultRatePref.setMax(getSeekBarProgressFromValue(KEY_DEFAULT_RATE, MAX_SPEECH_RATE));
        mDefaultRatePref.setContinuousUpdates(true);
        mDefaultRatePref.setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS);

        mDefaultPitchPref.setProgress(
                getSeekBarProgressFromValue(KEY_DEFAULT_PITCH, mDefaultPitch));
        mDefaultPitchPref.setOnPreferenceChangeListener(this);
        mDefaultPitchPref.setMax(getSeekBarProgressFromValue(KEY_DEFAULT_PITCH, MAX_SPEECH_PITCH));
        mDefaultPitchPref.setContinuousUpdates(true);
        mDefaultPitchPref.setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS);

        if (mTts != null) {
            mCurrentEngine = mTts.getCurrentEngine();
            mTts.setSpeechRate(mDefaultRate / 100.0f);
            mTts.setPitch(mDefaultPitch / 100.0f);
        }

        SettingsActivity activity = null;
        if (getActivity() instanceof SettingsActivity) {
            activity = (SettingsActivity) getActivity();
        } else {
            throw new IllegalStateException("TextToSpeechSettings used outside a " +
                    "Settings");
        }

        if (mCurrentEngine != null) {
            EngineInfo info = mEnginesHelper.getEngineInfo(mCurrentEngine);

            Preference mEnginePreference = findPreference(KEY_TTS_ENGINE_PREFERENCE);
            ((GearPreference) mEnginePreference).setOnGearClickListener(this);
            mEnginePreference.setSummary(info.label);
        }

        checkVoiceData(mCurrentEngine);
    }

    /**
     * The minimum speech pitch/rate value should be > 0 but the minimum value of a seekbar in
     * android is fixed at 0. Therefore, we increment the seekbar progress with MIN_SPEECH_VALUE so
     * that the minimum seekbar progress value is MIN_SPEECH_PITCH/RATE. SPEECH_VALUE =
     * MIN_SPEECH_VALUE + SEEKBAR_PROGRESS
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
     * Since we are appending the MIN_SPEECH value to the speech seekbar progress, the speech
     * seekbar progress should be set to (speechValue - MIN_SPEECH value).
     */
    private int getSeekBarProgressFromValue(String preferenceKey, int value) {
        if (preferenceKey.equals(KEY_DEFAULT_RATE)) {
            return value - MIN_SPEECH_RATE;
        } else if (preferenceKey.equals(KEY_DEFAULT_PITCH)) {
            return value - MIN_SPEECH_PITCH;
        }
        return value;
    }

    /** Called when the TTS engine is initialized. */
    public void onInitEngine(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (DBG) Log.d(TAG, "TTS engine for settings screen initialized.");
            successSetup();
        } else {
            if (DBG) {
                Log.d(TAG,
                        "TTS engine for settings screen failed to initialize successfully.");
            }
            updateWidgetState(false);
        }
    }

    /** Initialize TTS Settings on successful engine init. */
    private void successSetup() {
        checkDefaultLocale();
        getActivity().runOnUiThread(() -> mLocalePreference.setEnabled(true));
    }

    private void checkDefaultLocale() {
        Locale defaultLocale = mTts.getDefaultLanguage();
        if (defaultLocale == null) {
            Log.e(TAG, "Failed to get default language from engine " + mCurrentEngine);
            updateWidgetState(false);
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
            updateWidgetState(false);
            return false;
        }

        int defaultAvailable = mTts.setLanguage(mCurrentDefaultLocale);
        if (defaultAvailable == TextToSpeech.LANG_NOT_SUPPORTED ||
                defaultAvailable == TextToSpeech.LANG_MISSING_DATA ||
                notInAvailableLangauges) {
            if (DBG) Log.d(TAG, "Default locale for this TTS engine is not supported.");
            updateWidgetState(false);
            return false;
        } else {
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
            if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL) {
                updateDefaultLocalePref(data);
            }
        }
    }

    private void updateDefaultLocalePref(Intent data) {
        final ArrayList<String> availableLangs =
                data.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);

        final ArrayList<String> unavailableLangs =
                data.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES);

        if (availableLangs == null || availableLangs.size() == 0) {
            mLocalePreference.setEnabled(false);
            return;
        }
        Locale currentLocale = null;
        if (!mEnginesHelper.isLocaleSetToDefaultForEngine(mTts.getCurrentEngine())) {
            currentLocale = mEnginesHelper.getLocalePrefForEngine(mTts.getCurrentEngine());
        }

        ArrayList<Pair<String, Locale>> entryPairs =
                new ArrayList<Pair<String, Locale>>(availableLangs.size());
        for (int i = 0; i < availableLangs.size(); i++) {
            Locale locale = mEnginesHelper.parseLocaleString(availableLangs.get(i));
            if (locale != null) {
                entryPairs.add(new Pair<String, Locale>(locale.getDisplayName(), locale));
            }
        }

        // Get the primary locale and create a Collator to sort the strings
        Locale userLocale = getResources().getConfiguration().getLocales().get(0);
        Collator collator = Collator.getInstance(userLocale);

        // Sort the list
        Collections.sort(entryPairs, (lhs, rhs) -> collator.compare(lhs.first, rhs.first));

        // Get two arrays out of one of pairs
        mSelectedLocaleIndex = 0; // Will point to the R.string.tts_lang_use_system value
        CharSequence[] entries = new CharSequence[availableLangs.size() + 1];
        CharSequence[] entryValues = new CharSequence[availableLangs.size() + 1];

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
        } else if (preference == mLocalePreference) {
            String localeString = (String) objValue;
            updateLanguageTo(
                    (!TextUtils.isEmpty(localeString)
                            ? mEnginesHelper.parseLocaleString(localeString)
                            : null));
            checkDefaultLocale();
            return true;
        }
        return true;
    }

    private void updateLanguageTo(Locale locale) {
        int selectedLocaleIndex = -1;
        String localeString = (locale != null) ? locale.toString() : "";
        for (int i = 0; i < mLocalePreference.getEntryValues().length; i++) {
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

        mEnginesHelper.updateLocalePrefForEngine(mTts.getCurrentEngine(), locale);

        // Null locale means "use system default"
        mTts.setLanguage((locale != null) ? locale : Locale.getDefault());
    }

    private void resetTts() {
        // Reset button.
        int speechRateSeekbarProgress =
                getSeekBarProgressFromValue(
                        KEY_DEFAULT_RATE, TextToSpeech.Engine.DEFAULT_RATE);
        mDefaultRatePref.setProgress(speechRateSeekbarProgress);
        updateSpeechRate(speechRateSeekbarProgress);
        int pitchSeekbarProgress =
                getSeekBarProgressFromValue(
                        KEY_DEFAULT_PITCH, TextToSpeech.Engine.DEFAULT_PITCH);
        mDefaultPitchPref.setProgress(pitchSeekbarProgress);
        updateSpeechPitchValue(pitchSeekbarProgress);
    }

    private void updateSpeechRate(int speechRateSeekBarProgress) {
        mDefaultRate = getValueFromSeekBarProgress(KEY_DEFAULT_RATE, speechRateSeekBarProgress);
        try {
            updateTTSSetting(TTS_DEFAULT_RATE, mDefaultRate);
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
        mDefaultPitch = getValueFromSeekBarProgress(KEY_DEFAULT_PITCH, speechPitchSeekBarProgress);
        try {
            updateTTSSetting(TTS_DEFAULT_PITCH, mDefaultPitch);
            if (mTts != null) {
                mTts.setPitch(mDefaultPitch / 100.0f);
            }
            if (DBG) Log.d(TAG, "TTS default pitch changed, now" + mDefaultPitch);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist default TTS pitch setting", e);
        }
        return;
    }

    private void updateTTSSetting(String key, int value) {
        Secure.putInt(getContentResolver(), key, value);
        final int managedProfileUserId =
                Utils.getManagedProfileId(mUserManager, UserHandle.myUserId());
        if (managedProfileUserId != UserHandle.USER_NULL) {
            Secure.putIntForUser(getContentResolver(), key, value, managedProfileUserId);
        }
    }

    private void updateWidgetState(boolean enable) {
        getActivity().runOnUiThread(() -> {
            mActionButtons.setButton1Enabled(enable);
            mDefaultRatePref.setEnabled(enable);
            mDefaultPitchPref.setEnabled(enable);
        });
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

    /** Check whether the voice data for the engine is ok. */
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

    /** The voice data check is complete. */
    private void onVoiceDataIntegrityCheckDone(Intent data) {
        final String engine = mTts.getCurrentEngine();

        if (engine == null) {
            Log.e(TAG, "Voice data check complete, but no engine bound");
            return;
        }

        if (data == null) {
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
    }

    @Override
    public void onGearClick(GearPreference p) {
        if (KEY_TTS_ENGINE_PREFERENCE.equals(p.getKey())) {
            EngineInfo info = mEnginesHelper.getEngineInfo(mCurrentEngine);
            final Intent settingsIntent = mEnginesHelper.getSettingsIntent(info.name);
            if (settingsIntent != null) {
                startActivity(settingsIntent);
            } else {
                Log.e(TAG, "settingsIntent is null");
            }
            FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider()
                    .logClickedPreference(p, getMetricsCategory());
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.tts_settings);

}
