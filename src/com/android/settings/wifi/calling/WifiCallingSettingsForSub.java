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
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.ProvisioningManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

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
import com.android.settings.widget.SettingsMainSwitchPreference;

import java.util.List;

/**
 * This is the inner class of {@link WifiCallingSettings} fragment.
 * The preference screen lets you enable/disable Wi-Fi Calling and change Wi-Fi Calling mode.
 */
public class WifiCallingSettingsForSub extends SettingsPreferenceFragment
        implements OnCheckedChangeListener,
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "WifiCallingForSub";

    //String keys for preference lookup
    private static final String SWITCH_BAR = "wifi_calling_switch_bar";
    private static final String BUTTON_WFC_MODE = "wifi_calling_mode";
    private static final String BUTTON_WFC_ROAMING_MODE = "wifi_calling_roaming_mode";
    private static final String PREFERENCE_EMERGENCY_ADDRESS = "emergency_address_key";
    private static final String PREFERENCE_NO_OPTIONS_DESC = "no_options_description";

    @VisibleForTesting
    static final int REQUEST_CHECK_WFC_EMERGENCY_ADDRESS = 1;
    @VisibleForTesting
    static final int REQUEST_CHECK_WFC_DISCLAIMER = 2;

    public static final String EXTRA_LAUNCH_CARRIER_APP = "EXTRA_LAUNCH_CARRIER_APP";
    public static final String EXTRA_SUB_ID = "EXTRA_SUB_ID";

    protected static final String FRAGMENT_BUNDLE_SUBID = "subId";

    public static final int LAUNCH_APP_ACTIVATE = 0;
    public static final int LAUNCH_APP_UPDATE = 1;

    //UI objects
    private SettingsMainSwitchPreference mSwitchBar;
    private ListWithEntrySummaryPreference mButtonWfcMode;
    private ListWithEntrySummaryPreference mButtonWfcRoamingMode;
    private Preference mUpdateAddress;

    private boolean mEditableWfcMode = true;
    private boolean mEditableWfcRoamingMode = true;
    private boolean mUseWfcHomeModeForRoaming = false;

    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private ImsMmTelManager mImsMmTelManager;
    private ProvisioningManager mProvisioningManager;
    private TelephonyManager mTelephonyManager;

    private PhoneTelephonyCallback mTelephonyCallback;

    private class PhoneTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {
        /*
         * Enable/disable controls when in/out of a call and depending on
         * TTY mode and TTY support over VoLTE.
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
         * java.lang.String)
         */
        @Override
        public void onCallStateChanged(int state) {
            final SettingsActivity activity = (SettingsActivity) getActivity();

            boolean isWfcEnabled = false;
            boolean isCallStateIdle = false;

            final SettingsMainSwitchPreference prefSwitch = (SettingsMainSwitchPreference)
                    getPreferenceScreen().findPreference(SWITCH_BAR);
            if (prefSwitch != null) {
                isWfcEnabled = prefSwitch.isChecked();
                isCallStateIdle = getTelephonyManagerForSub(
                        WifiCallingSettingsForSub.this.mSubId).getCallStateForSubscription()
                        == TelephonyManager.CALL_STATE_IDLE;

                boolean isNonTtyOrTtyOnVolteEnabled = true;
                if (isWfcEnabled || isCallStateIdle) {
                    isNonTtyOrTtyOnVolteEnabled =
                            queryImsState(WifiCallingSettingsForSub.this.mSubId)
                            .isAllowUserControl();
                }

                isWfcEnabled = isWfcEnabled && isNonTtyOrTtyOnVolteEnabled;
                prefSwitch.setEnabled(isCallStateIdle && isNonTtyOrTtyOnVolteEnabled);
            }

            boolean isWfcModeEditable = true;
            boolean isWfcRoamingModeEditable = false;
            if (isWfcEnabled && isCallStateIdle) {
                final CarrierConfigManager configManager = (CarrierConfigManager)
                        activity.getSystemService(Context.CARRIER_CONFIG_SERVICE);
                if (configManager != null) {
                    PersistableBundle b = configManager.getConfigForSubId(
                            WifiCallingSettingsForSub.this.mSubId);
                    if (b != null) {
                        isWfcModeEditable = b.getBoolean(
                                CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL);
                        isWfcRoamingModeEditable = b.getBoolean(
                                CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL);
                    }
                }
            } else {
                isWfcModeEditable = false;
                isWfcRoamingModeEditable = false;
            }

            final Preference pref = getPreferenceScreen().findPreference(BUTTON_WFC_MODE);
            if (pref != null) {
                pref.setEnabled(isWfcModeEditable);
            }
            final Preference pref_roam =
                    getPreferenceScreen().findPreference(BUTTON_WFC_ROAMING_MODE);
            if (pref_roam != null) {
                pref_roam.setEnabled(isWfcRoamingModeEditable);
            }
        }
    }

    /*
     * Launch carrier emergency address management activity
     */
    private final OnPreferenceClickListener mUpdateAddressListener =
            preference -> {
                final Intent carrierAppIntent = getCarrierActivityIntent();
                if (carrierAppIntent != null) {
                    carrierAppIntent.putExtra(EXTRA_LAUNCH_CARRIER_APP, LAUNCH_APP_UPDATE);
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

    @VisibleForTesting
    void showAlert(Intent intent) {
        final Context context = getActivity();

        final CharSequence title =
                intent.getCharSequenceExtra(ImsManager.EXTRA_WFC_REGISTRATION_FAILURE_TITLE);
        final CharSequence message =
                intent.getCharSequenceExtra(ImsManager.EXTRA_WFC_REGISTRATION_FAILURE_MESSAGE);

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

        mSwitchBar = (SettingsMainSwitchPreference) findPreference(SWITCH_BAR);

        mButtonWfcMode = findPreference(BUTTON_WFC_MODE);
        mButtonWfcMode.setOnPreferenceChangeListener(this);

        mButtonWfcRoamingMode = findPreference(BUTTON_WFC_ROAMING_MODE);
        mButtonWfcRoamingMode.setOnPreferenceChangeListener(this);

        mUpdateAddress = findPreference(PREFERENCE_EMERGENCY_ADDRESS);
        mUpdateAddress.setOnPreferenceClickListener(mUpdateAddressListener);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ImsManager.ACTION_WFC_IMS_REGISTRATION_ERROR);

        updateDescriptionForOptions(
                List.of(mButtonWfcMode, mButtonWfcRoamingMode, mUpdateAddress));
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

        final ViewGroup prefs_container = view.findViewById(android.R.id.tabcontent);
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

        // NOTE: Buttons will be enabled/disabled in mTelephonyCallback
        final WifiCallingQueryImsState queryIms = queryImsState(mSubId);
        final boolean wfcEnabled = queryIms.isEnabledByUser()
                && queryIms.isAllowUserControl();
        mSwitchBar.setChecked(wfcEnabled);
        int wfcMode = ImsMmTelManager.WIFI_MODE_UNKNOWN;
        int wfcRoamingMode = ImsMmTelManager.WIFI_MODE_UNKNOWN;
        boolean hasException = false;
        try {
            wfcMode = mImsMmTelManager.getVoWiFiModeSetting();
            wfcRoamingMode = mImsMmTelManager.getVoWiFiRoamingModeSetting();
        } catch (IllegalArgumentException e) {
            hasException = true;
            Log.e(TAG, "getResourceIdForWfcMode: Exception", e);
        }
        mButtonWfcMode.setValue(Integer.toString(wfcMode));
        mButtonWfcRoamingMode.setValue(Integer.toString(wfcRoamingMode));
        updateButtonWfcMode(wfcEnabled && !hasException, wfcMode, wfcRoamingMode);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBody();
        Context context = getActivity();
        if (mTelephonyCallback == null && queryImsState(mSubId).isWifiCallingSupported()) {
            mTelephonyCallback = new PhoneTelephonyCallback();
            getTelephonyManagerForSub(mSubId).registerTelephonyCallback(
                    context.getMainExecutor(), mTelephonyCallback);
            mSwitchBar.addOnSwitchChangeListener(this);
        }
        context.registerReceiver(mIntentReceiver, mIntentFilter,
                Context.RECEIVER_EXPORTED_UNAUDITED);
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
        Context context = getActivity();
        if (mTelephonyCallback != null) {
            getTelephonyManagerForSub(mSubId).unregisterTelephonyCallback(mTelephonyCallback);
            mTelephonyCallback = null;
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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
        // Retrieve component name from carrier config
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
        boolean hasException = false;
        try {
            mImsMmTelManager.setVoWiFiSettingEnabled(wfcEnabled);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "updateWfcMode: Exception", e);
            hasException = true;
        }

        int wfcMode = ImsMmTelManager.WIFI_MODE_UNKNOWN;
        int wfcRoamingMode = ImsMmTelManager.WIFI_MODE_UNKNOWN;
        if (!hasException) {
            try {
                wfcMode = mImsMmTelManager.getVoWiFiModeSetting();
                wfcRoamingMode = mImsMmTelManager.getVoWiFiRoamingModeSetting();
            } catch (IllegalArgumentException e) {
                hasException = true;
                Log.e(TAG, "updateWfcMode: Exception", e);
            }
        }
        updateButtonWfcMode(wfcEnabled && !hasException, wfcMode, wfcRoamingMode);
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
                        carrierAppIntent.putExtra(EXTRA_LAUNCH_CARRIER_APP, LAUNCH_APP_ACTIVATE);
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
            // Don't show WFC (home) preference if it's not editable.
            mButtonWfcMode.setVisible(mEditableWfcMode);
            // Don't show WFC roaming preference if it's not editable.
            mButtonWfcRoamingMode.setVisible(
                    mEditableWfcRoamingMode && !mUseWfcHomeModeForRoaming);
            mUpdateAddress.setVisible(updateAddressEnabled);
        } else {
            mButtonWfcMode.setVisible(false);
            mButtonWfcRoamingMode.setVisible(false);
            mUpdateAddress.setVisible(false);
        }
        updateDescriptionForOptions(
                List.of(mButtonWfcMode, mButtonWfcRoamingMode, mUpdateAddress));
    }

    private void updateDescriptionForOptions(List<Preference> visibleOptions) {
        LinkifyDescriptionPreference pref = findPreference(PREFERENCE_NO_OPTIONS_DESC);
        if (pref == null) {
            return;
        }

        boolean optionsAvailable = visibleOptions.stream().anyMatch(Preference::isVisible);
        if (!optionsAvailable) {
            final Resources res = getResourcesForSubId();
            String emptyViewText = res.getString(R.string.wifi_calling_off_explanation,
                    res.getString(R.string.wifi_calling_off_explanation_2));
            pref.setSummary(emptyViewText);
        }
        pref.setVisible(!optionsAvailable);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean hasException = false;

        if (preference == mButtonWfcMode) {
            Log.d(TAG, "onPreferenceChange mButtonWfcMode " + newValue);
            mButtonWfcMode.setValue((String) newValue);
            final int buttonMode = Integer.valueOf((String) newValue);
            int currentWfcMode = ImsMmTelManager.WIFI_MODE_UNKNOWN;
            try {
                currentWfcMode = mImsMmTelManager.getVoWiFiModeSetting();
            } catch (IllegalArgumentException e) {
                hasException = true;
                Log.e(TAG, "onPreferenceChange: Exception", e);
            }
            if (buttonMode != currentWfcMode && !hasException) {
                try {
                    mImsMmTelManager.setVoWiFiModeSetting(buttonMode);
                    mButtonWfcMode.setSummary(getWfcModeSummary(buttonMode));
                    mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), buttonMode);

                    if (mUseWfcHomeModeForRoaming) {
                        mImsMmTelManager.setVoWiFiRoamingModeSetting(buttonMode);
                        // mButtonWfcRoamingMode.setSummary is not needed; summary is selected value
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "onPreferenceChange: Exception", e);
                }
            }
        } else if (preference == mButtonWfcRoamingMode) {
            mButtonWfcRoamingMode.setValue((String) newValue);
            final int buttonMode = Integer.valueOf((String) newValue);
            int currentMode = ImsMmTelManager.WIFI_MODE_UNKNOWN;
            try {
                currentMode = mImsMmTelManager.getVoWiFiRoamingModeSetting();
            } catch (IllegalArgumentException e) {
                hasException = true;
                Log.e(TAG, "updateWfcMode: Exception", e);
            }
            if (buttonMode != currentMode && !hasException) {
                try {
                    mImsMmTelManager.setVoWiFiRoamingModeSetting(buttonMode);
                    // mButtonWfcRoamingMode.setSummary is not needed; summary is just selected
                    // value.
                    mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), buttonMode);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "onPreferenceChange: Exception", e);
                }
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
