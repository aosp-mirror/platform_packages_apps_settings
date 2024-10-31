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

import static com.android.settingslib.flags.Flags.audioSharingHysteresisModeFix;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.ActionButtonsPreference;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioStreamButtonController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "AudioStreamButtonController";
    private static final String KEY = "audio_stream_button";
    private static final int SOURCE_ORIGIN_REPOSITORY = SourceOriginForLogging.REPOSITORY.ordinal();

    @VisibleForTesting
    final BluetoothLeBroadcastAssistant.Callback mBroadcastAssistantCallback =
            new AudioStreamsBroadcastAssistantCallback() {
                @Override
                public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
                    super.onSourceRemoved(sink, sourceId, reason);
                    updateButton();
                }

                @Override
                public void onSourceRemoveFailed(BluetoothDevice sink, int sourceId, int reason) {
                    super.onSourceRemoveFailed(sink, sourceId, reason);
                    updateButton();
                    mMetricsFeatureProvider.action(
                            mContext, SettingsEnums.ACTION_AUDIO_STREAM_LEAVE_FAILED);
                }

                @Override
                public void onReceiveStateChanged(
                        BluetoothDevice sink,
                        int sourceId,
                        BluetoothLeBroadcastReceiveState state) {
                    super.onReceiveStateChanged(sink, sourceId, state);
                    boolean shouldUpdateButton =
                            audioSharingHysteresisModeFix()
                                    ? AudioStreamsHelper.hasSourcePresent(state)
                                    : AudioStreamsHelper.isConnected(state);
                    if (shouldUpdateButton) {
                        updateButton();
                        if (AudioStreamsHelper.isConnected(state)) {
                            mMetricsFeatureProvider.action(
                                    mContext,
                                    SettingsEnums.ACTION_AUDIO_STREAM_JOIN_SUCCEED,
                                    SOURCE_ORIGIN_REPOSITORY);
                        }
                    }
                }

                @Override
                public void onSourceAddFailed(
                        BluetoothDevice sink, BluetoothLeBroadcastMetadata source, int reason) {
                    super.onSourceAddFailed(sink, source, reason);
                    updateButton();
                    mMetricsFeatureProvider.action(
                            mContext,
                            SettingsEnums.ACTION_AUDIO_STREAM_JOIN_FAILED_OTHER,
                            SOURCE_ORIGIN_REPOSITORY);
                }

                @Override
                public void onSourceLost(int broadcastId) {
                    super.onSourceLost(broadcastId);
                    updateButton();
                }
            };

    private AudioStreamsRepository mAudioStreamsRepository = AudioStreamsRepository.getInstance();
    private final Executor mExecutor;
    private final AudioStreamsHelper mAudioStreamsHelper;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private @Nullable ActionButtonsPreference mPreference;
    private int mBroadcastId = -1;

    public AudioStreamButtonController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mExecutor = Executors.newSingleThreadExecutor();
        mAudioStreamsHelper = new AudioStreamsHelper(Utils.getLocalBtManager(context));
        mLeBroadcastAssistant = mAudioStreamsHelper.getLeBroadcastAssistant();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "onStart(): LeBroadcastAssistant is null!");
            return;
        }
        mLeBroadcastAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "onStop(): LeBroadcastAssistant is null!");
            return;
        }
        mLeBroadcastAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
    }

    @Override
    public final void displayPreference(PreferenceScreen screen) {
        mPreference = screen.findPreference(getPreferenceKey());
        updateButton();
        super.displayPreference(screen);
    }

    private void updateButton() {
        if (mPreference == null) {
            Log.w(TAG, "updateButton(): preference is null!");
            return;
        }

        List<BluetoothLeBroadcastReceiveState> sources =
                audioSharingHysteresisModeFix()
                        ? mAudioStreamsHelper.getAllPresentSources()
                        : mAudioStreamsHelper.getAllConnectedSources();
        boolean isConnected =
                sources.stream()
                        .map(BluetoothLeBroadcastReceiveState::getBroadcastId)
                        .anyMatch(connectedBroadcastId -> connectedBroadcastId == mBroadcastId);

        View.OnClickListener onClickListener;

        if (isConnected) {
            onClickListener =
                    unused ->
                            ThreadUtils.postOnBackgroundThread(
                                    () -> {
                                        mAudioStreamsHelper.removeSource(mBroadcastId);
                                        mMetricsFeatureProvider.action(
                                                mContext,
                                                SettingsEnums
                                                        .ACTION_AUDIO_STREAM_LEAVE_BUTTON_CLICK);
                                        ThreadUtils.postOnMainThread(
                                                () -> {
                                                    if (mPreference != null) {
                                                        mPreference.setButton1Enabled(false);
                                                    }
                                                });
                                    });
            ThreadUtils.postOnMainThread(
                    () -> {
                        if (mPreference != null) {
                            mPreference.setButton1Enabled(true);
                            mPreference
                                    .setButton1Text(R.string.audio_streams_disconnect)
                                    .setButton1Icon(
                                            com.android.settings.R.drawable.ic_settings_close)
                                    .setButton1OnClickListener(onClickListener);
                        }
                    });
        } else {
            onClickListener =
                    unused ->
                            ThreadUtils.postOnBackgroundThread(
                                    () -> {
                                        var metadata =
                                                mAudioStreamsRepository.getSavedMetadata(
                                                        mContext, mBroadcastId);
                                        if (metadata != null) {
                                            mAudioStreamsHelper.addSource(metadata);
                                            mMetricsFeatureProvider.action(
                                                    mContext,
                                                    SettingsEnums.ACTION_AUDIO_STREAM_JOIN,
                                                    SOURCE_ORIGIN_REPOSITORY);
                                            ThreadUtils.postOnMainThread(
                                                    () -> {
                                                        if (mPreference != null) {
                                                            mPreference.setButton1Enabled(false);
                                                        }
                                                    });
                                        }
                                    });
            ThreadUtils.postOnMainThread(
                    () -> {
                        if (mPreference != null) {
                            mPreference.setButton1Enabled(true);
                            mPreference
                                    .setButton1Text(R.string.audio_streams_connect)
                                    .setButton1Icon(com.android.settings.R.drawable.ic_add_24dp)
                                    .setButton1OnClickListener(onClickListener);
                        }
                    });
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    /** Initialize with broadcast id */
    void init(int broadcastId) {
        mBroadcastId = broadcastId;
    }

    @VisibleForTesting
    void setAudioStreamsRepositoryForTesting(AudioStreamsRepository repository) {
        mAudioStreamsRepository = repository;
    }
}
