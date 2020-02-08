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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

/**
 * This controller helps to manage the switch state and visibility of USB tether switch
 * preference. It stores preference values when preference changed.
 *
 */
public final class UsbTetherPreferenceController extends BasePreferenceController implements
        LifecycleObserver, Preference.OnPreferenceChangeListener {

    private static final String TAG = "UsbTetherPrefController";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final ConnectivityManager mCm;
    private boolean mUsbConnected;
    private boolean mMassStorageActive;
    private Preference mPreference;
    private final SharedPreferences mSharedPreferences;

    public UsbTetherPreferenceController(Context context, String prefKey) {
        super(context, prefKey);
        mCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mSharedPreferences =
                context.getSharedPreferences(TetherEnabler.SHARED_PREF, Context.MODE_PRIVATE);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_STATE);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        mContext.registerReceiver(mUsbChangeReceiver, filter);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mContext.unregisterReceiver(mUsbChangeReceiver);
    }

    @Override
    public int getAvailabilityStatus() {
        String[] usbRegexs = mCm.getTetherableUsbRegexs();
        if (usbRegexs == null || usbRegexs.length == 0 || Utils.isMonkeyRunning()) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);
        if (mPreference != null && mPreference instanceof SwitchPreference) {
            ((SwitchPreference) mPreference)
                    .setChecked(mSharedPreferences.getBoolean(mPreferenceKey, false));
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            if (mUsbConnected && !mMassStorageActive) {
                preference.setEnabled(true);
            } else {
                preference.setEnabled(false);
            }
        }
    }

    @VisibleForTesting
    final BroadcastReceiver mUsbChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(Intent.ACTION_MEDIA_SHARED, action)) {
                mMassStorageActive = true;
            } else if (TextUtils.equals(Intent.ACTION_MEDIA_UNSHARED, action)) {
                mMassStorageActive = false;
            } else if (TextUtils.equals(UsbManager.ACTION_USB_STATE, action)) {
                mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
            }
            updateState(mPreference);
        }
    };

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (DEBUG) {
            Log.d(TAG, "preference changing to " + o);
        }
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(mPreferenceKey, (Boolean) o);
        editor.apply();
        return true;
    }
}
