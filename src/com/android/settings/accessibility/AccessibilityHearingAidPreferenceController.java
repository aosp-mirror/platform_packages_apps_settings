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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothDeviceDetailsFragment;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Controller that shows and updates the bluetooth device name
 */
public class AccessibilityHearingAidPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "AccessibilityHearingAidPreferenceController";
    private Preference mHearingAidPreference;

    private final BroadcastReceiver mHearingAidChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
                final int state = intent.getIntExtra(BluetoothHearingAid.EXTRA_STATE,
                        BluetoothHearingAid.STATE_DISCONNECTED);
                if (state == BluetoothHearingAid.STATE_CONNECTED) {
                    updateState(mHearingAidPreference);
                } else {
                    mHearingAidPreference
                            .setSummary(R.string.accessibility_hearingaid_not_connected_summary);
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state != BluetoothAdapter.STATE_ON) {
                    mHearingAidPreference
                            .setSummary(R.string.accessibility_hearingaid_not_connected_summary);
                }
            }
        }
    };

    private final LocalBluetoothManager mLocalBluetoothManager;
    private final BluetoothAdapter mBluetoothAdapter;
    //cache value of supporting hearing aid or not
    private boolean mHearingAidProfileSupported;
    private FragmentManager mFragmentManager;

    public AccessibilityHearingAidPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mLocalBluetoothManager = getLocalBluetoothManager();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHearingAidProfileSupported = isHearingAidProfileSupported();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mHearingAidPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return mHearingAidProfileSupported ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onStart() {
        if (mHearingAidProfileSupported) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            mContext.registerReceiver(mHearingAidChangedReceiver, filter);
        }
    }

    @Override
    public void onStop() {
        if (mHearingAidProfileSupported) {
            mContext.unregisterReceiver(mHearingAidChangedReceiver);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())){
            final CachedBluetoothDevice device = getConnectedHearingAidDevice();
            if (device == null) {
                launchHearingAidInstructionDialog();
            } else {
                launchBluetoothDeviceDetailSetting(device);
            }
            return true;
        }
        return false;
    }

    @Override
    public CharSequence getSummary() {
        final CachedBluetoothDevice device = getConnectedHearingAidDevice();
        if (device == null) {
            return mContext.getText(R.string.accessibility_hearingaid_not_connected_summary);
        }
        return device.getName();
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    @VisibleForTesting
    CachedBluetoothDevice getConnectedHearingAidDevice() {
        if (!mHearingAidProfileSupported) {
            return null;
        }
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            return null;
        }
        final List<BluetoothDevice> deviceList = mLocalBluetoothManager.getProfileManager()
                .getHearingAidProfile().getConnectedDevices();
        final Iterator it = deviceList.iterator();
        while (it.hasNext()) {
            BluetoothDevice obj = (BluetoothDevice)it.next();
            if (!mLocalBluetoothManager.getCachedDeviceManager().isSubDevice(obj)) {
                return mLocalBluetoothManager.getCachedDeviceManager().findDevice(obj);
            }
        }
        return null;
    }

    private boolean isHearingAidProfileSupported() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            return false;
        }
        final List<Integer> supportedList = mBluetoothAdapter.getSupportedProfiles();
        if (supportedList.contains(BluetoothProfile.HEARING_AID)) {
            return true;
        }
        return false;
    }

    private LocalBluetoothManager getLocalBluetoothManager() {
        final FutureTask<LocalBluetoothManager> localBtManagerFutureTask = new FutureTask<>(
                // Avoid StrictMode ThreadPolicy violation
                () -> com.android.settings.bluetooth.Utils.getLocalBtManager(mContext));
        try {
            localBtManagerFutureTask.run();
            return localBtManagerFutureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w(TAG, "Error getting LocalBluetoothManager.", e);
            return null;
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void setPreference(Preference preference) {
        mHearingAidPreference = preference;
    }

    @VisibleForTesting
    void launchBluetoothDeviceDetailSetting(final CachedBluetoothDevice device) {
        if (device == null) {
            return;
        }
        final Bundle args = new Bundle();
        args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS,
                device.getDevice().getAddress());

        new SubSettingLauncher(mContext)
                .setDestination(BluetoothDeviceDetailsFragment.class.getName())
                .setArguments(args)
                .setTitleRes(R.string.device_details_title)
                .setSourceMetricsCategory(SettingsEnums.ACCESSIBILITY)
                .launch();
    }

    @VisibleForTesting
    void launchHearingAidInstructionDialog() {
        HearingAidDialogFragment fragment = HearingAidDialogFragment.newInstance();
        fragment.show(mFragmentManager, HearingAidDialogFragment.class.toString());
    }
}