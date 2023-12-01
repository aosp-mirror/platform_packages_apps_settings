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

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settingslib.widget.TwoTargetPreference;

/**
 * Custom preference class for managing audio stream preferences with an optional lock icon. Extends
 * {@link TwoTargetPreference}.
 */
public class AudioStreamPreference extends TwoTargetPreference {
    private boolean mIsConnected = false;

    /**
     * Update preference UI based on connection status
     *
     * @param isConnected Is this streams connected
     */
    public void setIsConnected(
            boolean isConnected, @Nullable OnPreferenceClickListener onPreferenceClickListener) {
        if (mIsConnected == isConnected
                && getOnPreferenceClickListener() == onPreferenceClickListener) {
            // Nothing to update.
            return;
        }
        mIsConnected = isConnected;
        setSummary(isConnected ? "Listening now" : "");
        setOrder(isConnected ? 0 : 1);
        setOnPreferenceClickListener(onPreferenceClickListener);
        notifyChanged();
    }

    public AudioStreamPreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setIcon(R.drawable.ic_bt_audio_sharing);
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return mIsConnected;
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_lock;
    }
}
