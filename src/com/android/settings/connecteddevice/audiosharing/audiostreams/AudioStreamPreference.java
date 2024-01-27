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

import android.bluetooth.BluetoothLeAudioContentMetadata;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.TwoTargetPreference;

import com.google.common.base.Strings;

/**
 * Custom preference class for managing audio stream preferences with an optional lock icon. Extends
 * {@link TwoTargetPreference}.
 */
class AudioStreamPreference extends TwoTargetPreference {
    private boolean mIsConnected = false;
    private AudioStream mAudioStream;

    /**
     * Update preference UI based on connection status
     *
     * @param isConnected Is this stream connected
     * @param summary Summary text
     * @param onPreferenceClickListener Click listener for the preference
     */
    void setIsConnected(
            boolean isConnected,
            String summary,
            @Nullable OnPreferenceClickListener onPreferenceClickListener) {
        if (mIsConnected == isConnected
                && getSummary() == summary
                && getOnPreferenceClickListener() == onPreferenceClickListener) {
            // Nothing to update.
            return;
        }
        mIsConnected = isConnected;
        setSummary(summary);
        setOnPreferenceClickListener(onPreferenceClickListener);
        notifyChanged();
    }

    AudioStreamPreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setIcon(R.drawable.ic_bt_audio_sharing);
    }

    void setAudioStreamState(AudioStreamsProgressCategoryController.AudioStreamState state) {
        mAudioStream.setState(state);
    }

    void setAudioStreamMetadata(BluetoothLeBroadcastMetadata metadata) {
        mAudioStream.setMetadata(metadata);
    }

    int getAudioStreamBroadcastId() {
        return mAudioStream.getBroadcastId();
    }

    int getAudioStreamRssi() {
        return mAudioStream.getRssi();
    }

    @Nullable
    BluetoothLeBroadcastMetadata getAudioStreamMetadata() {
        return mAudioStream.getMetadata();
    }

    AudioStreamsProgressCategoryController.AudioStreamState getAudioStreamState() {
        return mAudioStream.getState();
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return mIsConnected;
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_lock;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View divider =
                holder.findViewById(
                        com.android.settingslib.widget.preference.twotarget.R.id
                                .two_target_divider);
        if (divider != null) {
            divider.setVisibility(View.GONE);
        }
    }

    static AudioStreamPreference fromMetadata(
            Context context, BluetoothLeBroadcastMetadata source) {
        AudioStreamPreference preference = new AudioStreamPreference(context, /* attrs= */ null);
        preference.setTitle(getBroadcastName(source));
        preference.setAudioStream(new AudioStream(source));
        return preference;
    }

    static AudioStreamPreference fromReceiveState(
            Context context, BluetoothLeBroadcastReceiveState receiveState) {
        AudioStreamPreference preference = new AudioStreamPreference(context, /* attrs= */ null);
        preference.setTitle(getBroadcastName(receiveState));
        preference.setAudioStream(new AudioStream(receiveState));
        return preference;
    }

    private void setAudioStream(AudioStream audioStream) {
        mAudioStream = audioStream;
    }

    private static String getBroadcastName(BluetoothLeBroadcastMetadata source) {
        return source.getSubgroups().stream()
                .map(s -> s.getContentMetadata().getProgramInfo())
                .filter(i -> !Strings.isNullOrEmpty(i))
                .findFirst()
                .orElse("Broadcast Id: " + source.getBroadcastId());
    }

    private static String getBroadcastName(BluetoothLeBroadcastReceiveState state) {
        return state.getSubgroupMetadata().stream()
                .map(BluetoothLeAudioContentMetadata::getProgramInfo)
                .filter(i -> !Strings.isNullOrEmpty(i))
                .findFirst()
                .orElse("Broadcast Id: " + state.getBroadcastId());
    }

    private static final class AudioStream {
        private static final int UNAVAILABLE = -1;
        @Nullable private BluetoothLeBroadcastMetadata mMetadata;
        @Nullable private BluetoothLeBroadcastReceiveState mReceiveState;
        private AudioStreamsProgressCategoryController.AudioStreamState mState =
                AudioStreamsProgressCategoryController.AudioStreamState.UNKNOWN;

        private AudioStream(BluetoothLeBroadcastMetadata metadata) {
            mMetadata = metadata;
        }

        private AudioStream(BluetoothLeBroadcastReceiveState receiveState) {
            mReceiveState = receiveState;
        }

        private int getBroadcastId() {
            return mMetadata != null
                    ? mMetadata.getBroadcastId()
                    : mReceiveState != null ? mReceiveState.getBroadcastId() : UNAVAILABLE;
        }

        private int getRssi() {
            return mMetadata != null ? mMetadata.getRssi() : Integer.MAX_VALUE;
        }

        private AudioStreamsProgressCategoryController.AudioStreamState getState() {
            return mState;
        }

        @Nullable
        private BluetoothLeBroadcastMetadata getMetadata() {
            return mMetadata;
        }

        private void setState(AudioStreamsProgressCategoryController.AudioStreamState state) {
            mState = state;
        }

        private void setMetadata(BluetoothLeBroadcastMetadata metadata) {
            mMetadata = metadata;
        }
    }
}
