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

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.speech.RecognitionService;
import android.speech.tts.TtsEngines;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Settings screen for voice input/output.
 */
public class VoiceInputOutputSettings implements OnPreferenceChangeListener {

    private static final String TAG = "VoiceInputOutputSettings";

    private static final String KEY_VOICE_CATEGORY = "voice_category";
    private static final String KEY_RECOGNIZER = "recognizer";
    private static final String KEY_RECOGNIZER_SETTINGS = "recognizer_settings";
    private static final String KEY_TTS_SETTINGS = "tts_settings";

    private PreferenceGroup mParent;
    private PreferenceCategory mVoiceCategory;
    private ListPreference mRecognizerPref;
    private Preference mRecognizerSettingsPref;
    private Preference mTtsSettingsPref;
    private PreferenceScreen mSettingsPref;
    private final SettingsPreferenceFragment mFragment;
    private final TtsEngines mTtsEngines;

    private HashMap<String, ResolveInfo> mAvailableRecognizersMap;

    public VoiceInputOutputSettings(SettingsPreferenceFragment fragment) {
        mFragment = fragment;
        mTtsEngines = new TtsEngines(fragment.getPreferenceScreen().getContext());
    }

    public void onCreate() {

        mParent = mFragment.getPreferenceScreen();
        mVoiceCategory = (PreferenceCategory) mParent.findPreference(KEY_VOICE_CATEGORY);
        mRecognizerPref = (ListPreference) mVoiceCategory.findPreference(KEY_RECOGNIZER);
        mRecognizerSettingsPref = mVoiceCategory.findPreference(KEY_RECOGNIZER_SETTINGS);
        mTtsSettingsPref = mVoiceCategory.findPreference(KEY_TTS_SETTINGS);
        mRecognizerPref.setOnPreferenceChangeListener(this);
        mSettingsPref = (PreferenceScreen)
                mVoiceCategory.findPreference(KEY_RECOGNIZER_SETTINGS);

        mAvailableRecognizersMap = new HashMap<String, ResolveInfo>();

        populateOrRemovePreferences();
    }

    private void populateOrRemovePreferences() {
        boolean hasRecognizerPrefs = populateOrRemoveRecognizerPrefs();
        boolean hasTtsPrefs = populateOrRemoveTtsPrefs();
        if (!hasRecognizerPrefs && !hasTtsPrefs) {
            // There were no TTS settings and no recognizer settings,
            // so it should be safe to hide the preference category
            // entirely.
            mFragment.getPreferenceScreen().removePreference(mVoiceCategory);
        }
    }

    private boolean populateOrRemoveRecognizerPrefs() {
        List<ResolveInfo> availableRecognitionServices =
                mFragment.getPackageManager().queryIntentServices(
                        new Intent(RecognitionService.SERVICE_INTERFACE),
                        PackageManager.GET_META_DATA);
        int numAvailable = availableRecognitionServices.size();

        if (numAvailable == 0) {
            mVoiceCategory.removePreference(mRecognizerPref);
            mVoiceCategory.removePreference(mRecognizerSettingsPref);
            return false;
        }

        if (numAvailable == 1) {
            // Only one recognizer available, so don't show the list of choices, but do
            // set up the link to settings for the available recognizer.
            mVoiceCategory.removePreference(mRecognizerPref);

            // But first set up the available recognizers map with just the one recognizer.
            ResolveInfo resolveInfo = availableRecognitionServices.get(0);
            String recognizerComponent =
                new ComponentName(resolveInfo.serviceInfo.packageName,
                        resolveInfo.serviceInfo.name).flattenToShortString();

            mAvailableRecognizersMap.put(recognizerComponent, resolveInfo);

            String currentSetting = Settings.Secure.getString(
                    mFragment.getContentResolver(), Settings.Secure.VOICE_RECOGNITION_SERVICE);
            updateSettingsLink(currentSetting);
        } else {
            // Multiple recognizers available, so show the full list of choices.
            populateRecognizerPreference(availableRecognitionServices);
        }

        // In this case, there was at least one available recognizer so
        // we populated the settings.
        return true;
    }

