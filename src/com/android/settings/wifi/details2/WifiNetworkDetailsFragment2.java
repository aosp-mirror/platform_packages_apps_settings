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
package com.android.settings.wifi.details2;

import static com.android.settings.wifi.WifiSettings.WIFI_DIALOG_ID;

import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.wifi.WifiConfigUiBase2;
import com.android.settings.wifi.WifiDialog2;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.wifitrackerlib.NetworkDetailsTracker;
import com.android.wifitrackerlib.WifiEntry;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Detail page for the currently connected wifi network.
 *
 * <p>The key of {@link WifiEntry} should be saved to the intent Extras when launching this class
 * in order to properly render this page.
 */
public class WifiNetworkDetailsFragment2 extends DashboardFragment implements
        WifiDialog2.WifiDialog2Listener {

    private static final String TAG = "WifiNetworkDetailsFrg2";

    // Key of a Bundle to save/restore the selected WifiEntry
    public static final String KEY_CHOSEN_WIFIENTRY_KEY = "key_chosen_wifientry_key";

    // Max age of tracked WifiEntries
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating SavedNetworkTracker scans
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    private NetworkDetailsTracker mNetworkDetailsTracker;
    private HandlerThread mWorkerThread;
    private WifiDetailPreferenceController2 mWifiDetailPreferenceController2;
    private List<WifiDialog2.WifiDialog2Listener> mWifiDialogListeners = new ArrayList<>();
    private List<AbstractPreferenceController> mControllers;

    @Override
    public void onDestroy() {
        mWorkerThread.quit();

        super.onDestroy();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_NETWORK_DETAILS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_network_details_fragment2;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == WIFI_DIALOG_ID) {
            return SettingsEnums.DIALOG_WIFI_AP_EDIT;
        }
        return 0;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (getActivity() == null || mWifiDetailPreferenceController2 == null) {
            return null;
        }

        final WifiEntry wifiEntry = mNetworkDetailsTracker.getWifiEntry();
        return WifiDialog2.createModal(getActivity(), this, wifiEntry,
                WifiConfigUiBase2.MODE_MODIFY);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.add(0, Menu.FIRST, 0, R.string.wifi_modify);
        item.setIcon(com.android.internal.R.drawable.ic_mode_edit);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case Menu.FIRST:
                if (!mWifiDetailPreferenceController2.canModifyNetwork()) {
                    EnforcedAdmin admin = RestrictedLockUtilsInternal.getDeviceOwner(getContext());
                    if (admin == null) {
                        final DevicePolicyManager dpm = (DevicePolicyManager)
                                getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
                        final UserManager um = (UserManager)
                                getContext().getSystemService(Context.USER_SERVICE);
                        final int profileOwnerUserId = Utils.getManagedProfileId(
                                um, UserHandle.myUserId());
                        if (profileOwnerUserId != UserHandle.USER_NULL) {
                            admin = new EnforcedAdmin(dpm.getProfileOwnerAsUser(profileOwnerUserId),
                                    null, UserHandle.of(profileOwnerUserId));
                        }
                    }
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), admin);
                } else {
                    showDialog(WIFI_DIALOG_ID);
                }
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        final ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        setupNetworksDetailTracker();
        final WifiEntry wifiEntry = mNetworkDetailsTracker.getWifiEntry();

        final WifiSecondSummaryController2 wifiSecondSummaryController2 =
                new WifiSecondSummaryController2(context);
        wifiSecondSummaryController2.setWifiEntry(wifiEntry);
        mControllers.add(wifiSecondSummaryController2);

        mWifiDetailPreferenceController2 = WifiDetailPreferenceController2.newInstance(
                wifiEntry,
                cm,
                context,
                this,
                new Handler(Looper.getMainLooper()),  // UI thread.
                getSettingsLifecycle(),
                context.getSystemService(WifiManager.class),
                mMetricsFeatureProvider);
        mControllers.add(mWifiDetailPreferenceController2);

        final WifiAutoConnectPreferenceController2 wifiAutoConnectPreferenceController2 =
                new WifiAutoConnectPreferenceController2(context);
        wifiAutoConnectPreferenceController2.setWifiEntry(wifiEntry);
        mControllers.add(wifiAutoConnectPreferenceController2);

        final AddDevicePreferenceController2 addDevicePreferenceController2 =
                new AddDevicePreferenceController2(context);
        addDevicePreferenceController2.setWifiEntry(wifiEntry);
        mControllers.add(addDevicePreferenceController2);

        final WifiMeteredPreferenceController2 meteredPreferenceController2 =
                new WifiMeteredPreferenceController2(context, wifiEntry);
        mControllers.add(meteredPreferenceController2);

        final WifiPrivacyPreferenceController2 privacyController2 =
                new WifiPrivacyPreferenceController2(context);
        privacyController2.setWifiEntry(wifiEntry);
        mControllers.add(privacyController2);

        final WifiSubscriptionDetailPreferenceController2
                wifiSubscriptionDetailPreferenceController2 =
                new WifiSubscriptionDetailPreferenceController2(context);
        wifiSubscriptionDetailPreferenceController2.setWifiEntry(wifiEntry);
        mControllers.add(wifiSubscriptionDetailPreferenceController2);

        // Sets callback listener for wifi dialog.
        mWifiDialogListeners.add(mWifiDetailPreferenceController2);
        mWifiDialogListeners.add(privacyController2);
        mWifiDialogListeners.add(meteredPreferenceController2);

        return mControllers;
    }

    @Override
    public void onSubmit(WifiDialog2 dialog) {
        for (WifiDialog2.WifiDialog2Listener listener : mWifiDialogListeners) {
            listener.onSubmit(dialog);
        }
    }

    private void setupNetworksDetailTracker() {
        if (mNetworkDetailsTracker != null) {
            return;
        }

        final Context context = getContext();
        mWorkerThread = new HandlerThread(TAG
                + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        final Clock elapsedRealtimeClock = new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };

        mNetworkDetailsTracker = NetworkDetailsTracker.createNetworkDetailsTracker(
                getSettingsLifecycle(),
                context,
                context.getSystemService(WifiManager.class),
                context.getSystemService(ConnectivityManager.class),
                context.getSystemService(NetworkScoreManager.class),
                new Handler(Looper.getMainLooper()),
                mWorkerThread.getThreadHandler(),
                elapsedRealtimeClock,
                MAX_SCAN_AGE_MILLIS,
                SCAN_INTERVAL_MILLIS,
                getArguments().getString(KEY_CHOSEN_WIFIENTRY_KEY));
    }

    /**
     * API call for refreshing the preferences in this fragment.
     */
    public void refreshPreferences() {
        final PreferenceScreen screen = getPreferenceScreen();
        for (AbstractPreferenceController controller : mControllers) {
            // WifiDetailPreferenceController2 gets the callback WifiEntryCallback#onUpdated,
            // it can control the visibility change by itself.
            // And WifiDetailPreferenceController2#updatePreference renew mEntityHeaderController
            // instance which will cause icon reset.
            if (controller instanceof WifiDetailPreferenceController2) {
                continue;
            }
            controller.displayPreference(screen);
        }
    }
}
