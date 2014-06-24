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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
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

    // Extra containing the resource name of the theme to be used
    private static final String EXTRA_THEME = "theme";
    private static final String THEME_HOLO = "holo";
    private static final String THEME_HOLO_LIGHT = "holo_light";
    private static final String THEME_MATERIAL = "material";
    private static final String THEME_MATERIAL_LIGHT = "material_light";

    // Activity result when pressing the Skip button
    private static final int RESULT_SKIP = Activity.RESULT_FIRST_USER;

    // From WizardManager (must match constants maintained there)
    private static final String ACTION_NEXT = "com.android.wizard.NEXT";
    private static final String EXTRA_SCRIPT_URI = "scriptUri";
    private static final String EXTRA_ACTION_ID = "actionId";
    private static final String EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode";
    private static final int NEXT_REQUEST = 10000;

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            // Before returning to the settings panel, forget any current access point so it will
            // not attempt to automatically reconnect and advance
            // FIXME: when coming back, it would be better to keep the current connection and
            // override the auto-advance feature
            final WifiManager wifiManager = (WifiManager)(getSystemService(Context.WIFI_SERVICE));
            if (wifiManager != null) {
                final WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null) {
                    int netId = info.getNetworkId();
                    if (netId != WifiConfiguration.INVALID_NETWORK_ID) {
                        wifiManager.forget(netId, null);
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
        nextIntent.putExtra(EXTRA_RESULT_CODE, resultCode);
        startActivityForResult(nextIntent, NEXT_REQUEST);
    }

    @Override
    public void onNavigationBarCreated(final SetupWizardNavBar bar) {
        final boolean useImmersiveMode =
                getIntent().getBooleanExtra(EXTRA_USE_IMMERSIVE_MODE, false);
        bar.setUseImmersiveMode(useImmersiveMode);
        if (useImmersiveMode) {
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        bar.getNextButton().setText(R.string.skip_label);

        if (!getIntent().getBooleanExtra(EXTRA_ALLOW_SKIP, true)) {
            bar.getNextButton().setEnabled(false);
        }
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        boolean isConnected = false;
        final ConnectivityManager connectivity = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            final NetworkInfo info = connectivity.getActiveNetworkInfo();
            isConnected = (info != null) && info.isConnected();
        }
        if (isConnected) {
            // Warn of possible data charges
            WifiSkipDialog.newInstance(R.string.wifi_skipped_message)
                    .show(getFragmentManager(), "dialog");
        } else {
            // Warn of lack of updates
            WifiSkipDialog.newInstance(R.string.wifi_and_mobile_skipped_message)
                    .show(getFragmentManager(), "dialog");
        }
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
