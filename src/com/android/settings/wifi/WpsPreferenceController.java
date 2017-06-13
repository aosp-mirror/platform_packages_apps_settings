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

import android.app.Dialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * {@link PreferenceControllerMixin} that shows Dialog for WPS progress. Disabled when Wi-Fi is off.
 */
public class WpsPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {

    private static final String KEY_WPS_PUSH = "wps_push_button";
    private static final String KEY_WPS_PIN = "wps_pin_entry";

    private final WifiManager mWifiManager;
    private final FragmentManager mFragmentManager;
    @VisibleForTesting
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            togglePreferences();
        }
    };
    private final IntentFilter mFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);

    private Preference mWpsPushPref;
    private Preference mWpsPinPref;

    public WpsPreferenceController(
            Context context,
            Lifecycle lifecycle,
            WifiManager wifiManager,
            FragmentManager fragmentManager) {
        super(context);
        mWifiManager = wifiManager;
        mFragmentManager = fragmentManager;
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        // Always show preference.
        return true;
    }

    @Override
    public String getPreferenceKey() {
        // Returns null because this controller contains more than 1 preference.
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mWpsPushPref = screen.findPreference(KEY_WPS_PUSH);
        mWpsPinPref = screen.findPreference(KEY_WPS_PIN);
        if (mWpsPushPref == null || mWpsPinPref == null) {
            return;
        }
        // WpsDialog: Create the dialog like WifiSettings does.
        mWpsPushPref.setOnPreferenceClickListener((arg) -> {
                    WpsFragment wpsFragment = new WpsFragment(WpsInfo.PBC);
                    wpsFragment.show(mFragmentManager, KEY_WPS_PUSH);
                    return true;
                }
        );

        // WpsDialog: Create the dialog like WifiSettings does.
        mWpsPinPref.setOnPreferenceClickListener((arg) -> {
            WpsFragment wpsFragment = new WpsFragment(WpsInfo.DISPLAY);
            wpsFragment.show(mFragmentManager, KEY_WPS_PIN);
            return true;
        });
        togglePreferences();
    }

    @Override
    public void onResume() {
        mContext.registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(mReceiver);
    }

    private void togglePreferences() {
        if (mWpsPushPref != null && mWpsPinPref != null) {
            boolean enabled = mWifiManager.isWifiEnabled();
            mWpsPushPref.setEnabled(enabled);
            mWpsPinPref.setEnabled(enabled);
        }
    }

    /**
     * Fragment for Dialog to show WPS progress.
     */
    public static class WpsFragment extends InstrumentedDialogFragment {
        private static int mWpsSetup;

        // Public default constructor is required for rotation.
        public WpsFragment() {
            super();
        }

        public WpsFragment(int wpsSetup) {
            super();
            mWpsSetup = wpsSetup;
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.DIALOG_WPS_SETUP;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new WpsDialog(getActivity(), mWpsSetup);
        }
    }
}
