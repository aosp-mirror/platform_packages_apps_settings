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

import android.app.Fragment;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import androidx.annotation.VisibleForTesting;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * {@link PreferenceControllerMixin} that controls whether the Wi-Fi Wakeup feature should be
 * enabled.
 */
public class WifiWakeupPreferenceController extends AbstractPreferenceController {

    private static final String TAG = "WifiWakeupPrefController";
    private static final String KEY_ENABLE_WIFI_WAKEUP = "enable_wifi_wakeup";

    private final Fragment mFragment;

    @VisibleForTesting
    SwitchPreference mPreference;
    @VisibleForTesting
    LocationManager mLocationManager;

    public WifiWakeupPreferenceController(Context context, DashboardFragment fragment) {
        super(context);
        mFragment = fragment;
        mLocationManager = (LocationManager) context.getSystemService(Service.LOCATION_SERVICE);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (SwitchPreference) screen.findPreference(KEY_ENABLE_WIFI_WAKEUP);
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

        if (!mLocationManager.isLocationEnabled()) {
            final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mFragment.startActivity(intent);
        } else if (getWifiWakeupEnabled()) {
            setWifiWakeupEnabled(false);
        } else if (!getWifiScanningEnabled()) {
            showScanningDialog();
        } else {
            setWifiWakeupEnabled(true);
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

        enableWifiWakeup.setChecked(getWifiWakeupEnabled()
                        && getWifiScanningEnabled()
                        && mLocationManager.isLocationEnabled());
        if (!mLocationManager.isLocationEnabled()) {
            preference.setSummary(getNoLocationSummary());
        } else {
            preference.setSummary(R.string.wifi_wakeup_summary);
        }
    }

    @VisibleForTesting CharSequence getNoLocationSummary() {
        AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo("link", null);
        CharSequence locationText = mContext.getText(R.string.wifi_wakeup_summary_no_location);
        return AnnotationSpan.linkify(locationText, linkInfo);
    }

    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode != WIFI_WAKEUP_REQUEST_CODE) {
            return;
        }
        if (mLocationManager.isLocationEnabled()) {
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

    private void setWifiWakeupEnabled(boolean enabled) {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.WIFI_WAKEUP_ENABLED,
                enabled ? 1 : 0);
    }
}
