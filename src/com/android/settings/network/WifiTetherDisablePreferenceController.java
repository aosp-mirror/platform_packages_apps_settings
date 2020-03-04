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
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
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
public final class WifiTetherDisablePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "WifiTetherDisablePreferenceController";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final ConnectivityManager mCm;
    private boolean mBluetoothTetherEnabled;
    private boolean mUSBTetherEnabled;
    private PreferenceScreen mScreen;
    private Preference mPreference;
    private final SharedPreferences mSharedPreferences;

    public WifiTetherDisablePreferenceController(Context context, String prefKey) {
        super(context, prefKey);
        mSharedPreferences =
                context.getSharedPreferences(TetherEnabler.SHARED_PREF, Context.MODE_PRIVATE);
        mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mUSBTetherEnabled = mSharedPreferences.getBoolean(
                TetherEnabler.USB_TETHER_KEY, false);
        mBluetoothTetherEnabled = mSharedPreferences.getBoolean(
                TetherEnabler.BLUETOOTH_TETHER_KEY, false);
    }

    @Override
    public boolean isChecked() {
        return !mSharedPreferences.getBoolean(TetherEnabler.KEY_ENABLE_WIFI_TETHERING, true);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        // The shared preference's value is in the opposite of this preference's value.
        final boolean enableWifi = !isChecked;
        if (DEBUG) {
            Log.d(TAG, "check state changing to " + isChecked);
        }
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(TetherEnabler.KEY_ENABLE_WIFI_TETHERING, enableWifi);
        editor.apply();
        return true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @VisibleForTesting
    boolean shouldShow() {
        return mBluetoothTetherEnabled || mUSBTetherEnabled;
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
        if (mUSBTetherEnabled && mBluetoothTetherEnabled) {
            return mContext.getString(R.string.disable_wifi_hotspot_when_usb_and_bluetooth_on);
        } else if (mUSBTetherEnabled) {
            return mContext.getString(R.string.disable_wifi_hotspot_when_usb_on);
        } else if (mBluetoothTetherEnabled) {
            return mContext.getString(R.string.disable_wifi_hotspot_when_bluetooth_on);
        }
        return mContext.getString(R.string.summary_placeholder);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        mPreference = screen.findPreference(mPreferenceKey);
        if (mPreference != null) {
            mPreference.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        setVisible(mScreen, mPreferenceKey, shouldShow());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        final boolean shouldShowBefore = shouldShow();
        if (TextUtils.equals(TetherEnabler.KEY_ENABLE_WIFI_TETHERING, key) && shouldShowBefore) {
            updateState(mPreference);
            return;
        }

        boolean shouldUpdateState = false;
        if (TextUtils.equals(TetherEnabler.USB_TETHER_KEY, key)) {
            mUSBTetherEnabled = sharedPreferences.getBoolean(key, false);
            shouldUpdateState = true;
        } else if (TextUtils.equals(TetherEnabler.BLUETOOTH_TETHER_KEY, key)) {
            mBluetoothTetherEnabled = sharedPreferences.getBoolean(key, false);
            shouldUpdateState = true;
        }

        // Check if we are hiding this preference. If so, make sure the preference is set to
        // unchecked to enable wifi tether.
        if (mPreference != null && mPreference instanceof SwitchPreference
                && shouldShowBefore && !shouldShow()) {
            final SwitchPreference switchPreference = (SwitchPreference) mPreference;
            if (switchPreference.isChecked()) {
                if (DEBUG) {
                    Log.d(TAG,
                            "All other types are unchecked, wifi tether enabled automatically");
                }
                // Need to call this method before internal state set.
                if (switchPreference.callChangeListener(false)) {
                    switchPreference.setChecked(false);
                }
            }
        }

        if (shouldUpdateState) {
            updateState(mPreference);
        }
    }
}
