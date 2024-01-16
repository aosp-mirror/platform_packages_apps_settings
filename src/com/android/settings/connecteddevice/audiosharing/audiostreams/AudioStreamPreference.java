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

import androidx.annotation.Nullable;

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
        setOrder(isConnected ? 0 : 1);
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

    static AudioStreamPreference fromMetadata(
            Context context,
            BluetoothLeBroadcastMetadata source,
            AudioStreamsProgressCategoryController.AudioStreamState streamState) {
        AudioStreamPreference preference = new AudioStreamPreference(context, /* attrs= */ null);
        preference.setTitle(getBroadcastName(source));
        preference.setAudioStream(new AudioStream(source.getBroadcastId(), streamState));
        return preference;
    }

    static AudioStreamPreference fromReceiveState(
            Context context,
            BluetoothLeBroadcastReceiveState receiveState,
            AudioStreamsProgressCategoryController.AudioStreamState streamState) {
        AudioStreamPreference preference = new AudioStreamPreference(context, /* attrs= */ null);
        preference.setTitle(getBroadcastName(receiveState));
        preference.setAudioStream(
                new AudioStream(
                        receiveState.getSourceId(), receiveState.getBroadcastId(), streamState));
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
        private int mSourceId;
        private int mBroadcastId;
        private AudioStreamsProgressCategoryController.AudioStreamState mState;

        private AudioStream(
                int broadcastId, AudioStreamsProgressCategoryController.AudioStreamState state) {
            mBroadcastId = broadcastId;
            mState = state;
        }

        private AudioStream(
                int sourceId,
                int broadcastId,
                AudioStreamsProgressCategoryController.AudioStreamState state) {
            mSourceId = sourceId;
            mBroadcastId = broadcastId;
            mState = state;
        }

        // TODO(chelseahao): use this to handleSourceRemoved
        private int getSourceId() {
            return mSourceId;
        }

        // TODO(chelseahao): use this to handleSourceRemoved
        private int getBroadcastId() {
            return mBroadcastId;
        }

        private AudioStreamsProgressCategoryController.AudioStreamState getState() {
            return mState;
        }

        private void setState(AudioStreamsProgressCategoryController.AudioStreamState state) {
            mState = state;
        }
    }
}
