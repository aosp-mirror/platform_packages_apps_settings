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

import static android.net.wifi.WifiManager.WIFI_AP_STATE_CHANGED_ACTION;

import static com.android.settings.wifi.WifiUtils.canShowWifiHotspot;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.FeatureFlags;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settings.wifi.WifiUtils;
import com.android.settingslib.TetherUtil;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.wifi.WifiEnterpriseRestrictionUtils;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class WifiTetherSettings extends RestrictedDashboardFragment
        implements WifiTetherBasePreferenceController.OnTetherConfigUpdateListener {

    private static final String TAG = "WifiTetherSettings";
    private static final IntentFilter TETHER_STATE_CHANGE_FILTER;
    private static final String KEY_WIFI_TETHER_SCREEN = "wifi_tether_settings_screen";

    @VisibleForTesting
    static final String KEY_WIFI_TETHER_NETWORK_NAME = "wifi_tether_network_name";
    @VisibleForTesting
    static final String KEY_WIFI_TETHER_SECURITY = "wifi_tether_security";
    @VisibleForTesting
    static final String KEY_WIFI_TETHER_NETWORK_PASSWORD = "wifi_tether_network_password";
    @VisibleForTesting
    static final String KEY_WIFI_TETHER_AUTO_OFF = "wifi_tether_auto_turn_off";
    @VisibleForTesting
    static final String KEY_WIFI_TETHER_MAXIMIZE_COMPATIBILITY =
            WifiTetherMaximizeCompatibilityPreferenceController.PREF_KEY;

    private WifiTetherSwitchBarController mSwitchBarController;
    private WifiTetherSSIDPreferenceController mSSIDPreferenceController;
    private WifiTetherPasswordPreferenceController mPasswordPreferenceController;
    private WifiTetherSecurityPreferenceController mSecurityPreferenceController;
    private WifiTetherMaximizeCompatibilityPreferenceController mMaxCompatibilityPrefController;

    private WifiManager mWifiManager;
    private boolean mRestartWifiApAfterConfigChange;
    private boolean mUnavailable;
    private WifiRestriction mWifiRestriction;
    @VisibleForTesting
    TetherChangeReceiver mTetherChangeReceiver;

    static {
        TETHER_STATE_CHANGE_FILTER = new IntentFilter(WIFI_AP_STATE_CHANGED_ACTION);
    }

    public WifiTetherSettings() {
        super(UserManager.DISALLOW_CONFIG_TETHERING);
        mWifiRestriction = new WifiRestriction();
    }

    public WifiTetherSettings(WifiRestriction wifiRestriction) {
        super(UserManager.DISALLOW_CONFIG_TETHERING);
        mWifiRestriction = wifiRestriction;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_TETHER_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return "WifiTetherSettings";
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (!canShowWifiHotspot(getContext())) {
            Log.e(TAG, "can not launch Wi-Fi hotspot settings"
                    + " because the config is not set to show.");
            finish();
            return;
        }

        setIfOnlyAvailableForAdmins(true);
        mUnavailable = isUiRestricted() || !mWifiRestriction.isHotspotAvailable(getContext());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mTetherChangeReceiver = new TetherChangeReceiver();

        mSSIDPreferenceController = use(WifiTetherSSIDPreferenceController.class);
        mSecurityPreferenceController = use(WifiTetherSecurityPreferenceController.class);
        mPasswordPreferenceController = use(WifiTetherPasswordPreferenceController.class);
        mMaxCompatibilityPrefController =
                use(WifiTetherMaximizeCompatibilityPreferenceController.class);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mUnavailable) {
            return;
        }
        // Assume we are in a SettingsActivity. This is only safe because we currently use
        // SettingsActivity as base for all preference fragments.
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SettingsMainSwitchBar switchBar = activity.getSwitchBar();
        switchBar.setTitle(getContext().getString(R.string.use_wifi_hotsopt_main_switch_title));
        mSwitchBarController = new WifiTetherSwitchBarController(activity, switchBar);
        getSettingsLifecycle().addObserver(mSwitchBarController);
        switchBar.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mWifiRestriction.isHotspotAvailable(getContext())) {
            getEmptyTextView().setText(R.string.not_allowed_by_ent);
            getPreferenceScreen().removeAll();
            return;
        }
        if (mUnavailable) {
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.tethering_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }
        final Context context = getContext();
        if (context != null) {
            context.registerReceiver(mTetherChangeReceiver, TETHER_STATE_CHANGE_FILTER,
                    Context.RECEIVER_EXPORTED_UNAUDITED);
            // The intent WIFI_AP_STATE_CHANGED_ACTION is not sticky intent anymore after SC-V2
            // Handle the initial state after register the receiver.
            updateDisplayWithNewConfig();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mUnavailable) {
            return;
        }
        final Context context = getContext();
        if (context != null) {
            context.unregisterReceiver(mTetherChangeReceiver);
        }
    }


    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_tether_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this::onTetherConfigUpdated);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            WifiTetherBasePreferenceController.OnTetherConfigUpdateListener listener) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new WifiTetherSSIDPreferenceController(context, listener));
        controllers.add(new WifiTetherSecurityPreferenceController(context, listener));
        controllers.add(new WifiTetherPasswordPreferenceController(context, listener));
        controllers.add(
                new WifiTetherAutoOffPreferenceController(context, KEY_WIFI_TETHER_AUTO_OFF));
        controllers.add(new WifiTetherMaximizeCompatibilityPreferenceController(context, listener));
        return controllers;
    }

    @Override
    public void onTetherConfigUpdated(AbstractPreferenceController context) {
        final SoftApConfiguration config = buildNewConfig();
        mPasswordPreferenceController.setSecurityType(config.getSecurityType());

        /**
         * if soft AP is stopped, bring up
         * else restart with new config
         * TODO: update config on a running access point when framework support is added
         */
        if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
            Log.d("TetheringSettings",
                    "Wifi AP config changed while enabled, stop and restart");
            mRestartWifiApAfterConfigChange = true;
            mSwitchBarController.stopTether();
        }
        mWifiManager.setSoftApConfiguration(config);
    }

    private SoftApConfiguration buildNewConfig() {
        final SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder();
        final int securityType = mSecurityPreferenceController.getSecurityType();
        configBuilder.setSsid(mSSIDPreferenceController.getSSID());
        if (securityType != SoftApConfiguration.SECURITY_TYPE_OPEN) {
            configBuilder.setPassphrase(
                    mPasswordPreferenceController.getPasswordValidated(securityType),
                    securityType);
        }
        mMaxCompatibilityPrefController.setupMaximizeCompatibility(configBuilder);
        return configBuilder.build();
    }

    private void startTether() {
        mRestartWifiApAfterConfigChange = false;
        mSwitchBarController.startTether();
    }

    private void updateDisplayWithNewConfig() {
        use(WifiTetherSSIDPreferenceController.class).updateDisplay();
        use(WifiTetherSecurityPreferenceController.class).updateDisplay();
        use(WifiTetherPasswordPreferenceController.class).updateDisplay();
        use(WifiTetherMaximizeCompatibilityPreferenceController.class).updateDisplay();
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new SearchIndexProvider(R.xml.wifi_tether_settings);

    @VisibleForTesting
    static class SearchIndexProvider extends BaseSearchIndexProvider {

        private final WifiRestriction mWifiRestriction;

        SearchIndexProvider(int xmlRes) {
            super(xmlRes);
            mWifiRestriction = new WifiRestriction();
        }

        @VisibleForTesting
        SearchIndexProvider(int xmlRes, WifiRestriction wifiRestriction) {
            super(xmlRes);
            mWifiRestriction = wifiRestriction;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = super.getNonIndexableKeys(context);

            if (!mWifiRestriction.isTetherAvailable(context)
                    || !mWifiRestriction.isHotspotAvailable(context)) {
                keys.add(KEY_WIFI_TETHER_NETWORK_NAME);
                keys.add(KEY_WIFI_TETHER_SECURITY);
                keys.add(KEY_WIFI_TETHER_NETWORK_PASSWORD);
                keys.add(KEY_WIFI_TETHER_AUTO_OFF);
                keys.add(KEY_WIFI_TETHER_MAXIMIZE_COMPATIBILITY);
            }

            // Remove duplicate
            keys.add(KEY_WIFI_TETHER_SCREEN);
            return keys;
        }

        @Override
        protected boolean isPageSearchEnabled(Context context) {
            if (context == null || !WifiUtils.canShowWifiHotspot(context)) return false;
            return !FeatureFlagUtils.isEnabled(context, FeatureFlags.TETHER_ALL_IN_ONE);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return buildPreferenceControllers(context, null /* listener */);
        }
    }

    @VisibleForTesting
    static class WifiRestriction {
        public boolean isTetherAvailable(@Nullable Context context) {
            if (context == null) return true;
            return TetherUtil.isTetherAvailable(context);
        }

        public boolean isHotspotAvailable(@Nullable Context context) {
            if (context == null) return true;
            return WifiEnterpriseRestrictionUtils.isWifiTetheringAllowed(context);
        }
    }

    @VisibleForTesting
    class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "updating display config due to receiving broadcast action " + action);
            updateDisplayWithNewConfig();
            if (action.equals(WIFI_AP_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, 0);
                if (state == WifiManager.WIFI_AP_STATE_DISABLED
                        && mRestartWifiApAfterConfigChange) {
                    startTether();
                }
            }
        }
    }
}
