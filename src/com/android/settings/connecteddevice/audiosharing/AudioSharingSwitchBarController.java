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

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_START_LE_AUDIO_SHARING;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioSharingSwitchBarController extends BasePreferenceController
        implements DefaultLifecycleObserver,
                OnCheckedChangeListener,
                LocalBluetoothProfileManager.ServiceListener,
                BluetoothCallback {
    private static final String TAG = "AudioSharingSwitchCtlr";
    private static final String PREF_KEY = "audio_sharing_main_switch";

    interface OnAudioSharingStateChangedListener {
        /**
         * The callback which will be triggered when:
         *
         * <p>1. Bluetooth on/off state changes. 2. Broadcast and assistant profile
         * connect/disconnect state changes. 3. Audio sharing start/stop state changes.
         */
        void onAudioSharingStateChanged();

        /**
         * The callback which will be triggered when:
         *
         * <p>Broadcast and assistant profile connected.
         */
        void onAudioSharingProfilesConnected();
    }

    private final SettingsMainSwitchBar mSwitchBar;
    private final BluetoothAdapter mBluetoothAdapter;
    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final BluetoothEventManager mEventManager;
    @Nullable private final LocalBluetoothProfileManager mProfileManager;
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private final LocalBluetoothLeBroadcastAssistant mAssistant;
    @Nullable private Fragment mFragment;
    private final Executor mExecutor;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final OnAudioSharingStateChangedListener mListener;
    private Map<Integer, List<BluetoothDevice>> mGroupedConnectedDevices = new HashMap<>();
    @Nullable private AudioSharingDeviceItem mTargetActiveItem;
    private List<AudioSharingDeviceItem> mDeviceItemsForSharing = new ArrayList<>();
    @VisibleForTesting IntentFilter mIntentFilter;
    private final AtomicBoolean mCallbacksRegistered = new AtomicBoolean(false);
    private AtomicInteger mIntentHandleStage =
            new AtomicInteger(StartIntentHandleStage.TO_HANDLE.ordinal());
    private CopyOnWriteArrayList<BluetoothDevice> mSinksInAdding = new CopyOnWriteArrayList<>();

    @VisibleForTesting
    BroadcastReceiver mReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateSwitch();
                    mListener.onAudioSharingStateChanged();
                }
            };

    @VisibleForTesting
    final BluetoothLeBroadcast.Callback mBroadcastCallback =
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
                    AudioSharingUtils.toastMessage(
                            mContext, mContext.getString(R.string.audio_sharing_sharing_label));
                    mListener.onAudioSharingStateChanged();
                }

                @Override
                public void onBroadcastStartFailed(int reason) {
                    Log.d(TAG, "onBroadcastStartFailed(), reason = " + reason);
                    updateSwitch();
                    mMetricsFeatureProvider.action(
                            mContext,
                            SettingsEnums.ACTION_AUDIO_SHARING_START_FAILED,
                            SettingsEnums.AUDIO_SHARING_SETTINGS);
                }

                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, @NonNull BluetoothLeBroadcastMetadata metadata) {
                    Log.d(
                            TAG,
                            "onBroadcastMetadataChanged(), broadcastId = "
                                    + broadcastId
                                    + ", metadata = "
                                    + metadata.getBroadcastName());
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
                    AudioSharingUtils.toastMessage(
                            mContext,
                            mContext.getString(R.string.audio_sharing_sharing_stopped_label));
                    mListener.onAudioSharingStateChanged();
                }

                @Override
                public void onBroadcastStopFailed(int reason) {
                    Log.d(TAG, "onBroadcastStopFailed(), reason = " + reason);
                    updateSwitch();
                    mMetricsFeatureProvider.action(
                            mContext,
                            SettingsEnums.ACTION_AUDIO_SHARING_STOP_FAILED,
                            SettingsEnums.AUDIO_SHARING_SETTINGS);
                }

                @Override
                public void onBroadcastUpdated(int reason, int broadcastId) {}

                @Override
                public void onBroadcastUpdateFailed(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStarted(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onPlaybackStarted(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                    if (mAssistant == null
                            || mAssistant.getAllConnectedDevices().stream()
                                    .anyMatch(
                                            device -> BluetoothUtils
                                                    .hasActiveLocalBroadcastSourceForBtDevice(
                                                            device, mBtManager))) {
                        Log.d(
                                TAG,
                                "Skip handleOnBroadcastReady: null assistant or "
                                        + "sink has active local source.");
                        cleanUp();
                        return;
                    }
                    handleOnBroadcastReady();
                }

                @Override
                public void onPlaybackStopped(int reason, int broadcastId) {}
            };

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
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceAddFailed(
                        @NonNull BluetoothDevice sink,
                        @NonNull BluetoothLeBroadcastMetadata source,
                        int reason) {
                    Log.d(
                            TAG,
                            "onSourceAddFailed(), sink = "
                                    + sink
                                    + ", source = "
                                    + source
                                    + ", reason = "
                                    + reason);
                    mMetricsFeatureProvider.action(
                            mContext,
                            SettingsEnums.ACTION_AUDIO_SHARING_JOIN_FAILED,
                            SettingsEnums.AUDIO_SHARING_SETTINGS);
                    AudioSharingUtils.toastMessage(
                            mContext,
                            String.format(
                                    Locale.US,
                                    "Fail to add source to %s reason %d",
                                    sink.getAddress(),
                                    reason));
                }

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
                        @NonNull BluetoothLeBroadcastReceiveState state) {
                    if (BluetoothUtils.isConnected(state)) {
                        if (mSinksInAdding.contains(sink)) {
                            mSinksInAdding.remove(sink);
                        }
                        dismissLoadingStateDialogIfNeeded();
                        Log.d(TAG, "onReceiveStateChanged() connected, sink = " + sink
                                + ", remaining sinks = " + mSinksInAdding);
                    }
                }
            };

    AudioSharingSwitchBarController(
            Context context,
            SettingsMainSwitchBar switchBar,
            OnAudioSharingStateChangedListener listener) {
        super(context, PREF_KEY);
        mSwitchBar = switchBar;
        mListener = listener;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBtManager = Utils.getLocalBtManager(context);
        mEventManager = mBtManager == null ? null : mBtManager.getEventManager();
        mProfileManager = mBtManager == null ? null : mBtManager.getProfileManager();
        mBroadcast = mProfileManager == null ? null : mProfileManager.getLeAudioBroadcastProfile();
        mAssistant =
                mProfileManager == null
                        ? null
                        : mProfileManager.getLeAudioBroadcastAssistantProfile();
        mExecutor = Executors.newSingleThreadExecutor();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mSwitchBar.getRootView().setAccessibilityDelegate(new MainSwitchAccessibilityDelegate());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip register callbacks. Feature is not available.");
            return;
        }
        mContext.registerReceiver(mReceiver, mIntentFilter, Context.RECEIVER_EXPORTED_UNAUDITED);
        updateSwitch();
        if (!AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            if (mProfileManager != null) {
                mProfileManager.addServiceListener(this);
            }
            Log.d(TAG, "Skip register callbacks. Profile is not ready.");
            return;
        }
        registerCallbacks();
        if (mIntentHandleStage.compareAndSet(
                StartIntentHandleStage.TO_HANDLE.ordinal(),
                StartIntentHandleStage.HANDLE_AUTO_ADD.ordinal())) {
            Log.d(TAG, "onStart: handleStartAudioSharingFromIntent");
            handleStartAudioSharingFromIntent();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip unregister callbacks. Feature is not available.");
            return;
        }
        mContext.unregisterReceiver(mReceiver);
        if (mProfileManager != null) {
            mProfileManager.removeServiceListener(this);
        }
        unregisterCallbacks();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Filter out unnecessary callbacks when switch is disabled.
        if (!buttonView.isEnabled()) return;
        if (mBroadcast == null || mAssistant == null) {
            mSwitchBar.setChecked(false);
            Log.d(TAG, "Skip onCheckedChanged, profile not support.");
            return;
        }
        mSwitchBar.setEnabled(false);
        boolean isBroadcasting = BluetoothUtils.isBroadcasting(mBtManager);
        if (isChecked) {
            if (isBroadcasting) {
                Log.d(TAG, "Skip startAudioSharing, already broadcasting.");
                mSwitchBar.setEnabled(true);
                return;
            }
            // FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST is always true in
            // prod. We can turn off the flag for debug purpose.
            if (FeatureFlagUtils.isEnabled(
                    mContext,
                    FeatureFlagUtils.SETTINGS_NEED_CONNECTED_BLE_DEVICE_FOR_BROADCAST)
                    && hasEmptyConnectedSink()) {
                // Pop up dialog to ask users to connect at least one lea buds before audio sharing.
                AudioSharingUtils.postOnMainThread(
                        mContext,
                        () -> {
                            mSwitchBar.setEnabled(true);
                            mSwitchBar.setChecked(false);
                            if (mFragment != null) {
                                AudioSharingConfirmDialogFragment.show(mFragment);
                            }
                        });
                return;
            }
            startAudioSharing();
        } else {
            if (!isBroadcasting) {
                Log.d(TAG, "Skip stopAudioSharing, already not broadcasting.");
                mSwitchBar.setEnabled(true);
                return;
            }
            stopAudioSharing();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return BluetoothUtils.isAudioSharingEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "onServiceConnected()");
        if (AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            registerCallbacks();
            updateSwitch();
            mListener.onAudioSharingProfilesConnected();
            mListener.onAudioSharingStateChanged();
            if (mProfileManager != null) {
                mProfileManager.removeServiceListener(this);
            }
            if (mIntentHandleStage.compareAndSet(
                    StartIntentHandleStage.TO_HANDLE.ordinal(),
                    StartIntentHandleStage.HANDLE_AUTO_ADD.ordinal())) {
                Log.d(TAG, "onServiceConnected: handleStartAudioSharingFromIntent");
                handleStartAudioSharingFromIntent();
            }
        }
    }

    @Override
    public void onServiceDisconnected() {
        Log.d(TAG, "onServiceDisconnected()");
        // Do nothing.
    }

    @Override
    public void onActiveDeviceChanged(@Nullable CachedBluetoothDevice activeDevice,
            int bluetoothProfile) {
        if (activeDevice != null) {
            Log.d(TAG, "onActiveDeviceChanged: device = "
                    + activeDevice.getDevice().getAnonymizedAddress()
                    + ", profile = " + bluetoothProfile);
            updateSwitch();
        }
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to host the {@link AudioSharingSwitchBarController} dialog.
     */
    public void init(@NonNull Fragment fragment) {
        this.mFragment = fragment;
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
        if (mBroadcast == null || mAssistant == null || mEventManager == null) {
            Log.d(TAG, "Skip registerCallbacks(). Profile not support on this device.");
            return;
        }
        if (!mCallbacksRegistered.get()) {
            Log.d(TAG, "registerCallbacks()");
            mSwitchBar.addOnSwitchChangeListener(this);
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
            mAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
            mEventManager.registerCallback(this);
            mCallbacksRegistered.set(true);
        }
    }

    private void unregisterCallbacks() {
        if (!isAvailable() || !AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            Log.d(TAG, "Skip unregisterCallbacks(). Feature is not available.");
            return;
        }
        if (mBroadcast == null || mAssistant == null || mEventManager == null) {
            Log.d(TAG, "Skip unregisterCallbacks(). Profile not support on this device.");
            return;
        }
        if (mCallbacksRegistered.get()) {
            Log.d(TAG, "unregisterCallbacks()");
            mSwitchBar.removeOnSwitchChangeListener(this);
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
            mAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
            mEventManager.unregisterCallback(this);
            mCallbacksRegistered.set(false);
        }
    }

    private void startAudioSharing() {
        // Compute the device connection state before start audio sharing since the devices will
        // be set to inactive after the broadcast started.
        mGroupedConnectedDevices = AudioSharingUtils.fetchConnectedDevicesByGroupId(mBtManager);
        List<AudioSharingDeviceItem> deviceItems =
                AudioSharingUtils.buildOrderedConnectedLeadAudioSharingDeviceItem(
                        mBtManager, mGroupedConnectedDevices, /* filterByInSharing= */ false);
        // deviceItems is ordered. The active device is the first place if exits.
        mDeviceItemsForSharing = new ArrayList<>(deviceItems);
        mTargetActiveItem = null;
        if (!deviceItems.isEmpty() && deviceItems.get(0).isActive()) {
            // If active device exists for audio sharing, share to it
            // automatically once the broadcast is started.
            mTargetActiveItem = deviceItems.get(0);
            mDeviceItemsForSharing.remove(0);
        }
        if (mBroadcast != null) {
            mBroadcast.startPrivateBroadcast();
            mSinksInAdding.clear();
            // TODO: use string res once finalized.
            AudioSharingUtils.postOnMainThread(mContext,
                    () -> AudioSharingLoadingStateDialogFragment.show(mFragment,
                            "Starting audio stream..."));
            mMetricsFeatureProvider.action(
                    mContext,
                    SettingsEnums.ACTION_AUDIO_SHARING_MAIN_SWITCH_ON,
                    deviceItems.size());
        }
    }

    private void stopAudioSharing() {
        if (mBroadcast != null) {
            mBroadcast.stopBroadcast(mBroadcast.getLatestBroadcastId());
            mMetricsFeatureProvider.action(
                    mContext, SettingsEnums.ACTION_AUDIO_SHARING_MAIN_SWITCH_OFF);
        }
    }

    private void updateSwitch() {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            boolean isBroadcasting = BluetoothUtils.isBroadcasting(mBtManager);
                            boolean hasActiveDevice =
                                    AudioSharingUtils.hasActiveConnectedLeadDevice(mBtManager);
                            boolean hasEmptyConnectedDevice = hasEmptyConnectedSink();
                            boolean isStateReady =
                                    isBluetoothOn()
                                            && AudioSharingUtils.isAudioSharingProfileReady(
                                            mProfileManager)
                                            && (isBroadcasting
                                            // Always enable toggle when no connected sink. We have
                                            // dialog to guide users to connect compatible devices
                                            // for audio sharing.
                                            || hasEmptyConnectedDevice
                                            // Disable toggle till device gets active after
                                            // broadcast ends.
                                            || hasActiveDevice);
                            AudioSharingUtils.postOnMainThread(
                                    mContext,
                                    () -> {
                                        if (mSwitchBar.isChecked() != isBroadcasting) {
                                            mSwitchBar.setChecked(isBroadcasting);
                                        }
                                        if (mSwitchBar.isEnabled() != isStateReady) {
                                            mSwitchBar.setEnabled(isStateReady);
                                        }
                                        Log.d(
                                                TAG,
                                                "updateSwitch, checked = "
                                                        + isBroadcasting
                                                        + ", enabled = "
                                                        + isStateReady);
                                    });
                        });
    }

    private boolean isBluetoothOn() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    private boolean hasEmptyConnectedSink() {
        return mAssistant != null && mAssistant.getAllConnectedDevices().isEmpty();
    }

    private void handleOnBroadcastReady() {
        List<BluetoothDevice> targetActiveSinks = mTargetActiveItem == null ? ImmutableList.of()
                : mGroupedConnectedDevices.getOrDefault(
                        mTargetActiveItem.getGroupId(), ImmutableList.of());
        Pair<Integer, Object>[] eventData =
                AudioSharingUtils.buildAudioSharingDialogEventData(
                        SettingsEnums.AUDIO_SHARING_SETTINGS,
                        SettingsEnums.DIALOG_AUDIO_SHARING_ADD_DEVICE,
                        /* userTriggered= */ false,
                        /* deviceCountInSharing= */ targetActiveSinks.isEmpty() ? 0 : 1,
                        /* candidateDeviceCount= */ mDeviceItemsForSharing.size());
        if (!targetActiveSinks.isEmpty() && mTargetActiveItem != null) {
            Log.d(TAG, "handleOnBroadcastReady: automatically add source to active sinks.");
            addSourceToTargetSinks(targetActiveSinks, mTargetActiveItem.getName());
            mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_AUTO_JOIN_AUDIO_SHARING);
            mTargetActiveItem = null;
            if (mIntentHandleStage.compareAndSet(
                            StartIntentHandleStage.HANDLE_AUTO_ADD.ordinal(),
                            StartIntentHandleStage.HANDLED.ordinal())
                    && mDeviceItemsForSharing.size() == 1) {
                Log.d(TAG, "handleOnBroadcastReady: auto add source to the second device");
                AudioSharingDeviceItem target = mDeviceItemsForSharing.get(0);
                List<BluetoothDevice> targetSinks = mGroupedConnectedDevices.getOrDefault(
                        target.getGroupId(), ImmutableList.of());
                addSourceToTargetSinks(targetSinks, target.getName());
                cleanUp();
                // TODO: Add metric for auto add by intent
                return;
            }
        }
        mIntentHandleStage.compareAndSet(
                StartIntentHandleStage.HANDLE_AUTO_ADD.ordinal(),
                StartIntentHandleStage.HANDLED.ordinal());
        if (mFragment == null) {
            Log.d(TAG, "handleOnBroadcastReady: dialog fail to show due to null fragment.");
            dismissLoadingStateDialogIfNeeded();
            cleanUp();
            return;
        }
        showDialog(eventData);
    }

    private void showDialog(Pair<Integer, Object>[] eventData) {
        AudioSharingDialogFragment.DialogEventListener listener =
                new AudioSharingDialogFragment.DialogEventListener() {
                    @Override
                    public void onItemClick(@NonNull AudioSharingDeviceItem item) {
                        List<BluetoothDevice> targetSinks = mGroupedConnectedDevices.getOrDefault(
                                item.getGroupId(), ImmutableList.of());
                        addSourceToTargetSinks(targetSinks, item.getName());
                        cleanUp();
                    }

                    @Override
                    public void onCancelClick() {
                        dismissLoadingStateDialogIfNeeded();
                        cleanUp();
                    }
                };
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> {
                    // Check nullability to pass NullAway check
                    if (mFragment != null) {
                        AudioSharingDialogFragment.show(
                                mFragment, mDeviceItemsForSharing, listener, eventData);
                    }
                });
    }

    private static final class MainSwitchAccessibilityDelegate extends View.AccessibilityDelegate {
        @Override
        public boolean onRequestSendAccessibilityEvent(
                @NonNull ViewGroup host, @NonNull View view, @NonNull AccessibilityEvent event) {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    && (event.getContentChangeTypes()
                                    & AccessibilityEvent.CONTENT_CHANGE_TYPE_ENABLED)
                            != 0) {
                Log.d(TAG, "Skip accessibility event for CONTENT_CHANGE_TYPE_ENABLED");
                return false;
            }
            return super.onRequestSendAccessibilityEvent(host, view, event);
        }
    }

    private void handleStartAudioSharingFromIntent() {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            if (mFragment == null
                                    || mFragment.getActivity() == null
                                    || mFragment.getActivity().getIntent() == null) {
                                Log.d(
                                        TAG,
                                        "Skip handleStartAudioSharingFromIntent, "
                                                + "fragment intent is null");
                                return;
                            }
                            Intent intent = mFragment.getActivity().getIntent();
                            Bundle args =
                                    intent.getBundleExtra(
                                            SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                            Boolean shouldStart =
                                    args != null
                                            && args.getBoolean(EXTRA_START_LE_AUDIO_SHARING, false);
                            if (!shouldStart) {
                                Log.d(TAG, "Skip handleStartAudioSharingFromIntent, arg false");
                                mIntentHandleStage.compareAndSet(
                                        StartIntentHandleStage.HANDLE_AUTO_ADD.ordinal(),
                                        StartIntentHandleStage.HANDLED.ordinal());
                                return;
                            }
                            if (BluetoothUtils.isBroadcasting(mBtManager)) {
                                Log.d(TAG, "Skip handleStartAudioSharingFromIntent, in broadcast");
                                mIntentHandleStage.compareAndSet(
                                        StartIntentHandleStage.HANDLE_AUTO_ADD.ordinal(),
                                        StartIntentHandleStage.HANDLED.ordinal());
                                return;
                            }
                            Log.d(TAG, "HandleStartAudioSharingFromIntent, start broadcast");
                            AudioSharingUtils.postOnMainThread(
                                    mContext, () -> mSwitchBar.setChecked(true));
                        });
    }

    private void addSourceToTargetSinks(List<BluetoothDevice> targetActiveSinks,
            @NonNull String sinkName) {
        mSinksInAdding.addAll(targetActiveSinks);
        AudioSharingUtils.addSourceToTargetSinks(targetActiveSinks, mBtManager);
        // TODO: move to res once finalized
        String loadingMessage = "Sharing with " + sinkName + "...";
        showLoadingStateDialog(loadingMessage);
    }

    private void showLoadingStateDialog(@NonNull String loadingMessage) {
        AudioSharingUtils.postOnMainThread(mContext,
                () -> AudioSharingLoadingStateDialogFragment.show(mFragment, loadingMessage));
    }

    private void dismissLoadingStateDialogIfNeeded() {
        if (mSinksInAdding.isEmpty()) {
            AudioSharingUtils.postOnMainThread(mContext,
                    () -> AudioSharingLoadingStateDialogFragment.dismiss(mFragment));
        }
    }

    private void cleanUp() {
        mGroupedConnectedDevices.clear();
        mDeviceItemsForSharing.clear();
    }

    private enum StartIntentHandleStage {
        TO_HANDLE,
        HANDLE_AUTO_ADD,
        HANDLED,
    }
}
