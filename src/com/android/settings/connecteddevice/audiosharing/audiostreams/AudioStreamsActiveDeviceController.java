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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

public class AudioStreamsActiveDeviceController extends BasePreferenceController
        implements AudioStreamsActiveDeviceSummaryUpdater.OnSummaryChangeListener,
                DefaultLifecycleObserver {

    public static final String KEY = "audio_streams_active_device";
    private final AudioStreamsActiveDeviceSummaryUpdater mSummaryHelper;
    @Nullable private Preference mPreference;

    public AudioStreamsActiveDeviceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSummaryHelper = new AudioStreamsActiveDeviceSummaryUpdater(mContext, this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onSummaryChanged(String summary) {
        if (mPreference != null) {
            mPreference.setSummary(summary);
        }
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        mSummaryHelper.register(true);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mSummaryHelper.register(false);
    }
}
