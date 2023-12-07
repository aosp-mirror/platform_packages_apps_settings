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

import static java.util.Collections.emptyList;

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

public class AudioStreamsProgressCategoryController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "AudioStreamsProgressCategoryController";
    private static final boolean DEBUG = BluetoothUtils.D;

    private final Executor mExecutor;
    private final AudioStreamsBroadcastAssistantCallback mBroadcastAssistantCallback;
    private final AudioStreamsHelper mAudioStreamsHelper;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final ConcurrentHashMap<Integer, AudioStreamPreference> mBroadcastIdToPreferenceMap =
            new ConcurrentHashMap<>();
    private AudioStreamsProgressCategoryPreference mCategoryPreference;

    public AudioStreamsProgressCategoryController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mExecutor = Executors.newSingleThreadExecutor();
        mAudioStreamsHelper = new AudioStreamsHelper(Utils.getLocalBtManager(mContext));
        mLeBroadcastAssistant = mAudioStreamsHelper.getLeBroadcastAssistant();
        mBroadcastAssistantCallback = new AudioStreamsBroadcastAssistantCallback(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategoryPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "onStart(): LeBroadcastAssistant is null!");
            return;
        }
        mBroadcastIdToPreferenceMap.clear();
        if (mCategoryPreference != null) {
            mCategoryPreference.removeAll();
        }
        mExecutor.execute(
                () -> {
                    mLeBroadcastAssistant.registerServiceCallBack(
                            mExecutor, mBroadcastAssistantCallback);
                    if (DEBUG) {
                        Log.d(TAG, "scanAudioStreamsStart()");
                    }
                    mLeBroadcastAssistant.startSearchingForSources(emptyList());
                    // Display currently connected streams
                    var unused =
                            ThreadUtils.postOnBackgroundThread(
                                    () ->
                                            mAudioStreamsHelper
                                                    .getAllSources()
                                                    .forEach(this::handleSourceConnected));
                });
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "onStop(): LeBroadcastAssistant is null!");
            return;
        }
        mExecutor.execute(
                () -> {
                    if (mLeBroadcastAssistant.isSearchInProgress()) {
                        if (DEBUG) {
                            Log.d(TAG, "scanAudioStreamsStop()");
                        }
                        mLeBroadcastAssistant.stopSearchingForSources();
                    }
                    mLeBroadcastAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
                });
    }

    void setScanning(boolean isScanning) {
        ThreadUtils.postOnMainThread(
                () -> {
                    if (mCategoryPreference != null) mCategoryPreference.setProgress(isScanning);
                });
    }

    void handleSourceFound(BluetoothLeBroadcastMetadata source) {
        Preference.OnPreferenceClickListener addSourceOrShowDialog =
                preference -> {
                    if (DEBUG) {
                        Log.d(TAG, "preferenceClicked(): attempt to join broadcast");
                    }
                    if (source.isEncrypted()) {
                        ThreadUtils.postOnMainThread(
                                () -> launchPasswordDialog(source, preference));
                    } else {
                        mAudioStreamsHelper.addSource(source);
                    }
                    return true;
                };
        mBroadcastIdToPreferenceMap.computeIfAbsent(
                source.getBroadcastId(),
                k -> {
                    var preference = AudioStreamPreference.fromMetadata(mContext, source);
                    ThreadUtils.postOnMainThread(
                            () -> {
                                preference.setIsConnected(false, addSourceOrShowDialog);
                                if (mCategoryPreference != null) {
                                    mCategoryPreference.addPreference(preference);
                                }
                            });
                    return preference;
                });
    }

    void handleSourceLost(int broadcastId) {
        var toRemove = mBroadcastIdToPreferenceMap.remove(broadcastId);
        if (toRemove != null) {
            ThreadUtils.postOnMainThread(
                    () -> {
                        if (mCategoryPreference != null) {
                            mCategoryPreference.removePreference(toRemove);
                        }
                    });
        }
        mAudioStreamsHelper.removeSource();
    }

    void handleSourceConnected(BluetoothLeBroadcastReceiveState state) {
        // TODO(chelseahao): only continue when the state indicates a successful connection
        mBroadcastIdToPreferenceMap.compute(
                state.getBroadcastId(),
                (k, v) -> {
                    // True if this source has been added either by scanning, or it's currently
                    // connected to another active sink.
                    boolean existed = v != null;
                    AudioStreamPreference preference =
                            existed ? v : AudioStreamPreference.fromReceiveState(mContext, state);

                    ThreadUtils.postOnMainThread(
                            () -> {
                                preference.setIsConnected(
                                        true, p -> launchDetailFragment((AudioStreamPreference) p));
                                if (mCategoryPreference != null && !existed) {
                                    mCategoryPreference.addPreference(preference);
                                }
                            });

                    return preference;
                });
    }

    void showToast(String msg) {
        AudioSharingUtils.toastMessage(mContext, msg);
    }

    private boolean launchDetailFragment(AudioStreamPreference preference) {
        // TODO(chelseahao): impl
        return true;
    }

    private void launchPasswordDialog(BluetoothLeBroadcastMetadata source, Preference preference) {
        // TODO(chelseahao): impl
    }
}
