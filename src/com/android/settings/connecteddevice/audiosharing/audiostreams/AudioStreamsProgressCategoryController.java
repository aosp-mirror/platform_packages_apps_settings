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

import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

public class AudioStreamsProgressCategoryController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "AudioStreamsProgressCategoryController";
    private static final boolean DEBUG = BluetoothUtils.D;
    private final BluetoothCallback mBluetoothCallback =
            new BluetoothCallback() {
                @Override
                public void onActiveDeviceChanged(
                        @Nullable CachedBluetoothDevice activeDevice, int bluetoothProfile) {
                    if (bluetoothProfile == BluetoothProfile.LE_AUDIO) {
                        mExecutor.execute(() -> init(activeDevice != null));
                    }
                }
            };

    private final Executor mExecutor;
    private final AudioStreamsBroadcastAssistantCallback mBroadcastAssistantCallback;
    private final AudioStreamsHelper mAudioStreamsHelper;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final @Nullable LocalBluetoothManager mBluetoothManager;
    private final ConcurrentHashMap<Integer, AudioStreamPreference> mBroadcastIdToPreferenceMap =
            new ConcurrentHashMap<>();
    private AudioStreamsProgressCategoryPreference mCategoryPreference;

    public AudioStreamsProgressCategoryController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mExecutor = Executors.newSingleThreadExecutor();
        mBluetoothManager = Utils.getLocalBtManager(mContext);
        mAudioStreamsHelper = new AudioStreamsHelper(mBluetoothManager);
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
        if (mBluetoothManager != null) {
            mBluetoothManager.getEventManager().registerCallback(mBluetoothCallback);
        }
        mExecutor.execute(
                () -> {
                    boolean hasActive =
                            AudioSharingUtils.getActiveSinkOnAssistant(mBluetoothManager)
                                    .isPresent();
                    init(hasActive);
                });
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mBluetoothManager != null) {
            mBluetoothManager.getEventManager().unregisterCallback(mBluetoothCallback);
        }
        mExecutor.execute(this::stopScanning);
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
                        Log.d(
                                TAG,
                                "preferenceClicked(): attempt to join broadcast id : "
                                        + source.getBroadcastId());
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
        mAudioStreamsHelper.removeSource(broadcastId);
    }

    void handleSourceConnected(BluetoothLeBroadcastReceiveState state) {
        if (!AudioStreamsHelper.isConnected(state)) {
            return;
        }
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
                                        true, p -> launchDetailFragment(state.getBroadcastId()));
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

    private void init(boolean hasActive) {
        mBroadcastIdToPreferenceMap.clear();
        ThreadUtils.postOnMainThread(
                () -> {
                    if (mCategoryPreference != null) {
                        mCategoryPreference.removeAll();
                        mCategoryPreference.setVisible(hasActive);
                    }
                });
        if (hasActive) {
            startScanning();
        } else {
            stopScanning();
        }
    }

    private void startScanning() {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "startScanning(): LeBroadcastAssistant is null!");
            return;
        }
        if (mLeBroadcastAssistant.isSearchInProgress()) {
            showToast("Failed to start scanning, please try again.");
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "startScanning()");
        }
        mLeBroadcastAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
        mLeBroadcastAssistant.startSearchingForSources(emptyList());

        // Display currently connected streams
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () ->
                                mAudioStreamsHelper
                                        .getAllSources()
                                        .forEach(this::handleSourceConnected));
    }

    private void stopScanning() {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "stopScanning(): LeBroadcastAssistant is null!");
            return;
        }
        if (mLeBroadcastAssistant.isSearchInProgress()) {
            if (DEBUG) {
                Log.d(TAG, "stopScanning()");
            }
            mLeBroadcastAssistant.stopSearchingForSources();
        }
        mLeBroadcastAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
    }

    private boolean launchDetailFragment(int broadcastId) {
        if (!mBroadcastIdToPreferenceMap.containsKey(broadcastId)) {
            Log.w(
                    TAG,
                    "launchDetailFragment(): broadcastId not exist in BroadcastIdToPreferenceMap!");
            return false;
        }
        AudioStreamPreference preference = mBroadcastIdToPreferenceMap.get(broadcastId);

        Bundle broadcast = new Bundle();
        broadcast.putString(
                AudioStreamDetailsFragment.BROADCAST_NAME_ARG, (String) preference.getTitle());
        broadcast.putInt(AudioStreamDetailsFragment.BROADCAST_ID_ARG, broadcastId);

        new SubSettingLauncher(mContext)
                .setTitleText("Audio stream details")
                .setDestination(AudioStreamDetailsFragment.class.getName())
                // TODO(chelseahao): Add logging enum
                .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                .setArguments(broadcast)
                .launch();
        return true;
    }

    private void launchPasswordDialog(BluetoothLeBroadcastMetadata source, Preference preference) {
        View layout =
                LayoutInflater.from(mContext)
                        .inflate(R.layout.bluetooth_find_broadcast_password_dialog, null);
        ((TextView) layout.requireViewById(R.id.broadcast_name_text))
                .setText(preference.getTitle());
        AlertDialog alertDialog =
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.find_broadcast_password_dialog_title)
                        .setView(layout)
                        .setNeutralButton(android.R.string.cancel, null)
                        .setPositiveButton(
                                R.string.bluetooth_connect_access_dialog_positive,
                                (dialog, which) -> {
                                    var code =
                                            ((EditText)
                                                            layout.requireViewById(
                                                                    R.id.broadcast_edit_text))
                                                    .getText()
                                                    .toString();
                                    mAudioStreamsHelper.addSource(
                                            new BluetoothLeBroadcastMetadata.Builder(source)
                                                    .setBroadcastCode(
                                                            code.getBytes(StandardCharsets.UTF_8))
                                                    .build());
                                })
                        .create();
        alertDialog.show();
    }
}
