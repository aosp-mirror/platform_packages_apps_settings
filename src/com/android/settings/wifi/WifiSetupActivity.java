/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.android.settings.ButtonBarHandler;
import com.android.settings.R;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;
import com.android.setupwizard.navigationbar.SetupWizardNavBar.NavigationBarListener;

public class WifiSetupActivity extends WifiPickerActivity
        implements ButtonBarHandler, NavigationBarListener {
    private static final String TAG = "WifiSetupActivity";

    private static final String EXTRA_ALLOW_SKIP = "allowSkip";
    private static final String EXTRA_USE_IMMERSIVE_MODE = "useImmersiveMode";

    // this boolean extra specifies whether to auto finish when connection is established
    private static final String EXTRA_AUTO_FINISH_ON_CONNECT = "wifi_auto_finish_on_connect";

    // Whether auto finish is suspended until user connects to an access point
    private static final String EXTRA_REQUIRE_USER_NETWORK_SELECTION =
            "wifi_require_user_network_selection";

    // Extra containing the resource name of the theme to be used
    private static final String EXTRA_THEME = "theme";
    private static final String THEME_HOLO = "holo";
    private static final String THEME_HOLO_LIGHT = "holo_light";
    private static final String THEME_MATERIAL = "material";
    private static final String THEME_MATERIAL_LIGHT = "material_light";

    // Key for whether the user selected network in saved instance state bundle
    private static final String PARAM_USER_SELECTED_NETWORK = "userSelectedNetwork";

    // Activity result when pressing the Skip button
    private static final int RESULT_SKIP = Activity.RESULT_FIRST_USER;

    // From WizardManager (must match constants maintained there)
    private static final String ACTION_NEXT = "com.android.wizard.NEXT";
    private static final String EXTRA_SCRIPT_URI = "scriptUri";
    private static final String EXTRA_ACTION_ID = "actionId";
    private static final String EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode";
    private static final int NEXT_REQUEST = 10000;

    // Whether we allow skipping without a valid network connection
    private boolean mAllowSkip = true;
    // Whether to auto finish when the user selected a network and successfully connected
    private boolean mAutoFinishOnConnection;
    // Whether the user connected to a network. This excludes the auto-connecting by the system.
    private boolean mUserSelectedNetwork;
    // Whether the device is connected to WiFi
    private boolean mWifiConnected;

    private SetupWizardNavBar mNavigationBar;

    private final IntentFilter mFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Refresh the connection state with the latest connection info. Use the connection info
            // from ConnectivityManager instead of the one attached in the intent to make sure
            // we have the most up-to-date connection state. b/17511772
            refreshConnectionState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        mAutoFinishOnConnection = intent.getBooleanExtra(EXTRA_AUTO_FINISH_ON_CONNECT, false);
        mAllowSkip = intent.getBooleanExtra(EXTRA_ALLOW_SKIP, true);
        // Behave like the user already selected a network if we do not require selection
        mUserSelectedNetwork = !intent.getBooleanExtra(EXTRA_REQUIRE_USER_NETWORK_SELECTION, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PARAM_USER_SELECTED_NETWORK, mUserSelectedNetwork);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mUserSelectedNetwork = savedInstanceState.getBoolean(PARAM_USER_SELECTED_NETWORK, true);
    }

    private void refreshConnectionState() {
        final ConnectivityManager connectivity = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = connectivity != null &&
                connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        refreshConnectionState(connected);
    }

    private void refreshConnectionState(boolean connected) {
        mWifiConnected = connected;
        if (connected) {
            if (mAutoFinishOnConnection && mUserSelectedNetwork) {
                Log.d(TAG, "Auto-finishing with connection");
                finishOrNext(Activity.RESULT_OK);
                // Require a user selection before auto-finishing next time we are here. The user
                // can either connect to a different network or press "next" to proceed.
                mUserSelectedNetwork = false;
            }
            if (mNavigationBar != null) {
                mNavigationBar.getNextButton().setText(R.string.setup_wizard_next_button_label);
                mNavigationBar.getNextButton().setEnabled(true);
            }
        } else {
            if (mNavigationBar != null) {
                mNavigationBar.getNextButton().setText(R.string.skip_label);
                mNavigationBar.getNextButton().setEnabled(mAllowSkip);
            }
        }
    }

    /* package */ void networkSelected() {
        Log.d(TAG, "Network selected by user");
        mUserSelectedNetwork = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mFilter);
        refreshConnectionState();
    }

    @Override
    public void onPause() {
        unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        String themeName = getIntent().getStringExtra(EXTRA_THEME);
        if (THEME_HOLO_LIGHT.equalsIgnoreCase(themeName) ||
                THEME_MATERIAL_LIGHT.equalsIgnoreCase(themeName)) {
            resid = R.style.SetupWizardWifiTheme_Light;
        } else if (THEME_HOLO.equalsIgnoreCase(themeName) ||
                THEME_MATERIAL.equalsIgnoreCase(themeName)) {
            resid = R.style.SetupWizardWifiTheme;
        }
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return WifiSettingsForSetupWizard.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends PreferenceFragment> getWifiSettingsClass() {
        return WifiSettingsForSetupWizard.class;
    }

    /**
     * Complete this activity and return the results to the caller. If using WizardManager, this
     * will invoke the next scripted action; otherwise, we simply finish.
     */
    public void finishOrNext(int resultCode) {
        Log.d(TAG, "finishOrNext resultCode=" + resultCode
                + " isUsingWizardManager=" + isUsingWizardManager());
        if (isUsingWizardManager()) {
            sendResultsToSetupWizard(resultCode);
        } else {
            setResult(resultCode);
            finish();
        }
    }

    private boolean isUsingWizardManager() {
        return getIntent().hasExtra(EXTRA_SCRIPT_URI);
    }

    /**
     * Send the results of this activity to WizardManager, which will then send out the next
     * scripted activity. WizardManager does not actually return an activity result, but if we
     * invoke WizardManager without requesting a result, the framework will choose not to issue a
     * call to onActivityResult with RESULT_CANCELED when navigating backward.
     */
    private void sendResultsToSetupWizard(int resultCode) {
        final Intent intent = getIntent();
        final Intent nextIntent = new Intent(ACTION_NEXT);
        nextIntent.putExtra(EXTRA_SCRIPT_URI, intent.getStringExtra(EXTRA_SCRIPT_URI));
        nextIntent.putExtra(EXTRA_ACTION_ID, intent.getStringExtra(EXTRA_ACTION_ID));
        nextIntent.putExtra(EXTRA_THEME, intent.getStringExtra(EXTRA_THEME));
        nextIntent.putExtra(EXTRA_RESULT_CODE, resultCode);
        startActivityForResult(nextIntent, NEXT_REQUEST);
    }

    @Override
    public void onNavigationBarCreated(final SetupWizardNavBar bar) {
        mNavigationBar = bar;
        final boolean useImmersiveMode =
                getIntent().getBooleanExtra(EXTRA_USE_IMMERSIVE_MODE, false);
        bar.setUseImmersiveMode(useImmersiveMode);
        if (useImmersiveMode) {
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        if (mWifiConnected) {
            finishOrNext(RESULT_OK);
        } else {
            // Warn of possible data charges if there is a network connection, or lack of updates
            // if there is none.
            final int message = isNetworkConnected() ? R.string.wifi_skipped_message :
                    R.string.wifi_and_mobile_skipped_message;
            WifiSkipDialog.newInstance(message).show(getFragmentManager(), "dialog");
        }
    }

    /**
     * @return True if there is a valid network connection, whether it is via WiFi, mobile data or
     *         other means.
     */
    private boolean isNetworkConnected() {
        final ConnectivityManager connectivity = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        }
        final NetworkInfo info = connectivity.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public static class WifiSkipDialog extends DialogFragment {
        public static WifiSkipDialog newInstance(int messageRes) {
            final Bundle args = new Bundle();
            args.putInt("messageRes", messageRes);
            final WifiSkipDialog dialog = new WifiSkipDialog();
            dialog.setArguments(args);
            return dialog;
        }

        public WifiSkipDialog() {
            // no-arg constructor for fragment
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int messageRes = getArguments().getInt("messageRes");
            return new AlertDialog.Builder(getActivity())
                    .setMessage(messageRes)
                    .setCancelable(false)
                    .setNegativeButton(R.string.wifi_skip_anyway,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            WifiSetupActivity activity = (WifiSetupActivity) getActivity();
                            activity.finishOrNext(RESULT_SKIP);
                        }
                    })
                    .setPositiveButton(R.string.wifi_dont_skip,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    })
                    .create();
        }
    }
}
