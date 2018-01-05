/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * PreferenceController to update of bluetooth {@link SwitchPreference}. It will
 *
 * 1. Invoke the user toggle
 * 2. Listen to the update from {@link LocalBluetoothManager}
 */
public class BluetoothSwitchPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    public static final String KEY_TOGGLE_BLUETOOTH = "toggle_bluetooth_switch";

    private LocalBluetoothManager mBluetoothManager;
    private SwitchPreference mBtPreference;
    private BluetoothEnabler mBluetoothEnabler;
    private RestrictionUtils mRestrictionUtils;
    @VisibleForTesting
    LocalBluetoothAdapter mBluetoothAdapter;

    public BluetoothSwitchPreferenceController(Context context) {
        this(context, Utils.getLocalBtManager(context), new RestrictionUtils());
    }

    @VisibleForTesting
    public BluetoothSwitchPreferenceController(Context context,
            LocalBluetoothManager bluetoothManager, RestrictionUtils restrictionUtils) {
        super(context, KEY_TOGGLE_BLUETOOTH);
        mBluetoothManager = bluetoothManager;
        mRestrictionUtils = restrictionUtils;

        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getBluetoothAdapter();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBtPreference = (SwitchPreference) screen.findPreference(KEY_TOGGLE_BLUETOOTH);
        mBluetoothEnabler = new BluetoothEnabler(mContext,
                new SwitchController(mBtPreference),
                FeatureFactory.getFactory(mContext).getMetricsFeatureProvider(), mBluetoothManager,
                MetricsEvent.ACTION_SETTINGS_MASTER_SWITCH_BLUETOOTH_TOGGLE,
                mRestrictionUtils);
    }

    @Override
    public int getAvailabilityStatus() {
        return mBluetoothAdapter != null ? AVAILABLE : DISABLED_UNSUPPORTED;
    }

    @Override
    public void onStart() {
        mBluetoothEnabler.resume(mContext);
    }

    @Override
    public void onStop() {
        mBluetoothEnabler.pause();
    }

    @Override
    public boolean isChecked() {
        return mBluetoothAdapter != null ? mBluetoothAdapter.isEnabled() : false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.setBluetoothEnabled(isChecked);
        }
        return true;
    }

    /**
     * Control the switch inside {@link SwitchPreference}
     */
    @VisibleForTesting
    class SwitchController extends SwitchWidgetController implements
            Preference.OnPreferenceChangeListener {
        private SwitchPreference mSwitchPreference;

        public SwitchController(SwitchPreference switchPreference) {
            mSwitchPreference = switchPreference;
        }

        @Override
        public void updateTitle(boolean isChecked) {
        }

        @Override
        public void startListening() {
            mSwitchPreference.setOnPreferenceChangeListener(this);
        }

        @Override
        public void stopListening() {
            mSwitchPreference.setOnPreferenceChangeListener(null);
        }

        @Override
        public void setChecked(boolean checked) {
            mSwitchPreference.setChecked(checked);
        }

        @Override
        public boolean isChecked() {
            return mSwitchPreference.isChecked();
        }

        @Override
        public void setEnabled(boolean enabled) {
            mSwitchPreference.setEnabled(enabled);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (mListener != null) {
                return mListener.onSwitchToggled((Boolean) newValue);
            }
            return false;
        }

        @Override
        public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
            mBtPreference.setEnabled(admin == null);
        }
    }
}
