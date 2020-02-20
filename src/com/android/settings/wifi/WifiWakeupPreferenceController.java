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
import android.net.wifi.WifiManager;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * {@link TogglePreferenceController} that controls whether the Wi-Fi Wakeup feature should be
 * enabled.
 */
public class WifiWakeupPreferenceController extends TogglePreferenceController implements
        LifecycleObserver, OnPause, OnResume {

    private static final String TAG = "WifiWakeupPrefController";
    private static final String KEY_ENABLE_WIFI_WAKEUP = "enable_wifi_wakeup";

    private Fragment mFragment;

    @VisibleForTesting
    SwitchPreference mPreference;

    @VisibleForTesting
    LocationManager mLocationManager;

    @VisibleForTesting
    WifiManager mWifiManager;

    private final BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateState(mPreference);
        }
    };

    private final IntentFilter mLocationFilter =
            new IntentFilter(LocationManager.MODE_CHANGED_ACTION);

    public WifiWakeupPreferenceController(Context context) {
        super(context, KEY_ENABLE_WIFI_WAKEUP);
        mLocationManager = (LocationManager) context.getSystemService(Service.LOCATION_SERVICE);
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    public void setFragment(Fragment hostFragment) {
        mFragment = hostFragment;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return getWifiWakeupEnabled()
                && getWifiScanningEnabled()
                && mLocationManager.isLocationEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            if (mFragment == null) {
                throw new IllegalStateException("No fragment to start activity");
            }

            if (!mLocationManager.isLocationEnabled()) {
                final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mFragment.startActivityForResult(intent, WIFI_WAKEUP_REQUEST_CODE);
                return false;
            } else if (!getWifiScanningEnabled()) {
                showScanningDialog();
                return false;
            }
        }

        setWifiWakeupEnabled(isChecked);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @Override
    public CharSequence getSummary() {
        if (!mLocationManager.isLocationEnabled()) {
            return getNoLocationSummary();
        } else {
            return mContext.getText(R.string.wifi_wakeup_summary);
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
            updateState(mPreference);
        }
    }

    private boolean getWifiScanningEnabled() {
        return mWifiManager.isScanAlwaysAvailable();
    }

    private void showScanningDialog() {
        final WifiScanningRequiredFragment dialogFragment =
                WifiScanningRequiredFragment.newInstance();
        dialogFragment.setTargetFragment(mFragment, WIFI_WAKEUP_REQUEST_CODE /* requestCode */);
        dialogFragment.show(mFragment.getFragmentManager(), TAG);
    }

    private boolean getWifiWakeupEnabled() {
        return mWifiManager.isAutoWakeupEnabled();
    }

    private void setWifiWakeupEnabled(boolean enabled) {
        mWifiManager.setAutoWakeupEnabled(enabled);
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
