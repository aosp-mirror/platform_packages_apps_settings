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
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import com.android.settings.ProgressCategory;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AudioStreamsProgressCategoryPreference extends ProgressCategory {

    public AudioStreamsProgressCategoryPreference(Context context) {
        super(context);
        init();
    }

    public AudioStreamsProgressCategoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioStreamsProgressCategoryPreference(
            Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AudioStreamsProgressCategoryPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    void addAudioStreamPreference(
            @NonNull AudioStreamPreference preference,
            Comparator<AudioStreamPreference> comparator) {
        super.addPreference(preference);

        List<AudioStreamPreference> preferences = getAllAudioStreamPreferences();
        preferences.sort(comparator);
        for (int i = 0; i < preferences.size(); i++) {
            // setOrder to i + 1, since the order 0 preference should always be the
            // "audio_streams_scan_qr_code"
            preferences.get(i).setOrder(i + 1);
        }
    }

    void removeAudioStreamPreferences() {
        List<AudioStreamPreference> streams = getAllAudioStreamPreferences();
        for (var toRemove : streams) {
            removePreference(toRemove);
        }
    }

    private List<AudioStreamPreference> getAllAudioStreamPreferences() {
        List<AudioStreamPreference> streams = new ArrayList<>();
        for (int i = 0; i < getPreferenceCount(); i++) {
            if (getPreference(i) instanceof AudioStreamPreference) {
                streams.add((AudioStreamPreference) getPreference(i));
            }
        }
        return streams;
    }

    private void init() {
        setEmptyTextRes(R.string.audio_streams_empty);
    }
}
