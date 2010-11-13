/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.settings.R;

import android.app.Activity;
import android.content.Context;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Collection;
import java.util.EnumMap;

/**
 * WifiSetings Activity specific for SetupWizard with X-Large screen size.
 */
public class WifiSettingsForSetupWizardXL extends Activity implements OnClickListener {
    private static final String TAG = "SetupWizard";

    // We limit the number of showable access points so that the ListView won't become larger
    // than the screen.
    private static int MAX_MENU_COUNT_IN_XL = 8;

    private static final EnumMap<DetailedState, DetailedState> stateMap =
            new EnumMap<DetailedState, DetailedState>(DetailedState.class);

    static {
        stateMap.put(DetailedState.IDLE, DetailedState.DISCONNECTED);
        stateMap.put(DetailedState.SCANNING, DetailedState.SCANNING);
        stateMap.put(DetailedState.CONNECTING, DetailedState.CONNECTING);
        stateMap.put(DetailedState.AUTHENTICATING, DetailedState.CONNECTING);
        stateMap.put(DetailedState.OBTAINING_IPADDR, DetailedState.CONNECTING);
        stateMap.put(DetailedState.CONNECTED, DetailedState.CONNECTED);
        stateMap.put(DetailedState.SUSPENDED, DetailedState.SUSPENDED);  // ?
        stateMap.put(DetailedState.DISCONNECTING, DetailedState.DISCONNECTED);
        stateMap.put(DetailedState.DISCONNECTED, DetailedState.DISCONNECTED);
        stateMap.put(DetailedState.FAILED, DetailedState.FAILED);
    }

    private WifiManager mWifiManager;

    private TextView mProgressText;
    private ProgressBar mProgressBar;
    private WifiSettings mWifiSettings;
    private TextView mStatusText;

    private Button mAddNetworkButton;
    private Button mRefreshButton;
    private Button mSkipOrNextButton;
    private Button mConnectButton;
    private Button mForgetButton;
    private Button mBackButton;

    // Not used now.
    private Button mDetailButton;

    // true when a user already pressed "Connect" button and waiting for connection.
    // Also true when the device is already connected to a wifi network on launch.
    private boolean mAfterTryConnect;

    private WifiConfigUiForSetupWizardXL mWifiConfig;

    private InputMethodManager mInputMethodManager;

    // This count reduces every time when there's a notification about WiFi status change.
    // During the term this is >0, The system refrains some actions which are not appropriate
    // at that timing.
    // - When network is connected at that timing, this screen doesn't call finish().
    //   This count is set to 0 when being detected (not decremente).
    // - When network status is "disconnected", we just show the message "connecting"
    //   regardless of the actual WiFi status.
    //   (After this count's becoming 0, the status message correctly reflects what WiFi Picker
    //    told it)
    // This is a tweak for letting users not confused with WiFi state during a first first steps.
    private int mIgnoringWifiNotificationCount = 5;

