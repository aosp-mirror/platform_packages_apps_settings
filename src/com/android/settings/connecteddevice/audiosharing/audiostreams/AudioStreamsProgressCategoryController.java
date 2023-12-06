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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudioContentMetadata;
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
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.base.Strings;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

public class AudioStreamsProgressCategoryController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "AudioStreamsProgressCategoryController";
    private static final boolean DEBUG = BluetoothUtils.D;

    private final Executor mExecutor;
    private final AudioStreamsBroadcastAssistantCallback mBroadcastAssistantCallback;
    private final LocalBluetoothManager mBluetoothManager;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final ConcurrentHashMap<Integer, AudioStreamPreference> mBroadcastIdToPreferenceMap =
            new ConcurrentHashMap<>();
    private @Nullable AudioStreamsProgressCategoryPreference mCategoryPreference;

    public AudioStreamsProgressCategoryController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mExecutor = Executors.newSingleThreadExecutor();
        mBluetoothManager = Utils.getLocalBtManager(mContext);
        mLeBroadcastAssistant = getLeBroadcastAssistant(mBluetoothManager);
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
                                    () -> {
                                        for (var sink :
                                                getActiveSinksOnAssistant(mBluetoothManager)) {
                                            mLeBroadcastAssistant
                                                    .getAllSources(sink)
                                                    .forEach(this::addSourceConnected);
                                        }
                                    });
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

    void addSourceFound(BluetoothLeBroadcastMetadata source) {
        Preference.OnPreferenceClickListener onClickListener =
                preference -> {
                    if (DEBUG) {
                        Log.d(TAG, "preferenceClicked(): attempt to join broadcast");
                    }

                    // TODO(chelseahao): add source to sink
                    return true;
                };
        mBroadcastIdToPreferenceMap.computeIfAbsent(
                source.getBroadcastId(),
                k -> {
                    var p = createPreference(source, onClickListener);
                    ThreadUtils.postOnMainThread(
                            () -> {
                                if (mCategoryPreference != null) {
                                    mCategoryPreference.addPreference(p);
                                }
                            });
                    return p;
                });
    }

    void removeSourceLost(int broadcastId) {
        var toRemove = mBroadcastIdToPreferenceMap.remove(broadcastId);
        if (toRemove != null) {
            ThreadUtils.postOnMainThread(
                    () -> {
                        if (mCategoryPreference != null) {
                            mCategoryPreference.removePreference(toRemove);
                        }
                    });
        }
        // TODO(chelseahao): remove source from sink
    }

    private void addSourceConnected(BluetoothLeBroadcastReceiveState state) {
        mBroadcastIdToPreferenceMap.compute(
                state.getBroadcastId(),
                (k, v) -> {
                    if (v == null) {
                        // Create a new preference as the source has not been added.
                        var p = createPreference(state);
                        ThreadUtils.postOnMainThread(
                                () -> {
                                    if (mCategoryPreference != null) {
                                        mCategoryPreference.addPreference(p);
                                    }
                                });
                        return p;
                    } else {
                        // This source has been added either by scanning, or it's currently
                        // connected to another active sink. Update its connection status to true
                        // if needed.
                        ThreadUtils.postOnMainThread(() -> v.setIsConnected(true, null));
                        return v;
                    }
                });
    }

    private AudioStreamPreference createPreference(
            BluetoothLeBroadcastMetadata source,
            Preference.OnPreferenceClickListener onPreferenceClickListener) {
        AudioStreamPreference preference = new AudioStreamPreference(mContext, /* attrs= */ null);
        preference.setTitle(
                source.getSubgroups().stream()
                        .map(s -> s.getContentMetadata().getProgramInfo())
                        .filter(i -> !Strings.isNullOrEmpty(i))
                        .findFirst()
                        .orElse("Broadcast Id: " + source.getBroadcastId()));
        preference.setIsConnected(false, onPreferenceClickListener);
        return preference;
    }

    private AudioStreamPreference createPreference(BluetoothLeBroadcastReceiveState state) {
        AudioStreamPreference preference = new AudioStreamPreference(mContext, /* attrs= */ null);
        preference.setTitle(
                state.getSubgroupMetadata().stream()
                        .map(BluetoothLeAudioContentMetadata::getProgramInfo)
                        .filter(i -> !Strings.isNullOrEmpty(i))
                        .findFirst()
                        .orElse("Broadcast Id: " + state.getBroadcastId()));
        preference.setIsConnected(true, null);
        return preference;
    }

    private static List<BluetoothDevice> getActiveSinksOnAssistant(LocalBluetoothManager manager) {
        if (manager == null) {
            Log.w(TAG, "getActiveSinksOnAssistant(): LocalBluetoothManager is null!");
            return emptyList();
        }
        return AudioSharingUtils.getActiveSinkOnAssistant(manager)
                .map(
                        cachedBluetoothDevice ->
                                Stream.concat(
                                                Stream.of(cachedBluetoothDevice.getDevice()),
                                                cachedBluetoothDevice.getMemberDevice().stream()
                                                        .map(CachedBluetoothDevice::getDevice))
                                        .toList())
                .orElse(emptyList());
    }

    private static @Nullable LocalBluetoothLeBroadcastAssistant getLeBroadcastAssistant(
            LocalBluetoothManager manager) {
        if (manager == null) {
            Log.w(TAG, "getLeBroadcastAssistant(): LocalBluetoothManager is null!");
            return null;
        }

        LocalBluetoothProfileManager profileManager = manager.getProfileManager();
        if (profileManager == null) {
            Log.w(TAG, "getLeBroadcastAssistant(): LocalBluetoothProfileManager is null!");
            return null;
        }

        return profileManager.getLeAudioBroadcastAssistantProfile();
    }

    void showToast(String msg) {
        AudioSharingUtils.toastMessage(mContext, msg);
    }
}
