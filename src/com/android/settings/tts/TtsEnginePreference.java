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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.RadioButton;


public class TtsEnginePreference extends Preference {

    /**
     * Key for the name of the TTS engine passed in to the engine
     * settings fragment {@link TtsEngineSettingsFragment}.
     */
    static final String FRAGMENT_ARGS_NAME = "name";

    /**
     * Key for the label of the TTS engine passed in to the engine
     * settings fragment. This is used as the title of the fragment
     * {@link TtsEngineSettingsFragment}.
     */
    static final String FRAGMENT_ARGS_LABEL = "label";

    /**
     * Key for the voice data data passed in to the engine settings
     * fragmetn {@link TtsEngineSettingsFragment}.
     */
    static final String FRAGMENT_ARGS_VOICES = "voices";

    /**
     * The preference activity that owns this preference. Required
     * for instantiating the engine specific settings screen.
     */
    private final PreferenceActivity mPreferenceActivity;

    /**
     * The engine information for the engine this preference represents.
     * Contains it's name, label etc. which are used for display.
     */
    private final EngineInfo mEngineInfo;

    /**
     * The shared radio button state, which button is checked etc.
     */
    private final RadioButtonGroupState mSharedState;

    /**
     * When true, the change callbacks on the radio button will not
     * fire.
     */
    private volatile boolean mPreventRadioButtonCallbacks;

    private View mSettingsIcon;
    private RadioButton mRadioButton;
    private Intent mVoiceCheckData;

    private final CompoundButton.OnCheckedChangeListener mRadioChangeListener =
        new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onRadioButtonClicked(buttonView, isChecked);
            }
        };

    public TtsEnginePreference(Context context, EngineInfo info, RadioButtonGroupState state,
            PreferenceActivity prefActivity) {
        super(context);
        setLayoutResource(R.layout.preference_tts_engine);

        mSharedState = state;
        mPreferenceActivity = prefActivity;
        mEngineInfo = info;
        mPreventRadioButtonCallbacks = false;

        setKey(mEngineInfo.name);
        setTitle(mEngineInfo.label);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        if (mSharedState == null) {
            throw new IllegalStateException("Call to getView() before a call to" +
                    "setSharedState()");
        }

        View view = super.getView(convertView, parent);
        final RadioButton rb = (RadioButton) view.findViewById(R.id.tts_engine_radiobutton);
        rb.setOnCheckedChangeListener(mRadioChangeListener);

        boolean isChecked = getKey().equals(mSharedState.getCurrentKey());
        if (isChecked) {
            mSharedState.setCurrentChecked(rb);
        }

        mPreventRadioButtonCallbacks = true;
        rb.setChecked(isChecked);
        mPreventRadioButtonCallbacks = false;

        mRadioButton = rb;

        View textLayout = view.findViewById(R.id.tts_engine_pref_text);
        textLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRadioButtonClicked(rb, !rb.isChecked());
            }
        });

        mSettingsIcon = view.findViewById(R.id.tts_engine_settings);
        // Will be enabled only the engine has passed the voice check, and
        // is currently enabled.
        mSettingsIcon.setEnabled(isChecked && mVoiceCheckData != null);
        mSettingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putString(FRAGMENT_ARGS_NAME, mEngineInfo.name);
                args.putString(FRAGMENT_ARGS_LABEL, mEngineInfo.label);
                if (mVoiceCheckData != null) {
                    args.putParcelable(FRAGMENT_ARGS_VOICES, mVoiceCheckData);
                }

                // Note that we use this instead of the (easier to use)
                // PreferenceActivity.startPreferenceFragment because the
                // title will not be updated correctly in the fragment
                // breadcrumb since it isn't inflated from the XML layout.
                mPreferenceActivity.startPreferencePanel(
                        TtsEngineSettingsFragment.class.getName(),
                        args, 0, mEngineInfo.label, null, 0);
            }
        });

        if (mVoiceCheckData != null) {
            mSettingsIcon.setEnabled(mRadioButton.isChecked());
        }

        return view;
    }

    public void setVoiceDataDetails(Intent data) {
        mVoiceCheckData = data;
        // This might end up running before getView aboive, in which
        // case mSettingsIcon && mRadioButton will be null. In this case
        // getView will set the right values.
        if (mSettingsIcon != null && mRadioButton != null) {
            mSettingsIcon.setEnabled(mRadioButton.isChecked());
        }
    }

    private void onRadioButtonClicked(CompoundButton buttonView, boolean isChecked) {
        if (mPreventRadioButtonCallbacks) {
            return;
        }

        if (isChecked) {
            if (mSharedState.getCurrentChecked() != null) {
                mSharedState.getCurrentChecked().setChecked(false);
            }
            mSharedState.setCurrentChecked(buttonView);
            mSharedState.setCurrentKey(getKey());
            callChangeListener(mSharedState.getCurrentKey());
        }

        mSettingsIcon.setEnabled(isChecked);
    }


    /**
     * Holds all state that is common to this group of radio buttons, such
     * as the currently selected key and the currently checked compound button.
     * (which corresponds to this key).
     */
    public interface RadioButtonGroupState {
        String getCurrentKey();
        Checkable getCurrentChecked();

        void setCurrentKey(String key);
        void setCurrentChecked(Checkable current);
    }

}