    private boolean mShowingConnectingMessageManually = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.wifi_settings_for_setup_wizard_xl);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        // There's no button here enabling wifi network, so we need to enable it without
        // users' request.
        mWifiManager.setWifiEnabled(true);

        mWifiSettings =
                (WifiSettings)getFragmentManager().findFragmentById(R.id.wifi_setup_fragment);
        mInputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        setup();
        // XXX: should we use method?
        getIntent().putExtra(WifiSettings.IN_XL_SETUP_WIZARD, true);
    }

    public void setup() {
        mProgressText = (TextView)findViewById(R.id.scanning_progress_text);
        mProgressBar = (ProgressBar)findViewById(R.id.scanning_progress_bar);
        mProgressBar.setMax(2);
        mStatusText = (TextView)findViewById(R.id.wifi_setup_status);

        mProgressText.setText(Summary.get(this, DetailedState.SCANNING));
        mProgressBar.setIndeterminate(true);
        mStatusText.setText(R.string.wifi_setup_status_scanning);

        mAddNetworkButton = (Button)findViewById(R.id.wifi_setup_add_network);
        mAddNetworkButton.setOnClickListener(this);
        mRefreshButton = (Button)findViewById(R.id.wifi_setup_refresh_list);
        mRefreshButton.setOnClickListener(this);
        mSkipOrNextButton = (Button)findViewById(R.id.wifi_setup_skip_or_next);
        mSkipOrNextButton.setOnClickListener(this);
        mConnectButton = (Button)findViewById(R.id.wifi_setup_connect);
        mConnectButton.setOnClickListener(this);
        mForgetButton = (Button)findViewById(R.id.wifi_setup_forget);
        mForgetButton.setOnClickListener(this);
        mBackButton = (Button)findViewById(R.id.wifi_setup_cancel);
        mBackButton.setOnClickListener(this);
        mDetailButton = (Button)findViewById(R.id.wifi_setup_detail);
        mDetailButton.setOnClickListener(this);
    }

    private void restoreFirstButtonVisibilityState() {
        mAddNetworkButton.setVisibility(View.VISIBLE);
        mRefreshButton.setVisibility(View.VISIBLE);
        mSkipOrNextButton.setVisibility(View.VISIBLE);
        mConnectButton.setVisibility(View.GONE);
        mForgetButton.setVisibility(View.GONE);
        mBackButton.setVisibility(View.GONE);
        mDetailButton.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        hideSoftwareKeyboard();
        if (view == mAddNetworkButton) {
            onAddNetworkButtonPressed();
        } else if (view == mRefreshButton) {
            refreshAccessPoints(true);
        } else if (view == mSkipOrNextButton) {
            if (TextUtils.equals(getString(R.string.wifi_setup_skip), ((Button)view).getText())) {
                // We don't want to let Wifi enabled when a user press skip without choosing
                // any access point.
                mWifiManager.setWifiEnabled(false);
            }
            setResult(Activity.RESULT_OK);
            finish();            
        } else if (view == mConnectButton) {
            onConnectButtonPressed();
        } else if (view == mForgetButton) {
            onForgetButtonPressed();
        } else if (view == mBackButton) {
            onBackButtonPressed();
        } else if (view == mDetailButton) {
            mWifiSettings.showDialogForSelectedPreference();
        }
    }

    private void hideSoftwareKeyboard() {
        Log.i(TAG, "Hiding software keyboard.");
        final View focusedView = getCurrentFocus();
        if (focusedView != null) {
            mInputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
            focusedView.clearFocus();
        }
    }

    // Called from WifiSettings
    /* package */ void updateConnectionState(DetailedState originalState) {
        final DetailedState state = stateMap.get(originalState);

        if (originalState == DetailedState.FAILED) {
            // We clean up the current connectivity status and let users select another network
            // if they want.
            refreshAccessPoints(true);
        }

        switch (state) {
        case SCANNING: {
            // Let users know the device is working correctly though currently there's
            // no visible network on the list.
            if (mWifiSettings.getAccessPointsCount() == 0) {
                mProgressBar.setIndeterminate(true);
                mProgressText.setText(Summary.get(this, DetailedState.SCANNING));
            } else {
                // Users already connected to a network, or see available networks.
                mProgressBar.setIndeterminate(false);
            }
            break;
        }
        case CONNECTING: {
            mShowingConnectingMessageManually = false;
            showConnectingStatus();
            break;
        }
        case CONNECTED: {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setProgress(2);
            mProgressText.setText(Summary.get(this, state));
            mStatusText.setText(R.string.wifi_setup_status_proceed_to_next);

            mAddNetworkButton.setVisibility(View.GONE);
            mRefreshButton.setVisibility(View.GONE);
            mBackButton.setVisibility(View.VISIBLE);
            mSkipOrNextButton.setVisibility(View.VISIBLE);
            mSkipOrNextButton.setEnabled(true);

            if (mIgnoringWifiNotificationCount > 0) {
                // The network is already available before doing anything. We avoid skip this
                // screen to avoid unnecessary trouble by doing so.
                mIgnoringWifiNotificationCount = 0;
                mAfterTryConnect = true;
            } else {
                mProgressText.setText(Summary.get(this, state));
                // setResult(Activity.RESULT_OK);
                // finish();
            }
            break;
        }
        case FAILED: {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setProgress(0);
            mStatusText.setText(R.string.wifi_setup_status_select_network);
            mProgressText.setText(Summary.get(this, state));
            
            restoreFirstButtonVisibilityState();
            mAddNetworkButton.setEnabled(true);
            mRefreshButton.setEnabled(true);
            mSkipOrNextButton.setEnabled(true);
            break;
        }
        default:  // Not connected.
            if (mWifiSettings.getAccessPointsCount() == 0 && mIgnoringWifiNotificationCount > 0) {
                Log.d(TAG, "Currently not connected, but we show \"Scanning\" for a moment");
                mIgnoringWifiNotificationCount--;
                mProgressBar.setIndeterminate(true);
                mProgressText.setText(Summary.get(this, DetailedState.SCANNING));
            } else if (mShowingConnectingMessageManually && mIgnoringWifiNotificationCount > 0) {
                Log.i(TAG, "Currently not connected, but we show \"connecting\" for a moment.");
                mIgnoringWifiNotificationCount--;
                showConnectingStatus();
            } else {
                if (mAfterTryConnect) {
                    // TODO: how to stop connecting the network?
                    Log.i(TAG, String.format(
                            "State %s has been notified after trying to connect a network. ",
                            state.toString()));
                }


                mShowingConnectingMessageManually = false;
                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(0);

                mStatusText.setText(R.string.wifi_setup_not_connected);
                mProgressText.setText(getString(R.string.wifi_setup_not_connected));

                mAddNetworkButton.setEnabled(true);
                mRefreshButton.setEnabled(true);
                mSkipOrNextButton.setEnabled(true);
            }

            break;
        }
    }

    private void showConnectingStatus() {
        mProgressBar.setIndeterminate(false);
        mProgressBar.setProgress(1);
        mStatusText.setText(R.string.wifi_setup_status_connecting);
        mProgressText.setText(Summary.get(this, DetailedState.CONNECTING));
    }

    private void onAddNetworkButtonPressed() {
        // onConfigUiShown() will be called.
        mWifiSettings.onAddNetworkPressed();

        // We don't need detail button since all the details are in the main screen.
        mDetailButton.setVisibility(View.GONE);
    }

    /**
     * Called when the screen enters wifi configuration UI. UI widget for configuring network
     * (a.k.a. ConfigPreference) should be taken care of by caller side.
     * This method should handle buttons' visibility/enabled.
     * @param selectedAccessPoint AccessPoint object being selected. null when a user pressed
     * "Add network" button, meaning there's no selected access point.
     */
    /* package */ void showConfigUi(AccessPoint selectedAccessPoint, boolean edit) {
        // We don't want to keep scanning Wi-Fi networks during users' configuring one network.
        mWifiSettings.pauseWifiScan();

        findViewById(R.id.wifi_setup).setVisibility(View.GONE);
        final ViewGroup parent = (ViewGroup)findViewById(R.id.wifi_config_ui);
        parent.setVisibility(View.VISIBLE);
        parent.removeAllViews();
        mWifiConfig = new WifiConfigUiForSetupWizardXL(this, parent, selectedAccessPoint, edit);
        final View view = mWifiConfig.getView();
        if (selectedAccessPoint != null) {
            view.findViewById(R.id.wifi_general_info).setVisibility(View.VISIBLE);
            ((TextView)view.findViewById(R.id.title)).setText(selectedAccessPoint.getTitle());
            ((TextView)view.findViewById(R.id.summary)).setText(selectedAccessPoint.getSummary());
        } else {
            view.findViewById(R.id.wifi_general_info).setVisibility(View.GONE);
        }
        // parent.addView(view);

        mStatusText.setText(R.string.wifi_setup_status_edit_network);
        mAddNetworkButton.setVisibility(View.GONE);
        mRefreshButton.setVisibility(View.GONE);
        mSkipOrNextButton.setVisibility(View.GONE);
        mConnectButton.setVisibility(View.VISIBLE);
        mConnectButton.setVisibility(View.VISIBLE);
        mBackButton.setVisibility(View.VISIBLE);
        // TODO: remove this after UI fix.
        // mDetailButton.setVisibility(View.VISIBLE);
    }

    // May be called when user press "connect" button in WifiDialog
    /* package */ void onConnectButtonPressed() {
        mAfterTryConnect = true;

        mWifiSettings.submit(mWifiConfig.getController());

        // updateConnectionState() isn't called soon after the user's "connect" action,
        // and the user still sees "not connected" message for a while, which looks strange.
        // We instead manually show "connecting" message before the system gets actual
        // "connecting" message from Wi-Fi module.
        showConnectingStatus();

        // Might be better to delay showing this button.
        mBackButton.setVisibility(View.VISIBLE);

        findViewById(R.id.wps_fields).setVisibility(View.GONE);
        findViewById(R.id.security_fields).setVisibility(View.GONE);
        findViewById(R.id.type).setVisibility(View.GONE);

        mSkipOrNextButton.setVisibility(View.VISIBLE);
        mSkipOrNextButton.setEnabled(false);
        mConnectButton.setVisibility(View.GONE);
        mAddNetworkButton.setVisibility(View.GONE);
        mRefreshButton.setVisibility(View.GONE);
        mDetailButton.setVisibility(View.GONE);

        mShowingConnectingMessageManually = true;
        mIgnoringWifiNotificationCount = 1;
    }

    // May be called when user press "forget" button in WifiDialog
    /* package */ void onForgetButtonPressed() {
        mWifiSettings.forget();

        refreshAccessPoints(false);
        restoreFirstButtonVisibilityState();
        mAddNetworkButton.setEnabled(true);
        mRefreshButton.setEnabled(true);
        mSkipOrNextButton.setEnabled(true);

        mProgressBar.setIndeterminate(false);
        mProgressBar.setProgress(0);
        mProgressText.setText(getString(R.string.wifi_setup_not_connected));
    }

    private void onBackButtonPressed() {
        if (mAfterTryConnect) {
            mAfterTryConnect = false;

            // When a user press "Back" button after pressing "Connect" button, we want to cancel
            // the "Connect" request and refresh the whole wifi status.
            restoreFirstButtonVisibilityState();
            mShowingConnectingMessageManually = false;

            mAddNetworkButton.setEnabled(false);
            mRefreshButton.setEnabled(false);
            mSkipOrNextButton.setEnabled(true);

            refreshAccessPoints(true);
        } else { // During user's Wifi configuration.
            mWifiSettings.resumeWifiScan();

            mStatusText.setText(R.string.wifi_setup_status_select_network);
            restoreFirstButtonVisibilityState();

            mAddNetworkButton.setEnabled(true);
            mRefreshButton.setEnabled(true);
            mSkipOrNextButton.setEnabled(true);
        }

        findViewById(R.id.wifi_setup).setVisibility(View.VISIBLE);
        final ViewGroup parent = (ViewGroup)findViewById(R.id.wifi_config_ui);
        parent.removeAllViews();
        parent.setVisibility(View.GONE);
        mWifiConfig = null;
    }

    /**
     * @param connected true when the device is connected to a specific network.
     */
    /* package */ void changeNextButtonState(boolean connected) {
        if (connected) {
            mSkipOrNextButton.setText(R.string.wifi_setup_next);
        } else {
            mSkipOrNextButton.setText(R.string.wifi_setup_skip);
        }
    }

    /**
     * Called when the list of AccessPoints are modified and this Activity needs to refresh
     * the list.
     */
    /* package */ void onAccessPointsUpdated(
            PreferenceCategory holder, Collection<AccessPoint> accessPoints) {
        int count = MAX_MENU_COUNT_IN_XL;
        for (AccessPoint accessPoint : accessPoints) {
            accessPoint.setLayoutResource(R.layout.custom_preference);
            holder.addPreference(accessPoint);
            count--;
            if (count <= 0) {
                break;
            }
        }
    }

    private void refreshAccessPoints(boolean disconnectNetwork) {
        mIgnoringWifiNotificationCount = 5;
        mProgressBar.setIndeterminate(true);
        ((Button)findViewById(R.id.wifi_setup_add_network)).setEnabled(false);
        ((Button)findViewById(R.id.wifi_setup_refresh_list)).setEnabled(false);
        mProgressText.setText(Summary.get(this, DetailedState.SCANNING));
        mStatusText.setText(R.string.wifi_setup_status_scanning);

        if (disconnectNetwork) {
            mWifiManager.disconnect();
        }

        mWifiSettings.refreshAccessPoints();
    }
}
