/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.development;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.debug.IAdbManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * This controls the primary switch controller in the developer options page for
 * "Wireless debugging".
 */
public class WirelessDebuggingPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin,
        LifecycleObserver, OnResume, OnPause {
    private static final String TAG = "WirelessDebugPrefCtrl";
    private final IAdbManager mAdbManager;
    private final ContentResolver mContentResolver;
    private final ContentObserver mSettingsObserver;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public static final String KEY_TOGGLE_ADB_WIRELESS = "toggle_adb_wireless";

    public WirelessDebuggingPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(Context.ADB_SERVICE));
        mSettingsObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateState(mPreference);
            }
        };
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public boolean isAvailable() {
        try {
            return mAdbManager.isAdbWifiSupported();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to check if adb wifi is supported.", e);
        }

        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TOGGLE_ADB_WIRELESS;
    }

    /**
     * Called when developer options is enabled and the preference is available
     */
    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        super.onDeveloperOptionsSwitchEnabled();
        mPreference.setEnabled(true);
    }

    /**
     * Called when developer options is disabled and the preference is available
     */
    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        mPreference.setEnabled(false);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADB_WIFI_ENABLED,
                AdbPreferenceController.ADB_SETTING_OFF);
    }

    @Override
    public void onResume() {
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ADB_WIFI_ENABLED), false,
                mSettingsObserver);
    }

    @Override
    public void onPause() {
        mContentResolver.unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public void updateState(Preference preference) {
        boolean enabled = Settings.Global.getInt(mContentResolver,
                Settings.Global.ADB_WIFI_ENABLED, AdbPreferenceController.ADB_SETTING_OFF)
                    != AdbPreferenceController.ADB_SETTING_OFF;
        ((PrimarySwitchPreference) preference).setChecked(enabled);
    }

    /**
     * Returns true if connected to Wi-Fi network.
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        for (Network network : cm.getAllNetworks()) {
            final NetworkCapabilities nc = cm.getNetworkCapabilities(network);
            if (nc == null) {
                continue;
            }
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean enabled = (Boolean) newValue;
        if (enabled && !isWifiConnected(mContext)) {
            // Cannot enable ADB over Wi-Fi if we're not connected to wifi.
            Toast.makeText(
                    mContext, com.android.settingslib.R.string.adb_wireless_no_network_msg,
                            Toast.LENGTH_LONG)
                    .show();
            return false;
        }

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADB_WIFI_ENABLED,
                enabled ? AdbPreferenceController.ADB_SETTING_ON
                : AdbPreferenceController.ADB_SETTING_OFF);
        return true;
    }
}
