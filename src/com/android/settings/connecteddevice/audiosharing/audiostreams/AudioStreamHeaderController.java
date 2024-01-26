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
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.LayoutPreference;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

public class AudioStreamHeaderController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "AudioStreamHeaderController";
    private static final String KEY = "audio_stream_header";
    private final Executor mExecutor;
    private final AudioStreamsHelper mAudioStreamsHelper;
    @Nullable private final LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final BluetoothLeBroadcastAssistant.Callback mBroadcastAssistantCallback =
            new AudioStreamsBroadcastAssistantCallback() {
                @Override
                public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
                    super.onSourceRemoved(sink, sourceId, reason);
                    updateSummary();
                }

                @Override
                public void onSourceLost(int broadcastId) {
                    super.onSourceLost(broadcastId);
                    updateSummary();
                }

                @Override
                public void onReceiveStateChanged(
                        BluetoothDevice sink,
                        int sourceId,
                        BluetoothLeBroadcastReceiveState state) {
                    super.onReceiveStateChanged(sink, sourceId, state);
                    if (mAudioStreamsHelper.isConnected(state)) {
                        updateSummary();
                    }
                }
            };

    private @Nullable EntityHeaderController mHeaderController;
    private @Nullable DashboardFragment mFragment;
    private String mBroadcastName = "";
    private int mBroadcastId = -1;

    public AudioStreamHeaderController(Context context, String preferenceKey) {
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
        LayoutPreference headerPreference = screen.findPreference(KEY);
        if (headerPreference != null && mFragment != null) {
            mHeaderController =
                    EntityHeaderController.newInstance(
                            mFragment.getActivity(),
                            mFragment,
                            headerPreference.findViewById(R.id.entity_header));
            if (mBroadcastName != null) {
                mHeaderController.setLabel(mBroadcastName);
            }
            mHeaderController.setIcon(
                    screen.getContext().getDrawable(R.drawable.ic_bt_audio_sharing));
            screen.addPreference(headerPreference);
            updateSummary();
        }
        super.displayPreference(screen);
    }

    private void updateSummary() {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            var latestSummary =
                                    mAudioStreamsHelper.getAllConnectedSources().stream()
                                                    .map(
                                                            BluetoothLeBroadcastReceiveState
                                                                    ::getBroadcastId)
                                                    .anyMatch(
                                                            connectedBroadcastId ->
                                                                    connectedBroadcastId
                                                                            == mBroadcastId)
                                            ? "Listening now"
                                            : "";
                            ThreadUtils.postOnMainThread(
                                    () -> {
                                        if (mHeaderController != null) {
                                            mHeaderController.setSummary(latestSummary);
                                            mHeaderController.done(true);
                                        }
                                    });
                        });
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    /** Initialize with {@link AudioStreamDetailsFragment} and broadcast name and id */
    void init(
            AudioStreamDetailsFragment audioStreamDetailsFragment,
            String broadcastName,
            int broadcastId) {
        mFragment = audioStreamDetailsFragment;
        mBroadcastName = broadcastName;
        mBroadcastId = broadcastId;
    }
}
