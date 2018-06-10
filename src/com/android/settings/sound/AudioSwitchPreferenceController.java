/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.sound;

import static android.media.AudioManager.STREAM_DEVICES_CHANGED_ACTION;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.AudioManager.STREAM_VOICE_CALL;
import static android.media.AudioSystem.DEVICE_OUT_ALL_A2DP;
import static android.media.AudioSystem.DEVICE_OUT_ALL_SCO;
import static android.media.AudioSystem.DEVICE_OUT_HEARING_AID;
import static android.media.MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.media.MediaRouter.Callback;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.FeatureFlags;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Abstract class for audio switcher controller to notify subclass
 * updating the current status of switcher entry. Subclasses must overwrite
 * {@link #setActiveBluetoothDevice(BluetoothDevice)} to set the
 * active device for corresponding profile.
 */
public abstract class AudioSwitchPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener, BluetoothCallback,
        LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "AudioSwitchPreferenceController";
    private static final int INVALID_INDEX = -1;

    protected final List<BluetoothDevice> mConnectedDevices;
    protected final AudioManager mAudioManager;
    protected final MediaRouter mMediaRouter;
    protected int mSelectedIndex;
    protected Preference mPreference;
    protected LocalBluetoothProfileManager mProfileManager;
    protected AudioSwitchCallback mAudioSwitchPreferenceCallback;

    private final AudioManagerAudioDeviceCallback mAudioManagerAudioDeviceCallback;
    private final MediaRouterCallback mMediaRouterCallback;
    private final WiredHeadsetBroadcastReceiver mReceiver;
    private final Handler mHandler;
    private LocalBluetoothManager mLocalBluetoothManager;

    public interface AudioSwitchCallback {
        void onPreferenceDataChanged(ListPreference preference);
    }

    public AudioSwitchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mMediaRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        mAudioManagerAudioDeviceCallback = new AudioManagerAudioDeviceCallback();
        mReceiver = new WiredHeadsetBroadcastReceiver();
        mMediaRouterCallback = new MediaRouterCallback();
        mConnectedDevices = new ArrayList<>();
        final FutureTask<LocalBluetoothManager> localBtManagerFutureTask = new FutureTask<>(
                // Avoid StrictMode ThreadPolicy violation
                () -> Utils.getLocalBtManager(mContext));
        try {
            localBtManagerFutureTask.run();
            mLocalBluetoothManager = localBtManagerFutureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w(TAG, "Error getting LocalBluetoothManager.", e);
            return;
        }
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mProfileManager = mLocalBluetoothManager.getProfileManager();
    }

    /**
     * Make this method as final, ensure that subclass will checking
     * the feature flag and they could mistakenly break it via overriding.
     */
    @Override
    public final int getAvailabilityStatus() {
        return FeatureFlagUtils.isEnabled(mContext, FeatureFlags.AUDIO_SWITCHER_SETTINGS) &&
                mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String address = (String) newValue;
        if (!(preference instanceof ListPreference)) {
            return false;
        }

        final ListPreference listPreference = (ListPreference) preference;
        if (TextUtils.equals(address, mContext.getText(R.string.media_output_default_summary))) {
            // Switch to default device which address is device name
            mSelectedIndex = getDefaultDeviceIndex();
            setActiveBluetoothDevice(null);
            listPreference.setSummary(mContext.getText(R.string.media_output_default_summary));
        } else {
            // Switch to BT device which address is hardware address
            final int connectedDeviceIndex = getConnectedDeviceIndex(address);
            if (connectedDeviceIndex == INVALID_INDEX) {
                return false;
            }
            final BluetoothDevice btDevice = mConnectedDevices.get(connectedDeviceIndex);
            mSelectedIndex = connectedDeviceIndex;
            setActiveBluetoothDevice(btDevice);
            listPreference.setSummary(btDevice.getAliasName());
        }
        return true;
    }

    public abstract void setActiveBluetoothDevice(BluetoothDevice device);

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);
        mPreference.setVisible(false);
    }

    @Override
    public void onStart() {
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mLocalBluetoothManager.setForegroundActivity(mContext);
        register();
    }

    @Override
    public void onStop() {
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mLocalBluetoothManager.setForegroundActivity(null);
        unregister();
    }

    /**
     * Only concerned about whether the local adapter is connected to any profile of any device and
     * are not really concerned about which profile.
     */
    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        updateState(mPreference);
    }

    @Override
    public void onAudioModeChanged() {
        updateState(mPreference);
    }

    @Override
    public void onProfileConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state,
            int bluetoothProfile) {
        updateState(mPreference);
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
    }

    /**
     * The local Bluetooth adapter has started the remote device discovery process.
     */
    @Override
    public void onScanningStateChanged(boolean started) {
    }

    /**
     * Indicates a change in the bond state of a remote
     * device. For example, if a device is bonded (paired).
     */
    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        updateState(mPreference);
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
    }

    public void setCallback(AudioSwitchCallback callback) {
        mAudioSwitchPreferenceCallback = callback;
    }

    protected boolean isStreamFromOutputDevice(int streamType, int device) {
        return (device & mAudioManager.getDevicesForStream(streamType)) != 0;
    }

    /**
     * get hands free profile(HFP) connected device
     */
    protected List<BluetoothDevice> getConnectedHfpDevices() {
        final List<BluetoothDevice> connectedDevices = new ArrayList<>();
        final HeadsetProfile hfpProfile = mProfileManager.getHeadsetProfile();
        if (hfpProfile == null) {
            return connectedDevices;
        }
        final List<BluetoothDevice> devices = hfpProfile.getConnectedDevices();
        for (BluetoothDevice device : devices) {
            if (device.isConnected()) {
                connectedDevices.add(device);
            }
        }
        return connectedDevices;
    }

    /**
     * get A2dp connected device
     */
    protected List<BluetoothDevice> getConnectedA2dpDevices() {
        final List<BluetoothDevice> connectedDevices = new ArrayList<>();
        final A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();
        if (a2dpProfile == null) {
            return connectedDevices;
        }
        final List<BluetoothDevice> devices = a2dpProfile.getConnectedDevices();
        for (BluetoothDevice device : devices) {
            if (device.isConnected()) {
                connectedDevices.add(device);
            }
        }
        return connectedDevices;
    }

    /**
     * get hearing aid profile connected device, exclude other devices with same hiSyncId.
     */
    protected List<BluetoothDevice> getConnectedHearingAidDevices() {
        final List<BluetoothDevice> connectedDevices = new ArrayList<>();
        final HearingAidProfile hapProfile = mProfileManager.getHearingAidProfile();
        if (hapProfile == null) {
            return connectedDevices;
        }
        final List<Long> devicesHiSyncIds = new ArrayList<>();
        final List<BluetoothDevice> devices = hapProfile.getConnectedDevices();
        for (BluetoothDevice device : devices) {
            final long hiSyncId = hapProfile.getHiSyncId(device);
            // device with same hiSyncId should not be shown in the UI.
            // So do not add it into connectedDevices.
            if (!devicesHiSyncIds.contains(hiSyncId) && device.isConnected()) {
                devicesHiSyncIds.add(hiSyncId);
                connectedDevices.add(device);
            }
        }
        return connectedDevices;
    }

    /**
     * According to different stream and output device, find the active device from
     * the corresponding profile. Hearing aid device could stream both STREAM_MUSIC
     * and STREAM_VOICE_CALL.
     *
     * @param streamType the type of audio streams.
     * @return the active device. Return null if the active device is current device
     * or streamType is not STREAM_MUSIC or STREAM_VOICE_CALL.
     */
    protected BluetoothDevice findActiveDevice(int streamType) {
        if (streamType != STREAM_MUSIC && streamType != STREAM_VOICE_CALL) {
            return null;
        }
        if (isStreamFromOutputDevice(STREAM_MUSIC, DEVICE_OUT_ALL_A2DP)) {
            return mProfileManager.getA2dpProfile().getActiveDevice();
        } else if (isStreamFromOutputDevice(STREAM_VOICE_CALL, DEVICE_OUT_ALL_SCO)) {
            return mProfileManager.getHeadsetProfile().getActiveDevice();
        } else if (isStreamFromOutputDevice(streamType, DEVICE_OUT_HEARING_AID)) {
            // The first element is the left active device; the second element is
            // the right active device. And they will have same hiSyncId. If either
            // or both side is not active, it will be null on that position.
            List<BluetoothDevice> activeDevices =
                    mProfileManager.getHearingAidProfile().getActiveDevices();
            for (BluetoothDevice btDevice : activeDevices) {
                if (btDevice != null && mConnectedDevices.contains(btDevice)) {
                    // also need to check mConnectedDevices, because one of
                    // the device(same hiSyncId) might not be shown in the UI.
                    return btDevice;
                }
            }
        }
        return null;
    }

    int getDefaultDeviceIndex() {
        // Default device is after all connected devices.
        return mConnectedDevices.size();
    }

    void setupPreferenceEntries(CharSequence[] mediaOutputs, CharSequence[] mediaValues,
            BluetoothDevice activeDevice) {
        // default to current device
        mSelectedIndex = getDefaultDeviceIndex();
        // default device is after all connected devices.
        mediaOutputs[mSelectedIndex] = mContext.getText(R.string.media_output_default_summary);
        // use default device name as address
        mediaValues[mSelectedIndex] = mContext.getText(R.string.media_output_default_summary);
        for (int i = 0, size = mConnectedDevices.size(); i < size; i++) {
            final BluetoothDevice btDevice = mConnectedDevices.get(i);
            mediaOutputs[i] = btDevice.getAliasName();
            mediaValues[i] = btDevice.getAddress();
            if (btDevice.equals(activeDevice)) {
                // select the active connected device.
                mSelectedIndex = i;
            }
        }
    }

    void setPreference(CharSequence[] mediaOutputs, CharSequence[] mediaValues,
            Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setEntries(mediaOutputs);
        listPreference.setEntryValues(mediaValues);
        listPreference.setValueIndex(mSelectedIndex);
        listPreference.setSummary(mediaOutputs[mSelectedIndex]);
        mAudioSwitchPreferenceCallback.onPreferenceDataChanged(listPreference);
    }

    private int getConnectedDeviceIndex(String hardwareAddress) {
        if (mConnectedDevices != null) {
            for (int i = 0, size = mConnectedDevices.size(); i < size; i++) {
                final BluetoothDevice btDevice = mConnectedDevices.get(i);
                if (TextUtils.equals(btDevice.getAddress(), hardwareAddress)) {
                    return i;
                }
            }
        }
        return INVALID_INDEX;
    }

    private void register() {
        mLocalBluetoothManager.getEventManager().registerCallback(this);
        mAudioManager.registerAudioDeviceCallback(mAudioManagerAudioDeviceCallback, mHandler);
        mMediaRouter.addCallback(ROUTE_TYPE_REMOTE_DISPLAY, mMediaRouterCallback);

        // Register for misc other intent broadcasts.
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(STREAM_DEVICES_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    private void unregister() {
        mLocalBluetoothManager.getEventManager().unregisterCallback(this);
        mAudioManager.unregisterAudioDeviceCallback(mAudioManagerAudioDeviceCallback);
        mMediaRouter.removeCallback(mMediaRouterCallback);
        mContext.unregisterReceiver(mReceiver);
    }

    /** Notifications of audio device connection and disconnection events. */
    private class AudioManagerAudioDeviceCallback extends AudioDeviceCallback {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            updateState(mPreference);
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] devices) {
            updateState(mPreference);
        }
    }

    /** Receiver for wired headset plugged and unplugged events. */
    private class WiredHeadsetBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (AudioManager.ACTION_HEADSET_PLUG.equals(action) ||
                    AudioManager.STREAM_DEVICES_CHANGED_ACTION.equals(action)) {
                updateState(mPreference);
            }
        }
    }

    /** Callback for cast device events. */
    private class MediaRouterCallback extends Callback {
        @Override
        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            if (info != null && !info.isDefault()) {
                // cast mode
                updateState(mPreference);
            }
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            if (info != null && !info.isDefault()) {
                // cast mode
                updateState(mPreference);
            }
        }

        @Override
        public void onRouteGrouped(MediaRouter router, MediaRouter.RouteInfo info,
                MediaRouter.RouteGroup group, int index) {
        }

        @Override
        public void onRouteUngrouped(MediaRouter router, MediaRouter.RouteInfo info,
                MediaRouter.RouteGroup group) {
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo info) {
        }
    }
}
