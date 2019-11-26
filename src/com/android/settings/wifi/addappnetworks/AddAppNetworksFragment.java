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

package com.android.settings.wifi.addappnetworks;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fragment list those networks, which is proposed by other app, to user, and handle user's
 * choose on either saving those networks or rejecting the request.
 */
public class AddAppNetworksFragment extends InstrumentedFragment {
    public static final String TAG = "AddAppNetworksFragment";

    // Security types of a requested or saved network.
    private static final String SECURITY_NO_PASSWORD = "nopass";
    private static final String SECURITY_WEP = "wep";
    private static final String SECURITY_WPA_PSK = "wpa";
    private static final String SECURITY_SAE = "sae";

    // Possible result values in each item of the returned result list, which is used
    // to inform the caller APP the processed result of each specified network.
    private static final int RESULT_NETWORK_INITIAL = -1;  //initial value
    private static final int RESULT_NETWORK_SUCCESS = 0;
    private static final int RESULT_NETWORK_ADD_ERROR = 1;
    private static final int RESULT_NETWORK_ALREADY_EXISTS = 2;

    // Handler messages for controlling different state and delay showing the status message.
    private static final int MESSAGE_START_SAVING_NETWORK = 1;
    private static final int MESSAGE_SHOW_SAVED_AND_CONNECT_NETWORK = 2;
    private static final int MESSAGE_SHOW_SAVE_FAILED = 3;
    private static final int MESSAGE_FINISH = 4;

    // Signal level for the constant signal icon.
    private static final int MAX_RSSI_SIGNAL_LEVEL = 4;

    // Duration for showing different status message.
    private static final long SHOW_SAVING_INTERVAL_MILLIS = 500L;
    private static final long SHOW_SAVED_INTERVAL_MILLIS = 1000L;

    @VisibleForTesting
    FragmentActivity mActivity;
    @VisibleForTesting
    View mLayoutView;
    @VisibleForTesting
    Button mCancelButton;
    @VisibleForTesting
    Button mSaveButton;
    @VisibleForTesting
    String mCallingPackageName;

    private TextView mSummaryView;
    private TextView mSingleNetworkProcessingStatusView;
    private int mSavingIndex;
    private List<WifiConfiguration> mAllSpecifiedNetworksList;
    private ArrayList<Integer> mResultCodeArrayList;
    private WifiManager.ActionListener mSaveListener;
    private WifiManager mWifiManager;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_START_SAVING_NETWORK:
                    mSaveButton.setEnabled(false);
                    // Set the initial text color for status message.
                    mSingleNetworkProcessingStatusView.setTextColor(
                            com.android.settingslib.Utils.getColorAttr(mActivity,
                                    android.R.attr.textColorSecondary));
                    mSingleNetworkProcessingStatusView.setText(
                            getString(R.string.wifi_add_app_single_network_saving_summary));
                    mSingleNetworkProcessingStatusView.setVisibility(View.VISIBLE);

                    // Save the proposed network.
                    saveNetworks();
                    break;

                case MESSAGE_SHOW_SAVED_AND_CONNECT_NETWORK:
                    mSingleNetworkProcessingStatusView.setText(
                            getString(R.string.wifi_add_app_single_network_saved_summary));

                    // For the single network case, we need to call connection after saved.
                    connectNetwork();

                    sendEmptyMessageDelayed(MESSAGE_FINISH,
                            SHOW_SAVED_INTERVAL_MILLIS);
                    break;

                case MESSAGE_SHOW_SAVE_FAILED:
                    mSingleNetworkProcessingStatusView.setText(
                            getString(R.string.wifi_add_app_single_network_save_failed_summary));
                    // Error message need to use colorError attribute to show.
                    mSingleNetworkProcessingStatusView.setTextColor(
                            com.android.settingslib.Utils.getColorAttr(mActivity,
                                    android.R.attr.colorError));
                    mSaveButton.setEnabled(true);
                    break;

                case MESSAGE_FINISH:
                    finishWithResult(RESULT_OK, mResultCodeArrayList);
                    break;

                default:
                    // Do nothing.
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mActivity = getActivity();
        mWifiManager = mActivity.getSystemService(WifiManager.class);

