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

package com.android.settings.connecteddevice.audiosharing;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.flags.Flags;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/** PreferenceController to control the dialog to choose the active device for calls and alarms */
public class AudioSharingCallAudioPreferenceController extends AudioSharingBasePreferenceController
        implements BluetoothCallback {
    private static final String TAG = "CallAudioPrefController";
    private static final String PREF_KEY = "calls_and_alarms";

    @VisibleForTesting
    enum ChangeCallAudioType {
        UNKNOWN,
        CONNECTED_EARLIER,
        CONNECTED_LATER
    }

    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final BluetoothEventManager mEventManager;
    @Nullable private final ContentResolver mContentResolver;
    @Nullable private final LocalBluetoothLeBroadcastAssistant mAssistant;
    @Nullable private final CachedBluetoothDeviceManager mCacheManager;
    private final Executor mExecutor;
    private final ContentObserver mSettingsObserver;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    @Nullable private Fragment mFragment;
    Map<Integer, List<BluetoothDevice>> mGroupedConnectedDevices = new HashMap<>();
    private List<AudioSharingDeviceItem> mDeviceItemsInSharingSession = new ArrayList<>();
    private final AtomicBoolean mCallbacksRegistered = new AtomicBoolean(false);

    @VisibleForTesting
    final BluetoothLeBroadcastAssistant.Callback mBroadcastAssistantCallback =
            new BluetoothLeBroadcastAssistant.Callback() {
                @Override
                public void onSearchStarted(int reason) {}

                @Override
                public void onSearchStartFailed(int reason) {}

                @Override
                public void onSearchStopped(int reason) {}

                @Override
                public void onSearchStopFailed(int reason) {}

                @Override
                public void onSourceFound(@NonNull BluetoothLeBroadcastMetadata source) {}

                @Override
                public void onSourceAdded(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                    Log.d(TAG, "onSourceAdded: updateSummary");
                    updateSummary();
                }

                @Override
                public void onSourceAddFailed(
                        @NonNull BluetoothDevice sink,
                        @NonNull BluetoothLeBroadcastMetadata source,
                        int reason) {}

                @Override
                public void onSourceModified(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceModifyFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceRemoved(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceRemoveFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onReceiveStateChanged(
                        @NonNull BluetoothDevice sink,
                        int sourceId,
                        @NonNull BluetoothLeBroadcastReceiveState state) {}
            };

    public AudioSharingCallAudioPreferenceController(Context context) {
        super(context, PREF_KEY);
        mBtManager = Utils.getLocalBtManager(mContext);
        LocalBluetoothProfileManager profileManager =
                mBtManager == null ? null : mBtManager.getProfileManager();
        mEventManager = mBtManager == null ? null : mBtManager.getEventManager();
        mAssistant =
                profileManager == null
                        ? null
                        : profileManager.getLeAudioBroadcastAssistantProfile();
        mCacheManager = mBtManager == null ? null : mBtManager.getCachedDeviceManager();
        mExecutor = Executors.newSingleThreadExecutor();
        mContentResolver = context.getContentResolver();
        mSettingsObserver = new FallbackDeviceGroupIdSettingsObserver();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    private class FallbackDeviceGroupIdSettingsObserver extends ContentObserver {
        FallbackDeviceGroupIdSettingsObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "onChange, fallback device group id has been changed");
            var unused =
                    ThreadUtils.postOnBackgroundThread(
                            AudioSharingCallAudioPreferenceController.this::updateSummary);
        }
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mPreference != null) {
            mPreference.setVisible(false);
            updateSummary();
            mPreference.setOnPreferenceClickListener(
                    preference -> {
                        if (mFragment == null) {
                            Log.w(TAG, "Dialog fail to show due to null host.");
                            return true;
                        }
                        updateDeviceItemsInSharingSession();
                        if (!mDeviceItemsInSharingSession.isEmpty()) {
                            Pair<Integer, AudioSharingDeviceItem> pair = getActiveItemWithIndex();
                            AudioSharingCallAudioDialogFragment.show(
                                    mFragment,
                                    mDeviceItemsInSharingSession,
                                    pair == null ? -1 : pair.first,
                                    (AudioSharingDeviceItem item) -> {
                                        int currentGroupId =
                                                BluetoothUtils.getPrimaryGroupIdForBroadcast(
                                                        mContext.getContentResolver());
                                        int clickedGroupId = item.getGroupId();
                                        if (clickedGroupId == currentGroupId) {
                                            Log.d(TAG, "Skip set call audio device: unchanged");
                                            return;
                                        }
                                        List<BluetoothDevice> devices =
                                                mGroupedConnectedDevices.getOrDefault(
                                                        clickedGroupId, ImmutableList.of());
                                        CachedBluetoothDevice lead =
                                                AudioSharingUtils.getLeadDevice(
                                                        mCacheManager, devices);
                                        if (lead != null) {
                                            String addr = lead.getDevice().getAnonymizedAddress();
                                            Log.d(TAG, "Set call audio device: " + addr);
                                            AudioSharingUtils.setPrimary(mContext, lead);
                                            logCallAudioDeviceChange(currentGroupId, lead);
                                        } else {
                                            Log.d(TAG, "Skip set call audio device: no lead");
                                        }
                                    });
                        }
                        return true;
                    });
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        super.onStart(owner);
        registerCallbacks();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        super.onStop(owner);
        unregisterCallbacks();
    }

    @Override
    public void onProfileConnectionStateChanged(
            @NonNull CachedBluetoothDevice cachedDevice,
            @ConnectionState int state,
            int bluetoothProfile) {
        if (state == BluetoothAdapter.STATE_DISCONNECTED
                && bluetoothProfile == BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT) {
            Log.d(TAG, "updatePreference, LE_AUDIO_BROADCAST_ASSISTANT is disconnected.");
            // The fallback active device could be updated if the previous fallback device is
            // disconnected.
            updateSummary();
        }
    }

    @Override
    public void onActiveDeviceChanged(@Nullable CachedBluetoothDevice activeDevice,
            int bluetoothProfile) {
        if (activeDevice != null && bluetoothProfile == BluetoothProfile.LE_AUDIO
                && BluetoothUtils.isBroadcasting(mBtManager)) {
            Log.d(TAG, "onActiveDeviceChanged: update summary, device = "
                    + activeDevice.getDevice().getAnonymizedAddress()
                    + ", profile = " + bluetoothProfile);
            updateSummary();
        }
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to host the {@link AudioSharingCallAudioDialogFragment} dialog.
     */
    public void init(Fragment fragment) {
        this.mFragment = fragment;
    }

    @VisibleForTesting
    ContentObserver getSettingsObserver() {
        return mSettingsObserver;
    }

    /** Test only: set callback registration status in tests. */
    @VisibleForTesting
    void setCallbacksRegistered(boolean registered) {
        mCallbacksRegistered.set(registered);
    }

    private void registerCallbacks() {
        if (!isAvailable()) {
            Log.d(TAG, "Skip registerCallbacks(). Feature is not available.");
            return;
        }
        if (mEventManager == null || mContentResolver == null || mAssistant == null) {
            Log.d(
                    TAG,
                    "Skip registerCallbacks(). Init is not ready: eventManager = "
                            + (mEventManager == null)
                            + ", contentResolver"
                            + (mContentResolver == null));
            return;
        }
        if (!mCallbacksRegistered.get()) {
            Log.d(TAG, "registerCallbacks()");
            mEventManager.registerCallback(this);
            mContentResolver.registerContentObserver(
                    Settings.Secure.getUriFor(BluetoothUtils.getPrimaryGroupIdUriForBroadcast()),
                    false,
                    mSettingsObserver);
            mAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
            mCallbacksRegistered.set(true);
        }
    }

    private void unregisterCallbacks() {
        if (!isAvailable()) {
            Log.d(TAG, "Skip unregisterCallbacks(). Feature is not available.");
            return;
        }
        if (mEventManager == null || mContentResolver == null || mAssistant == null) {
            Log.d(TAG, "Skip unregisterCallbacks(). Init is not ready.");
            return;
        }
        if (mCallbacksRegistered.get()) {
            Log.d(TAG, "unregisterCallbacks()");
            mEventManager.unregisterCallback(this);
            mContentResolver.unregisterContentObserver(mSettingsObserver);
            mAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
            mCallbacksRegistered.set(false);
        }
    }

    /**
     * Update the preference summary: current headset for call audio.
     *
     * <p>The summary should be updated when:
     *
     * <p>1. displayPreference.
     *
     * <p>2. ContentObserver#onChange: the fallback device value in SettingsProvider is changed.
     *
     * <p>3. onProfileConnectionStateChanged: the assistant profile of fallback device disconnected.
     * When the last headset in audio sharing disconnected, both Settings and bluetooth framework
     * won't set the SettingsProvider, so no ContentObserver#onChange.
     *
     * <p>4. onReceiveStateChanged: new headset join the audio sharing. If the headset has already
     * been set as fallback device in SettingsProvider by bluetooth framework when the broadcast is
     * started, Settings won't set the SettingsProvider again when the headset join the audio
     * sharing, so there won't be ContentObserver#onChange. We need listen to onReceiveStateChanged
     * to handle this scenario.
     */
    private void updateSummary() {
        updateDeviceItemsInSharingSession();
        Pair<Integer, AudioSharingDeviceItem> pair = getActiveItemWithIndex();
        if (pair != null) {
            Log.d(TAG, "updateSummary, group = " + pair.second.getGroupId());
            AudioSharingUtils.postOnMainThread(
                    mContext,
                    () -> {
                        if (mPreference != null) {
                            mPreference.setSummary(
                                    mContext.getString(
                                            R.string.audio_sharing_call_audio_description,
                                            pair.second.getName()));
                        }
                    });
            return;
        }
        Log.d(TAG, "updateSummary: set empty");
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> {
                    if (mPreference != null) {
                        mPreference.setSummary("");
                    }
                });
    }

    private void updateDeviceItemsInSharingSession() {
        mGroupedConnectedDevices = AudioSharingUtils.fetchConnectedDevicesByGroupId(mBtManager);
        if (Flags.enableTemporaryBondDevicesUi()) {
            mGroupedConnectedDevices =
                    mGroupedConnectedDevices.entrySet().stream()
                            .filter(entry -> !anyTemporaryBondDevice(entry.getValue()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        mDeviceItemsInSharingSession =
                AudioSharingUtils.buildOrderedConnectedLeadAudioSharingDeviceItem(
                        mBtManager, mGroupedConnectedDevices, /* filterByInSharing= */ true);
    }

    private boolean anyTemporaryBondDevice(List<BluetoothDevice> connectedDevices) {
        return connectedDevices.stream().anyMatch(BluetoothUtils::isTemporaryBondDevice);
    }

    @Nullable
    private Pair<Integer, AudioSharingDeviceItem> getActiveItemWithIndex() {
        List<AudioSharingDeviceItem> deviceItems = new ArrayList<>(mDeviceItemsInSharingSession);
        int fallbackActiveGroupId =
                BluetoothUtils.getPrimaryGroupIdForBroadcast(mContext.getContentResolver());
        if (fallbackActiveGroupId != BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
            for (AudioSharingDeviceItem item : deviceItems) {
                if (item.getGroupId() == fallbackActiveGroupId) {
                    Log.d(TAG, "getActiveItemWithIndex, fallback group = " + item.getGroupId());
                    return new Pair<>(deviceItems.indexOf(item), item);
                }
            }
        }
        for (AudioSharingDeviceItem item : deviceItems) {
            if (item.isActive()) {
                Log.d(TAG, "getActiveItemWithIndex, active LEA group = " + item.getGroupId());
                return new Pair<>(deviceItems.indexOf(item), item);
            }
        }
        return null;
    }

    @VisibleForTesting
    void logCallAudioDeviceChange(int currentGroupId, CachedBluetoothDevice target) {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            ChangeCallAudioType type = ChangeCallAudioType.UNKNOWN;
                            if (mCacheManager != null) {
                                int targetDeviceGroupId = BluetoothUtils.getGroupId(target);
                                List<BluetoothDevice> mostRecentDevices =
                                        BluetoothAdapter.getDefaultAdapter()
                                                .getMostRecentlyConnectedDevices();
                                int targetDeviceIdx = -1;
                                int currentDeviceIdx = -1;
                                for (int idx = 0; idx < mostRecentDevices.size(); idx++) {
                                    BluetoothDevice device = mostRecentDevices.get(idx);
                                    CachedBluetoothDevice cachedDevice =
                                            mCacheManager.findDevice(device);
                                    int groupId =
                                            cachedDevice != null
                                                    ? BluetoothUtils.getGroupId(cachedDevice)
                                                    : BluetoothCsipSetCoordinator.GROUP_ID_INVALID;
                                    if (groupId != BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
                                        if (groupId == targetDeviceGroupId) {
                                            targetDeviceIdx = idx;
                                        } else if (groupId == currentGroupId) {
                                            currentDeviceIdx = idx;
                                        }
                                    }
                                    if (targetDeviceIdx != -1 && currentDeviceIdx != -1) break;
                                }
                                if (targetDeviceIdx != -1 && currentDeviceIdx != -1) {
                                    type =
                                            targetDeviceIdx < currentDeviceIdx
                                                    ? ChangeCallAudioType.CONNECTED_LATER
                                                    : ChangeCallAudioType.CONNECTED_EARLIER;
                                }
                            }
                            mMetricsFeatureProvider.action(
                                    mContext,
                                    SettingsEnums.ACTION_AUDIO_SHARING_CHANGE_CALL_AUDIO,
                                    type.ordinal());
                        });
    }
}
