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

package com.android.settings.wifi.calling;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.ProvisioningManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

import com.android.ims.ImsConfig;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settings.widget.SwitchBar;

/**
 * This is the inner class of {@link WifiCallingSettings} fragment.
 * The preference screen lets you enable/disable Wi-Fi Calling and change Wi-Fi Calling mode.
 */
public class WifiCallingSettingsForSub extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener,
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "WifiCallingForSub";

    //String keys for preference lookup
    private static final String BUTTON_WFC_MODE = "wifi_calling_mode";
    private static final String BUTTON_WFC_ROAMING_MODE = "wifi_calling_roaming_mode";
    private static final String PREFERENCE_EMERGENCY_ADDRESS = "emergency_address_key";

    @VisibleForTesting
    static final int REQUEST_CHECK_WFC_EMERGENCY_ADDRESS = 1;
    @VisibleForTesting
    static final int REQUEST_CHECK_WFC_DISCLAIMER = 2;

    public static final String EXTRA_LAUNCH_CARRIER_APP = "EXTRA_LAUNCH_CARRIER_APP";
    public static final String EXTRA_SUB_ID = "EXTRA_SUB_ID";

    protected static final String FRAGMENT_BUNDLE_SUBID = "subId";

    public static final int LAUCH_APP_ACTIVATE = 0;
    public static final int LAUCH_APP_UPDATE = 1;

    //UI objects
    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private ListWithEntrySummaryPreference mButtonWfcMode;
    private ListWithEntrySummaryPreference mButtonWfcRoamingMode;
    private Preference mUpdateAddress;
    private TextView mEmptyView;

    private boolean mValidListener = false;
    private boolean mEditableWfcMode = true;
    private boolean mEditableWfcRoamingMode = true;
    private boolean mUseWfcHomeModeForRoaming = false;

    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private ImsMmTelManager mImsMmTelManager;
    private ProvisioningManager mProvisioningManager;
    private TelephonyManager mTelephonyManager;

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /*
         * Enable/disable controls when in/out of a call and depending on
         * TTY mode and TTY support over VoLTE.
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
         * java.lang.String)
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            final SettingsActivity activity = (SettingsActivity) getActivity();
            final boolean isNonTtyOrTtyOnVolteEnabled =
                    queryImsState(WifiCallingSettingsForSub.this.mSubId).isAllowUserControl();
            final boolean isWfcEnabled = mSwitchBar.isChecked()
                    && isNonTtyOrTtyOnVolteEnabled;
            boolean isCallStateIdle = getTelephonyManagerForSub(
                    WifiCallingSettingsForSub.this.mSubId).getCallState()
                    == TelephonyManager.CALL_STATE_IDLE;
            mSwitchBar.setEnabled(isCallStateIdle
                    && isNonTtyOrTtyOnVolteEnabled);

            boolean isWfcModeEditable = true;
            boolean isWfcRoamingModeEditable = false;
            final CarrierConfigManager configManager = (CarrierConfigManager)
                    activity.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configManager != null) {
                PersistableBundle b =
                        configManager.getConfigForSubId(WifiCallingSettingsForSub.this.mSubId);
                if (b != null) {
                    isWfcModeEditable = b.getBoolean(
                            CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL);
                    isWfcRoamingModeEditable = b.getBoolean(
                            CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL);
                }
            }

            final Preference pref = getPreferenceScreen().findPreference(BUTTON_WFC_MODE);
            if (pref != null) {
                pref.setEnabled(isWfcEnabled && isWfcModeEditable
                        && isCallStateIdle);
            }
            final Preference pref_roam =
                    getPreferenceScreen().findPreference(BUTTON_WFC_ROAMING_MODE);
            if (pref_roam != null) {
                pref_roam.setEnabled(isWfcEnabled && isWfcRoamingModeEditable
                        && isCallStateIdle);
            }
        }
    };

    /*
     * Launch carrier emergency address managemnent activity
     */
    private final OnPreferenceClickListener mUpdateAddressListener =
            preference -> {
                final Intent carrierAppIntent = getCarrierActivityIntent();
                if (carrierAppIntent != null) {
                    carrierAppIntent.putExtra(EXTRA_LAUNCH_CARRIER_APP, LAUCH_APP_UPDATE);
                    startActivity(carrierAppIntent);
                }
                return true;
            };

    private final ProvisioningManager.Callback mProvisioningCallback =
            new ProvisioningManager.Callback() {
                @Override
                public void onProvisioningIntChanged(int item, int value) {
                    if (item == ImsConfig.ConfigConstants.VOICE_OVER_WIFI_SETTING_ENABLED
                            || item == ImsConfig.ConfigConstants.VLT_SETTING_ENABLED) {
                        // The provisioning policy might have changed. Update the body to make sure
                        // this change takes effect if needed.
                        updateBody();
                    }
                }
            };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mEmptyView = getView().findViewById(android.R.id.empty);
        setEmptyView(mEmptyView);
        final Resources res = getResourcesForSubId();
        final String emptyViewText = res.getString(R.string.wifi_calling_off_explanation,
                res.getString(R.string.wifi_calling_off_explanation_2));
        mEmptyView.setText(emptyViewText);

        mSwitchBar = getView().findViewById(R.id.switch_bar);
        mSwitchBar.show();
        mSwitch = mSwitchBar.getSwitch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @VisibleForTesting
    void showAlert(Intent intent) {
        final Context context = getActivity();

        final CharSequence title = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_TITLE);
        final CharSequence message = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_MESSAGE);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setTitle(title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, null);
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private IntentFilter mIntentFilter;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(ImsManager.ACTION_WFC_IMS_REGISTRATION_ERROR)) {
                // If this fragment is active then we are immediately
                // showing alert on screen. There is no need to add
                // notification in this case.
                //
                // In order to communicate to ImsPhone that it should
                // not show notification, we are changing result code here.
                setResultCode(Activity.RESULT_CANCELED);

                showAlert(intent);
            }
        }
    };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_CALLING_FOR_SUB;
    }

    @Override
    public int getHelpResource() {
        // Return 0 to suppress help icon. The help will be populated by parent page.
        return 0;
    }

    @VisibleForTesting
    TelephonyManager getTelephonyManagerForSub(int subId) {
        if (mTelephonyManager == null) {
            mTelephonyManager = getContext().getSystemService(TelephonyManager.class);
        }
        return mTelephonyManager.createForSubscriptionId(subId);
    }

    @VisibleForTesting
    WifiCallingQueryImsState queryImsState(int subId) {
        return new WifiCallingQueryImsState(getContext(), subId);
    }

    @VisibleForTesting
    ProvisioningManager getImsProvisioningManager() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return null;
        }
        return ProvisioningManager.createForSubscriptionId(mSubId);
    }

    @VisibleForTesting
    ImsMmTelManager getImsMmTelManager() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return null;
        }
        return ImsMmTelManager.createForSubscriptionId(mSubId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wifi_calling_settings);

        // SubId should always be specified when creating this fragment. Either through
        // fragment.setArguments() or through savedInstanceState.
        if (getArguments() != null && getArguments().containsKey(FRAGMENT_BUNDLE_SUBID)) {
            mSubId = getArguments().getInt(FRAGMENT_BUNDLE_SUBID);
        } else if (savedInstanceState != null) {
            mSubId = savedInstanceState.getInt(
                    FRAGMENT_BUNDLE_SUBID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }

        mProvisioningManager = getImsProvisioningManager();
        mImsMmTelManager = getImsMmTelManager();

        mButtonWfcMode = findPreference(BUTTON_WFC_MODE);
        mButtonWfcMode.setOnPreferenceChangeListener(this);

        mButtonWfcRoamingMode =  findPreference(BUTTON_WFC_ROAMING_MODE);
        mButtonWfcRoamingMode.setOnPreferenceChangeListener(this);

        mUpdateAddress = findPreference(PREFERENCE_EMERGENCY_ADDRESS);
        mUpdateAddress.setOnPreferenceClickListener(mUpdateAddressListener);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ImsManager.ACTION_WFC_IMS_REGISTRATION_ERROR);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(FRAGMENT_BUNDLE_SUBID, mSubId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final View view = inflater.inflate(
                R.layout.wifi_calling_settings_preferences, container, false);

        final ViewGroup prefs_container = view.findViewById(R.id.prefs_container);
        Utils.prepareCustomPreferencesList(container, view, prefs_container, false);
        final View prefs = super.onCreateView(inflater, prefs_container, savedInstanceState);
        prefs_container.addView(prefs);

        return view;
    }

    @VisibleForTesting
    boolean isWfcProvisionedOnDevice() {
        return queryImsState(mSubId).isWifiCallingProvisioned();
    }

    private void updateBody() {
        if (!isWfcProvisionedOnDevice()) {
            // This screen is not allowed to be shown due to provisioning policy and should
            // therefore be closed.
            finish();
            return;
        }

        final CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        boolean isWifiOnlySupported = true;

        if (configManager != null) {
            final PersistableBundle b = configManager.getConfigForSubId(mSubId);
            if (b != null) {
                mEditableWfcMode = b.getBoolean(
                        CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL);
                mEditableWfcRoamingMode = b.getBoolean(
                        CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL);
                mUseWfcHomeModeForRoaming = b.getBoolean(
                        CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL,
                        false);
                isWifiOnlySupported = b.getBoolean(
                        CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true);
            }
        }

        final Resources res = getResourcesForSubId();
        mButtonWfcMode.setTitle(res.getString(R.string.wifi_calling_mode_title));
        mButtonWfcMode.setDialogTitle(res.getString(R.string.wifi_calling_mode_dialog_title));
        mButtonWfcRoamingMode.setTitle(res.getString(R.string.wifi_calling_roaming_mode_title));
        mButtonWfcRoamingMode.setDialogTitle(
                res.getString(R.string.wifi_calling_roaming_mode_dialog_title));

        if (isWifiOnlySupported) {
            // Set string resources WITH option wifi only in mButtonWfcMode.
            mButtonWfcMode.setEntries(
                    res.getStringArray(R.array.wifi_calling_mode_choices));
            mButtonWfcMode.setEntryValues(res.getStringArray(R.array.wifi_calling_mode_values));
            mButtonWfcMode.setEntrySummaries(
                    res.getStringArray(R.array.wifi_calling_mode_summaries));

            // Set string resources WITH option wifi only in mButtonWfcRoamingMode.
            mButtonWfcRoamingMode.setEntries(
                    res.getStringArray(R.array.wifi_calling_mode_choices_v2));
            mButtonWfcRoamingMode.setEntryValues(
                    res.getStringArray(R.array.wifi_calling_mode_values));
            mButtonWfcRoamingMode.setEntrySummaries(
                    res.getStringArray(R.array.wifi_calling_mode_summaries));
        } else {
            // Set string resources WITHOUT option wifi only in mButtonWfcMode.
            mButtonWfcMode.setEntries(
                    res.getStringArray(R.array.wifi_calling_mode_choices_without_wifi_only));
            mButtonWfcMode.setEntryValues(
                    res.getStringArray(R.array.wifi_calling_mode_values_without_wifi_only));
            mButtonWfcMode.setEntrySummaries(
                    res.getStringArray(R.array.wifi_calling_mode_summaries_without_wifi_only));

            // Set string resources WITHOUT option wifi only in mButtonWfcRoamingMode.
            mButtonWfcRoamingMode.setEntries(
                    res.getStringArray(R.array.wifi_calling_mode_choices_v2_without_wifi_only));
            mButtonWfcRoamingMode.setEntryValues(
                    res.getStringArray(R.array.wifi_calling_mode_values_without_wifi_only));
            mButtonWfcRoamingMode.setEntrySummaries(
                    res.getStringArray(R.array.wifi_calling_mode_summaries_without_wifi_only));
        }

        // NOTE: Buttons will be enabled/disabled in mPhoneStateListener
        final WifiCallingQueryImsState queryIms = queryImsState(mSubId);
        final boolean wfcEnabled = queryIms.isEnabledByUser()
                && queryIms.isAllowUserControl();
        mSwitch.setChecked(wfcEnabled);
        final int wfcMode = mImsMmTelManager.getVoWiFiModeSetting();
        final int wfcRoamingMode = mImsMmTelManager.getVoWiFiRoamingModeSetting();
        mButtonWfcMode.setValue(Integer.toString(wfcMode));
        mButtonWfcRoamingMode.setValue(Integer.toString(wfcRoamingMode));
        updateButtonWfcMode(wfcEnabled, wfcMode, wfcRoamingMode);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateBody();

        if (queryImsState(mSubId).isWifiCallingSupported()) {
            getTelephonyManagerForSub(mSubId).listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE);

            mSwitchBar.addOnSwitchChangeListener(this);

            mValidListener = true;
        }

        final Context context = getActivity();
        context.registerReceiver(mIntentReceiver, mIntentFilter);

        final Intent intent = getActivity().getIntent();
        if (intent.getBooleanExtra(Phone.EXTRA_KEY_ALERT_SHOW, false)) {
            showAlert(intent);
        }

        // Register callback for provisioning changes.
        registerProvisioningChangedCallback();
    }

    @Override
    public void onPause() {
        super.onPause();

        final Context context = getActivity();

        if (mValidListener) {
            mValidListener = false;

            getTelephonyManagerForSub(mSubId).listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);

            mSwitchBar.removeOnSwitchChangeListener(this);
        }

        context.unregisterReceiver(mIntentReceiver);

        // Remove callback for provisioning changes.
        unregisterProvisioningChangedCallback();
    }

    /**
     * Listens to the state change of the switch.
     */
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Log.d(TAG, "onSwitchChanged(" + isChecked + ")");

        if (!isChecked) {
            updateWfcMode(false);
            return;
        }

        // Launch disclaimer fragment before turning on WFC
        final Context context = getActivity();
        final Bundle args = new Bundle();
        args.putInt(EXTRA_SUB_ID, mSubId);
        new SubSettingLauncher(context)
                .setDestination(WifiCallingDisclaimerFragment.class.getName())
                .setArguments(args)
                .setTitleRes(R.string.wifi_calling_settings_title)
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(this, REQUEST_CHECK_WFC_DISCLAIMER)
                .launch();
    }

    /*
     * Get the Intent to launch carrier emergency address management activity.
     * Return null when no activity found.
     */
    private Intent getCarrierActivityIntent() {
        // Retrive component name from carrier config
        final CarrierConfigManager configManager =
                getActivity().getSystemService(CarrierConfigManager.class);
        if (configManager == null) return null;

        final PersistableBundle bundle = configManager.getConfigForSubId(mSubId);
        if (bundle == null) return null;

        final String carrierApp = bundle.getString(
                CarrierConfigManager.KEY_WFC_EMERGENCY_ADDRESS_CARRIER_APP_STRING);
        if (TextUtils.isEmpty(carrierApp)) return null;

        final ComponentName componentName = ComponentName.unflattenFromString(carrierApp);
        if (componentName == null) return null;

        // Build and return intent
        final Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.putExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, mSubId);
        return intent;
    }

    /*
     * Turn on/off WFC mode with ImsManager and update UI accordingly
     */
    private void updateWfcMode(boolean wfcEnabled) {
        Log.i(TAG, "updateWfcMode(" + wfcEnabled + ")");
        mImsMmTelManager.setVoWiFiSettingEnabled(wfcEnabled);

        final int wfcMode = mImsMmTelManager.getVoWiFiModeSetting();
        final int wfcRoamingMode = mImsMmTelManager.getVoWiFiRoamingModeSetting();
        updateButtonWfcMode(wfcEnabled, wfcMode, wfcRoamingMode);
        if (wfcEnabled) {
            mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), wfcMode);
        } else {
            mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), -1);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "WFC activity request = " + requestCode + " result = " + resultCode);

        switch (requestCode) {
            case REQUEST_CHECK_WFC_EMERGENCY_ADDRESS:
                if (resultCode == Activity.RESULT_OK) {
                    updateWfcMode(true);
                }
                break;
            case REQUEST_CHECK_WFC_DISCLAIMER:
                if (resultCode == Activity.RESULT_OK) {
                    // Call address management activity before turning on WFC
                    final Intent carrierAppIntent = getCarrierActivityIntent();
                    if (carrierAppIntent != null) {
                        carrierAppIntent.putExtra(EXTRA_LAUNCH_CARRIER_APP, LAUCH_APP_ACTIVATE);
                        startActivityForResult(carrierAppIntent,
                                REQUEST_CHECK_WFC_EMERGENCY_ADDRESS);
                    } else {
                        updateWfcMode(true);
                    }
                }
                break;
            default:
                Log.e(TAG, "Unexpected request: " + requestCode);
                break;
        }
    }

    private void updateButtonWfcMode(boolean wfcEnabled,
            int wfcMode, int wfcRoamingMode) {
        mButtonWfcMode.setSummary(getWfcModeSummary(wfcMode));
        mButtonWfcMode.setEnabled(wfcEnabled && mEditableWfcMode);
        // mButtonWfcRoamingMode.setSummary is not needed; summary is just selected value.
        mButtonWfcRoamingMode.setEnabled(wfcEnabled && mEditableWfcRoamingMode);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final boolean updateAddressEnabled = (getCarrierActivityIntent() != null);
        if (wfcEnabled) {
            if (mEditableWfcMode) {
                preferenceScreen.addPreference(mButtonWfcMode);
            } else {
                // Don't show WFC (home) preference if it's not editable.
                preferenceScreen.removePreference(mButtonWfcMode);
            }
            if (mEditableWfcRoamingMode && !mUseWfcHomeModeForRoaming) {
                preferenceScreen.addPreference(mButtonWfcRoamingMode);
            } else {
                // Don't show WFC roaming preference if it's not editable.
                preferenceScreen.removePreference(mButtonWfcRoamingMode);
            }
            if (updateAddressEnabled) {
                preferenceScreen.addPreference(mUpdateAddress);
            } else {
                preferenceScreen.removePreference(mUpdateAddress);
            }
        } else {
            preferenceScreen.removePreference(mButtonWfcMode);
            preferenceScreen.removePreference(mButtonWfcRoamingMode);
            preferenceScreen.removePreference(mUpdateAddress);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mButtonWfcMode) {
            Log.d(TAG, "onPreferenceChange mButtonWfcMode " + newValue);
            mButtonWfcMode.setValue((String) newValue);
            final int buttonMode = Integer.valueOf((String) newValue);
            final int currentWfcMode = mImsMmTelManager.getVoWiFiModeSetting();
            if (buttonMode != currentWfcMode) {
                mImsMmTelManager.setVoWiFiModeSetting(buttonMode);
                mButtonWfcMode.setSummary(getWfcModeSummary(buttonMode));
                mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), buttonMode);

                if (mUseWfcHomeModeForRoaming) {
                    mImsMmTelManager.setVoWiFiRoamingModeSetting(buttonMode);
                    // mButtonWfcRoamingMode.setSummary is not needed; summary is selected value
                }
            }
        } else if (preference == mButtonWfcRoamingMode) {
            mButtonWfcRoamingMode.setValue((String) newValue);
            final int buttonMode = Integer.valueOf((String) newValue);
            final int currentMode = mImsMmTelManager.getVoWiFiRoamingModeSetting();
            if (buttonMode != currentMode) {
                mImsMmTelManager.setVoWiFiRoamingModeSetting(buttonMode);
                // mButtonWfcRoamingMode.setSummary is not needed; summary is just selected value.
                mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), buttonMode);
            }
        }
        return true;
    }

    private CharSequence getWfcModeSummary(int wfcMode) {
        int resId = com.android.internal.R.string.wifi_calling_off_summary;
        if (queryImsState(mSubId).isEnabledByUser()) {
            switch (wfcMode) {
                case ImsMmTelManager.WIFI_MODE_WIFI_ONLY:
                    resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                    break;
                case ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED:
                    resId = com.android.internal.R.string.wfc_mode_cellular_preferred_summary;
                    break;
                case ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED:
                    resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                    break;
                default:
                    Log.e(TAG, "Unexpected WFC mode value: " + wfcMode);
            }
        }
        return getResourcesForSubId().getString(resId);
    }

    @VisibleForTesting
    Resources getResourcesForSubId() {
        return SubscriptionManager.getResourcesForSubId(getContext(), mSubId);
    }

    @VisibleForTesting
    void registerProvisioningChangedCallback() {
        if (mProvisioningManager == null) {
            return;
        }
        try {
            mProvisioningManager.registerProvisioningChangedCallback(getContext().getMainExecutor(),
                    mProvisioningCallback);
        } catch (Exception ex) {
            Log.w(TAG, "onResume: Unable to register callback for provisioning changes.");
        }
    }

    @VisibleForTesting
    void unregisterProvisioningChangedCallback() {
        if (mProvisioningManager == null) {
            return;
        }
        mProvisioningManager.unregisterProvisioningChangedCallback(mProvisioningCallback);
    }
}
