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
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * This controller helps to manage the switch state and visibility of wifi tether disable switch
 * preference. When the preference checked, wifi tether will be disabled.
 * It stores preference value when preference changed and listens to usb tether and bluetooth tether
 * preferences.
 *
 * @see BluetoothTetherPreferenceController
 * @see UsbTetherPreferenceController
 * TODO(b/147272749): Extend BasePreferenceController.java instead.
 *
 */
public final class WifiTetherDisablePreferenceController extends AbstractPreferenceController
        implements LifecycleObserver, Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "WifiTetherDisablePreferenceController";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    public static final String PREF_KEY = "disable_wifi_tethering";

    // This KEY is used for a shared preference value, not for any displayed preferences.
    public static final String KEY_ENABLE_WIFI_TETHERING = "enable_wifi_tethering";

    private final ConnectivityManager mCm;
    private boolean mBluetoothTetherEnabled;
    private boolean mUSBTetherEnabled;
    private PreferenceScreen mScreen;
    private Preference mPreference;
    private final SharedPreferences mSharedPreferences;

    public WifiTetherDisablePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mSharedPreferences =
                context.getSharedPreferences(TetherEnabler.SHARED_PREF, Context.MODE_PRIVATE);
        mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mUSBTetherEnabled = mSharedPreferences.getBoolean(
                TetherEnabler.USB_TETHER_KEY, false);
        mBluetoothTetherEnabled = mSharedPreferences.getBoolean(
                TetherEnabler.BLUETOOTH_TETHER_KEY, false);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean isAvailable() {
        final String[] wifiRegexs = mCm.getTetherableWifiRegexs();
        return wifiRegexs != null && wifiRegexs.length > 0 && shouldShow();
    }

    @VisibleForTesting
    boolean shouldShow() {
        return mBluetoothTetherEnabled || mUSBTetherEnabled;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public CharSequence getSummary() {
        // TODO(b/146818850): Update summary accordingly.
        return super.getSummary();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        mPreference = screen.findPreference(PREF_KEY);
        if (mPreference != null && mPreference instanceof SwitchPreference) {
            ((SwitchPreference) mPreference)
                    .setChecked(!mSharedPreferences.getBoolean(KEY_ENABLE_WIFI_TETHERING, true));
            mPreference.setOnPreferenceChangeListener(this);
        }
        updateState(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        setVisible(mScreen, PREF_KEY, shouldShow());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (TextUtils.equals(TetherEnabler.USB_TETHER_KEY, key)) {
            mUSBTetherEnabled = sharedPreferences.getBoolean(key, false);
        } else if (TextUtils.equals(TetherEnabler.BLUETOOTH_TETHER_KEY, key)) {
            mBluetoothTetherEnabled = sharedPreferences.getBoolean(key, false);
        }

        // Check if we are hiding this preference. If so,  make sure the preference is set to
        // unchecked to enable wifi tether.
        if (mPreference != null && mPreference instanceof SwitchPreference && !shouldShow()) {
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

        updateState(mPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        // The shared preference's value is in the opposite of this preference's value.
        final boolean enableWifi = !(boolean) o;
        if (DEBUG) {
            Log.d(TAG, "check state changing to " + o);
        }
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(KEY_ENABLE_WIFI_TETHERING, enableWifi);
        editor.apply();
        return true;
    }
}
