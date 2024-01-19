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
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
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

    private final AudioStreamsRepository mAudioStreamsRepository =
            AudioStreamsRepository.getInstance();

    enum AudioStreamState {
        // When mTimedSourceFromQrCode is present and this source has not been synced.
        WAIT_FOR_SYNC,
        // When source has been synced but not added to any sink.
        SYNCED,
        // When addSource is called for this source and waiting for response.
        WAIT_FOR_SOURCE_ADD,
        // Source is added to active sink.
        SOURCE_ADDED,
    }

    private final Executor mExecutor;
    private final AudioStreamsProgressCategoryCallback mBroadcastAssistantCallback;
    private final AudioStreamsHelper mAudioStreamsHelper;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final @Nullable LocalBluetoothManager mBluetoothManager;
    private final ConcurrentHashMap<Integer, AudioStreamPreference> mBroadcastIdToPreferenceMap =
            new ConcurrentHashMap<>();
    private TimedSourceFromQrCode mTimedSourceFromQrCode;
    private AudioStreamsProgressCategoryPreference mCategoryPreference;
    private AudioStreamsDashboardFragment mFragment;

    public AudioStreamsProgressCategoryController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mExecutor = Executors.newSingleThreadExecutor();
        mBluetoothManager = Utils.getLocalBtManager(mContext);
        mAudioStreamsHelper = new AudioStreamsHelper(mBluetoothManager);
        mLeBroadcastAssistant = mAudioStreamsHelper.getLeBroadcastAssistant();
        mBroadcastAssistantCallback = new AudioStreamsProgressCategoryCallback(this);
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

    void setFragment(AudioStreamsDashboardFragment fragment) {
        mFragment = fragment;
    }

    void setSourceFromQrCode(BluetoothLeBroadcastMetadata source) {
        mTimedSourceFromQrCode =
                new TimedSourceFromQrCode(source, () -> handleSourceLost(source.getBroadcastId()));
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
                                () ->
                                        launchPasswordDialog(
                                                source, (AudioStreamPreference) preference));
                    } else {
                        mAudioStreamsHelper.addSource(source);
                        mAudioStreamsRepository.cacheMetadata(source);
                        ((AudioStreamPreference) preference)
                                .setAudioStreamState(AudioStreamState.WAIT_FOR_SOURCE_ADD);
                        updatePreferenceConnectionState(
                                (AudioStreamPreference) preference,
                                AudioStreamState.WAIT_FOR_SOURCE_ADD,
                                null);
                    }
                    return true;
                };

        var broadcastIdFound = source.getBroadcastId();
        mBroadcastIdToPreferenceMap.compute(
                broadcastIdFound,
                (k, v) -> {
                    if (v == null) {
                        return addNewPreference(
                                source, AudioStreamState.SYNCED, addSourceOrShowDialog);
                    }
                    var fromState = v.getAudioStreamState();
                    if (fromState == AudioStreamState.WAIT_FOR_SYNC) {
                        var pendingSource = mTimedSourceFromQrCode.get();
                        if (pendingSource == null) {
                            Log.w(
                                    TAG,
                                    "handleSourceFound(): unexpected state with null pendingSource:"
                                            + fromState
                                            + " for broadcastId : "
                                            + broadcastIdFound);
                            v.setAudioStreamState(AudioStreamState.SYNCED);
                            return v;
                        }
                        mAudioStreamsHelper.addSource(pendingSource);
                        mAudioStreamsRepository.cacheMetadata(pendingSource);
                        mTimedSourceFromQrCode.consumed();
                        v.setAudioStreamState(AudioStreamState.WAIT_FOR_SOURCE_ADD);
                        updatePreferenceConnectionState(
                                v, AudioStreamState.WAIT_FOR_SOURCE_ADD, null);
                    } else {
                        if (fromState != AudioStreamState.SOURCE_ADDED) {
                            Log.w(
                                    TAG,
                                    "handleSourceFound(): unexpected state : "
                                            + fromState
                                            + " for broadcastId : "
                                            + broadcastIdFound);
                        }
                    }
                    return v;
                });
    }

    private void handleSourceFromQrCodeIfExists() {
        if (mTimedSourceFromQrCode == null || mTimedSourceFromQrCode.get() == null) {
            return;
        }
        var metadataFromQrCode = mTimedSourceFromQrCode.get();
        mBroadcastIdToPreferenceMap.compute(
                metadataFromQrCode.getBroadcastId(),
                (k, v) -> {
                    if (v == null) {
                        mTimedSourceFromQrCode.waitForConsume();
                        return addNewPreference(
                                metadataFromQrCode, AudioStreamState.WAIT_FOR_SYNC, null);
                    }
                    var fromState = v.getAudioStreamState();
                    if (fromState == AudioStreamState.SYNCED) {
                        mAudioStreamsHelper.addSource(metadataFromQrCode);
                        mAudioStreamsRepository.cacheMetadata(metadataFromQrCode);
                        mTimedSourceFromQrCode.consumed();
                        v.setAudioStreamState(AudioStreamState.WAIT_FOR_SOURCE_ADD);
                        updatePreferenceConnectionState(
                                v, AudioStreamState.WAIT_FOR_SOURCE_ADD, null);
                    } else {
                        Log.w(
                                TAG,
                                "handleSourceFromQrCode(): unexpected state : "
                                        + fromState
                                        + " for broadcastId : "
                                        + metadataFromQrCode.getBroadcastId());
                    }
                    return v;
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

    void handleSourceConnected(BluetoothLeBroadcastReceiveState receiveState) {
        if (!mAudioStreamsHelper.isConnected(receiveState)) {
            return;
        }
        var sourceAddedState = AudioStreamState.SOURCE_ADDED;
        var broadcastIdConnected = receiveState.getBroadcastId();
        mBroadcastIdToPreferenceMap.compute(
                broadcastIdConnected,
                (k, v) -> {
                    if (v == null) {
                        return addNewPreference(
                                receiveState,
                                sourceAddedState,
                                p -> launchDetailFragment(broadcastIdConnected));
                    }
                    var fromState = v.getAudioStreamState();
                    if (fromState == AudioStreamState.WAIT_FOR_SOURCE_ADD
                            || fromState == AudioStreamState.SYNCED
                            || fromState == AudioStreamState.WAIT_FOR_SYNC) {
                        if (mTimedSourceFromQrCode != null) {
                            mTimedSourceFromQrCode.consumed();
                        }
                    } else {
                        if (fromState != AudioStreamState.SOURCE_ADDED) {
                            Log.w(
                                    TAG,
                                    "handleSourceConnected(): unexpected state : "
                                            + fromState
                                            + " for broadcastId : "
                                            + broadcastIdConnected);
                        }
                    }
                    v.setAudioStreamState(sourceAddedState);
                    updatePreferenceConnectionState(
                            v, sourceAddedState, p -> launchDetailFragment(broadcastIdConnected));
                    return v;
                });
        // Saved connected metadata for user to re-join this broadcast later.
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            var cached =
                                    mAudioStreamsRepository.getCachedMetadata(broadcastIdConnected);
                            if (cached != null) {
                                mAudioStreamsRepository.saveMetadata(mContext, cached);
                            }
                        });
    }

    private static String getPreferenceSummary(AudioStreamState state) {
        return switch (state) {
            case WAIT_FOR_SYNC -> "Scanning...";
            case WAIT_FOR_SOURCE_ADD -> "Connecting...";
            case SOURCE_ADDED -> "Listening now";
            default -> "";
        };
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
            ThreadUtils.postOnMainThread(
                    () -> AudioStreamsDialogFragment.show(mFragment, getNoLeDeviceDialog()));
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

        // Handle QR code scan and display currently connected streams
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            handleSourceFromQrCodeIfExists();
                            mAudioStreamsHelper
                                    .getAllConnectedSources()
                                    .forEach(this::handleSourceConnected);
                        });
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
        if (mTimedSourceFromQrCode != null) {
            mTimedSourceFromQrCode.consumed();
        }
    }

    private AudioStreamPreference addNewPreference(
            BluetoothLeBroadcastReceiveState receiveState,
            AudioStreamState state,
            Preference.OnPreferenceClickListener onClickListener) {
        var preference = AudioStreamPreference.fromReceiveState(mContext, receiveState, state);
        updatePreferenceConnectionState(preference, state, onClickListener);
        return preference;
    }

    private AudioStreamPreference addNewPreference(
            BluetoothLeBroadcastMetadata metadata,
            AudioStreamState state,
            Preference.OnPreferenceClickListener onClickListener) {
        var preference = AudioStreamPreference.fromMetadata(mContext, metadata, state);
        updatePreferenceConnectionState(preference, state, onClickListener);
        return preference;
    }

    private void updatePreferenceConnectionState(
            AudioStreamPreference preference,
            AudioStreamState state,
            Preference.OnPreferenceClickListener onClickListener) {
        ThreadUtils.postOnMainThread(
                () -> {
                    preference.setIsConnected(
                            state == AudioStreamState.SOURCE_ADDED,
                            getPreferenceSummary(state),
                            onClickListener);
                    if (mCategoryPreference != null) {
                        mCategoryPreference.addPreference(preference);
                    }
                });
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

    private void launchPasswordDialog(
            BluetoothLeBroadcastMetadata source, AudioStreamPreference preference) {
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
                                    var metadata =
                                            new BluetoothLeBroadcastMetadata.Builder(source)
                                                    .setBroadcastCode(
                                                            code.getBytes(StandardCharsets.UTF_8))
                                                    .build();
                                    mAudioStreamsHelper.addSource(metadata);
                                    mAudioStreamsRepository.cacheMetadata(metadata);
                                    preference.setAudioStreamState(
                                            AudioStreamState.WAIT_FOR_SOURCE_ADD);
                                    updatePreferenceConnectionState(
                                            preference, AudioStreamState.WAIT_FOR_SOURCE_ADD, null);
                                })
                        .create();
        alertDialog.show();
    }

    private AudioStreamsDialogFragment.DialogBuilder getNoLeDeviceDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(mContext)
                .setTitle("Connect compatible headphones")
                .setSubTitle1(
                        "To listen to an audio stream, first connect headphones that support LE"
                                + " Audio to this device. Learn more")
                .setLeftButtonText("Close")
                .setLeftButtonOnClickListener(AlertDialog::dismiss)
                .setRightButtonText("Connect a device")
                .setRightButtonOnClickListener(
                        unused ->
                                mContext.startActivity(
                                        new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
    }

    private AudioStreamsDialogFragment.DialogBuilder getBroadcastUnavailableDialog(
            String broadcastName) {
        return new AudioStreamsDialogFragment.DialogBuilder(mContext)
                .setTitle("Audio stream isn't available")
                .setSubTitle1(broadcastName)
                .setSubTitle2("This audio stream isn't playing anything right now")
                .setLeftButtonText("Close")
                .setLeftButtonOnClickListener(AlertDialog::dismiss)
                .setRightButtonText("Retry")
                // TODO(chelseahao): Add retry action
                .setRightButtonOnClickListener(AlertDialog::dismiss);
    }

    private class TimedSourceFromQrCode {
        private static final int WAIT_FOR_SYNC_TIMEOUT_MILLIS = 15000;
        private final CountDownTimer mTimer;
        private BluetoothLeBroadcastMetadata mSourceFromQrCode;

        private TimedSourceFromQrCode(
                BluetoothLeBroadcastMetadata sourceFromQrCode, Runnable timeoutAction) {
            mSourceFromQrCode = sourceFromQrCode;
            mTimer =
                    new CountDownTimer(WAIT_FOR_SYNC_TIMEOUT_MILLIS, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {}

                        @Override
                        public void onFinish() {
                            timeoutAction.run();
                            ThreadUtils.postOnMainThread(
                                    () ->
                                            AudioStreamsDialogFragment.show(
                                                    mFragment,
                                                    getBroadcastUnavailableDialog(
                                                            sourceFromQrCode.getBroadcastName())));
                        }
                    };
        }

        private void waitForConsume() {
            mTimer.start();
        }

        private void consumed() {
            mTimer.cancel();
            mSourceFromQrCode = null;
        }

        private BluetoothLeBroadcastMetadata get() {
            return mSourceFromQrCode;
        }
    }
}
