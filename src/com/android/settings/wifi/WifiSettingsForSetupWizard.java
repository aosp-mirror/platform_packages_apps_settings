/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.android.settings.R;

/**
 * This customized version of WifiSettings is shown to the user only during Setup Wizard. Menu
 * selections are limited, clicking on an access point will auto-advance to the next screen (once
 * connected), and, if the user opts to skip ahead without a wifi connection, a warning message
 * alerts of possible carrier data charges or missing software updates.
 */
public class WifiSettingsForSetupWizard extends WifiSettings {

    private static final String TAG = "WifiSettingsForSetupWizard";

    /* Used in Wifi Setup context */

    // this boolean extra specifies whether to disable the Next button when not connected
    private static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";

    // this boolean extra specifies whether to auto finish when connection is established
    private static final String EXTRA_AUTO_FINISH_ON_CONNECT = "wifi_auto_finish_on_connect";

    // this boolean extra shows a custom button that we can control
    protected static final String EXTRA_SHOW_CUSTOM_BUTTON = "wifi_show_custom_button";

    // show a text regarding data charges when wifi connection is required during setup wizard
    protected static final String EXTRA_SHOW_WIFI_REQUIRED_INFO = "wifi_show_wifi_required_info";

    // this boolean extra is set if we are being invoked by the Setup Wizard
    private static final String EXTRA_IS_FIRST_RUN = "firstRun";

    // Activity result when pressing the Skip button
    private static final int RESULT_SKIP = Activity.RESULT_FIRST_USER;

    // From WizardManager (must match constants maintained there)
    private static final String ACTION_NEXT = "com.android.wizard.NEXT";
    private static final String EXTRA_SCRIPT_URI = "scriptUri";
    private static final String EXTRA_ACTION_ID = "actionId";
    private static final String EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode";
    private static final int NEXT_REQUEST = 10000;

    // should Next button only be enabled when we have a connection?
    private boolean mEnableNextOnConnection;

    // should activity finish once we have a connection?
    private boolean mAutoFinishOnConnection;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;

