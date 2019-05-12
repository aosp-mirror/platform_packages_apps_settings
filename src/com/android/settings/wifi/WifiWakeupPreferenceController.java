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

package com.android.settings.wifi;

import static com.android.settings.wifi.ConfigureWifiSettings.WIFI_WAKEUP_REQUEST_CODE;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * {@link PreferenceControllerMixin} that controls whether the Wi-Fi Wakeup feature should be
 * enabled.
 */
public class WifiWakeupPreferenceController extends AbstractPreferenceController implements
        LifecycleObserver, OnPause, OnResume {

    private static final String TAG = "WifiWakeupPrefController";
    private static final String KEY_ENABLE_WIFI_WAKEUP = "enable_wifi_wakeup";

    private final Fragment mFragment;

    @VisibleForTesting
    SwitchPreference mPreference;
    @VisibleForTesting
    LocationManager mLocationManager;
    private final BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateState(mPreference);
        }
    };
    private final IntentFilter mLocationFilter =
            new IntentFilter(LocationManager.MODE_CHANGED_ACTION);

    public WifiWakeupPreferenceController(Context context, DashboardFragment fragment,
            Lifecycle lifecycle) {
        super(context);
        mFragment = fragment;
        mLocationManager = (LocationManager) context.getSystemService(Service.LOCATION_SERVICE);
        lifecycle.addObserver(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_ENABLE_WIFI_WAKEUP);
        updateState(mPreference);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_ENABLE_WIFI_WAKEUP)) {
            return false;
        }
        if (!(preference instanceof SwitchPreference)) {
            return false;
        }

        // TODO(b/132391311): WifiWakeupPreferenceController is essentially reimplementing
        // TogglePreferenceController. Refactor it into TogglePreferenceController.

        // Toggle wifi-wakeup setting between 1/0 based on its current state, and some other checks.
        if (isWifiWakeupAvailable()) {
            setWifiWakeupEnabled(false);
        } else {
            if (!mLocationManager.isLocationEnabled()) {
                final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mFragment.startActivityForResult(intent, WIFI_WAKEUP_REQUEST_CODE);
                return true;
            } else if (!getWifiScanningEnabled()) {
                showScanningDialog();
            } else {
                setWifiWakeupEnabled(true);
            }
        }

        updateState(mPreference);
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ENABLE_WIFI_WAKEUP;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        final SwitchPreference enableWifiWakeup = (SwitchPreference) preference;

        enableWifiWakeup.setChecked(isWifiWakeupAvailable());
        if (!mLocationManager.isLocationEnabled()) {
            preference.setSummary(getNoLocationSummary());
        } else {
            preference.setSummary(R.string.wifi_wakeup_summary);
        }
    }

    @VisibleForTesting
    CharSequence getNoLocationSummary() {
        AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo("link", null);
        CharSequence locationText = mContext.getText(R.string.wifi_wakeup_summary_no_location);
        return AnnotationSpan.linkify(locationText, linkInfo);
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode != WIFI_WAKEUP_REQUEST_CODE) {
            return;
        }
        if (mLocationManager.isLocationEnabled() && getWifiScanningEnabled()) {
            setWifiWakeupEnabled(true);
        }
        updateState(mPreference);
    }

    private boolean getWifiScanningEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1;
    }

    private void showScanningDialog() {
        final WifiScanningRequiredFragment dialogFragment =
                WifiScanningRequiredFragment.newInstance();
        dialogFragment.setTargetFragment(mFragment, WIFI_WAKEUP_REQUEST_CODE /* requestCode */);
        dialogFragment.show(mFragment.getFragmentManager(), TAG);
    }

    private boolean getWifiWakeupEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WIFI_WAKEUP_ENABLED, 0) == 1;
    }

    /**
     * Wifi wakeup is available only when both location and Wi-Fi scanning are enabled.
     */
    private boolean isWifiWakeupAvailable() {
        return getWifiWakeupEnabled()
                && getWifiScanningEnabled()
                && mLocationManager.isLocationEnabled();
    }

    private void setWifiWakeupEnabled(boolean enabled) {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.WIFI_WAKEUP_ENABLED,
                enabled ? 1 : 0);
    }

    @Override
    public void onResume() {
        mContext.registerReceiver(mLocationReceiver, mLocationFilter);
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(mLocationReceiver);
    }
}
