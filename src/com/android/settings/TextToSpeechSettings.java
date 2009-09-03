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
import static android.provider.Settings.Secure.TTS_DEFAULT_LANG;
import static android.provider.Settings.Secure.TTS_DEFAULT_COUNTRY;
import static android.provider.Settings.Secure.TTS_DEFAULT_VARIANT;
import static android.provider.Settings.Secure.TTS_DEFAULT_SYNTH;

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
import android.provider.Settings.SettingNotFoundException;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class TextToSpeechSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        TextToSpeech.OnInitListener {

    private static final String TAG = "TextToSpeechSettings";

    private static final String KEY_TTS_PLAY_EXAMPLE = "tts_play_example";
    private static final String KEY_TTS_INSTALL_DATA = "tts_install_data";
    private static final String KEY_TTS_USE_DEFAULT = "toggle_use_default_tts_settings";
    private static final String KEY_TTS_DEFAULT_RATE = "tts_default_rate";
    private static final String KEY_TTS_DEFAULT_LANG = "tts_default_lang";
    private static final String KEY_TTS_DEFAULT_COUNTRY = "tts_default_country";
    private static final String KEY_TTS_DEFAULT_VARIANT = "tts_default_variant";

    private static final String LOCALE_DELIMITER = "-";

    private static final String FALLBACK_TTS_DEFAULT_SYNTH =
            TextToSpeech.Engine.DEFAULT_SYNTH;

    private Preference         mPlayExample = null;
    private Preference         mInstallData = null;
    private CheckBoxPreference mUseDefaultPref = null;
    private ListPreference     mDefaultRatePref = null;
    private ListPreference     mDefaultLocPref = null;
    private String             mDefaultLanguage = null;
    private String             mDefaultCountry = null;
    private String             mDefaultLocVariant = null;
    private String             mDefaultEng = "";

    private boolean mEnableDemo = false;

    private TextToSpeech mTts = null;

    /**
     * Request code (arbitrary value) for voice data check through
     * startActivityForResult.
     */
    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;
    /**
     * Request code (arbitrary value) for voice data installation through
     * startActivityForResult.
     */
    private static final int VOICE_DATA_INSTALLATION = 1980;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.tts_settings);

        setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);

        mEnableDemo = false;
        initClickers();
        initDefaultSettings();
    }


    @Override
    protected void onStart() {
        super.onStart();
        // whenever we return to this screen, we don't know the state of the
        // system, so we have to recheck that we can play the demo, or it must be disabled.
        // TODO make the TTS service listen to "changes in the system", i.e. sd card un/mount
        initClickers();
        updateWidgetState();
        checkVoiceData();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTts != null) {
            mTts.shutdown();
        }
    }


    private void initClickers() {
        mPlayExample = findPreference(KEY_TTS_PLAY_EXAMPLE);
        mPlayExample.setOnPreferenceClickListener(this);

        mInstallData = findPreference(KEY_TTS_INSTALL_DATA);
        mInstallData.setOnPreferenceClickListener(this);
    }


    private void initDefaultSettings() {
        ContentResolver resolver = getContentResolver();
        int intVal = 0;

        // Find the default TTS values in the settings, initialize and store the
        // settings if they are not found.

        // "Use Defaults"
        mUseDefaultPref = (CheckBoxPreference) findPreference(KEY_TTS_USE_DEFAULT);
        try {
            intVal = Settings.Secure.getInt(resolver, TTS_USE_DEFAULTS);
        } catch (SettingNotFoundException e) {
            // "use default" setting not found, initialize it
            intVal = TextToSpeech.Engine.USE_DEFAULTS;
            Settings.Secure.putInt(resolver, TTS_USE_DEFAULTS, intVal);
        }
        mUseDefaultPref.setChecked(intVal == 1);
        mUseDefaultPref.setOnPreferenceChangeListener(this);

        // Default engine
        String engine = Settings.Secure.getString(resolver, TTS_DEFAULT_SYNTH);
        if (engine == null) {
            // TODO move FALLBACK_TTS_DEFAULT_SYNTH to TextToSpeech
            engine = FALLBACK_TTS_DEFAULT_SYNTH;
            Settings.Secure.putString(resolver, TTS_DEFAULT_SYNTH, engine);
        }
        mDefaultEng = engine;

        // Default rate
        mDefaultRatePref = (ListPreference) findPreference(KEY_TTS_DEFAULT_RATE);
        try {
            intVal = Settings.Secure.getInt(resolver, TTS_DEFAULT_RATE);
        } catch (SettingNotFoundException e) {
            // default rate setting not found, initialize it
            intVal = TextToSpeech.Engine.DEFAULT_RATE;
            Settings.Secure.putInt(resolver, TTS_DEFAULT_RATE, intVal);
        }
        mDefaultRatePref.setValue(String.valueOf(intVal));
        mDefaultRatePref.setOnPreferenceChangeListener(this);

        // Default language / country / variant : these three values map to a single ListPref
        // representing the matching Locale
        String language = null;
        String country = null;
        String variant = null;
        mDefaultLocPref = (ListPreference) findPreference(KEY_TTS_DEFAULT_LANG);
        language = Settings.Secure.getString(resolver, TTS_DEFAULT_LANG);
        if (language == null) {
            // the default language property isn't set, use that of the current locale
            Locale currentLocale = Locale.getDefault();
            language = currentLocale.getISO3Language();
            country = currentLocale.getISO3Country();
            variant = currentLocale.getVariant();
            Settings.Secure.putString(resolver, TTS_DEFAULT_LANG, language);
            Settings.Secure.putString(resolver, TTS_DEFAULT_COUNTRY, country);
            Settings.Secure.putString(resolver, TTS_DEFAULT_VARIANT, variant);
        }
        mDefaultLanguage = language;
        if (country == null) {
            // country wasn't initialized yet because a default language was found
            country = Settings.Secure.getString(resolver, KEY_TTS_DEFAULT_COUNTRY);
            if (country == null) {
                // default country setting not found, initialize it, as well as the variant;
                Locale currentLocale = Locale.getDefault();
                country = currentLocale.getISO3Country();
                variant = currentLocale.getVariant();
                Settings.Secure.putString(resolver, TTS_DEFAULT_COUNTRY, country);
                Settings.Secure.putString(resolver, TTS_DEFAULT_VARIANT, variant);
            }
        }
        mDefaultCountry = country;
        if (variant == null) {
            // variant wasn't initialized yet because a default country was found
            variant = Settings.Secure.getString(resolver, KEY_TTS_DEFAULT_VARIANT);
            if (variant == null) {
                // default variant setting not found, initialize it
                Locale currentLocale = Locale.getDefault();
                variant = currentLocale.getVariant();
                Settings.Secure.putString(resolver, TTS_DEFAULT_VARIANT, variant);
            }
        }
        mDefaultLocVariant = variant;

        setDefaultLocalePref(language, country, variant);
        mDefaultLocPref.setOnPreferenceChangeListener(this);
    }


    /**
     * Ask the current default engine to launch the matching CHECK_TTS_DATA activity
     * to check the required TTS files are properly installed.
     */
    private void checkVoiceData() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
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
     * Ask the current default engine to launch the matching INSTALL_TTS_DATA activity
     * so the required TTS files are properly installed.
     */
    private void installVoiceData() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        // query only the package that matches that of the default engine
        for (int i = 0; i < resolveInfos.size(); i++) {
            ActivityInfo currentActivityInfo = resolveInfos.get(i).activityInfo;
            if (mDefaultEng.equals(currentActivityInfo.packageName)) {
                intent.setClassName(mDefaultEng, currentActivityInfo.name);
                this.startActivityForResult(intent, VOICE_DATA_INSTALLATION);
            }
        }
    }


    /**
     * Called when the TTS engine is initialized.
     */
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Log.v(TAG, "TTS engine for settings screen initialized.");
            mEnableDemo = true;
        } else {
            Log.v(TAG, "TTS engine for settings screen failed to initialize successfully.");
            mEnableDemo = false;
        }
        updateWidgetState();
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
                    mTts.setLanguage(Locale.getDefault());
                }
            } else {
                Log.v(TAG, "Voice data check failed");
                mEnableDemo = false;
                updateWidgetState();
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
                if (mTts != null) {
                    mTts.setSpeechRate((float)(value/100.0f));
                }
                Log.i(TAG, "TTS default rate is "+value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist default TTS rate setting", e);
            }
        } else if (KEY_TTS_DEFAULT_LANG.equals(preference.getKey())) {
            // Default locale
            ContentResolver resolver = getContentResolver();
            parseLocaleInfo((String) objValue);
            Settings.Secure.putString(resolver, TTS_DEFAULT_LANG, mDefaultLanguage);
            Settings.Secure.putString(resolver, TTS_DEFAULT_COUNTRY, mDefaultCountry);
            Settings.Secure.putString(resolver, TTS_DEFAULT_VARIANT, mDefaultLocVariant);
            Log.v(TAG, "TTS default lang/country/variant set to "
                    + mDefaultLanguage + "/" + mDefaultCountry + "/" + mDefaultLocVariant);
            if (mTts != null) {
                mTts.setLanguage(new Locale(mDefaultLanguage, mDefaultCountry));
            }
        }

        return true;
    }


    /**
     * Called when mPlayExample or mInstallData is clicked
     */
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mPlayExample) {
            // Play example
            if (mTts != null) {
                mTts.speak(getResources().getString(R.string.tts_demo),
                        TextToSpeech.QUEUE_FLUSH, null);
            }
            return true;
        }
        if (preference == mInstallData) {
            installVoiceData();
            // quit this activity so it needs to be restarted after installation of the voice data
            finish();
            return true;
        }
        return false;
    }


    private void updateWidgetState() {
        mPlayExample.setEnabled(mEnableDemo);
        mUseDefaultPref.setEnabled(mEnableDemo);
        mDefaultRatePref.setEnabled(mEnableDemo);
        mDefaultLocPref.setEnabled(mEnableDemo);

        mInstallData.setEnabled(!mEnableDemo);
    }


    private void parseLocaleInfo(String locale) {
        StringTokenizer tokenizer = new StringTokenizer(locale, LOCALE_DELIMITER);
        mDefaultLanguage = "";
        mDefaultCountry = "";
        mDefaultLocVariant = "";
        if (tokenizer.hasMoreTokens()) {
            mDefaultLanguage = tokenizer.nextToken().trim();
        }
        if (tokenizer.hasMoreTokens()) {
            mDefaultCountry = tokenizer.nextToken().trim();
        }
        if (tokenizer.hasMoreTokens()) {
            mDefaultLocVariant = tokenizer.nextToken().trim();
        }
    }


    private void setDefaultLocalePref(String language, String country, String variant) {
        // build a string from the default lang/country/variant trio,
        String localeString = new String(language);
        if (country.compareTo("") != 0) {
            localeString += LOCALE_DELIMITER + country;
        } else {
            localeString += LOCALE_DELIMITER + " ";
        }
        if (variant.compareTo("") != 0) {
            localeString += LOCALE_DELIMITER + variant;
        }

        if (mDefaultLocPref.findIndexOfValue(localeString) > -1) {
            mDefaultLocPref.setValue(localeString);
        } else {
            mDefaultLocPref.setValueIndex(0);
        }

        Log.v(TAG, "In initDefaultSettings: localeString=" + localeString);
    }

}
