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

import static java.util.Collections.emptyList;

import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

public class AudioStreamsProgressCategoryController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "AudioStreamsProgressCategoryController";
    private static final boolean DEBUG = BluetoothUtils.D;
    @VisibleForTesting static final int UNSET_BROADCAST_ID = -1;

    @VisibleForTesting
    final BluetoothCallback mBluetoothCallback =
            new BluetoothCallback() {
                @Override
                public void onBluetoothStateChanged(@AdapterState int bluetoothState) {
                    Log.d(TAG, "onBluetoothStateChanged() with bluetoothState : " + bluetoothState);
                    if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                        mExecutor.execute(() -> init());
                    }
                }

                @Override
                public void onProfileConnectionStateChanged(
                        @NonNull CachedBluetoothDevice cachedDevice,
                        @ConnectionState int state,
                        int bluetoothProfile) {
                    Log.d(
                            TAG,
                            "onProfileConnectionStateChanged() with cachedDevice : "
                                    + cachedDevice.getAddress()
                                    + " with state : "
                                    + state
                                    + " on profile : "
                                    + bluetoothProfile);
                    if (bluetoothProfile == BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT
                            && (state == BluetoothAdapter.STATE_CONNECTED
                                    || state == BluetoothAdapter.STATE_DISCONNECTED)) {
                        mExecutor.execute(() -> init());
                    }
                }
            };

    private final Comparator<AudioStreamPreference> mComparator =
            Comparator.<AudioStreamPreference, Boolean>comparing(
                            p ->
                                    (p.getAudioStreamState()
                                                    == AudioStreamsProgressCategoryController
                                                            .AudioStreamState.SOURCE_ADDED
                                            || (audioSharingHysteresisModeFix()
                                                    && p.getAudioStreamState()
                                                            == AudioStreamsProgressCategoryController
                                                                    .AudioStreamState
                                                                    .SOURCE_PRESENT)))
                    .thenComparingInt(AudioStreamPreference::getAudioStreamRssi)
                    .reversed();

    public enum AudioStreamState {
        UNKNOWN,
        // When mSourceFromQrCode is present and this source has not been synced.
        WAIT_FOR_SYNC,
        // When source has been synced but not added to any sink.
        SYNCED,
        // When addSource is called for this source and waiting for response.
        ADD_SOURCE_WAIT_FOR_RESPONSE,
        // When addSource result in a bad code response.
        ADD_SOURCE_BAD_CODE,
        // When addSource result in other bad state.
        ADD_SOURCE_FAILED,
        // Source is present on sink.
        SOURCE_PRESENT,
        // Source is added to active sink.
        SOURCE_ADDED,
    }

    @VisibleForTesting Executor mExecutor;
    private final AudioStreamsProgressCategoryCallback mBroadcastAssistantCallback;
    private final AudioStreamsHelper mAudioStreamsHelper;
    private final MediaControlHelper mMediaControlHelper;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final @Nullable LocalBluetoothManager mBluetoothManager;
    private final ConcurrentHashMap<Integer, AudioStreamPreference> mBroadcastIdToPreferenceMap =
            new ConcurrentHashMap<>();
    private @Nullable BluetoothLeBroadcastMetadata mSourceFromQrCode;
    private SourceOriginForLogging mSourceFromQrCodeOriginForLogging;
    @Nullable private AudioStreamsProgressCategoryPreference mCategoryPreference;
    @Nullable private Fragment mFragment;

    public AudioStreamsProgressCategoryController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mExecutor = Executors.newSingleThreadExecutor();
        mBluetoothManager = Utils.getLocalBtManager(mContext);
        mAudioStreamsHelper = new AudioStreamsHelper(mBluetoothManager);
        mMediaControlHelper = new MediaControlHelper(mContext, mBluetoothManager);
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
        mExecutor.execute(this::init);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mBluetoothManager != null) {
            mBluetoothManager.getEventManager().unregisterCallback(mBluetoothCallback);
        }
        mExecutor.execute(this::stopScanning);
    }

    void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    @Nullable
    Fragment getFragment() {
        return mFragment;
    }

    void setSourceFromQrCode(
            BluetoothLeBroadcastMetadata source, SourceOriginForLogging sourceOriginForLogging) {
        if (DEBUG) {
            Log.d(TAG, "setSourceFromQrCode(): broadcastId " + source.getBroadcastId());
        }
        mSourceFromQrCode = source;
        mSourceFromQrCodeOriginForLogging = sourceOriginForLogging;
    }

    void setScanning(boolean isScanning) {
        ThreadUtils.postOnMainThread(
                () -> {
                    if (mCategoryPreference != null) mCategoryPreference.setProgress(isScanning);
                });
    }

    // Find preference by scanned source and decide next state.
    // Expect one of the following:
    // 1) No preference existed, create new preference with state SYNCED
    // 2) WAIT_FOR_SYNC, move to ADD_SOURCE_WAIT_FOR_RESPONSE
    // 3) SOURCE_ADDED, leave as-is
    void handleSourceFound(BluetoothLeBroadcastMetadata source) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceFound()");
        }
        var broadcastIdFound = source.getBroadcastId();

        if (mSourceFromQrCode != null && mSourceFromQrCode.getBroadcastId() == UNSET_BROADCAST_ID) {
            // mSourceFromQrCode could have no broadcast Id, we fill in the broadcast Id from the
            // scanned metadata.
            if (DEBUG) {
                Log.d(
                        TAG,
                        "handleSourceFound() : processing mSourceFromQrCode with broadcastId"
                                + " unset");
            }
            boolean updated =
                    maybeUpdateId(
                            AudioStreamsHelper.getBroadcastName(source), source.getBroadcastId());
            if (updated && mBroadcastIdToPreferenceMap.containsKey(UNSET_BROADCAST_ID)) {
                var preference = mBroadcastIdToPreferenceMap.remove(UNSET_BROADCAST_ID);
                mBroadcastIdToPreferenceMap.put(source.getBroadcastId(), preference);
            }
        }

        mBroadcastIdToPreferenceMap.compute(
                broadcastIdFound,
                (k, existingPreference) -> {
                    if (existingPreference == null) {
                        return addNewPreference(
                                source,
                                AudioStreamState.SYNCED,
                                SourceOriginForLogging.BROADCAST_SEARCH);
                    }
                    var fromState = existingPreference.getAudioStreamState();
                    if (fromState == AudioStreamState.WAIT_FOR_SYNC && mSourceFromQrCode != null) {
                        // A preference with source founded is existed from a QR code scan. As the
                        // source is now synced, we update the preference with source from scanning
                        // as it includes complete broadcast info.
                        existingPreference.setAudioStreamMetadata(
                                new BluetoothLeBroadcastMetadata.Builder(source)
                                        .setBroadcastCode(mSourceFromQrCode.getBroadcastCode())
                                        .build());
                        moveToState(
                                existingPreference, AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE);
                    } else {
                        // A preference with source founded existed either because it's already
                        // connected (SOURCE_ADDED) or present (SOURCE_PRESENT). Any other reason
                        // is unexpected. We update the preference with this source and won't
                        // change it's state.
                        existingPreference.setAudioStreamMetadata(source);
                        if (fromState != AudioStreamState.SOURCE_ADDED
                                && (!audioSharingHysteresisModeFix()
                                        || fromState != AudioStreamState.SOURCE_PRESENT)) {
                            Log.w(
                                    TAG,
                                    "handleSourceFound(): unexpected state : "
                                            + fromState
                                            + " for broadcastId : "
                                            + broadcastIdFound);
                        }
                    }
                    return existingPreference;
                });
    }

    private boolean maybeUpdateId(String targetBroadcastName, int broadcastIdToSet) {
        if (mSourceFromQrCode == null) {
            return false;
        }
        if (targetBroadcastName.equals(AudioStreamsHelper.getBroadcastName(mSourceFromQrCode))) {
            if (DEBUG) {
                Log.d(
                        TAG,
                        "maybeUpdateId() : updating unset broadcastId for metadataFromQrCode with"
                                + " broadcastName: "
                                + AudioStreamsHelper.getBroadcastName(mSourceFromQrCode)
                                + " to broadcast Id: "
                                + broadcastIdToSet);
            }
            mSourceFromQrCode =
                    new BluetoothLeBroadcastMetadata.Builder(mSourceFromQrCode)
                            .setBroadcastId(broadcastIdToSet)
                            .build();
            return true;
        }
        return false;
    }

    // Find preference by mSourceFromQrCode and decide next state.
    // Expect no preference existed, create new preference with state WAIT_FOR_SYNC
    private void handleSourceFromQrCodeIfExists() {
        if (DEBUG) {
            Log.d(TAG, "handleSourceFromQrCodeIfExists()");
        }
        if (mSourceFromQrCode == null) {
            return;
        }
        mBroadcastIdToPreferenceMap.compute(
                mSourceFromQrCode.getBroadcastId(),
                (k, existingPreference) -> {
                    if (existingPreference == null) {
                        // No existing preference for this source from the QR code scan, add one and
                        // set initial state to WAIT_FOR_SYNC.
                        // Check nullability to bypass NullAway check.
                        if (mSourceFromQrCode != null) {
                            return addNewPreference(
                                    mSourceFromQrCode,
                                    AudioStreamState.WAIT_FOR_SYNC,
                                    mSourceFromQrCodeOriginForLogging);
                        }
                    }
                    Log.w(
                            TAG,
                            "handleSourceFromQrCodeIfExists(): unexpected state : "
                                    + existingPreference.getAudioStreamState()
                                    + " for broadcastId : "
                                    + (mSourceFromQrCode == null
                                            ? "null"
                                            : mSourceFromQrCode.getBroadcastId()));
                    return existingPreference;
                });
    }

    void handleSourceLost(int broadcastId) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceLost()");
        }
        if (mAudioStreamsHelper.getAllConnectedSources().stream()
                .anyMatch(connected -> connected.getBroadcastId() == broadcastId)) {
            Log.d(
                    TAG,
                    "handleSourceLost() : keep this preference as the source is still connected.");
            return;
        }
        var toRemove = mBroadcastIdToPreferenceMap.remove(broadcastId);
        if (toRemove != null) {
            ThreadUtils.postOnMainThread(
                    () -> {
                        if (mCategoryPreference != null) {
                            mCategoryPreference.removePreference(toRemove);
                        }
                    });
        }
    }

    void handleSourceRemoved() {
        if (DEBUG) {
            Log.d(TAG, "handleSourceRemoved()");
        }
        for (var entry : mBroadcastIdToPreferenceMap.entrySet()) {
            var preference = entry.getValue();

            // Look for preference has SOURCE_ADDED or SOURCE_PRESENT state, re-check if they are
            // still connected. If
            // not, means the source is removed from the sink, we move back the preference to SYNCED
            // state.
            if ((preference.getAudioStreamState() == AudioStreamState.SOURCE_ADDED
                            || (audioSharingHysteresisModeFix()
                                    && preference.getAudioStreamState()
                                            == AudioStreamState.SOURCE_PRESENT))
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

    // Find preference by receiveState and decide next state.
    // Expect one of the following:
    // 1) No preference existed, create new preference with state SOURCE_ADDED
    // 2) Any other state, move to SOURCE_ADDED
    void handleSourceConnected(BluetoothLeBroadcastReceiveState receiveState) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceConnected()");
        }
        if (!AudioStreamsHelper.isConnected(receiveState)) {
            return;
        }

        var broadcastIdConnected = receiveState.getBroadcastId();
        if (mSourceFromQrCode != null && mSourceFromQrCode.getBroadcastId() == UNSET_BROADCAST_ID) {
            // mSourceFromQrCode could have no broadcast Id, we fill in the broadcast Id from the
            // connected source receiveState.
            if (DEBUG) {
                Log.d(
                        TAG,
                        "handleSourceConnected() : processing mSourceFromQrCode with broadcastId"
                                + " unset");
            }
            boolean updated =
                    maybeUpdateId(
                            AudioStreamsHelper.getBroadcastName(receiveState),
                            receiveState.getBroadcastId());
            if (updated && mBroadcastIdToPreferenceMap.containsKey(UNSET_BROADCAST_ID)) {
                var preference = mBroadcastIdToPreferenceMap.remove(UNSET_BROADCAST_ID);
                mBroadcastIdToPreferenceMap.put(receiveState.getBroadcastId(), preference);
            }
        }

        mBroadcastIdToPreferenceMap.compute(
                broadcastIdConnected,
                (k, existingPreference) -> {
                    if (existingPreference == null) {
                        // No existing preference for this source even if it's already connected,
                        // add one and set initial state to SOURCE_ADDED. This could happen because
                        // we retrieves the connected source during onStart() from
                        // AudioStreamsHelper#getAllConnectedSources() even before the source is
                        // founded by scanning.
                        return addNewPreference(receiveState, AudioStreamState.SOURCE_ADDED);
                    }
                    if (existingPreference.getAudioStreamState() == AudioStreamState.WAIT_FOR_SYNC
                            && existingPreference.getAudioStreamBroadcastId() == UNSET_BROADCAST_ID
                            && mSourceFromQrCode != null) {
                        existingPreference.setAudioStreamMetadata(mSourceFromQrCode);
                    }
                    moveToState(existingPreference, AudioStreamState.SOURCE_ADDED);
                    return existingPreference;
                });
    }

    // Find preference by receiveState and decide next state.
    // Expect one preference existed, move to ADD_SOURCE_BAD_CODE
    void handleSourceConnectBadCode(BluetoothLeBroadcastReceiveState receiveState) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceConnectBadCode()");
        }
        if (!AudioStreamsHelper.isBadCode(receiveState)) {
            return;
        }
        mBroadcastIdToPreferenceMap.computeIfPresent(
                receiveState.getBroadcastId(),
                (k, existingPreference) -> {
                    moveToState(existingPreference, AudioStreamState.ADD_SOURCE_BAD_CODE);
                    return existingPreference;
                });
    }

    // Find preference by broadcastId and decide next state.
    // Expect one preference existed, move to ADD_SOURCE_FAILED
    void handleSourceFailedToConnect(int broadcastId) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceFailedToConnect()");
        }
        mBroadcastIdToPreferenceMap.computeIfPresent(
                broadcastId,
                (k, existingPreference) -> {
                    moveToState(existingPreference, AudioStreamState.ADD_SOURCE_FAILED);
                    return existingPreference;
                });
    }

    // Find preference by receiveState and decide next state.
    // Expect one preference existed, move to SOURCE_PRESENT
    void handleSourcePresent(BluetoothLeBroadcastReceiveState receiveState) {
        if (DEBUG) {
            Log.d(TAG, "handleSourcePresent()");
        }
        if (!AudioStreamsHelper.hasSourcePresent(receiveState)) {
            return;
        }

        var broadcastIdConnected = receiveState.getBroadcastId();
        if (mSourceFromQrCode != null && mSourceFromQrCode.getBroadcastId() == UNSET_BROADCAST_ID) {
            // mSourceFromQrCode could have no broadcast Id, we fill in the broadcast Id from the
            // connected source receiveState.
            if (DEBUG) {
                Log.d(
                        TAG,
                        "handleSourcePresent() : processing mSourceFromQrCode with broadcastId"
                                + " unset");
            }
            boolean updated =
                    maybeUpdateId(
                            AudioStreamsHelper.getBroadcastName(receiveState),
                            receiveState.getBroadcastId());
            if (updated && mBroadcastIdToPreferenceMap.containsKey(UNSET_BROADCAST_ID)) {
                var preference = mBroadcastIdToPreferenceMap.remove(UNSET_BROADCAST_ID);
                mBroadcastIdToPreferenceMap.put(receiveState.getBroadcastId(), preference);
            }
        }

        mBroadcastIdToPreferenceMap.compute(
                broadcastIdConnected,
                (k, existingPreference) -> {
                    if (existingPreference == null) {
                        // No existing preference for this source even if it's already connected,
                        // add one and set initial state to SOURCE_PRESENT. This could happen
                        // because
                        // we retrieves the connected source during onStart() from
                        // AudioStreamsHelper#getAllPresentSources() even before the source is
                        // founded by scanning.
                        return addNewPreference(receiveState, AudioStreamState.SOURCE_PRESENT);
                    }
                    if (existingPreference.getAudioStreamState() == AudioStreamState.WAIT_FOR_SYNC
                            && existingPreference.getAudioStreamBroadcastId() == UNSET_BROADCAST_ID
                            && mSourceFromQrCode != null) {
                        existingPreference.setAudioStreamMetadata(mSourceFromQrCode);
                    }
                    moveToState(existingPreference, AudioStreamState.SOURCE_PRESENT);
                    return existingPreference;
                });
    }

    // Find preference by metadata and decide next state.
    // Expect one preference existed, move to ADD_SOURCE_WAIT_FOR_RESPONSE
    void handleSourceAddRequest(
            AudioStreamPreference preference, BluetoothLeBroadcastMetadata metadata) {
        if (DEBUG) {
            Log.d(TAG, "handleSourceAddRequest()");
        }
        mBroadcastIdToPreferenceMap.computeIfPresent(
                metadata.getBroadcastId(),
                (k, existingPreference) -> {
                    if (!existingPreference.equals(preference)) {
                        Log.w(TAG, "handleSourceAddedRequest(): existing preference not match");
                    }
                    existingPreference.setAudioStreamMetadata(metadata);
                    moveToState(existingPreference, AudioStreamState.ADD_SOURCE_WAIT_FOR_RESPONSE);
                    return existingPreference;
                });
    }

    void showToast(String msg) {
        AudioSharingUtils.toastMessage(mContext, msg);
    }

    private void init() {
        mBroadcastIdToPreferenceMap.clear();
        boolean hasConnected =
                AudioStreamsHelper.getCachedBluetoothDeviceInSharingOrLeConnected(mBluetoothManager)
                        .isPresent();
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> {
                    if (mCategoryPreference != null) {
                        mCategoryPreference.removeAudioStreamPreferences();
                        mCategoryPreference.setVisible(hasConnected);
                    }
                });
        if (hasConnected) {
            startScanning();
            AudioSharingUtils.postOnMainThread(
                    mContext,
                    () -> {
                        if (mFragment != null) {
                            AudioStreamsDialogFragment.dismissAll(mFragment);
                        }
                    });
        } else {
            stopScanning();
            AudioSharingUtils.postOnMainThread(
                    mContext,
                    () -> {
                        if (mFragment != null) {
                            AudioStreamsDialogFragment.show(
                                    mFragment,
                                    getNoLeDeviceDialog(),
                                    SettingsEnums.DIALOG_AUDIO_STREAM_MAIN_NO_LE_DEVICE);
                        }
                    });
        }
    }

    private void startScanning() {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "startScanning(): LeBroadcastAssistant is null!");
            return;
        }
        if (mLeBroadcastAssistant.isSearchInProgress()) {
            Log.w(TAG, "startScanning(): scanning still in progress, stop scanning first.");
            stopScanning();
        }
        mLeBroadcastAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
        mExecutor.execute(
                () -> {
                    // Handle QR code scan, display currently connected streams then start scanning
                    // sequentially
                    handleSourceFromQrCodeIfExists();
                    if (audioSharingHysteresisModeFix()) {
                        // With hysteresis mode, we prioritize showing connected sources first.
                        // If no connected sources are found, we then show present sources.
                        List<BluetoothLeBroadcastReceiveState> sources =
                                mAudioStreamsHelper.getAllConnectedSources();
                        if (!sources.isEmpty()) {
                            sources.forEach(this::handleSourceConnected);
                        } else {
                            mAudioStreamsHelper
                                    .getAllPresentSources()
                                    .forEach(this::handleSourcePresent);
                        }
                    } else {
                        mAudioStreamsHelper
                                .getAllConnectedSources()
                                .forEach(this::handleSourceConnected);
                    }
                    mLeBroadcastAssistant.startSearchingForSources(emptyList());
                    mMediaControlHelper.start();
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
            mLeBroadcastAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
        }
        mMediaControlHelper.stop();
        mSourceFromQrCode = null;
    }

    private AudioStreamPreference addNewPreference(
            BluetoothLeBroadcastReceiveState receiveState, AudioStreamState state) {
        var preference = AudioStreamPreference.fromReceiveState(mContext, receiveState);
        moveToState(preference, state);
        return preference;
    }

    private AudioStreamPreference addNewPreference(
            BluetoothLeBroadcastMetadata metadata,
            AudioStreamState state,
            SourceOriginForLogging sourceOriginForLogging) {
        var preference =
                AudioStreamPreference.fromMetadata(mContext, metadata, sourceOriginForLogging);
        moveToState(preference, state);
        return preference;
    }

    @VisibleForTesting
    void moveToState(AudioStreamPreference preference, AudioStreamState state) {
        AudioStreamStateHandler stateHandler =
                switch (state) {
                    case SYNCED -> SyncedState.getInstance();
                    case WAIT_FOR_SYNC -> WaitForSyncState.getInstance();
                    case ADD_SOURCE_WAIT_FOR_RESPONSE ->
                            AddSourceWaitForResponseState.getInstance();
                    case ADD_SOURCE_BAD_CODE -> AddSourceBadCodeState.getInstance();
                    case ADD_SOURCE_FAILED -> AddSourceFailedState.getInstance();
                    case SOURCE_PRESENT -> SourcePresentState.getInstance();
                    case SOURCE_ADDED -> SourceAddedState.getInstance();
                    default -> throw new IllegalArgumentException("Unsupported state: " + state);
                };

        stateHandler.handleStateChange(preference, this, mAudioStreamsHelper);

        // Update UI with the updated preference
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> {
                    if (mCategoryPreference != null) {
                        mCategoryPreference.addAudioStreamPreference(preference, mComparator);
                    }
                });
    }

    private AudioStreamsDialogFragment.DialogBuilder getNoLeDeviceDialog() {
        return new AudioStreamsDialogFragment.DialogBuilder(mContext)
                .setTitle(mContext.getString(R.string.audio_streams_dialog_no_le_device_title))
                .setSubTitle2(
                        mContext.getString(R.string.audio_streams_dialog_no_le_device_subtitle))
                .setLeftButtonText(mContext.getString(R.string.audio_streams_dialog_close))
                .setLeftButtonOnClickListener(AlertDialog::dismiss)
                .setRightButtonText(
                        mContext.getString(R.string.audio_streams_dialog_no_le_device_button))
                .setRightButtonOnClickListener(
                        dialog -> {
                            new SubSettingLauncher(mContext)
                                    .setDestination(
                                            ConnectedDeviceDashboardFragment.class.getName())
                                    .setSourceMetricsCategory(
                                            SettingsEnums.DIALOG_AUDIO_STREAM_MAIN_NO_LE_DEVICE)
                                    .launch();
                            dialog.dismiss();
                        });
    }
}
