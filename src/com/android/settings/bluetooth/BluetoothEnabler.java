/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.Toast;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * BluetoothEnabler is a helper to manage the Bluetooth on/off checkbox
 * preference. It turns on/off Bluetooth and ensures the summary of the
 * preference reflects the current state.
 */
public final class BluetoothEnabler implements SwitchWidgetController.OnSwitchChangeListener {
    private final Switch mSwitch;
    private final SwitchWidgetController mSwitchWidget;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private Context mContext;
    private boolean mValidListener;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final IntentFilter mIntentFilter;
    private final RestrictionUtils mRestrictionUtils;

    private static final String EVENT_DATA_IS_BT_ON = "is_bluetooth_on";
    private static final int EVENT_UPDATE_INDEX = 0;
    private final int mMetricsEvent;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Broadcast receiver is always running on the UI thread here,
            // so we don't need consider thread synchronization.
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            handleStateChanged(state);
        }
    };

    public BluetoothEnabler(Context context, SwitchWidgetController switchWidget,
            MetricsFeatureProvider metricsFeatureProvider, LocalBluetoothManager manager,
            int metricsEvent) {
        this(context, switchWidget, metricsFeatureProvider, manager, metricsEvent,
                new RestrictionUtils());
    }

    public BluetoothEnabler(Context context, SwitchWidgetController switchWidget,
            MetricsFeatureProvider metricsFeatureProvider, LocalBluetoothManager manager,
            int metricsEvent, RestrictionUtils restrictionUtils) {
        mContext = context;
        mMetricsFeatureProvider = metricsFeatureProvider;
        mSwitchWidget = switchWidget;
        mSwitch = mSwitchWidget.getSwitch();
        mSwitchWidget.setListener(this);
        mValidListener = false;
        mMetricsEvent = metricsEvent;

        if (manager == null) {
            // Bluetooth is not supported
            mLocalAdapter = null;
            mSwitchWidget.setEnabled(false);
        } else {
            mLocalAdapter = manager.getBluetoothAdapter();
        }
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mRestrictionUtils = restrictionUtils;
    }

    public void setupSwitchController() {
        mSwitchWidget.setupView();
    }

    public void teardownSwitchController() {
        mSwitchWidget.teardownView();
    }

    public void resume(Context context) {
        if (mContext != context) {
            mContext = context;
        }

        final boolean restricted = maybeEnforceRestrictions();

        if (mLocalAdapter == null) {
            mSwitchWidget.setEnabled(false);
            return;
        }

        // Bluetooth state is not sticky, so set it manually
        if (!restricted) {
            handleStateChanged(mLocalAdapter.getBluetoothState());
        }

        mSwitchWidget.startListening();
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mValidListener = true;
    }

    public void pause() {
        if (mLocalAdapter == null) {
            return;
        }
        if (mValidListener) {
            mSwitchWidget.stopListening();
            mContext.unregisterReceiver(mReceiver);
            mValidListener = false;
        }
    }

    void handleStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                mSwitchWidget.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_ON:
                setChecked(true);
                mSwitchWidget.setEnabled(true);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mSwitchWidget.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_OFF:
                setChecked(false);
                mSwitchWidget.setEnabled(true);
                break;
            default:
                setChecked(false);
                mSwitchWidget.setEnabled(true);
        }
    }

    private void setChecked(boolean isChecked) {
        final boolean currentState =
                (mSwitchWidget.getSwitch() != null) && mSwitchWidget.getSwitch().isChecked();
        if (isChecked != currentState) {
            // set listener to null, so onCheckedChanged won't be called
            // if the checked status on Switch isn't changed by user click
            if (mValidListener) {
                mSwitchWidget.stopListening();
            }
            mSwitchWidget.setChecked(isChecked);
            if (mValidListener) {
                mSwitchWidget.startListening();
            }
        }
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        if (maybeEnforceRestrictions()) {
            return true;
        }

        // Show toast message if Bluetooth is not allowed in airplane mode
        if (isChecked &&
                !WirelessUtils.isRadioAllowed(mContext, Settings.Global.RADIO_BLUETOOTH)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off
            mSwitch.setChecked(false);
            return false;
        }

        mMetricsFeatureProvider.action(mContext, mMetricsEvent, isChecked);

        if (mLocalAdapter != null) {
            boolean status = mLocalAdapter.setBluetoothEnabled(isChecked);
            // If we cannot toggle it ON then reset the UI assets:
            // a) The switch should be OFF but it should still be togglable (enabled = True)
            // b) The switch bar should have OFF text.
            if (isChecked && !status) {
                mSwitch.setChecked(false);
                mSwitch.setEnabled(true);
                mSwitchWidget.updateTitle(false);
                return false;
            }
        }
        mSwitchWidget.setEnabled(false);
        return true;
    }

    /**
     * Enforces user restrictions disallowing Bluetooth (or its configuration) if there are any.
     *
     * @return if there was any user restriction to enforce.
     */
    @VisibleForTesting
    boolean maybeEnforceRestrictions() {
        EnforcedAdmin admin = getEnforcedAdmin(mRestrictionUtils, mContext);
        mSwitchWidget.setDisabledByAdmin(admin);
        if (admin != null) {
            mSwitchWidget.setChecked(false);
            if (mSwitch != null) {
                mSwitch.setEnabled(false);
                mSwitch.setChecked(false);
            }
        }
        return admin != null;
    }

    public static EnforcedAdmin getEnforcedAdmin(RestrictionUtils mRestrictionUtils,
            Context mContext) {
        EnforcedAdmin admin = mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_BLUETOOTH);
        if (admin == null) {
            admin = mRestrictionUtils.checkIfRestrictionEnforced(
                    mContext, UserManager.DISALLOW_CONFIG_BLUETOOTH);
        }
        return admin;
    }

}
