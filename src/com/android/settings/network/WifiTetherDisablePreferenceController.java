/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network;

import android.content.Context;
import android.net.TetheringManager;

import androidx.lifecycle.LifecycleObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.TetherUtil;

/**
 * This controller helps to manage the switch state and visibility of wifi tether disable switch
 * preference. When the preference checked, wifi tether will be disabled.
 * It stores preference value when preference changed and listens to usb tether and bluetooth tether
 * preferences.
 *
 * @see BluetoothTetherPreferenceController
 * @see UsbTetherPreferenceController
 */
public final class WifiTetherDisablePreferenceController extends TetherBasePreferenceController
        implements LifecycleObserver {

    private static final String TAG = "WifiTetherDisablePreferenceController";

    private boolean mBluetoothTethering;
    private boolean mUsbTethering;
    private boolean mWifiTethering;
    private PreferenceScreen mScreen;

    public WifiTetherDisablePreferenceController(Context context, String prefKey) {
        super(context, prefKey);
    }

    @Override
    public boolean isChecked() {
        return !mWifiTethering;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mTetherEnabler == null) {
            return false;
        }
        if (isChecked) {
            mTetherEnabler.stopTethering(TetheringManager.TETHERING_WIFI);
        } else {
            mTetherEnabler.startTethering(TetheringManager.TETHERING_WIFI);
        }
        return true;
    }

    @VisibleForTesting
    boolean shouldShow() {
        return mBluetoothTethering || mUsbTethering;
    }

    @Override
    public int getAvailabilityStatus() {
        final String[] wifiRegexs = mCm.getTetherableWifiRegexs();
        if (wifiRegexs == null || wifiRegexs.length == 0 || !shouldShow()
                || !TetherUtil.isTetherAvailable(mContext)) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public CharSequence getSummary() {
        if (mUsbTethering && mBluetoothTethering) {
            return mContext.getString(R.string.disable_wifi_hotspot_when_usb_and_bluetooth_on);
        } else if (mUsbTethering) {
            return mContext.getString(R.string.disable_wifi_hotspot_when_usb_on);
        } else if (mBluetoothTethering) {
            return mContext.getString(R.string.disable_wifi_hotspot_when_bluetooth_on);
        }
        return mContext.getString(R.string.summary_placeholder);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        if (mPreference != null) {
            mPreference.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        setVisible(mScreen, mPreferenceKey, shouldShow());
        refreshSummary(preference);
    }

    @Override
    public void onTetherStateUpdated(int state) {
        mUsbTethering = TetherEnabler.isUsbTethering(state);
        mBluetoothTethering = TetherEnabler.isBluetoothTethering(state);
        mWifiTethering = TetherEnabler.isWifiTethering(state);
        updateState(mPreference);
    }
}
