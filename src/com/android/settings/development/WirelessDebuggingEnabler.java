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
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * Class to control the switch bar in the wireless debugging fragment.
 */
public class WirelessDebuggingEnabler implements SwitchWidgetController.OnSwitchChangeListener,
        LifecycleObserver, OnResume, OnPause {
    private static final String TAG = "WirelessDebuggingEnabler";

    private final SwitchWidgetController mSwitchWidget;
    private Context mContext;
    private boolean mListeningToOnSwitchChange = false;
    private OnEnabledListener mListener;
    private final ContentResolver mContentResolver;
    private final ContentObserver mSettingsObserver;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public WirelessDebuggingEnabler(Context context, SwitchWidgetController switchWidget,
            OnEnabledListener listener, Lifecycle lifecycle) {
        mContext = context;
        mSwitchWidget = switchWidget;
        mSwitchWidget.setListener(this);
        mSwitchWidget.setupView();

        mListener = listener;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }

        mContentResolver = context.getContentResolver();
        mSettingsObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                Log.i(TAG, "ADB_WIFI_ENABLED=" + isAdbWifiEnabled());
                onWirelessDebuggingEnabled(isAdbWifiEnabled());
            }
        };
    }

    private boolean isAdbWifiEnabled() {
        return Settings.Global.getInt(mContentResolver, Settings.Global.ADB_WIFI_ENABLED,
                AdbPreferenceController.ADB_SETTING_OFF)
                != AdbPreferenceController.ADB_SETTING_OFF;
    }

    /**
     * Tears down the switch controller for the wireless debugging switch.
     */
    public void teardownSwitchController() {
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
        mSwitchWidget.teardownView();
    }

    @Override
    public void onResume() {
        if (!mListeningToOnSwitchChange) {
            mSwitchWidget.startListening();
            mListeningToOnSwitchChange = true;
        }
        onWirelessDebuggingEnabled(isAdbWifiEnabled());
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ADB_WIFI_ENABLED), false,
                mSettingsObserver);
    }

    @Override
    public void onPause() {
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
        mContentResolver.unregisterContentObserver(mSettingsObserver);
    }

    private void onWirelessDebuggingEnabled(boolean enabled) {
        mSwitchWidget.setChecked(enabled);
        if (mListener != null) {
            mListener.onEnabled(enabled);
        }
    }

    protected void writeAdbWifiSetting(boolean enabled) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADB_WIFI_ENABLED, enabled ? AdbPreferenceController.ADB_SETTING_ON
                : AdbPreferenceController.ADB_SETTING_OFF);
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        if (isChecked && !WirelessDebuggingPreferenceController.isWifiConnected(mContext)) {
            // No connected Wi-Fi network. Reset the switch to off.
            Toast.makeText(
                    mContext, R.string.adb_wireless_no_network_msg, Toast.LENGTH_LONG)
                    .show();
            mSwitchWidget.setChecked(false);
            return false;
        }

        writeAdbWifiSetting(isChecked);
        return true;
    }

    /**
     * Interface for subscribers to implement in order to listen for
     * wireless debugging state changes.
     */
    public interface OnEnabledListener {
        /**
         * Called when wireless debugging state changes.
         *
         * @param enabled the state of wireless debugging
         */
        void onEnabled(boolean enabled);
    }
}