    private boolean populateOrRemoveTtsPrefs() {
        if (mTtsEngines.getEngines().isEmpty()) {
            mVoiceCategory.removePreference(mTtsSettingsPref);
            return false;
        }

        return true;
    }

    private void populateRecognizerPreference(List<ResolveInfo> recognizers) {
        int size = recognizers.size();
        CharSequence[] entries = new CharSequence[size];
        CharSequence[] values = new CharSequence[size];
        
        // Get the current value from the secure setting.
        String currentSetting = Settings.Secure.getString(
                mFragment.getContentResolver(), Settings.Secure.VOICE_RECOGNITION_SERVICE);
        
        // Iterate through all the available recognizers and load up their info to show
        // in the preference. Also build up a map of recognizer component names to their
        // ResolveInfos - we'll need that a little later.
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = recognizers.get(i);
            String recognizerComponent =
                    new ComponentName(resolveInfo.serviceInfo.packageName,
                            resolveInfo.serviceInfo.name).flattenToShortString();
            
            mAvailableRecognizersMap.put(recognizerComponent, resolveInfo);

            entries[i] = resolveInfo.loadLabel(mFragment.getPackageManager());
            values[i] = recognizerComponent;
        }
        
        mRecognizerPref.setEntries(entries);
        mRecognizerPref.setEntryValues(values);
        
        mRecognizerPref.setDefaultValue(currentSetting);
        mRecognizerPref.setValue(currentSetting);
        
        updateSettingsLink(currentSetting);
    }
    
    private void updateSettingsLink(String currentSetting) {
        ResolveInfo currentRecognizer = mAvailableRecognizersMap.get(currentSetting);
        if (currentRecognizer == null) return;

        ServiceInfo si = currentRecognizer.serviceInfo;
        XmlResourceParser parser = null;
        String settingsActivity = null;
        try {
            parser = si.loadXmlMetaData(mFragment.getPackageManager(),
                    RecognitionService.SERVICE_META_DATA);
            if (parser == null) {
                throw new XmlPullParserException("No " + RecognitionService.SERVICE_META_DATA +
                        " meta-data for " + si.packageName);
            }
            
            Resources res = mFragment.getPackageManager().getResourcesForApplication(
                    si.applicationInfo);
            
            AttributeSet attrs = Xml.asAttributeSet(parser);
            
            int type;
            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }
            
            String nodeName = parser.getName();
            if (!"recognition-service".equals(nodeName)) {
                throw new XmlPullParserException(
                        "Meta-data does not start with recognition-service tag");
            }
            
            TypedArray array = res.obtainAttributes(attrs,
                    com.android.internal.R.styleable.RecognitionService);
            settingsActivity = array.getString(
                    com.android.internal.R.styleable.RecognitionService_settingsActivity);
            array.recycle();
        } catch (XmlPullParserException e) {
            Log.e(TAG, "error parsing recognition service meta-data", e);
        } catch (IOException e) {
            Log.e(TAG, "error parsing recognition service meta-data", e);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "error parsing recognition service meta-data", e);
        } finally {
            if (parser != null) parser.close();
        }
        
        if (settingsActivity == null) {
            // No settings preference available - hide the preference.
            Log.w(TAG, "no recognizer settings available for " + si.packageName);
            mSettingsPref.setIntent(null);
            mVoiceCategory.removePreference(mSettingsPref);
        } else {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(new ComponentName(si.packageName, settingsActivity));
            mSettingsPref.setIntent(i);
            mRecognizerPref.setSummary(currentRecognizer.loadLabel(mFragment.getPackageManager()));
        }
    }
    
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRecognizerPref) {
            String setting = (String) newValue;

            // Put the new value back into secure settings.
            Settings.Secure.putString(mFragment.getContentResolver(),
                    Settings.Secure.VOICE_RECOGNITION_SERVICE,
                    setting);

            // Update the settings item so it points to the right settings.
            updateSettingsLink(setting);
        }
        return true;
    }
}