    public WifiSettingsForSetupWizard() {
        super();

        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                        WifiManager.EXTRA_NETWORK_INFO);
                changeNextButtonState(info.isConnected());
                if (mAutoFinishOnConnection && info.isConnected()) {
                    finishOrNext(Activity.RESULT_OK);
                }
            }
        };
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.setup_preference, container, false);
        final View other = view.findViewById(R.id.other_network);
        other.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWifiManager.isWifiEnabled()) {
                    onAddNetworkPressed();
                }
            }
        });
        final ImageButton b = (ImageButton) view.findViewById(R.id.more);
        if (b != null) {
            b.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mWifiManager.isWifiEnabled()) {
                        PopupMenu pm = new PopupMenu(inflater.getContext(), b);
                        pm.inflate(R.menu.wifi_setup);
                        pm.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if (R.id.wifi_wps == item.getItemId()) {
                                    showDialog(WPS_PBC_DIALOG_ID);
                                    return true;
                                }
                                return false;
                            }
                        });
                        pm.show();
                    }
                }
            });
        }

        final Intent intent = getActivity().getIntent();
        if (intent.getBooleanExtra(EXTRA_SHOW_CUSTOM_BUTTON, false)) {
            view.findViewById(R.id.button_bar).setVisibility(View.VISIBLE);
            view.findViewById(R.id.back_button).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.skip_button).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.next_button).setVisibility(View.INVISIBLE);

            Button customButton = (Button) view.findViewById(R.id.custom_button);
            customButton.setVisibility(View.VISIBLE);
            customButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isConnected = false;
                    Activity activity = getActivity();
                    final ConnectivityManager connectivity = (ConnectivityManager)
                            activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connectivity != null) {
                        final NetworkInfo info = connectivity.getActiveNetworkInfo();
                        isConnected = (info != null) && info.isConnected();
                    }
                    if (isConnected) {
                        // Warn of possible data charges
                        showDialog(WIFI_SKIPPED_DIALOG_ID);
                    } else {
                        // Warn of lack of updates
                        showDialog(WIFI_AND_MOBILE_SKIPPED_DIALOG_ID);
                    }
                }
            });
        }

        if (intent.getBooleanExtra(EXTRA_SHOW_WIFI_REQUIRED_INFO, false)) {
            view.findViewById(R.id.wifi_required_info).setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getView().setSystemUiVisibility(
                View.STATUS_BAR_DISABLE_HOME |
                View.STATUS_BAR_DISABLE_RECENT |
                View.STATUS_BAR_DISABLE_NOTIFICATION_ALERTS |
                View.STATUS_BAR_DISABLE_CLOCK);

        final Activity activity = getActivity();
        final Intent intent = activity.getIntent();

        // first if we're supposed to finish once we have a connection
        mAutoFinishOnConnection = intent.getBooleanExtra(EXTRA_AUTO_FINISH_ON_CONNECT, false);

        if (mAutoFinishOnConnection) {
            // Hide the next button
            if (hasNextButton()) {
                getNextButton().setVisibility(View.GONE);
            }

            final ConnectivityManager connectivity = (ConnectivityManager)
                    activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null
                    && connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                finishOrNext(Activity.RESULT_OK);
                return;
            }
        }

        // if we're supposed to enable/disable the Next button based on our current connection
        // state, start it off in the right state
        mEnableNextOnConnection = intent.getBooleanExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, false);

        if (mEnableNextOnConnection) {
            if (hasNextButton()) {
                final ConnectivityManager connectivity = (ConnectivityManager)
                        activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivity != null) {
                    NetworkInfo info = connectivity.getNetworkInfo(
                            ConnectivityManager.TYPE_WIFI);
                    changeNextButtonState(info.isConnected());
                }
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case WIFI_SKIPPED_DIALOG_ID:
                return new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.wifi_skipped_message)
                            .setCancelable(false)
                            .setNegativeButton(R.string.wifi_skip_anyway,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    finishOrNext(RESULT_SKIP);
                                }
                            })
                            .setPositiveButton(R.string.wifi_dont_skip,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            })
                            .create();
            case WIFI_AND_MOBILE_SKIPPED_DIALOG_ID:
                return new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.wifi_and_mobile_skipped_message)
                            .setCancelable(false)
                            .setNegativeButton(R.string.wifi_skip_anyway,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    finishOrNext(RESULT_SKIP);
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
        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            // Before returning to the settings panel, forget any current access point so it will
            // not attempt to automatically reconnect and advance
            // FIXME: when coming back, it would be better to keep the current connection and
            // override the auto-advance feature
            final WifiInfo info = mWifiManager.getConnectionInfo();
            if (null != info) {
                int netId = info.getNetworkId();
                if (netId != WifiConfiguration.INVALID_NETWORK_ID) {
                    mWifiManager.forget(netId, null);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void registerForContextMenu(View view) {
        // Suppressed during setup wizard
    }

    @Override
    /* package */ WifiEnabler createWifiEnabler() {
        // Not shown during setup wizard
        return null;
    }

    @Override
    /* package */ void addOptionsMenuItems(Menu menu) {
        final boolean wifiIsEnabled = mWifiManager.isWifiEnabled();
        final TypedArray ta = getActivity().getTheme()
                .obtainStyledAttributes(new int[] {R.attr.ic_wps});
        menu.add(Menu.NONE, MENU_ID_WPS_PBC, 0, R.string.wifi_menu_wps_pbc)
                .setIcon(ta.getDrawable(0))
                .setEnabled(wifiIsEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENU_ID_ADD_NETWORK, 0, R.string.wifi_add_network)
                .setEnabled(wifiIsEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        ta.recycle();
    }

    @Override
    /* package */ void forget() {
        super.forget();

        // We need to rename/replace "Next" button in wifi setup context.
        changeNextButtonState(false);
    }

    /**
     * Renames/replaces "Next" button when appropriate. "Next" button usually exists in
     * Wifi setup screens, not in usual wifi settings screen.
     *
     * @param enabled true when the device is connected to a wifi network.
     */
    private void changeNextButtonState(boolean enabled) {
        if (mEnableNextOnConnection && hasNextButton()) {
            getNextButton().setEnabled(enabled);
        }
    }

    /**
     * Complete this activity and return the results to the caller. If using WizardManager, this
     * will invoke the next scripted action; otherwise, we simply finish.
     */
    private void finishOrNext(int resultCode) {
        Log.d(TAG, "finishOrNext resultCode=" + resultCode
                + " isUsingWizardManager=" + isUsingWizardManager());
        if (isUsingWizardManager()) {
            sendResultsToSetupWizard(resultCode);
        } else {
            Activity activity = getActivity();
            activity.setResult(resultCode);
            activity.finish();
        }
    }

    private boolean isUsingWizardManager() {
        return getActivity().getIntent().hasExtra(EXTRA_SCRIPT_URI);
    }

    /**
     * Send the results of this activity to WizardManager, which will then send out the next
     * scripted activity. WizardManager does not actually return an activity result, but if we
     * invoke WizardManager without requesting a result, the framework will choose not to issue a
     * call to onActivityResult with RESULT_CANCELED when navigating backward.
     */
    private void sendResultsToSetupWizard(int resultCode) {
        final Intent intent = getActivity().getIntent();
        final Intent nextIntent = new Intent(ACTION_NEXT);
        nextIntent.putExtra(EXTRA_SCRIPT_URI, intent.getStringExtra(EXTRA_SCRIPT_URI));
        nextIntent.putExtra(EXTRA_ACTION_ID, intent.getStringExtra(EXTRA_ACTION_ID));
        nextIntent.putExtra(EXTRA_RESULT_CODE, resultCode);
        startActivityForResult(nextIntent, NEXT_REQUEST);
    }
}
