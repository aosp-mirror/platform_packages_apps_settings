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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.ActionButtonsPreference;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioStreamButtonController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "AudioStreamButtonController";
    private static final String KEY = "audio_stream_button";
    private final BluetoothLeBroadcastAssistant.Callback mBroadcastAssistantCallback =
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
                }

                @Override
                public void onReceiveStateChanged(
                        BluetoothDevice sink,
                        int sourceId,
                        BluetoothLeBroadcastReceiveState state) {
                    super.onReceiveStateChanged(sink, sourceId, state);
                    if (mAudioStreamsHelper.isConnected(state)) {
                        updateButton();
                    }
                }

                @Override
                public void onSourceAddFailed(
                        BluetoothDevice sink, BluetoothLeBroadcastMetadata source, int reason) {
                    super.onSourceAddFailed(sink, source, reason);
                    updateButton();
                }

                @Override
                public void onSourceLost(int broadcastId) {
                    super.onSourceLost(broadcastId);
                    updateButton();
                }
            };

    private final AudioStreamsRepository mAudioStreamsRepository =
            AudioStreamsRepository.getInstance();
    private final Executor mExecutor;
    private final AudioStreamsHelper mAudioStreamsHelper;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private @Nullable ActionButtonsPreference mPreference;
    private int mBroadcastId = -1;

    public AudioStreamButtonController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mExecutor = Executors.newSingleThreadExecutor();
        mAudioStreamsHelper = new AudioStreamsHelper(Utils.getLocalBtManager(context));
        mLeBroadcastAssistant = mAudioStreamsHelper.getLeBroadcastAssistant();
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
        if (mPreference != null) {
            if (mAudioStreamsHelper.getAllConnectedSources().stream()
                    .map(BluetoothLeBroadcastReceiveState::getBroadcastId)
                    .anyMatch(connectedBroadcastId -> connectedBroadcastId == mBroadcastId)) {
                ThreadUtils.postOnMainThread(
                        () -> {
                            if (mPreference != null) {
                                mPreference.setButton1Enabled(true);
                                mPreference
                                        .setButton1Text(
                                                R.string.bluetooth_device_context_disconnect)
                                        .setButton1Icon(R.drawable.ic_settings_close)
                                        .setButton1OnClickListener(
                                                unused -> {
                                                    if (mPreference != null) {
                                                        mPreference.setButton1Enabled(false);
                                                    }
                                                    mAudioStreamsHelper.removeSource(mBroadcastId);
                                                });
                            }
                        });
            } else {
                View.OnClickListener clickToRejoin =
                        unused ->
                                ThreadUtils.postOnBackgroundThread(
                                        () -> {
                                            var metadata =
                                                    mAudioStreamsRepository.getSavedMetadata(
                                                            mContext, mBroadcastId);
                                            if (metadata != null) {
                                                mAudioStreamsHelper.addSource(metadata);
                                                ThreadUtils.postOnMainThread(
                                                        () -> {
                                                            if (mPreference != null) {
                                                                mPreference.setButton1Enabled(
                                                                        false);
                                                            }
                                                        });
                                            }
                                        });
                ThreadUtils.postOnMainThread(
                        () -> {
                            if (mPreference != null) {
                                mPreference.setButton1Enabled(true);
                                mPreference
                                        .setButton1Text(R.string.bluetooth_device_context_connect)
                                        .setButton1Icon(R.drawable.ic_add_24dp)
                                        .setButton1OnClickListener(clickToRejoin);
                            }
                        });
            }
        } else {
            Log.w(TAG, "updateButton(): preference is null!");
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
}
