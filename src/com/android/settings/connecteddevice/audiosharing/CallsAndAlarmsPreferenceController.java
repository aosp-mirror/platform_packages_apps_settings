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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** PreferenceController to control the dialog to choose the active device for calls and alarms */
public class CallsAndAlarmsPreferenceController extends AudioSharingBasePreferenceController
        implements BluetoothCallback {

    private static final String TAG = "CallsAndAlarmsPreferenceController";
    private static final String PREF_KEY = "calls_and_alarms";

    private final LocalBluetoothManager mLocalBtManager;
    private final Executor mExecutor;
    @Nullable private LocalBluetoothLeBroadcastAssistant mAssistant = null;
    private DashboardFragment mFragment;
    Map<Integer, List<CachedBluetoothDevice>> mGroupedConnectedDevices = new HashMap<>();
    private ArrayList<AudioSharingDeviceItem> mDeviceItemsInSharingSession = new ArrayList<>();

    private BluetoothLeBroadcastAssistant.Callback mBroadcastAssistantCallback =
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
                public void onSourceAdded(@NonNull BluetoothDevice sink, int sourceId, int reason) {
                    Log.d(TAG, "onSourceAdded");
                    updatePreference();
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
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                    Log.d(TAG, "onSourceRemoved");
                    updatePreference();
                }

                @Override
                public void onSourceRemoveFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onReceiveStateChanged(
                        BluetoothDevice sink,
                        int sourceId,
                        BluetoothLeBroadcastReceiveState state) {}
            };

    public CallsAndAlarmsPreferenceController(Context context) {
        super(context, PREF_KEY);
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        if (mLocalBtManager != null) {
            mAssistant = mLocalBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        }
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference.setOnPreferenceClickListener(
                preference -> {
                    if (mFragment == null) {
                        Log.w(TAG, "Dialog fail to show due to null host.");
                        return true;
                    }
                    updateDeviceItemsInSharingSession();
                    if (mDeviceItemsInSharingSession.size() >= 1) {
                        CallsAndAlarmsDialogFragment.show(
                                mFragment,
                                mDeviceItemsInSharingSession,
                                (AudioSharingDeviceItem item) -> {
                                    if (!mGroupedConnectedDevices.containsKey(item.getGroupId())) {
                                        return;
                                    }
                                    List<CachedBluetoothDevice> devices =
                                            mGroupedConnectedDevices.get(item.getGroupId());
                                    @Nullable
                                    CachedBluetoothDevice lead =
                                            AudioSharingUtils.getLeadDevice(devices);
                                    if (lead != null) {
                                        Log.d(
                                                TAG,
                                                "Set fallback active device: "
                                                        + lead.getDevice().getAnonymizedAddress());
                                        lead.setActive();
                                        updatePreference();
                                    } else {
                                        Log.w(
                                                TAG,
                                                "Fail to set fallback active device: no lead"
                                                        + " device");
                                    }
                                });
                    }
                    return true;
                });
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        super.onStart(owner);
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().registerCallback(this);
        }
        if (mAssistant != null) {
            mAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        super.onStop(owner);
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().unregisterCallback(this);
        }
        if (mAssistant != null) {
            mAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
        }
    }

    @Override
    public void updateVisibility() {
        if (mPreference == null) return;
        var unused = ThreadUtils.postOnBackgroundThread(() -> updatePreference());
    }

    private void updatePreference() {
        boolean isVisible = isBroadcasting() && isBluetoothStateOn();
        if (!isVisible) {
            AudioSharingUtils.postOnMainThread(mContext, () -> mPreference.setVisible(false));
            return;
        }
        updateDeviceItemsInSharingSession();
        int fallbackActiveGroupId = AudioSharingUtils.getFallbackActiveGroupId(mContext);
        Log.d(TAG, "updatePreference: get fallback active group " + fallbackActiveGroupId);
        if (fallbackActiveGroupId != BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
            for (AudioSharingDeviceItem item : mDeviceItemsInSharingSession) {
                if (item.getGroupId() == fallbackActiveGroupId) {
                    AudioSharingUtils.postOnMainThread(
                            mContext,
                            () -> {
                                mPreference.setSummary(item.getName());
                                mPreference.setVisible(true);
                            });
                    return;
                }
            }
        }
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> {
                    mPreference.setSummary("No active device in sharing");
                    mPreference.setVisible(true);
                });
    }

    @Override
    public void onProfileConnectionStateChanged(
            @NonNull CachedBluetoothDevice cachedDevice,
            @ConnectionState int state,
            int bluetoothProfile) {
        if (state == BluetoothAdapter.STATE_DISCONNECTED
                && bluetoothProfile == BluetoothProfile.LE_AUDIO) {
            // The fallback active device could be updated if the previous fallback device is
            // disconnected.
            updatePreference();
        }
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to host the {@link CallsAndAlarmsDialogFragment} dialog.
     */
    public void init(DashboardFragment fragment) {
        this.mFragment = fragment;
    }

    private void updateDeviceItemsInSharingSession() {
        mGroupedConnectedDevices =
                AudioSharingUtils.fetchConnectedDevicesByGroupId(mLocalBtManager);
        mDeviceItemsInSharingSession =
                AudioSharingUtils.buildOrderedConnectedLeadAudioSharingDeviceItem(
                        mLocalBtManager, mGroupedConnectedDevices, /* filterByInSharing= */ true);
    }
}
