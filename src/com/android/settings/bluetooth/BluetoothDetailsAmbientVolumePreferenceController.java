/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static android.bluetooth.AudioInputControl.MUTE_NOT_MUTED;
import static android.bluetooth.AudioInputControl.MUTE_MUTED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;

import static com.android.settings.bluetooth.AmbientVolumePreference.SIDE_UNIFIED;
import static com.android.settings.bluetooth.AmbientVolumePreference.VALID_SIDES;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.ORDER_AMBIENT_VOLUME;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_INVALID;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;
import static com.android.settingslib.bluetooth.HearingDeviceLocalDataManager.Data.INVALID_VOLUME;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.bluetooth.AmbientVolumeController;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingDeviceLocalDataManager;
import com.android.settingslib.bluetooth.HearingDeviceLocalDataManager.Data;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Map;
import java.util.Set;

/** A {@link BluetoothDetailsController} that manages ambient volume control preferences. */
public class BluetoothDetailsAmbientVolumePreferenceController extends
        BluetoothDetailsController implements Preference.OnPreferenceChangeListener,
        HearingDeviceLocalDataManager.OnDeviceLocalDataChangeListener, OnStart, OnStop,
        AmbientVolumeController.AmbientVolumeControlCallback, BluetoothCallback {

    private static final boolean DEBUG = true;
    private static final String TAG = "AmbientPrefController";

    static final String KEY_AMBIENT_VOLUME = "ambient_volume";
    static final String KEY_AMBIENT_VOLUME_SLIDER = "ambient_volume_slider";
    private static final int ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED = 0;
    private static final int ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED = 1;

    private final LocalBluetoothManager mBluetoothManager;
    private final Set<CachedBluetoothDevice> mCachedDevices = new ArraySet<>();
    private final BiMap<Integer, BluetoothDevice> mSideToDeviceMap = HashBiMap.create();
    private final BiMap<Integer, SeekBarPreference> mSideToSliderMap = HashBiMap.create();
    private final HearingDeviceLocalDataManager mLocalDataManager;
    private final AmbientVolumeController mVolumeController;

    @Nullable
    private PreferenceCategory mDeviceControls;
    @Nullable
    private AmbientVolumePreference mPreference;
    @Nullable
    private Toast mToast;

    public BluetoothDetailsAmbientVolumePreferenceController(@NonNull Context context,
            @NonNull LocalBluetoothManager manager,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mBluetoothManager = manager;
        mLocalDataManager = new HearingDeviceLocalDataManager(context);
        mLocalDataManager.setOnDeviceLocalDataChangeListener(this,
                ThreadUtils.getBackgroundExecutor());
        mVolumeController = new AmbientVolumeController(manager.getProfileManager(), this);
    }

    @VisibleForTesting
    BluetoothDetailsAmbientVolumePreferenceController(@NonNull Context context,
            @NonNull LocalBluetoothManager manager,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle,
            @NonNull HearingDeviceLocalDataManager localSettings,
            @NonNull AmbientVolumeController volumeController) {
        super(context, fragment, device, lifecycle);
        mBluetoothManager = manager;
        mLocalDataManager = localSettings;
        mVolumeController = volumeController;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mDeviceControls = screen.findPreference(KEY_HEARING_DEVICE_GROUP);
        if (mDeviceControls == null) {
            return;
        }
        loadDevices();
    }

    @Override
    public void onStart() {
        ThreadUtils.postOnBackgroundThread(() -> {
            mBluetoothManager.getEventManager().registerCallback(this);
            mLocalDataManager.start();
            mCachedDevices.forEach(device -> {
                device.registerCallback(ThreadUtils.getBackgroundExecutor(), this);
                mVolumeController.registerCallback(ThreadUtils.getBackgroundExecutor(),
                        device.getDevice());
            });
        });
    }

    @Override
    public void onResume() {
        refresh();
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onStop() {
        ThreadUtils.postOnBackgroundThread(() -> {
            mBluetoothManager.getEventManager().unregisterCallback(this);
            mLocalDataManager.stop();
            mCachedDevices.forEach(device -> {
                device.unregisterCallback(this);
                mVolumeController.unregisterCallback(device.getDevice());
            });
        });
    }

    @Override
    protected void refresh() {
        if (!isAvailable()) {
            return;
        }
        boolean shouldShowAmbientControl = isAmbientControlAvailable();
        if (shouldShowAmbientControl) {
            if (mPreference != null) {
                mPreference.setVisible(true);
            }
            loadRemoteDataToUi();
        } else {
            if (mPreference != null) {
                mPreference.setVisible(false);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return mCachedDevice.getProfiles().stream().anyMatch(
                profile -> profile instanceof VolumeControlProfile);
    }

    @Nullable
    @Override
    public String getPreferenceKey() {
        return KEY_AMBIENT_VOLUME;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @Nullable Object newValue) {
        if (preference instanceof SeekBarPreference && newValue instanceof final Integer value) {
            final int side = mSideToSliderMap.inverse().getOrDefault(preference, SIDE_INVALID);
            if (DEBUG) {
                Log.d(TAG, "onPreferenceChange: side=" + side + ", value=" + value);
            }
            setVolumeIfValid(side, value);

            Runnable setAmbientRunnable = () -> {
                if (side == SIDE_UNIFIED) {
                    mSideToDeviceMap.forEach((s, d) -> mVolumeController.setAmbient(d, value));
                } else {
                    final BluetoothDevice device = mSideToDeviceMap.get(side);
                    mVolumeController.setAmbient(device, value);
                }
            };

            if (isControlMuted()) {
                // User drag on the volume slider when muted. Unmute the devices first.
                if (mPreference != null) {
                    mPreference.setMuted(false);
                }
                for (BluetoothDevice device : mSideToDeviceMap.values()) {
                    mVolumeController.setMuted(device, false);
                }
                // Restore the value before muted
                loadLocalDataToUi();
                // Delay set ambient on remote device since the immediately sequential command
                // might get failed sometimes
                mContext.getMainThreadHandler().postDelayed(setAmbientRunnable, 1000L);
            } else {
                setAmbientRunnable.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onProfileConnectionStateChanged(@NonNull CachedBluetoothDevice cachedDevice,
            int state, int bluetoothProfile) {
        if (bluetoothProfile == BluetoothProfile.VOLUME_CONTROL
                && state == BluetoothProfile.STATE_CONNECTED
                && mCachedDevices.contains(cachedDevice)) {
            // After VCP connected, AICS may not ready yet and still return invalid value, delay
            // a while to wait AICS ready as a workaround
            mContext.getMainThreadHandler().postDelayed(this::refresh, 1000L);
        }
    }

    @Override
    public void onDeviceAttributesChanged() {
        mCachedDevices.forEach(device -> {
            device.unregisterCallback(this);
            mVolumeController.unregisterCallback(device.getDevice());
        });
        mContext.getMainExecutor().execute(() -> {
            loadDevices();
            if (!mCachedDevices.isEmpty()) {
                refresh();
            }
            ThreadUtils.postOnBackgroundThread(() ->
                    mCachedDevices.forEach(device -> {
                        device.registerCallback(ThreadUtils.getBackgroundExecutor(), this);
                        mVolumeController.registerCallback(ThreadUtils.getBackgroundExecutor(),
                                device.getDevice());
                    })
            );
        });
    }

    @Override
    public void onDeviceLocalDataChange(@NonNull String address, @Nullable Data data) {
        if (data == null) {
            // The local data is removed because the device is unpaired, do nothing
            return;
        }
        for (BluetoothDevice device : mSideToDeviceMap.values()) {
            if (device.getAnonymizedAddress().equals(address)) {
                mContext.getMainExecutor().execute(() -> loadLocalDataToUi(device));
                return;
            }
        }
    }

    @Override
    public void onVolumeControlServiceConnected() {
        mCachedDevices.forEach(
                device -> mVolumeController.registerCallback(ThreadUtils.getBackgroundExecutor(),
                        device.getDevice()));
    }

    @Override
    public void onAmbientChanged(@NonNull BluetoothDevice device, int gainSettings) {
        if (DEBUG) {
            Log.d(TAG, "onAmbientChanged, value:" + gainSettings + ", device:" + device);
        }
        Data data = mLocalDataManager.get(device);
        boolean isInitiatedFromUi = (isControlExpanded() && data.ambient() == gainSettings)
                || (!isControlExpanded() && data.groupAmbient() == gainSettings);
        if (isInitiatedFromUi) {
            // The change is initiated from UI, no need to update UI
            return;
        }

        // We have to check if we need to expand the controls by getting all remote
        // device's ambient value, delay for a while to wait all remote devices update
        // to the latest value to avoid unnecessary expand action.
        mContext.getMainThreadHandler().postDelayed(this::refresh, 1200L);
    }

    @Override
    public void onMuteChanged(@NonNull BluetoothDevice device, int mute) {
        if (DEBUG) {
            Log.d(TAG, "onMuteChanged, mute:" + mute + ", device:" + device);
        }
        boolean isInitiatedFromUi = (isControlMuted() && mute == MUTE_MUTED)
                || (!isControlMuted() && mute == MUTE_NOT_MUTED);
        if (isInitiatedFromUi) {
            // The change is initiated from UI, no need to update UI
            return;
        }

        // We have to check if we need to mute the devices by getting all remote
        // device's mute state, delay for a while to wait all remote devices update
        // to the latest value.
        mContext.getMainThreadHandler().postDelayed(this::refresh, 1200L);
    }

    @Override
    public void onCommandFailed(@NonNull BluetoothDevice device) {
        Log.w(TAG, "onCommandFailed, device:" + device);
        mContext.getMainExecutor().execute(() -> {
            showErrorToast();
            refresh();
        });
    }

    private void loadDevices() {
        mSideToDeviceMap.clear();
        mCachedDevices.clear();
        if (VALID_SIDES.contains(mCachedDevice.getDeviceSide())
                && mCachedDevice.getBondState() == BOND_BONDED) {
            mSideToDeviceMap.put(mCachedDevice.getDeviceSide(), mCachedDevice.getDevice());
            mCachedDevices.add(mCachedDevice);
        }
        for (CachedBluetoothDevice memberDevice : mCachedDevice.getMemberDevice()) {
            if (VALID_SIDES.contains(memberDevice.getDeviceSide())
                    && memberDevice.getBondState() == BOND_BONDED) {
                mSideToDeviceMap.put(memberDevice.getDeviceSide(), memberDevice.getDevice());
                mCachedDevices.add(memberDevice);
            }
        }
        createAmbientVolumePreference();
        createSliderPreferences();
        if (mPreference != null) {
            mPreference.setExpandable(mSideToDeviceMap.size() > 1);
            mPreference.setSliders((mSideToSliderMap));
        }
    }

    private void createAmbientVolumePreference() {
        if (mPreference != null || mDeviceControls == null) {
            return;
        }

        mPreference = new AmbientVolumePreference(mDeviceControls.getContext());
        mPreference.setKey(KEY_AMBIENT_VOLUME);
        mPreference.setOrder(ORDER_AMBIENT_VOLUME);
        mPreference.setOnIconClickListener(
                new AmbientVolumePreference.OnIconClickListener() {
                    @Override
                    public void onExpandIconClick() {
                        mSideToDeviceMap.forEach((s, d) -> {
                            if (!isControlMuted()) {
                                // Apply previous collapsed/expanded volume to remote device
                                Data data = mLocalDataManager.get(d);
                                int volume = isControlExpanded()
                                        ? data.ambient() : data.groupAmbient();
                                mVolumeController.setAmbient(d, volume);
                            }
                            // Update new value to local data
                            mLocalDataManager.updateAmbientControlExpanded(d, isControlExpanded());
                        });
                    }

                    @Override
                    public void onAmbientVolumeIconClick() {
                        if (!isControlMuted()) {
                            loadLocalDataToUi();
                        }
                        for (BluetoothDevice device : mSideToDeviceMap.values()) {
                            mVolumeController.setMuted(device, isControlMuted());
                        }
                    }
                });
        if (mDeviceControls.findPreference(mPreference.getKey()) == null) {
            mDeviceControls.addPreference(mPreference);
        }
    }

    private void createSliderPreferences() {
        mSideToDeviceMap.forEach((s, d) ->
                createSliderPreference(s, ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED + s));
        createSliderPreference(SIDE_UNIFIED, ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED);
    }

    private void createSliderPreference(int side, int order) {
        if (mSideToSliderMap.containsKey(side) || mDeviceControls == null) {
            return;
        }
        SeekBarPreference preference = new SeekBarPreference(mDeviceControls.getContext());
        preference.setKey(KEY_AMBIENT_VOLUME_SLIDER + "_" + side);
        preference.setOrder(order);
        preference.setOnPreferenceChangeListener(this);
        if (side == SIDE_LEFT) {
            preference.setTitle(mContext.getString(R.string.bluetooth_ambient_volume_control_left));
        } else if (side == SIDE_RIGHT) {
            preference.setTitle(
                    mContext.getString(R.string.bluetooth_ambient_volume_control_right));
        }
        mSideToSliderMap.put(side, preference);
    }

    /** Refreshes the control UI visibility and enabled state. */
    private void refreshControlUi() {
        if (mPreference != null) {
            boolean isAnySliderEnabled = false;
            for (Map.Entry<Integer, BluetoothDevice> entry : mSideToDeviceMap.entrySet()) {
                final int side = entry.getKey();
                final BluetoothDevice device = entry.getValue();
                final boolean enabled = isDeviceConnectedToVcp(device)
                        && mVolumeController.isAmbientControlAvailable(device);
                isAnySliderEnabled |= enabled;
                mPreference.setSliderEnabled(side, enabled);
            }
            mPreference.setSliderEnabled(SIDE_UNIFIED, isAnySliderEnabled);
            mPreference.updateLayout();
        }
    }

    /** Sets the volume to the corresponding control slider. */
    private void setVolumeIfValid(int side, int volume) {
        if (volume == INVALID_VOLUME) {
            return;
        }
        if (mPreference != null) {
            mPreference.setSliderValue(side, volume);
        }
        // Update new value to local data
        if (side == SIDE_UNIFIED) {
            mSideToDeviceMap.forEach((s, d) -> mLocalDataManager.updateGroupAmbient(d, volume));
        } else {
            mLocalDataManager.updateAmbient(mSideToDeviceMap.get(side), volume);
        }
    }

    private void loadLocalDataToUi() {
        mSideToDeviceMap.forEach((s, d) -> loadLocalDataToUi(d));
    }

    private void loadLocalDataToUi(BluetoothDevice device) {
        final Data data = mLocalDataManager.get(device);
        if (DEBUG) {
            Log.d(TAG, "loadLocalDataToUi, data=" + data + ", device=" + device);
        }
        final int side = mSideToDeviceMap.inverse().getOrDefault(device, SIDE_INVALID);
        if (isDeviceConnectedToVcp(device) && !isControlMuted()) {
            setVolumeIfValid(side, data.ambient());
            setVolumeIfValid(SIDE_UNIFIED, data.groupAmbient());
        }
        setControlExpanded(data.ambientControlExpanded());
        refreshControlUi();
    }

    private void loadRemoteDataToUi() {
        BluetoothDevice leftDevice = mSideToDeviceMap.get(SIDE_LEFT);
        AmbientVolumeController.RemoteAmbientState leftState =
                mVolumeController.refreshAmbientState(leftDevice);
        BluetoothDevice rightDevice = mSideToDeviceMap.get(SIDE_RIGHT);
        AmbientVolumeController.RemoteAmbientState rightState =
                mVolumeController.refreshAmbientState(rightDevice);
        if (DEBUG) {
            Log.d(TAG, "loadRemoteDataToUi, left=" + leftState + ", right=" + rightState);
        }

        if (mPreference != null) {
            mSideToDeviceMap.forEach((side, device) -> {
                int ambientMax = mVolumeController.getAmbientMax(device);
                int ambientMin = mVolumeController.getAmbientMin(device);
                if (ambientMin != ambientMax) {
                    mPreference.setSliderRange(side, ambientMin, ambientMax);
                    mPreference.setSliderRange(SIDE_UNIFIED, ambientMin, ambientMax);
                }
            });
        }

        // Update ambient volume
        final int leftAmbient = leftState != null ? leftState.gainSetting() : INVALID_VOLUME;
        final int rightAmbient = rightState != null ? rightState.gainSetting() : INVALID_VOLUME;
        if (isControlExpanded()) {
            setVolumeIfValid(SIDE_LEFT, leftAmbient);
            setVolumeIfValid(SIDE_RIGHT, rightAmbient);
        } else {
            if (leftAmbient != rightAmbient && leftAmbient != INVALID_VOLUME
                    && rightAmbient != INVALID_VOLUME) {
                setVolumeIfValid(SIDE_LEFT, leftAmbient);
                setVolumeIfValid(SIDE_RIGHT, rightAmbient);
                setControlExpanded(true);
            } else {
                int unifiedAmbient = leftAmbient != INVALID_VOLUME ? leftAmbient : rightAmbient;
                setVolumeIfValid(SIDE_UNIFIED, unifiedAmbient);
            }
        }
        // Initialize local data between side and group value
        initLocalDataIfNeeded();

        // Update mute state
        boolean mutable = true;
        boolean muted = true;
        if (isDeviceConnectedToVcp(leftDevice) && leftState != null) {
            mutable &= leftState.isMutable();
            muted &= leftState.isMuted();
        }
        if (isDeviceConnectedToVcp(rightDevice) && rightState != null) {
            mutable &= rightState.isMutable();
            muted &= rightState.isMuted();
        }
        if (mPreference != null) {
            mPreference.setMutable(mutable);
            mPreference.setMuted(muted);
        }

        // Ensure remote device mute state is synced
        syncMuteStateIfNeeded(leftDevice, leftState, muted);
        syncMuteStateIfNeeded(rightDevice, rightState, muted);

        refreshControlUi();
    }

    /** Check if any device in the group has valid ambient control points */
    private boolean isAmbientControlAvailable() {
        for (BluetoothDevice device : mSideToDeviceMap.values()) {
            // Found ambient local data for this device, show the ambient control
            if (mLocalDataManager.get(device).hasAmbientData()) {
                return true;
            }
            // Found remote ambient control points on this device, show the ambient control
            if (mVolumeController.isAmbientControlAvailable(device)) {
                return true;
            }
        }
        return false;
    }

    private boolean isControlExpanded() {
        return mPreference != null && mPreference.isExpanded();
    }

    private void setControlExpanded(boolean expanded) {
        if (mPreference != null && mPreference.isExpanded() != expanded) {
            mPreference.setExpanded(expanded);
        }
        mSideToDeviceMap.forEach((s, d) -> {
            // Update new value to local data
            mLocalDataManager.updateAmbientControlExpanded(d, expanded);
        });
    }

    private boolean isControlMuted() {
        return mPreference != null && mPreference.isMuted();
    }

    private void initLocalDataIfNeeded() {
        int smallerVolumeAmongGroup = Integer.MAX_VALUE;
        for (BluetoothDevice device : mSideToDeviceMap.values()) {
            Data data = mLocalDataManager.get(device);
            if (data.ambient() != INVALID_VOLUME) {
                smallerVolumeAmongGroup = Math.min(data.ambient(), smallerVolumeAmongGroup);
            } else if (data.groupAmbient() != INVALID_VOLUME) {
                // Initialize side ambient from group ambient value
                mLocalDataManager.updateAmbient(device, data.groupAmbient());
            }
        }
        if (smallerVolumeAmongGroup != Integer.MAX_VALUE) {
            for (BluetoothDevice device : mSideToDeviceMap.values()) {
                Data data = mLocalDataManager.get(device);
                if (data.groupAmbient() == INVALID_VOLUME) {
                    // Initialize group ambient from smaller side ambient value
                    mLocalDataManager.updateGroupAmbient(device, smallerVolumeAmongGroup);
                }
            }
        }
    }

    private void syncMuteStateIfNeeded(@Nullable BluetoothDevice device,
            @Nullable AmbientVolumeController.RemoteAmbientState state, boolean muted) {
        if (isDeviceConnectedToVcp(device) && state != null && state.isMutable()) {
            if (state.isMuted() != muted) {
                mVolumeController.setMuted(device, muted);
            }
        }
    }

    private boolean isDeviceConnectedToVcp(@Nullable BluetoothDevice device) {
        return device != null && device.isConnected()
                && mBluetoothManager.getProfileManager().getVolumeControlProfile()
                .getConnectionStatus(device) == BluetoothProfile.STATE_CONNECTED;
    }

    private void showErrorToast() {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(mContext, R.string.bluetooth_ambient_volume_error,
                Toast.LENGTH_SHORT);
        mToast.show();
    }
}
