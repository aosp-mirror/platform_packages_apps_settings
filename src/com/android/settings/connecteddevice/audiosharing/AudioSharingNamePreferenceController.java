/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.ValidatedEditTextPreference;

public class AudioSharingNamePreferenceController extends BasePreferenceController
        implements ValidatedEditTextPreference.Validator, Preference.OnPreferenceChangeListener {

    private static final String TAG = "AudioSharingNamePreferenceController";

    private static final String PREF_KEY = "audio_sharing_stream_name";

    private AudioSharingNameTextValidator mAudioSharingNameTextValidator;

    public AudioSharingNamePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mAudioSharingNameTextValidator = new AudioSharingNameTextValidator();
    }

    @Override
    public int getAvailabilityStatus() {
        return AudioSharingUtils.isFeatureEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO: update broadcast when name is changed.
        return true;
    }

    @Override
    public boolean isTextValid(String value) {
        return mAudioSharingNameTextValidator.isTextValid(value);
    }
}