        return inflater.inflate(R.layout.wifi_add_app_networks, container, false);
    }

    // TODO: Makesure function work correctly after rotate.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initial UI variable.
        mLayoutView = view;
        mCancelButton = view.findViewById(R.id.cancel);
        mSaveButton = view.findViewById(R.id.save);
        mSummaryView = view.findViewById(R.id.app_summary);
        mSingleNetworkProcessingStatusView = view.findViewById(R.id.single_status);
        // Assigns button listeners and network save listener.
        mCancelButton.setOnClickListener(getCancelListener());
        mSaveButton.setOnClickListener(getSaveListener());
        prepareSaveResultListener();

        // Prepare the non-UI variables.
        final Bundle bundle = getArguments();
        createContent(bundle);
    }

    private void createContent(Bundle bundle) {
        mAllSpecifiedNetworksList =
                bundle.getParcelableArrayList(Settings.EXTRA_WIFI_CONFIGURATION_LIST);

        // If there is no networks in the request intent, then just finish activity.
        if (mAllSpecifiedNetworksList == null || mAllSpecifiedNetworksList.isEmpty()) {
            finishWithResult(RESULT_CANCELED, null /* resultArrayList */);
            return;
        }

        // Initial the result arry.
        initializeResultCodeArray();

        // Filter out the saved networks, don't show saved networks to user.
        checkSavedNetworks();

        if (mAllSpecifiedNetworksList.size() == 1) {
            // If the only one requested network is already saved, just return with existence.
            if (mResultCodeArrayList.get(0) == RESULT_NETWORK_ALREADY_EXISTS) {
                finishWithResult(RESULT_OK, mResultCodeArrayList);
                return;
            }

            // Show signal icon for single network case.
            setSingleNetworkSignalIcon();
            // Show the SSID of the proposed network.
            ((TextView) mLayoutView.findViewById(R.id.single_ssid)).setText(
                    mAllSpecifiedNetworksList.get(0).SSID);
            // Set the status view as gone when UI is initialized.
            mSingleNetworkProcessingStatusView.setVisibility(View.GONE);
        } else {
            // TODO: Add code for processing multiple networks case.
        }

        // Assigns caller app icon, title, and summary.
        mCallingPackageName =
                bundle.getString(AddAppNetworksActivity.KEY_CALLING_PACKAGE_NAME);
        assignAppIcon(mActivity, mCallingPackageName);
        assignTitleAndSummary(mActivity, mCallingPackageName);
    }

    private void initializeResultCodeArray() {
        final int networksSize = mAllSpecifiedNetworksList.size();
        mResultCodeArrayList = new ArrayList<>();

        for (int i = 0; i < networksSize; i++) {
            mResultCodeArrayList.add(RESULT_NETWORK_INITIAL);
        }
    }

    /**
     * Classify security type into following types:
     * 1. {@Code SECURITY_NO_PASSWORD}: No password network or OWE network.
     * 2. {@Code SECURITY_WEP}: Traditional WEP encryption network.
     * 3. {@Code SECURITY_WPA_PSK}: WPA/WPA2 preshare key type.
     * 4. {@Code SECURITY_SAE}: SAE type network.
     */
    private String getSecurityType(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.SAE)) {
            return SECURITY_SAE;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.OWE)) {
            return SECURITY_NO_PASSWORD;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK) || config.allowedKeyManagement.get(
                KeyMgmt.WPA2_PSK)) {
            return SECURITY_WPA_PSK;
        }
        return (config.wepKeys[0] == null) ? SECURITY_NO_PASSWORD : SECURITY_WEP;
    }

    /**
     * For the APP specified networks, need to filter out those saved ones and mark them as existed.
     */
    private void checkSavedNetworks() {
        final List<WifiConfiguration> privilegedWifiConfigurations =
                mWifiManager.getPrivilegedConfiguredNetworks();
        boolean foundInSavedList;
        int networkPositionInBundle = 0;
        for (WifiConfiguration specifiecConfig : mAllSpecifiedNetworksList) {
            foundInSavedList = false;
            final String ssidWithQuotation = addQuotationIfNeeded(specifiecConfig.SSID);
            final String securityType = getSecurityType(specifiecConfig);

            for (WifiConfiguration privilegedWifiConfiguration : privilegedWifiConfigurations) {
                final String savedSecurityType = getSecurityType(privilegedWifiConfiguration);

                // If SSID or security type is different, should be new network or need to updated
                // network.
                if (!ssidWithQuotation.equals(privilegedWifiConfiguration.SSID)
                        || !securityType.equals(savedSecurityType)) {
                    continue;
                }

                //  If specified network and saved network have same security types, we'll check
                //  more information according to their security type to judge if they are same.
                switch (securityType) {
                    case SECURITY_NO_PASSWORD:
                        foundInSavedList = true;
                        break;
                    case SECURITY_WEP:
                        if (specifiecConfig.wepKeys[0].equals(
                                privilegedWifiConfiguration.wepKeys[0])) {
                            foundInSavedList = true;
                        }
                        break;
                    case SECURITY_WPA_PSK:
                    case SECURITY_SAE:
                        if (specifiecConfig.preSharedKey.equals(
                                privilegedWifiConfiguration.preSharedKey)) {
                            foundInSavedList = true;
                        }
                        break;
                    default:
                        break;
                }
            }

            if (foundInSavedList) {
                // If this requested network already in the saved networks, mark this item in the
                // result code list as existed.
                mResultCodeArrayList.set(networkPositionInBundle, RESULT_NETWORK_ALREADY_EXISTS);
            } else {
                // TODO: for multiple networks case, need to add to adapter for present list to user
            }
            networkPositionInBundle++;
        }
    }

    private void setSingleNetworkSignalIcon() {
        // TODO: Check level of the network to show signal icon.
        final Drawable wifiIcon = mActivity.getDrawable(
                Utils.getWifiIconResource(MAX_RSSI_SIGNAL_LEVEL)).mutate();
        final Drawable wifiIconDark = wifiIcon.getConstantState().newDrawable().mutate();
        wifiIconDark.setTintList(
                Utils.getColorAttr(mActivity, android.R.attr.colorControlNormal));
        ((ImageView) mLayoutView.findViewById(R.id.signal_strength)).setImageDrawable(wifiIconDark);
    }

    private String addQuotationIfNeeded(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }

        if (input.length() >= 2 && input.startsWith("\"") && input.endsWith("\"")) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(input).append("\"");
        return sb.toString();
    }

    private void assignAppIcon(Context context, String callingPackageName) {
        final Drawable drawable = loadPackageIconDrawable(context, callingPackageName);
        ((ImageView) mLayoutView.findViewById(R.id.app_icon)).setImageDrawable(drawable);
    }

    private Drawable loadPackageIconDrawable(Context context, String callingPackageName) {
        Drawable icon = null;
        try {
            icon = context.getPackageManager().getApplicationIcon(callingPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Cannot get application icon", e);
        }

        return icon;
    }

    private void assignTitleAndSummary(Context context, String callingPackageName) {
        // Assigns caller app name to title
        ((TextView) mLayoutView.findViewById(R.id.app_title)).setText(getTitle());

        // Set summary
        mSummaryView.setText(getAddNetworkRequesterSummary(
                Utils.getApplicationLabel(context, callingPackageName)));
    }

    private CharSequence getAddNetworkRequesterSummary(CharSequence appName) {
        return getString(R.string.wifi_add_app_single_network_summary, appName);
    }

    private CharSequence getTitle() {
        return getString(R.string.wifi_add_app_single_network_title);
    }

    View.OnClickListener getCancelListener() {
        return (v) -> {
            Log.d(TAG, "User rejected to add network");
            finishWithResult(RESULT_CANCELED, null /* resultArrayList */);
        };
    }

    View.OnClickListener getSaveListener() {
        return (v) -> {
            Log.d(TAG, "User agree to add networks");
            // Start to process saving networks.
            final Message message = mHandler.obtainMessage(MESSAGE_START_SAVING_NETWORK);
            message.sendToTarget();
        };
    }

    private void prepareSaveResultListener() {
        mSaveListener = new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {
                mResultCodeArrayList.set(mSavingIndex, RESULT_NETWORK_SUCCESS);
                Message nextState_Message = mHandler.obtainMessage(
                        MESSAGE_SHOW_SAVED_AND_CONNECT_NETWORK);
                // Delay to change to next state for showing saving mesage for a period.
                mHandler.sendMessageDelayed(nextState_Message, SHOW_SAVING_INTERVAL_MILLIS);
            }

            @Override
            public void onFailure(int reason) {
                mResultCodeArrayList.set(mSavingIndex, RESULT_NETWORK_ADD_ERROR);
                Message nextState_Message = mHandler.obtainMessage(MESSAGE_SHOW_SAVE_FAILED);
                // Delay to change to next state for showing saving mesage for a period.
                mHandler.sendMessageDelayed(nextState_Message, SHOW_SAVING_INTERVAL_MILLIS);
            }
        };
    }

    private void saveNetworks() {
        final WifiConfiguration wifiConfiguration = mAllSpecifiedNetworksList.get(0);
        wifiConfiguration.SSID = addQuotationIfNeeded(wifiConfiguration.SSID);
        mWifiManager.save(wifiConfiguration, mSaveListener);
    }

    private void connectNetwork() {
        final WifiConfiguration wifiConfiguration = mAllSpecifiedNetworksList.get(0);
        // Don't need to handle the connect result.
        mWifiManager.connect(wifiConfiguration, null /* ActionListener */);
    }

    private void finishWithResult(int resultCode, ArrayList<Integer> resultArrayList) {
        if (resultArrayList != null) {
            Intent intent = new Intent();
            intent.putIntegerArrayListExtra(Settings.EXTRA_WIFI_CONFIGURATION_RESULT_LIST,
                    resultArrayList);
            mActivity.setResult(resultCode, intent);
        }
        mActivity.finish();
    }

    @Override
    public int getMetricsCategory() {
        // TODO(b/144891278): Need to define a new metric for this page, use the WIFI item first.
        return SettingsEnums.WIFI;
    }
}
