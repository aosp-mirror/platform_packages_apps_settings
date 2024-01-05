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

package com.android.settings.wifi.tether;

import static android.net.TetheringManager.TETHERING_WIFI;
import static android.net.wifi.WifiManager.SAP_START_FAILURE_GENERAL;

import static com.android.settings.wifi.WifiUtils.canShowWifiHotspot;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiClient;
import android.net.wifi.WifiManager;
import android.text.BidiFormatter;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.network.tether.TetheringManagerModel;
import com.android.settings.widget.GenericSwitchController;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.wifi.WifiEnterpriseRestrictionUtils;
import com.android.settingslib.wifi.WifiUtils;

import java.util.List;

public class WifiTetherPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop,
        SwitchWidgetController.OnSwitchChangeListener {

    private static final String WIFI_TETHER_SETTINGS = "wifi_tether";

    private WifiManager mWifiManager;
    private boolean mIsWifiTetheringAllow;
    private int mSoftApState;
    @VisibleForTesting
    PrimarySwitchPreference mPreference;
    @VisibleForTesting
    WifiTetherSoftApManager mWifiTetherSoftApManager;
    @VisibleForTesting
    TetheringManagerModel mTetheringManagerModel;
    @VisibleForTesting
    boolean mIsDataSaverEnabled;
    @VisibleForTesting
    SwitchWidgetController mSwitch;

    public WifiTetherPreferenceController(Context context, Lifecycle lifecycle,
            TetheringManagerModel tetheringManagerModel) {
        // TODO(b/246537032):Use fragment context to WifiManager service will caused memory leak
        this(context, lifecycle,
                context.getApplicationContext().getSystemService(WifiManager.class),
                true /* initSoftApManager */,
                WifiEnterpriseRestrictionUtils.isWifiTetheringAllowed(context),
                tetheringManagerModel);
    }

    @VisibleForTesting
    WifiTetherPreferenceController(
            Context context,
            Lifecycle lifecycle,
            WifiManager wifiManager,
            boolean initSoftApManager,
            boolean isWifiTetheringAllow,
            TetheringManagerModel tetheringManagerModel) {
        super(context);
        mIsWifiTetheringAllow = isWifiTetheringAllow;
        if (!isWifiTetheringAllow) return;

        mTetheringManagerModel = tetheringManagerModel;
        mWifiManager = wifiManager;

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        if (initSoftApManager) {
            initWifiTetherSoftApManager();
        }
    }

    @Override
    public boolean isAvailable() {
        return canShowWifiHotspot(mContext) && !Utils.isMonkeyRunning();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(WIFI_TETHER_SETTINGS);
        if (mPreference == null) {
            // unavailable
            return;
        }
        if (mSwitch == null) {
            mSwitch = new GenericSwitchController(mPreference);
            mSwitch.setListener(this);
            updateSwitch();
        }
        mPreference.setEnabled(canEnabled());
        if (!mIsWifiTetheringAllow) {
            mPreference.setSummary(R.string.not_allowed_by_ent);
        }
    }

    @Override
    public String getPreferenceKey() {
        return WIFI_TETHER_SETTINGS;
    }

    @Override
    public void onStart() {
        if (mPreference != null) {
            if (mWifiTetherSoftApManager != null) {
                mWifiTetherSoftApManager.registerSoftApCallback();
            }
            if (mSwitch != null) {
                mSwitch.startListening();
            }
        }
    }

    @Override
    public void onStop() {
        if (mPreference != null) {
            if (mWifiTetherSoftApManager != null) {
                mWifiTetherSoftApManager.unRegisterSoftApCallback();
            }
            if (mSwitch != null) {
                mSwitch.stopListening();
            }
        }
    }

    @VisibleForTesting
    void initWifiTetherSoftApManager() {
        // This manager only handles the number of connected devices, other parts are handled by
        // normal BroadcastReceiver in this controller
        mWifiTetherSoftApManager = new WifiTetherSoftApManager(mWifiManager,
                new WifiTetherSoftApManager.WifiTetherSoftApCallback() {
                    @Override
                    public void onStateChanged(int state, int failureReason) {
                        mSoftApState = state;
                        handleWifiApStateChanged(state, failureReason);
                    }

                    @Override
                    public void onConnectedClientsChanged(List<WifiClient> clients) {
                        if (mPreference != null
                                && mSoftApState == WifiManager.WIFI_AP_STATE_ENABLED) {
                            // Only show the number of clients when state is on
                            mPreference.setSummary(
                                    WifiUtils.getWifiTetherSummaryForConnectedDevices(mContext,
                                            clients.size()));
                        }
                    }
                });
    }

    @VisibleForTesting
    void handleWifiApStateChanged(int state, int reason) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                mPreference.setSummary(R.string.wifi_tether_starting);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                mSwitch.setChecked(true);
                final SoftApConfiguration softApConfig = mWifiManager.getSoftApConfiguration();
                updateConfigSummary(softApConfig);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                mPreference.setSummary(R.string.wifi_tether_stopping);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                mSwitch.setChecked(false);
                mPreference.setSummary(R.string.wifi_hotspot_off_subtext);
                break;
            default:
                if (reason == WifiManager.SAP_START_FAILURE_NO_CHANNEL) {
                    mPreference.setSummary(R.string.wifi_sap_no_channel_error);
                } else {
                    mPreference.setSummary(R.string.wifi_error);
                }
        }
    }

    private void updateConfigSummary(@NonNull SoftApConfiguration softApConfig) {
        if (softApConfig == null) {
            // Should never happen.
            return;
        }
        mPreference.setSummary(mContext.getString(R.string.wifi_tether_enabled_subtext,
                BidiFormatter.getInstance().unicodeWrap(softApConfig.getSsid())));
    }

    /**
     * Sets the Data Saver state for preference update.
     */
    public void setDataSaverEnabled(boolean enabled) {
        mIsDataSaverEnabled = enabled;
        if (mPreference != null) {
            mPreference.setEnabled(canEnabled());
        }
        if (mSwitch != null) {
            mSwitch.setEnabled(canEnabled());
        }
    }

    private boolean canEnabled() {
        return mIsWifiTetheringAllow && !mIsDataSaverEnabled;
    }

    @VisibleForTesting
    protected void updateSwitch() {
        if (mWifiManager == null) return;
        int wifiApState = mWifiManager.getWifiApState();
        mSwitch.setEnabled(canEnabled());
        mSwitch.setChecked(wifiApState == WifiManager.WIFI_AP_STATE_ENABLED);
        handleWifiApStateChanged(wifiApState, SAP_START_FAILURE_GENERAL);
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        if (isChecked) {
            mTetheringManagerModel.startTethering(TETHERING_WIFI);
        } else {
            mTetheringManagerModel.stopTethering(TETHERING_WIFI);
        }
        return true;
    }
}
