/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE;
import static android.media.AudioManager.AUDIO_DEVICE_CATEGORY_CARKIT;
import static android.media.AudioManager.AUDIO_DEVICE_CATEGORY_HEADPHONES;
import static android.media.AudioManager.AUDIO_DEVICE_CATEGORY_HEARING_AID;
import static android.media.AudioManager.AUDIO_DEVICE_CATEGORY_OTHER;
import static android.media.AudioManager.AUDIO_DEVICE_CATEGORY_SPEAKER;
import static android.media.AudioManager.AUDIO_DEVICE_CATEGORY_UNKNOWN;
import static android.media.audio.Flags.automaticBtDeviceType;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.AudioDeviceCategory;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * Controller responsible for the bluetooth audio device type selection
 */
public class BluetoothDetailsAudioDeviceTypeController extends BluetoothDetailsController
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "BluetoothDetailsAudioDeviceTypeController";

    private static final boolean DEBUG = false;

    private static final String KEY_BT_AUDIO_DEVICE_TYPE_GROUP =
            "bluetooth_audio_device_type_group";
    private static final String KEY_BT_AUDIO_DEVICE_TYPE = "bluetooth_audio_device_type";

    private final AudioManager mAudioManager;

    private ListPreference mAudioDeviceTypePreference;

    private final LocalBluetoothProfileManager mProfileManager;

    @VisibleForTesting
    PreferenceCategory mProfilesContainer;

    public BluetoothDetailsAudioDeviceTypeController(
            Context context,
            PreferenceFragmentCompat fragment,
            LocalBluetoothManager manager,
            CachedBluetoothDevice device,
            Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mAudioManager = context.getSystemService(AudioManager.class);
        mProfileManager = manager.getProfileManager();
    }

    @Override
    public boolean isAvailable() {
        // Available only for A2DP and BLE devices.
        A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();
        boolean a2dpProfileEnabled = false;
        if (a2dpProfile != null) {
            a2dpProfileEnabled = a2dpProfile.isEnabled(mCachedDevice.getDevice());
        }

        LeAudioProfile leAudioProfile = mProfileManager.getLeAudioProfile();
        boolean leAudioProfileEnabled = false;
        if (leAudioProfile != null) {
            leAudioProfileEnabled = leAudioProfile.isEnabled(mCachedDevice.getDevice());
        }

        return a2dpProfileEnabled || leAudioProfileEnabled;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof ListPreference) {
            final ListPreference pref = (ListPreference) preference;
            final String key = pref.getKey();
            if (key.equals(KEY_BT_AUDIO_DEVICE_TYPE)) {
                if (newValue instanceof String) {
                    final String value = (String) newValue;
                    final int index = pref.findIndexOfValue(value);
                    if (index >= 0) {
                        pref.setSummary(pref.getEntries()[index]);
                        if (automaticBtDeviceType()) {
                            mAudioManager.setBluetoothAudioDeviceCategory(
                                    mCachedDevice.getAddress(), Integer.parseInt(value));
                        } else {
                            mAudioManager.setBluetoothAudioDeviceCategory_legacy(
                                    mCachedDevice.getAddress(),
                                    mCachedDevice.getDevice().getType() == DEVICE_TYPE_LE,
                                    Integer.parseInt(value));
                        }
                        mCachedDevice.onAudioDeviceCategoryChanged();
                    }
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BT_AUDIO_DEVICE_TYPE_GROUP;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mProfilesContainer = screen.findPreference(getPreferenceKey());
        refresh();
    }

    @Override
    protected void refresh() {
        mAudioDeviceTypePreference = mProfilesContainer.findPreference(
                KEY_BT_AUDIO_DEVICE_TYPE);
        if (mAudioDeviceTypePreference == null) {
            createAudioDeviceTypePreference(mProfilesContainer.getContext());
            mProfilesContainer.addPreference(mAudioDeviceTypePreference);
        }
    }

    @VisibleForTesting
    void createAudioDeviceTypePreference(Context context) {
        mAudioDeviceTypePreference = new ListPreference(context);
        mAudioDeviceTypePreference.setKey(KEY_BT_AUDIO_DEVICE_TYPE);
        mAudioDeviceTypePreference.setTitle(
                mContext.getString(R.string.bluetooth_details_audio_device_types_title));
        mAudioDeviceTypePreference.setEntries(new CharSequence[]{
                mContext.getString(R.string.bluetooth_details_audio_device_type_unknown),
                mContext.getString(R.string.bluetooth_details_audio_device_type_speaker),
                mContext.getString(R.string.bluetooth_details_audio_device_type_headphones),
                mContext.getString(R.string.bluetooth_details_audio_device_type_carkit),
                mContext.getString(R.string.bluetooth_details_audio_device_type_hearing_aid),
                mContext.getString(R.string.bluetooth_details_audio_device_type_other),
        });
        mAudioDeviceTypePreference.setEntryValues(new CharSequence[]{
                Integer.toString(AUDIO_DEVICE_CATEGORY_UNKNOWN),
                Integer.toString(AUDIO_DEVICE_CATEGORY_SPEAKER),
                Integer.toString(AUDIO_DEVICE_CATEGORY_HEADPHONES),
                Integer.toString(AUDIO_DEVICE_CATEGORY_CARKIT),
                Integer.toString(AUDIO_DEVICE_CATEGORY_HEARING_AID),
                Integer.toString(AUDIO_DEVICE_CATEGORY_OTHER),
        });

        @AudioDeviceCategory int deviceCategory;
        if (automaticBtDeviceType()) {
            deviceCategory = mAudioManager.getBluetoothAudioDeviceCategory(
                    mCachedDevice.getAddress());
        } else {
            deviceCategory = mAudioManager.getBluetoothAudioDeviceCategory_legacy(
                    mCachedDevice.getAddress(),
                    mCachedDevice.getDevice().getType() == DEVICE_TYPE_LE);
        }
        if (DEBUG) {
            Log.v(TAG, "getBluetoothAudioDeviceCategory() device: "
                    + mCachedDevice.getDevice().getAnonymizedAddress()
                    + ", has audio device category: " + deviceCategory);
        }
        mAudioDeviceTypePreference.setValue(Integer.toString(deviceCategory));

        if (automaticBtDeviceType()) {
            if (mAudioManager.isBluetoothAudioDeviceCategoryFixed(mCachedDevice.getAddress())) {
                mAudioDeviceTypePreference.setEnabled(false);
            }
        }

        mAudioDeviceTypePreference.setSummary(mAudioDeviceTypePreference.getEntry());
        mAudioDeviceTypePreference.setOnPreferenceChangeListener(this);
    }

    @VisibleForTesting
    ListPreference getAudioDeviceTypePreference() {
        return mAudioDeviceTypePreference;
    }
}
