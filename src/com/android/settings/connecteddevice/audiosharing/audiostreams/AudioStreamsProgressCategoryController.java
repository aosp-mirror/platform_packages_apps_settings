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

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsScanQrCodeController.REQUEST_SCAN_BT_BROADCAST_QR_CODE;

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
import com.android.settings.connecteddevice.audiosharing.audiostreams.qrcode.QrCodeScanModeActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.bluetooth.BluetoothBroadcastUtils;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
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

    private final Preference.OnPreferenceClickListener mAddSourceOrShowDialog =
            preference -> {
                var p = (AudioStreamPreference) preference;
                if (DEBUG) {
                    Log.d(
                            TAG,
                            "preferenceClicked(): attempt to join broadcast id : "
                                    + p.getAudioStreamBroadcastId());
                }
                var source = p.getAudioStreamMetadata();
                if (source != null) {
                    if (source.isEncrypted()) {
                        ThreadUtils.postOnMainThread(() -> launchPasswordDialog(source, p));
                    } else {
                        moveToState(p, AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE);
                    }
                }
                return true;
            };

    private final Preference.OnPreferenceClickListener mLaunchDetailFragment =
            preference -> {
                var p = (AudioStreamPreference) preference;
                Bundle broadcast = new Bundle();
                broadcast.putString(
                        AudioStreamDetailsFragment.BROADCAST_NAME_ARG, (String) p.getTitle());
                broadcast.putInt(
                        AudioStreamDetailsFragment.BROADCAST_ID_ARG, p.getAudioStreamBroadcastId());

                new SubSettingLauncher(mContext)
                        .setTitleText("Audio stream details")
                        .setDestination(AudioStreamDetailsFragment.class.getName())
                        // TODO(chelseahao): Add logging enum
                        .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                        .setArguments(broadcast)
                        .launch();
                return true;
            };

    private final AudioStreamsRepository mAudioStreamsRepository =
            AudioStreamsRepository.getInstance();

    enum AudioStreamState {
        UNKNOWN,
        // When mTimedSourceFromQrCode is present and this source has not been synced.
        WAIT_FOR_SYNC,
        // When source has been synced but not added to any sink.
        SYNCED,
        // When addSource is called for this source and waiting for response.
        ADD_SOURCE_WAIT_FOR_RESPONSE,
        // Source is added to active sink.
        SOURCE_ADDED,
    }

    private final Comparator<AudioStreamPreference> mComparator =
            Comparator.<AudioStreamPreference, Boolean>comparing(
                            p ->
                                    p.getAudioStreamState()
                                            == AudioStreamsProgressCategoryController
                                                    .AudioStreamState.SOURCE_ADDED)
                    .thenComparingInt(AudioStreamPreference::getAudioStreamRssi)
                    .reversed();

    private final Executor mExecutor;
    private final AudioStreamsProgressCategoryCallback mBroadcastAssistantCallback;
    private final AudioStreamsHelper mAudioStreamsHelper;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final @Nullable LocalBluetoothManager mBluetoothManager;
    private final ConcurrentHashMap<Integer, AudioStreamPreference> mBroadcastIdToPreferenceMap =
            new ConcurrentHashMap<>();
    private @Nullable TimedSourceFromQrCode mTimedSourceFromQrCode;
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
        var broadcastIdFound = source.getBroadcastId();
        mBroadcastIdToPreferenceMap.compute(
                broadcastIdFound,
                (k, v) -> {
                    if (v == null) {
                        // No existing preference for this source founded, add one and set initial
                        // state to SYNCED.
                        return addNewPreference(source, AudioStreamState.SYNCED);
                    }
                    var fromState = v.getAudioStreamState();
                    if (fromState == AudioStreamState.WAIT_FOR_SYNC
                            && mTimedSourceFromQrCode != null) {
                        var pendingSource = mTimedSourceFromQrCode.get();
                        if (pendingSource == null) {
                            Log.w(
                                    TAG,
                                    "handleSourceFound(): unexpected state with null pendingSource:"
                                            + fromState
                                            + " for broadcastId : "
                                            + broadcastIdFound);
                            v.setAudioStreamMetadata(source);
                            moveToState(v, AudioStreamState.SYNCED);
                            return v;
                        }
                        // A preference with source founded is existed from a QR code scan. As the
                        // source is now synced, we update the preference with pendingSource from QR
                        // code scan and add source with it (since it has the password).
                        v.setAudioStreamMetadata(pendingSource);
                        moveToState(v, AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE);
                    } else {
                        // A preference with source founded existed either because it's already
                        // connected (SOURCE_ADDED), or other unexpected reason. We update the
                        // preference with this source and won't change it's state.
                        v.setAudioStreamMetadata(source);
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
                        // No existing preference for this source from the QR code scan, add one and
                        // set initial state to WAIT_FOR_SYNC.
                        return addNewPreference(metadataFromQrCode, AudioStreamState.WAIT_FOR_SYNC);
                    }
                    var fromState = v.getAudioStreamState();
                    if (fromState == AudioStreamState.SYNCED) {
                        // A preference with source from the QR code is existed because it has been
                        // founded during scanning, now we have the password, we can add source.
                        v.setAudioStreamMetadata(metadataFromQrCode);
                        moveToState(v, AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE);
                    } else {
                        v.setAudioStreamMetadata(metadataFromQrCode);
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

    void handleSourceRemoved() {
        for (var entry : mBroadcastIdToPreferenceMap.entrySet()) {
            var preference = entry.getValue();

            // Look for preference has SOURCE_ADDED state, re-check if they are still connected. If
            // not, means the source is removed from the sink, we move back the preference to SYNCED
            // state.
            if (preference.getAudioStreamState() == AudioStreamState.SOURCE_ADDED
                    && mAudioStreamsHelper.getAllConnectedSources().stream()
                            .noneMatch(
                                    connected ->
                                            connected.getBroadcastId()
                                                    == preference.getAudioStreamBroadcastId())) {

                ThreadUtils.postOnMainThread(
                        () -> {
                            var metadata = preference.getAudioStreamMetadata();

                            if (metadata != null) {
                                moveToState(preference, AudioStreamState.SYNCED);
                            } else {
                                handleSourceLost(preference.getAudioStreamBroadcastId());
                            }
                        });

                return;
            }
        }
    }

    void handleSourceConnected(BluetoothLeBroadcastReceiveState receiveState) {
        if (!mAudioStreamsHelper.isConnected(receiveState)) {
            return;
        }
        var broadcastIdConnected = receiveState.getBroadcastId();
        mBroadcastIdToPreferenceMap.compute(
                broadcastIdConnected,
                (k, v) -> {
                    if (v == null) {
                        // No existing preference for this source even if it's already connected,
                        // add one and set initial state to SOURCE_ADDED. This could happen because
                        // we retrieves the connected source during onStart() from
                        // AudioStreamsHelper#getAllConnectedSources() even before the source is
                        // founded by scanning.
                        return addNewPreference(receiveState, AudioStreamState.SOURCE_ADDED);
                    }
                    var fromState = v.getAudioStreamState();
                    if (fromState == AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE
                            || fromState == AudioStreamState.SYNCED
                            || fromState == AudioStreamState.WAIT_FOR_SYNC
                            || fromState == AudioStreamState.SOURCE_ADDED) {
                        // Expected state, do nothing
                    } else {
                        Log.w(
                                TAG,
                                "handleSourceConnected(): unexpected state : "
                                        + fromState
                                        + " for broadcastId : "
                                        + broadcastIdConnected);
                    }
                    moveToState(v, AudioStreamState.SOURCE_ADDED);
                    return v;
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
                        mCategoryPreference.removeAudioStreamPreferences();
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

        // Handle QR code scan and display currently connected streams
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            handleSourceFromQrCodeIfExists();
                            mAudioStreamsHelper
                                    .getAllConnectedSources()
                                    .forEach(this::handleSourceConnected);
                            mLeBroadcastAssistant.startSearchingForSources(emptyList());
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
            mTimedSourceFromQrCode.cleanup();
            mTimedSourceFromQrCode = null;
        }
    }

    private AudioStreamPreference addNewPreference(
            BluetoothLeBroadcastReceiveState receiveState, AudioStreamState state) {
        var preference = AudioStreamPreference.fromReceiveState(mContext, receiveState);
        moveToState(preference, state);
        return preference;
    }

    private AudioStreamPreference addNewPreference(
            BluetoothLeBroadcastMetadata metadata, AudioStreamState state) {
        var preference = AudioStreamPreference.fromMetadata(mContext, metadata);
        moveToState(preference, state);
        return preference;
    }

    private void moveToState(AudioStreamPreference preference, AudioStreamState state) {
        if (preference.getAudioStreamState() == state) {
            return;
        }
        preference.setAudioStreamState(state);

        // Perform action according to the new state
        if (state == AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE) {
            if (mTimedSourceFromQrCode != null) {
                mTimedSourceFromQrCode.consumed(preference.getAudioStreamBroadcastId());
            }
            var metadata = preference.getAudioStreamMetadata();
            if (metadata != null) {
                mAudioStreamsHelper.addSource(metadata);
                // Cache the metadata that used for add source, if source is added successfully, we
                // will save it persistently.
                mAudioStreamsRepository.cacheMetadata(metadata);
            }
        } else if (state == AudioStreamState.SOURCE_ADDED) {
            if (mTimedSourceFromQrCode != null) {
                mTimedSourceFromQrCode.consumed(preference.getAudioStreamBroadcastId());
            }
            // Saved connected metadata for user to re-join this broadcast later.
            var cached =
                    mAudioStreamsRepository.getCachedMetadata(
                            preference.getAudioStreamBroadcastId());
            if (cached != null) {
                mAudioStreamsRepository.saveMetadata(mContext, cached);
            }
        } else if (state == AudioStreamState.WAIT_FOR_SYNC) {
            if (mTimedSourceFromQrCode != null) {
                mTimedSourceFromQrCode.waitForConsume();
            }
        }

        // Get preference click listener according to the new state
        Preference.OnPreferenceClickListener listener;
        if (state == AudioStreamState.SYNCED) {
            listener = mAddSourceOrShowDialog;
        } else if (state == AudioStreamState.SOURCE_ADDED) {
            listener = mLaunchDetailFragment;
        } else {
            listener = null;
        }

        // Get preference summary according to the new state
        String summary;
        if (state == AudioStreamState.WAIT_FOR_SYNC) {
            summary = "Scanning...";
        } else if (state == AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE) {
            summary = "Connecting...";
        } else if (state == AudioStreamState.SOURCE_ADDED) {
            summary = "Listening now";
        } else {
            summary = "";
        }

        // Update UI
        ThreadUtils.postOnMainThread(
                () -> {
                    preference.setIsConnected(
                            state == AudioStreamState.SOURCE_ADDED, summary, listener);
                    if (mCategoryPreference != null) {
                        mCategoryPreference.addAudioStreamPreference(preference, mComparator);
                    }
                });
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
                                    // Update the metadata after user entered the password
                                    preference.setAudioStreamMetadata(metadata);
                                    moveToState(
                                            preference,
                                            AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE);
                                })
                        .create();
        alertDialog.show();
    }

    private AudioStreamsDialogFragment.DialogBuilder getNoLeDeviceDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(mContext)
                .setTitle("Connect compatible headphones")
                .setSubTitle2(
                        "To listen to an audio stream, first connect headphones that support LE"
                                + " Audio to this device. Learn more")
                .setLeftButtonText("Close")
                .setLeftButtonOnClickListener(AlertDialog::dismiss)
                .setRightButtonText("Connect a device")
                .setRightButtonOnClickListener(
                        dialog -> {
                            mContext.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                            dialog.dismiss();
                        });
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
                .setRightButtonOnClickListener(
                        dialog -> {
                            if (mFragment != null) {
                                Intent intent = new Intent(mContext, QrCodeScanModeActivity.class);
                                intent.setAction(
                                        BluetoothBroadcastUtils
                                                .ACTION_BLUETOOTH_LE_AUDIO_QR_CODE_SCANNER);
                                mFragment.startActivityForResult(
                                        intent, REQUEST_SCAN_BT_BROADCAST_QR_CODE);
                                dialog.dismiss();
                            }
                        });
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

        private void cleanup() {
            mTimer.cancel();
            mSourceFromQrCode = null;
        }

        private void consumed(int broadcastId) {
            if (mSourceFromQrCode == null || broadcastId != mSourceFromQrCode.getBroadcastId()) {
                return;
            }
            cleanup();
        }

        private BluetoothLeBroadcastMetadata get() {
            return mSourceFromQrCode;
        }
    }
}
