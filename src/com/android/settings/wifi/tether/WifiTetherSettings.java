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
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import static com.android.settings.wifi.WifiUtils.canShowWifiHotspot;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SoftApConfiguration;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.overlay.FeatureFactory;
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
    @VisibleForTesting
    static final String KEY_WIFI_HOTSPOT_SECURITY = "wifi_hotspot_security";
    @VisibleForTesting
    static final String KEY_WIFI_HOTSPOT_SPEED = "wifi_hotspot_speed";
    @VisibleForTesting
    static final String KEY_INSTANT_HOTSPOT = "wifi_hotspot_instant";

    @VisibleForTesting
    SettingsMainSwitchBar mMainSwitchBar;
    private WifiTetherSwitchBarController mSwitchBarController;
    @VisibleForTesting
    WifiTetherSSIDPreferenceController mSSIDPreferenceController;
    @VisibleForTesting
    WifiTetherPasswordPreferenceController mPasswordPreferenceController;
    @VisibleForTesting
    WifiTetherSecurityPreferenceController mSecurityPreferenceController;
    @VisibleForTesting
    WifiTetherMaximizeCompatibilityPreferenceController mMaxCompatibilityPrefController;
    @VisibleForTesting
    WifiTetherAutoOffPreferenceController mWifiTetherAutoOffPreferenceController;

    @VisibleForTesting
    boolean mUnavailable;
    private WifiRestriction mWifiRestriction;
    @VisibleForTesting
    TetherChangeReceiver mTetherChangeReceiver;

    @VisibleForTesting
    WifiTetherViewModel mWifiTetherViewModel;
    @VisibleForTesting
    Preference mWifiHotspotSecurity;
    @VisibleForTesting
    Preference mWifiHotspotSpeed;
    @VisibleForTesting
    Preference mInstantHotspot;

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
        if (mUnavailable) {
            return;
        }

        mWifiTetherViewModel = FeatureFactory.getFeatureFactory().getWifiFeatureProvider()
                .getWifiTetherViewModel(this);
        if (mWifiTetherViewModel != null) {
            setupSpeedFeature(mWifiTetherViewModel.isSpeedFeatureAvailable());
            setupInstantHotspot(mWifiTetherViewModel.isInstantHotspotFeatureAvailable());
            mWifiTetherViewModel.getRestarting().observe(this, this::onRestartingChanged);
        }
    }

    @VisibleForTesting
    void setupSpeedFeature(boolean isSpeedFeatureAvailable) {
        mWifiHotspotSecurity = findPreference(KEY_WIFI_HOTSPOT_SECURITY);
        mWifiHotspotSpeed = findPreference(KEY_WIFI_HOTSPOT_SPEED);
        if (mWifiHotspotSecurity == null || mWifiHotspotSpeed == null) {
            return;
        }
        mWifiHotspotSecurity.setVisible(isSpeedFeatureAvailable);
        mWifiHotspotSpeed.setVisible(isSpeedFeatureAvailable);
        if (isSpeedFeatureAvailable) {
            mWifiTetherViewModel.getSecuritySummary().observe(this, this::onSecuritySummaryChanged);
            mWifiTetherViewModel.getSpeedSummary().observe(this, this::onSpeedSummaryChanged);
        }
    }

    @VisibleForTesting
    void setupInstantHotspot(boolean isFeatureAvailable) {
        if (!isFeatureAvailable) {
            return;
        }
        mInstantHotspot = findPreference(KEY_INSTANT_HOTSPOT);
        if (mInstantHotspot == null) {
            Log.e(TAG, "Failed to find Instant Hotspot preference:" + KEY_INSTANT_HOTSPOT);
            return;
        }
        mWifiTetherViewModel.getInstantHotspotSummary()
                .observe(this, this::onInstantHotspotChanged);
        mInstantHotspot.setOnPreferenceClickListener(p -> {
            mWifiTetherViewModel.launchInstantHotspotSettings();
            return true;
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mTetherChangeReceiver = new TetherChangeReceiver();

        mSSIDPreferenceController = use(WifiTetherSSIDPreferenceController.class);
        mSecurityPreferenceController = use(WifiTetherSecurityPreferenceController.class);
        mPasswordPreferenceController = use(WifiTetherPasswordPreferenceController.class);
        mMaxCompatibilityPrefController =
                use(WifiTetherMaximizeCompatibilityPreferenceController.class);
        mWifiTetherAutoOffPreferenceController = use(WifiTetherAutoOffPreferenceController.class);
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
        mMainSwitchBar = activity.getSwitchBar();
        mMainSwitchBar.setTitle(getString(R.string.use_wifi_hotsopt_main_switch_title));
        mSwitchBarController = new WifiTetherSwitchBarController(activity, mMainSwitchBar);
        getSettingsLifecycle().addObserver(mSwitchBarController);
        mMainSwitchBar.show();
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
                getEmptyTextView()
                        .setText(com.android.settingslib.R.string.tethering_settings_not_available);
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
        mWifiTetherViewModel.refresh();
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

    protected void onSecuritySummaryChanged(Integer securityResId) {
        mWifiHotspotSecurity.setSummary(securityResId);
    }

    protected void onSpeedSummaryChanged(Integer summaryResId) {
        mWifiHotspotSpeed.setSummary(summaryResId);
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

        mWifiTetherViewModel.setSoftApConfiguration(config);
    }

    @VisibleForTesting
    void onRestartingChanged(Boolean restarting) {
        mMainSwitchBar.setVisibility((restarting) ? INVISIBLE : VISIBLE);
        setLoading(restarting, false);
    }

    @VisibleForTesting
    void onInstantHotspotChanged(String summary) {
        if (summary == null) {
            mInstantHotspot.setVisible(false);
            return;
        }
        mInstantHotspot.setVisible(true);
        mInstantHotspot.setSummary(summary);
    }

    @VisibleForTesting
    SoftApConfiguration buildNewConfig() {
        SoftApConfiguration currentConfig = mWifiTetherViewModel.getSoftApConfiguration();
        SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder(currentConfig);
        configBuilder.setSsid(mSSIDPreferenceController.getSSID());
        int securityType =
                mWifiTetherViewModel.isSpeedFeatureAvailable()
                        ? currentConfig.getSecurityType()
                        : mSecurityPreferenceController.getSecurityType();
        String passphrase =
                securityType == SoftApConfiguration.SECURITY_TYPE_OPEN
                        ? null
                        : mPasswordPreferenceController.getPasswordValidated(securityType);
        configBuilder.setPassphrase(passphrase, securityType);
        if (!mWifiTetherViewModel.isSpeedFeatureAvailable()) {
            mMaxCompatibilityPrefController.setupMaximizeCompatibility(configBuilder);
        }
        configBuilder.setAutoShutdownEnabled(
                mWifiTetherAutoOffPreferenceController.isEnabled());
        return configBuilder.build();
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
            if (context == null) {
                return false;
            }
            UserManager userManager = context.getSystemService(UserManager.class);
            if (userManager == null || !userManager.isAdminUser()) {
                return false;
            }
            return WifiUtils.canShowWifiHotspot(context);
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
        }
    }
}
