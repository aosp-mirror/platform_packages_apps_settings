/*
 * Copyright 2018 The Android Open Source Project
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
package com.android.settings.connecteddevice;

import static com.android.settingslib.Utils.isAudioModeOngoingCall;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.HearingAidUtils;
import com.android.settings.bluetooth.AvailableMediaBluetoothDeviceUpdater;
import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingDialogHandler;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Controller to maintain the {@link androidx.preference.PreferenceGroup} for all available media
 * devices. It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}
 */
public class AvailableMediaDeviceGroupController extends BasePreferenceController
        implements DefaultLifecycleObserver, DevicePreferenceCallback, BluetoothCallback {
    private static final String TAG = "AvailableMediaDeviceGroupController";
    private static final String KEY = "available_device_list";

    private final Executor mExecutor;
    @VisibleForTesting @Nullable LocalBluetoothManager mBtManager;
    @VisibleForTesting @Nullable PreferenceGroup mPreferenceGroup;
    @Nullable private LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Nullable private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    @Nullable private FragmentManager mFragmentManager;
    @Nullable private AudioSharingDialogHandler mDialogHandler;
    private BluetoothLeBroadcast.Callback mBroadcastCallback =
            new BluetoothLeBroadcast.Callback() {
                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, BluetoothLeBroadcastMetadata metadata) {}

                @Override
                public void onBroadcastStartFailed(int reason) {}

                @Override
                public void onBroadcastStarted(int reason, int broadcastId) {
                    Log.d(TAG, "onBroadcastStarted: update title.");
                    updateTitle();
                }

                @Override
                public void onBroadcastStopFailed(int reason) {}

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {
                    Log.d(TAG, "onBroadcastStopped: update title.");
                    updateTitle();
                }

                @Override
                public void onBroadcastUpdateFailed(int reason, int broadcastId) {}

                @Override
                public void onBroadcastUpdated(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStarted(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStopped(int reason, int broadcastId) {}
            };

    private BluetoothLeBroadcastAssistant.Callback mAssistantCallback =
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
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

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
                    Log.d(TAG, "onSourceRemoved: update media device list.");
                    if (mBluetoothDeviceUpdater != null) {
                        mBluetoothDeviceUpdater.forceUpdate();
                    }
                }

                @Override
                public void onSourceRemoveFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onReceiveStateChanged(
                        @NonNull BluetoothDevice sink,
                        int sourceId,
                        @NonNull BluetoothLeBroadcastReceiveState state) {
                    if (BluetoothUtils.isConnected(state)) {
                        Log.d(TAG, "onReceiveStateChanged: synced, update media device list.");
                        if (mBluetoothDeviceUpdater != null) {
                            mBluetoothDeviceUpdater.forceUpdate();
                        }
                    }
                }
            };

    public AvailableMediaDeviceGroupController(Context context) {
        super(context, KEY);
        mBtManager = Utils.getLocalBtManager(mContext);
        mExecutor = Executors.newSingleThreadExecutor();
        if (BluetoothUtils.isAudioSharingEnabled()) {
            mBroadcast =
                    mBtManager == null
                            ? null
                            : mBtManager.getProfileManager().getLeAudioBroadcastProfile();
            mAssistant =
                    mBtManager == null
                            ? null
                            : mBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (isAvailable()) {
            updateTitle();
        }
        if (mBtManager == null) {
            Log.d(TAG, "onStart() Bluetooth is not supported on this device");
            return;
        }
        if (BluetoothUtils.isAudioSharingEnabled()) {
            registerAudioSharingCallbacks();
        }
        mBtManager.getEventManager().registerCallback(this);
        if (mBluetoothDeviceUpdater != null) {
            mBluetoothDeviceUpdater.registerCallback();
            mBluetoothDeviceUpdater.refreshPreference();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mBtManager == null) {
            Log.d(TAG, "onStop() Bluetooth is not supported on this device");
            return;
        }
        if (BluetoothUtils.isAudioSharingEnabled()) {
            unregisterAudioSharingCallbacks();
        }
        if (mBluetoothDeviceUpdater != null) {
            mBluetoothDeviceUpdater.unregisterCallback();
        }
        mBtManager.getEventManager().unregisterCallback(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceGroup = screen.findPreference(KEY);
        if (mPreferenceGroup != null) {
            mPreferenceGroup.setVisible(false);
        }

        if (isAvailable()) {
            if (mBluetoothDeviceUpdater != null) {
                mBluetoothDeviceUpdater.setPrefContext(screen.getContext());
                mBluetoothDeviceUpdater.forceUpdate();
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (mPreferenceGroup != null) {
            if (mPreferenceGroup.getPreferenceCount() == 0) {
                mPreferenceGroup.setVisible(true);
            }
            mPreferenceGroup.addPreference(preference);
        }
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        if (mPreferenceGroup != null) {
            mPreferenceGroup.removePreference(preference);
            if (mPreferenceGroup.getPreferenceCount() == 0) {
                mPreferenceGroup.setVisible(false);
            }
        }
    }

    @Override
    public void onDeviceClick(Preference preference) {
        final CachedBluetoothDevice cachedDevice =
                ((BluetoothDevicePreference) preference).getBluetoothDevice();
        if (BluetoothUtils.isAudioSharingEnabled() && mDialogHandler != null) {
            mDialogHandler.handleDeviceConnected(cachedDevice, /* userTriggered= */ true);
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                    .action(mContext, SettingsEnums.ACTION_MEDIA_DEVICE_CLICK);
        } else {
            cachedDevice.setActive();
        }
    }

    public void init(DashboardFragment fragment) {
        mFragmentManager = fragment.getParentFragmentManager();
        mBluetoothDeviceUpdater =
                new AvailableMediaBluetoothDeviceUpdater(
                        fragment.getContext(),
                        AvailableMediaDeviceGroupController.this,
                        fragment.getMetricsCategory());
        if (BluetoothUtils.isAudioSharingEnabled()) {
            mDialogHandler = new AudioSharingDialogHandler(mContext, fragment);
        }
    }

    @VisibleForTesting
    public void setFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    @VisibleForTesting
    public void setBluetoothDeviceUpdater(BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
    }

    @VisibleForTesting
    public void setDialogHandler(AudioSharingDialogHandler dialogHandler) {
        mDialogHandler = dialogHandler;
    }

    @Override
    public void onAudioModeChanged() {
        updateTitle();
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        // exclude inactive device
        if (activeDevice == null) {
            return;
        }

        if (bluetoothProfile == BluetoothProfile.HEARING_AID) {
            HearingAidUtils.launchHearingAidPairingDialog(
                    mFragmentManager, activeDevice, getMetricsCategory());
        }
    }

    private void updateTitle() {
        if (mPreferenceGroup == null) return;
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            int titleResId;
                            if (isAudioModeOngoingCall(mContext)) {
                                // in phone call
                                titleResId = R.string.connected_device_call_device_title;
                            } else if (BluetoothUtils.isAudioSharingEnabled()
                                    && BluetoothUtils.isBroadcasting(mBtManager)) {
                                // without phone call, in audio sharing
                                titleResId = R.string.audio_sharing_media_device_group_title;
                            } else {
                                // without phone call, not audio sharing
                                titleResId = R.string.connected_device_media_device_title;
                            }
                            mContext.getMainExecutor()
                                    .execute(
                                            () -> {
                                                if (mPreferenceGroup != null) {
                                                    mPreferenceGroup.setTitle(titleResId);
                                                }
                                            });
                        });
    }

    private void registerAudioSharingCallbacks() {
        if (mBroadcast != null) {
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
        }
        if (mAssistant != null) {
            mAssistant.registerServiceCallBack(mExecutor, mAssistantCallback);
        }
        if (mDialogHandler != null) {
            mDialogHandler.registerCallbacks(mExecutor);
        }
    }

    private void unregisterAudioSharingCallbacks() {
        if (mBroadcast != null) {
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
        }
        if (mAssistant != null) {
            mAssistant.unregisterServiceCallBack(mAssistantCallback);
        }
        if (mDialogHandler != null) {
            mDialogHandler.unregisterCallbacks();
        }
    }
}
