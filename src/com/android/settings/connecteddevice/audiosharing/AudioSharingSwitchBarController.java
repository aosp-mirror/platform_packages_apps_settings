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

import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.util.Log;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingSwitchBarController extends BasePreferenceController
        implements DefaultLifecycleObserver, OnMainSwitchChangeListener {
    private static final String TAG = "AudioSharingSwitchBarCtl";
    private static final String PREF_KEY = "audio_sharing_main_switch";
    private final SettingsMainSwitchBar mSwitchBar;
    private final LocalBluetoothManager mBtManager;
    private final LocalBluetoothLeBroadcast mBroadcast;
    private final Executor mExecutor;
    private DashboardFragment mFragment;

    private final BluetoothLeBroadcast.Callback mBroadcastCallback =
            new BluetoothLeBroadcast.Callback() {
                @Override
                public void onBroadcastStarted(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onBroadcastStarted(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                    updateSwitch();
                }

                @Override
                public void onBroadcastStartFailed(int reason) {
                    Log.d(TAG, "onBroadcastStartFailed(), reason = " + reason);
                    // TODO: handle broadcast start fail
                    updateSwitch();
                }

                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, @NonNull BluetoothLeBroadcastMetadata metadata) {
                    Log.d(
                            TAG,
                            "onBroadcastMetadataChanged(), broadcastId = "
                                    + broadcastId
                                    + ", metadata = "
                                    + metadata);
                    // TODO: handle add sink if there are connected lea devices.
                }

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onBroadcastStopped(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                    updateSwitch();
                }

                @Override
                public void onBroadcastStopFailed(int reason) {
                    Log.d(TAG, "onBroadcastStopFailed(), reason = " + reason);
                    // TODO: handle broadcast stop fail
                    updateSwitch();
                }

                @Override
                public void onBroadcastUpdated(int reason, int broadcastId) {}

                @Override
                public void onBroadcastUpdateFailed(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStarted(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStopped(int reason, int broadcastId) {}
            };

    AudioSharingSwitchBarController(Context context, SettingsMainSwitchBar switchBar) {
        super(context, PREF_KEY);
        mSwitchBar = switchBar;
        mBtManager = Utils.getLocalBtManager(context);
        mBroadcast = mBtManager.getProfileManager().getLeAudioBroadcastProfile();
        mExecutor = Executors.newSingleThreadExecutor();
        mSwitchBar.setChecked(isBroadcasting());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mSwitchBar.addOnSwitchChangeListener(this);
        if (mBroadcast != null) {
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mSwitchBar.removeOnSwitchChangeListener(this);
        if (mBroadcast != null) {
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        // Filter out unnecessary callbacks when switch is disabled.
        if (!switchView.isEnabled()) return;
        if (isChecked) {
            startAudioSharing();
        } else {
            stopAudioSharing();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableLeAudioSharing() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to host the {@link AudioSharingSwitchBarController} dialog.
     */
    public void init(DashboardFragment fragment) {
        this.mFragment = fragment;
    }

    private void startAudioSharing() {
        mSwitchBar.setEnabled(false);
        if (mBroadcast == null || isBroadcasting()) {
            Log.d(TAG, "Already in broadcasting or broadcast not support, ignore!");
            mSwitchBar.setEnabled(true);
            return;
        }
        if (mFragment == null) {
            Log.w(TAG, "Dialog fail to show due to null fragment.");
            mSwitchBar.setEnabled(true);
            return;
        }
        ArrayList<String> deviceNames = new ArrayList<>();
        AudioSharingDialogFragment.show(
                mFragment,
                deviceNames,
                new AudioSharingDialogFragment.DialogEventListener() {
                    @Override
                    public void onItemClick(int position) {
                        // TODO: handle broadcast based on the dialog device item clicked
                    }

                    @Override
                    public void onCancelClick() {
                        mBroadcast.startBroadcast("test", /* language= */ null);
                    }
                });
    }

    private void stopAudioSharing() {
        mSwitchBar.setEnabled(false);
        if (mBroadcast == null || !isBroadcasting()) {
            Log.d(TAG, "Already not broadcasting or broadcast not support, ignore!");
            mSwitchBar.setEnabled(true);
            return;
        }
        mBroadcast.stopBroadcast(mBroadcast.getLatestBroadcastId());
    }

    private void updateSwitch() {
        ThreadUtils.postOnMainThread(
                () -> {
                    mSwitchBar.setChecked(isBroadcasting());
                    mSwitchBar.setEnabled(true);
                });
    }

    private boolean isBroadcasting() {
        return mBroadcast != null && mBroadcast.isEnabled(null);
    }
}
