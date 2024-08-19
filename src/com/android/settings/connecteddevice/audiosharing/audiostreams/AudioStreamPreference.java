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

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.TwoTargetPreference;

/**
 * Custom preference class for managing audio stream preferences with an optional lock icon. Extends
 * {@link TwoTargetPreference}.
 */
class AudioStreamPreference extends TwoTargetPreference {
    private boolean mIsConnected = false;
    private boolean mIsEncrypted = true;
    @Nullable private AudioStream mAudioStream;

    /**
     * Update preference UI based on connection status
     *
     * @param isConnected Is this stream connected
     */
    void setIsConnected(boolean isConnected) {
        if (mIsConnected != isConnected) {
            mIsConnected = isConnected;
            notifyChanged();
        }
    }

    @VisibleForTesting
    AudioStreamPreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing);
    }

    void setAudioStreamState(AudioStreamsProgressCategoryController.AudioStreamState state) {
        if (mAudioStream != null) {
            mAudioStream.setState(state);
        }
    }

    void setAudioStreamMetadata(BluetoothLeBroadcastMetadata metadata) {
        if (mAudioStream != null) {
            mAudioStream.setMetadata(metadata);
            // Update title based on the metadata
            String broadcastName = AudioStreamsHelper.getBroadcastName(metadata);
            ThreadUtils.postOnMainThread(() -> setTitle(broadcastName));
        }
    }

    int getAudioStreamBroadcastId() {
        return mAudioStream != null ? mAudioStream.getBroadcastId() : -1;
    }

    @Nullable
    String getAudioStreamBroadcastName() {
        return mAudioStream != null ? mAudioStream.getBroadcastName() : null;
    }

    int getAudioStreamRssi() {
        return mAudioStream != null ? mAudioStream.getRssi() : -1;
    }

    @Nullable
    BluetoothLeBroadcastMetadata getAudioStreamMetadata() {
        return mAudioStream != null ? mAudioStream.getMetadata() : null;
    }

    AudioStreamsProgressCategoryController.AudioStreamState getAudioStreamState() {
        return mAudioStream != null
                ? mAudioStream.getState()
                : AudioStreamsProgressCategoryController.AudioStreamState.UNKNOWN;
    }

    SourceOriginForLogging getSourceOriginForLogging() {
        return mAudioStream != null
                ? mAudioStream.getSourceOriginForLogging()
                : SourceOriginForLogging.UNKNOWN;
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return mIsConnected || !mIsEncrypted;
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
            Context context,
            BluetoothLeBroadcastMetadata source,
            SourceOriginForLogging sourceOriginForLogging) {
        AudioStreamPreference preference = new AudioStreamPreference(context, /* attrs= */ null);
        preference.setIsEncrypted(source.isEncrypted());
        preference.setTitle(AudioStreamsHelper.getBroadcastName(source));
        preference.setAudioStream(new AudioStream(source, sourceOriginForLogging));
        return preference;
    }

    static AudioStreamPreference fromReceiveState(
            Context context, BluetoothLeBroadcastReceiveState receiveState) {
        AudioStreamPreference preference = new AudioStreamPreference(context, /* attrs= */ null);
        preference.setTitle(AudioStreamsHelper.getBroadcastName(receiveState));
        preference.setAudioStream(new AudioStream(receiveState));
        return preference;
    }

    private void setAudioStream(AudioStream audioStream) {
        mAudioStream = audioStream;
    }

    private void setIsEncrypted(boolean isEncrypted) {
        mIsEncrypted = isEncrypted;
    }

    private static final class AudioStream {
        private static final int UNAVAILABLE = -1;
        @Nullable private BluetoothLeBroadcastMetadata mMetadata;
        @Nullable private BluetoothLeBroadcastReceiveState mReceiveState;
        private SourceOriginForLogging mSourceOriginForLogging = SourceOriginForLogging.UNKNOWN;
        private AudioStreamsProgressCategoryController.AudioStreamState mState =
                AudioStreamsProgressCategoryController.AudioStreamState.UNKNOWN;

        private AudioStream(
                BluetoothLeBroadcastMetadata metadata,
                SourceOriginForLogging sourceOriginForLogging) {
            mMetadata = metadata;
            mSourceOriginForLogging = sourceOriginForLogging;
        }

        private AudioStream(BluetoothLeBroadcastReceiveState receiveState) {
            mReceiveState = receiveState;
        }

        private int getBroadcastId() {
            return mMetadata != null
                    ? mMetadata.getBroadcastId()
                    : mReceiveState != null ? mReceiveState.getBroadcastId() : UNAVAILABLE;
        }

        private @Nullable String getBroadcastName() {
            return mMetadata != null
                    ? AudioStreamsHelper.getBroadcastName(mMetadata)
                    : mReceiveState != null
                            ? AudioStreamsHelper.getBroadcastName(mReceiveState)
                            : null;
        }

        private int getRssi() {
            return mMetadata != null ? mMetadata.getRssi() : Integer.MAX_VALUE;
        }

        private AudioStreamsProgressCategoryController.AudioStreamState getState() {
            return mState;
        }

        private SourceOriginForLogging getSourceOriginForLogging() {
            return mSourceOriginForLogging;
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
