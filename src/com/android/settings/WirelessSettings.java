/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.network.AirplaneModePreferenceController;
import com.android.settings.network.MobileNetworkPreferenceController;
import com.android.settings.network.MobilePlanPreferenceController;
import com.android.settings.network.NetworkResetPreferenceController;
import com.android.settings.network.ProxyPreferenceController;
import com.android.settings.network.TetherPreferenceController;
import com.android.settings.network.VpnPreferenceController;
import com.android.settings.network.WifiCallingPreferenceController;
import com.android.settings.nfc.NfcPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.android.settings.network.MobilePlanPreferenceController
        .MANAGE_MOBILE_PLAN_DIALOG_ID;

public class WirelessSettings extends SettingsPreferenceFragment implements Indexable,
        MobilePlanPreferenceController.MobilePlanPreferenceHost {
    private static final String TAG = "WirelessSettings";

    private static final String KEY_WIMAX_SETTINGS = "wimax_settings";

    private UserManager mUm;

    private AirplaneModePreferenceController mAirplaneModePreferenceController;
    private TetherPreferenceController mTetherPreferenceController;
    private MobileNetworkPreferenceController mMobileNetworkPreferenceController;
    private VpnPreferenceController mVpnPreferenceController;
    private NetworkResetPreferenceController mNetworkResetPreferenceController;
    private WifiCallingPreferenceController mWifiCallingPreferenceController;
    private ProxyPreferenceController mProxyPreferenceController;
    private MobilePlanPreferenceController mMobilePlanPreferenceController;
    private NfcPreferenceController mNfcPreferenceController;

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceFragment's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        log("onPreferenceTreeClick: preference=" + preference);
        if (mAirplaneModePreferenceController.handlePreferenceTreeClick(preference)) {
            return true;
        }
        if (mMobilePlanPreferenceController.handlePreferenceTreeClick(preference)) {
            return true;
        }
        // Let the intents be launched by the Preference manager
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        log("onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case MANAGE_MOBILE_PLAN_DIALOG_ID:
                return new AlertDialog.Builder(getActivity())
                        .setMessage(mMobilePlanPreferenceController.getMobilePlanDialogMessage())
                        .setCancelable(false)
                        .setPositiveButton(com.android.internal.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        log("MANAGE_MOBILE_PLAN_DIALOG.onClickListener id=" + id);
                                        mMobilePlanPreferenceController
                                                .setMobilePlanDialogMessage(null);
                                    }
                                })
                        .create();
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (MANAGE_MOBILE_PLAN_DIALOG_ID == dialogId) {
            return MetricsEvent.DIALOG_MANAGE_MOBILE_PLAN;
        }
        return 0;
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.WIRELESS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUm = (UserManager) getSystemService(Context.USER_SERVICE);

        addPreferencesFromResource(R.xml.wireless_settings);

        final boolean isAdmin = mUm.isAdminUser();

        final Activity activity = getActivity();

        final PreferenceScreen screen = getPreferenceScreen();
        mAirplaneModePreferenceController = new AirplaneModePreferenceController(activity, this);
        mTetherPreferenceController = new TetherPreferenceController(activity);
        mMobileNetworkPreferenceController = new MobileNetworkPreferenceController(activity);
        mVpnPreferenceController = new VpnPreferenceController(activity);
        mWifiCallingPreferenceController = new WifiCallingPreferenceController(activity);
        mNetworkResetPreferenceController = new NetworkResetPreferenceController(activity);
        mProxyPreferenceController = new ProxyPreferenceController(activity);
        mMobilePlanPreferenceController = new MobilePlanPreferenceController(activity, this);
        mNfcPreferenceController = new NfcPreferenceController(activity);

        mMobilePlanPreferenceController.onCreate(savedInstanceState);

        mAirplaneModePreferenceController.displayPreference(screen);
        mTetherPreferenceController.displayPreference(screen);
        mMobileNetworkPreferenceController.displayPreference(screen);
        mVpnPreferenceController.displayPreference(screen);
        mWifiCallingPreferenceController.displayPreference(screen);
        mNetworkResetPreferenceController.displayPreference(screen);
        mProxyPreferenceController.displayPreference(screen);
        mMobilePlanPreferenceController.displayPreference(screen);
        mNfcPreferenceController.displayPreference(screen);

        String toggleable = Settings.Global.getString(activity.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);

        //enable/disable wimax depending on the value in config.xml
        final boolean isWimaxEnabled = isAdmin && this.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if (!isWimaxEnabled || RestrictedLockUtils.hasBaseUserRestriction(activity,
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, UserHandle.myUserId())) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(KEY_WIMAX_SETTINGS);
            if (ps != null) root.removePreference(ps);
        } else {
            if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_WIMAX)
                    && isWimaxEnabled) {
                Preference ps = findPreference(KEY_WIMAX_SETTINGS);
                ps.setDependency(AirplaneModePreferenceController.KEY_TOGGLE_AIRPLANE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mAirplaneModePreferenceController.onResume();
        mNfcPreferenceController.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMobilePlanPreferenceController.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAirplaneModePreferenceController.onPause();
        mNfcPreferenceController.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mAirplaneModePreferenceController.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void showMobilePlanMessageDialog() {
        showDialog(MANAGE_MOBILE_PLAN_DIALOG_ID);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_more_networks;
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    // Remove wireless settings from search in demo mode
                    if (UserManager.isDeviceInDemoMode(context)) {
                        return Collections.emptyList();
                    }
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.wireless_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final ArrayList<String> result = new ArrayList<String>();

                    final UserManager um = (UserManager) context.getSystemService(
                            Context.USER_SERVICE);
                    final boolean isSecondaryUser = !um.isAdminUser();
                    final boolean isWimaxEnabled = !isSecondaryUser
                            && context.getResources().getBoolean(
                            com.android.internal.R.bool.config_wimaxEnabled);
                    if (!isWimaxEnabled) {
                        result.add(KEY_WIMAX_SETTINGS);
                    }

                    new VpnPreferenceController(context).updateNonIndexableKeys(result);

                    new NfcPreferenceController(context).updateNonIndexableKeys(result);
                    new MobilePlanPreferenceController(context, null /* MobilePlanClickHandler */)
                            .updateNonIndexableKeys(result);
                    new MobileNetworkPreferenceController(context).updateNonIndexableKeys(result);

                    // Remove Airplane Mode settings if it's a stationary device such as a TV.
                    new AirplaneModePreferenceController(context, null /* fragment */)
                            .updateNonIndexableKeys(result);

                    new ProxyPreferenceController(context).updateNonIndexableKeys(result);

                    new TetherPreferenceController(context).updateNonIndexableKeys(result);
                    new WifiCallingPreferenceController(context).updateNonIndexableKeys(result);
                    new NetworkResetPreferenceController(context).updateNonIndexableKeys(result);

                    return result;
                }
            };
}
