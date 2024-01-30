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

import android.annotation.Nullable;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** PreferenceController to control the dialog to choose the active device for calls and alarms */
public class CallsAndAlarmsPreferenceController extends AudioSharingBasePreferenceController
        implements BluetoothCallback {

    private static final String TAG = "CallsAndAlarmsPreferenceController";
    private static final String PREF_KEY = "calls_and_alarms";

    private final LocalBluetoothManager mLocalBtManager;
    private DashboardFragment mFragment;
    Map<Integer, List<CachedBluetoothDevice>> mGroupedConnectedDevices = new HashMap<>();
    private ArrayList<AudioSharingDeviceItem> mDeviceItemsInSharingSession = new ArrayList<>();

    public CallsAndAlarmsPreferenceController(Context context) {
        super(context, PREF_KEY);
        mLocalBtManager = Utils.getLocalBtManager(mContext);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference.setOnPreferenceClickListener(
                preference -> {
                    if (mFragment == null) {
                        Log.w(TAG, "Dialog fail to show due to null host.");
                        return true;
                    }
                    updateDeviceItemsInSharingSession();
                    if (mDeviceItemsInSharingSession.size() >= 2) {
                        CallsAndAlarmsDialogFragment.show(
                                mFragment,
                                mDeviceItemsInSharingSession,
                                (AudioSharingDeviceItem item) -> {
                                    for (CachedBluetoothDevice device :
                                            mGroupedConnectedDevices.get(item.getGroupId())) {
                                        device.setActive();
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
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        super.onStop(owner);
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().unregisterCallback(this);
        }
    }

    @Override
    public void updateVisibility() {
        if (mPreference == null) return;
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            boolean isVisible = isBroadcasting() && isBluetoothStateOn();
                            if (!isVisible) {
                                ThreadUtils.postOnMainThread(() -> mPreference.setVisible(false));
                            } else {
                                updateDeviceItemsInSharingSession();
                                // mDeviceItemsInSharingSession is ordered. The active device is the
                                // first
                                // place if exits.
                                if (!mDeviceItemsInSharingSession.isEmpty()
                                        && mDeviceItemsInSharingSession.get(0).isActive()) {
                                    ThreadUtils.postOnMainThread(
                                            () -> {
                                                mPreference.setVisible(true);
                                                mPreference.setSummary(
                                                        mDeviceItemsInSharingSession
                                                                .get(0)
                                                                .getName());
                                            });
                                } else {
                                    ThreadUtils.postOnMainThread(
                                            () -> {
                                                mPreference.setVisible(true);
                                                mPreference.setSummary(
                                                        "No active device in sharing");
                                            });
                                }
                            }
                        });
    }

    @Override
    public void onActiveDeviceChanged(
            @Nullable CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        if (bluetoothProfile != BluetoothProfile.LE_AUDIO) {
            Log.d(TAG, "Ignore onActiveDeviceChanged, not LE_AUDIO profile");
            return;
        }
        mPreference.setSummary(activeDevice == null ? "" : activeDevice.getName());
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
