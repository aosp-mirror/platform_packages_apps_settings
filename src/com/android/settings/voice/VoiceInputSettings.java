/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.voice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.preference.Preference;
import android.provider.Settings;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionServiceInfo;
import android.speech.RecognitionService;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.voice.VoiceInputPreference.RadioButtonGroupState;

import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.widget.Checkable;

import java.util.ArrayList;
import java.util.List;

public class VoiceInputSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceClickListener, RadioButtonGroupState, Indexable {

    private static final String TAG = "VoiceInputSettings";
    private static final boolean DBG = false;

    /**
     * Preference key for the engine selection preference.
     */
    private static final String KEY_SERVICE_PREFERENCE_SECTION =
            "voice_service_preference_section";

    private PreferenceCategory mServicePreferenceCategory;

    private CharSequence mInteractorSummary;
    private CharSequence mRecognizerSummary;
    private CharSequence mInteractorWarning;

    /**
     * The currently selected engine.
     */
    private String mCurrentKey;

    /**
     * The engine checkbox that is currently checked. Saves us a bit of effort
     * in deducing the right one from the currently selected engine.
     */
    private Checkable mCurrentChecked;

    private VoiceInputHelper mHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.voice_input_settings);

        mServicePreferenceCategory = (PreferenceCategory) findPreference(
                KEY_SERVICE_PREFERENCE_SECTION);

        mInteractorSummary = getActivity().getText(
                R.string.voice_interactor_preference_summary);
        mRecognizerSummary = getActivity().getText(
                R.string.voice_recognizer_preference_summary);
        mInteractorWarning = getActivity().getText(R.string.voice_interaction_security_warning);
    }

    @Override
    public void onStart() {
        super.onStart();
        initSettings();
    }

    private void initSettings() {
        mHelper = new VoiceInputHelper(getActivity());
        mHelper.buildUi();

        mServicePreferenceCategory.removeAll();

        if (mHelper.mCurrentVoiceInteraction != null) {
            mCurrentKey = mHelper.mCurrentVoiceInteraction.flattenToShortString();
        } else if (mHelper.mCurrentRecognizer != null) {
            mCurrentKey = mHelper.mCurrentRecognizer.flattenToShortString();
        } else {
            mCurrentKey = null;
        }

        for (int i=0; i<mHelper.mAvailableInteractionInfos.size(); i++) {
            VoiceInputHelper.InteractionInfo info = mHelper.mAvailableInteractionInfos.get(i);
            VoiceInputPreference pref = new VoiceInputPreference(getActivity(), info,
                    mInteractorSummary, mInteractorWarning, this);
            mServicePreferenceCategory.addPreference(pref);
        }

        for (int i=0; i<mHelper.mAvailableRecognizerInfos.size(); i++) {
            VoiceInputHelper.RecognizerInfo info = mHelper.mAvailableRecognizerInfos.get(i);
            VoiceInputPreference pref = new VoiceInputPreference(getActivity(), info,
                    mRecognizerSummary, null, this);
            mServicePreferenceCategory.addPreference(pref);
        }
    }

    @Override
    public Checkable getCurrentChecked() {
        return mCurrentChecked;
    }

    @Override
    public String getCurrentKey() {
        return mCurrentKey;
    }

    @Override
    public void setCurrentChecked(Checkable current) {
        mCurrentChecked = current;
    }

    @Override
    public void setCurrentKey(String key) {
        mCurrentKey = key;
        for (int i=0; i<mHelper.mAvailableInteractionInfos.size(); i++) {
            VoiceInputHelper.InteractionInfo info = mHelper.mAvailableInteractionInfos.get(i);
            if (info.key.equals(key)) {
                // Put the new value back into secure settings.
                Settings.Secure.putString(getActivity().getContentResolver(),
                        Settings.Secure.VOICE_INTERACTION_SERVICE, key);
                // Eventually we will require that an interactor always specify a recognizer
                if (info.settings != null) {
                    Settings.Secure.putString(getActivity().getContentResolver(),
                            Settings.Secure.VOICE_RECOGNITION_SERVICE,
                            info.settings.flattenToShortString());
                }
                return;
            }
        }

        for (int i=0; i<mHelper.mAvailableRecognizerInfos.size(); i++) {
            VoiceInputHelper.RecognizerInfo info = mHelper.mAvailableRecognizerInfos.get(i);
            if (info.key.equals(key)) {
                Settings.Secure.putString(getActivity().getContentResolver(),
                        Settings.Secure.VOICE_INTERACTION_SERVICE, "");
                Settings.Secure.putString(getActivity().getContentResolver(),
                        Settings.Secure.VOICE_RECOGNITION_SERVICE, key);
                return;
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof VoiceInputPreference) {
            ((VoiceInputPreference)preference).doClick();
        }
        return true;
    }

    // For Search
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                    boolean enabled) {

                List<SearchIndexableRaw> indexables = new ArrayList<>();

                final String screenTitle = context.getString(R.string.voice_input_settings_title);

                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                indexable.key = "voice_service_preference_section_title";
                indexable.title = context.getString(R.string.voice_service_preference_section_title);
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);

                final List<ResolveInfo> voiceInteractions =
                        context.getPackageManager().queryIntentServices(
                                new Intent(VoiceInteractionService.SERVICE_INTERFACE),
                                PackageManager.GET_META_DATA);

                final int countInteractions = voiceInteractions.size();
                for (int i = 0; i < countInteractions; i++) {
                    ResolveInfo info = voiceInteractions.get(i);
                    VoiceInteractionServiceInfo visInfo = new VoiceInteractionServiceInfo(
                            context.getPackageManager(), info.serviceInfo);
                    if (visInfo.getParseError() != null) {
                        continue;
                    }
                    indexables.add(getSearchIndexableRaw(context, info, screenTitle));
                }

                final List<ResolveInfo> recognitions =
                        context.getPackageManager().queryIntentServices(
                                new Intent(RecognitionService.SERVICE_INTERFACE),
                                PackageManager.GET_META_DATA);

                final int countRecognitions = recognitions.size();
                for (int i = 0; i < countRecognitions; i++) {
                    ResolveInfo info = recognitions.get(i);
                    indexables.add(getSearchIndexableRaw(context, info, screenTitle));
                }

                return indexables;
            }

            private SearchIndexableRaw getSearchIndexableRaw(Context context,
                    ResolveInfo info, String screenTitle) {

                ServiceInfo serviceInfo = info.serviceInfo;
                ComponentName componentName = new ComponentName(serviceInfo.packageName,
                        serviceInfo.name);

                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                indexable.key = componentName.flattenToString();
                indexable.title = info.loadLabel(context.getPackageManager()).toString();
                indexable.screenTitle = screenTitle;

                return indexable;
            }
        };
}
