/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.PreferredNetworkModeContentObserver;
import com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Preference controller for "Enabled network mode"
 */
public class EnabledNetworkModePreferenceController extends
        TelephonyBasePreferenceController implements
        ListPreference.OnPreferenceChangeListener, LifecycleObserver {

    private static final String LOG_TAG = "EnabledNetworkMode";
    private PreferredNetworkModeContentObserver mPreferredNetworkModeObserver;
    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private TelephonyManager mTelephonyManager;
    private CarrierConfigManager mCarrierConfigManager;
    private PreferenceEntriesBuilder mBuilder;

    public EnabledNetworkModePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        boolean visible;
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            visible = false;
        } else if (carrierConfig == null) {
            visible = false;
        } else if (carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)
                || carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL)) {
            visible = false;
        } else if (carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            visible = false;
        } else {
            visible = true;
        }

        return visible ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        if (mPreferredNetworkModeObserver == null) {
            return;
        }
        mPreferredNetworkModeObserver.register(mContext, mSubId);
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        if (mPreferredNetworkModeObserver == null) {
            return;
        }
        mPreferredNetworkModeObserver.unregister(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ListPreference listPreference = (ListPreference) preference;

        mBuilder.setPreferenceEntries();
        mBuilder.setPreferenceValueAndSummary();

        listPreference.setEntries(mBuilder.getEntries());
        listPreference.setEntryValues(mBuilder.getEntryValues());
        listPreference.setValue(Integer.toString(mBuilder.getSelectedEntryValue()));
        listPreference.setSummary(mBuilder.getSummary());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        final int newPreferredNetworkMode = Integer.parseInt((String) object);
        final ListPreference listPreference = (ListPreference) preference;

        if (mTelephonyManager.setPreferredNetworkTypeBitmask(
                MobileNetworkUtils.getRafFromNetworkType(newPreferredNetworkMode))) {
            mBuilder.setPreferenceValueAndSummary(newPreferredNetworkMode);
            listPreference.setValue(Integer.toString(mBuilder.getSelectedEntryValue()));
            listPreference.setSummary(mBuilder.getSummary());
            return true;
        }
        return false;
    }

    public void init(Lifecycle lifecycle, int subId) {
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
        mCarrierConfigManager = mContext.getSystemService(CarrierConfigManager.class);
        mBuilder = new PreferenceEntriesBuilder(mContext, mSubId);

        if (mPreferredNetworkModeObserver == null) {
            mPreferredNetworkModeObserver = new PreferredNetworkModeContentObserver(
                    new Handler(Looper.getMainLooper()));
            mPreferredNetworkModeObserver.setPreferredNetworkModeChangedListener(
                    () -> updatePreference());
        }

        lifecycle.addObserver(this);
    }

    private void updatePreference() {
        if (mPreferenceScreen != null) {
            displayPreference(mPreferenceScreen);
        }
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    private final static class PreferenceEntriesBuilder {
        private CarrierConfigManager mCarrierConfigManager;
        private Context mContext;
        private TelephonyManager mTelephonyManager;

        private boolean mAllowed5gNetworkType;
        private boolean mIsGlobalCdma;
        private boolean mIs5gEntryDisplayed;
        private boolean mShow4gForLTE;
        private boolean mSupported5gRadioAccessFamily;
        private int mSelectedEntry;
        private int mSubId;
        private String mSummary;

        private List<String> mEntries = new ArrayList<>();
        private List<Integer> mEntriesValue = new ArrayList<>();

        enum EnabledNetworks {
            ENABLED_NETWORKS_UNKNOWN,
            ENABLED_NETWORKS_CDMA_CHOICES,
            ENABLED_NETWORKS_CDMA_NO_LTE_CHOICES,
            ENABLED_NETWORKS_CDMA_ONLY_LTE_CHOICES,
            ENABLED_NETWORKS_TDSCDMA_CHOICES,
            ENABLED_NETWORKS_EXCEPT_GSM_LTE_CHOICES,
            ENABLED_NETWORKS_EXCEPT_GSM_4G_CHOICES,
            ENABLED_NETWORKS_EXCEPT_GSM_CHOICES,
            ENABLED_NETWORKS_EXCEPT_LTE_CHOICES,
            ENABLED_NETWORKS_4G_CHOICES,
            ENABLED_NETWORKS_CHOICES,
            PREFERRED_NETWORK_MODE_CHOICES_WORLD_MODE
        }

        PreferenceEntriesBuilder(Context context, int subId) {
            this.mContext = context;
            this.mSubId = subId;

            mCarrierConfigManager = mContext.getSystemService(CarrierConfigManager.class);
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                    .createForSubscriptionId(mSubId);

            final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
            final boolean isNrEnabledFromCarrierConfig = carrierConfig != null
                    && carrierConfig.getBoolean(CarrierConfigManager.KEY_NR_ENABLED_BOOL);
            mAllowed5gNetworkType = checkSupportedRadioBitmask(
                    mTelephonyManager.getAllowedNetworkTypes(),
                    TelephonyManager.NETWORK_TYPE_BITMASK_NR);
            mSupported5gRadioAccessFamily = isNrEnabledFromCarrierConfig
                    && checkSupportedRadioBitmask(mTelephonyManager.getSupportedRadioAccessFamily(),
                    TelephonyManager.NETWORK_TYPE_BITMASK_NR);
            mIsGlobalCdma = mTelephonyManager.isLteCdmaEvdoGsmWcdmaEnabled()
                    && carrierConfig != null
                    && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
            mShow4gForLTE = carrierConfig != null && carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL);
        }

        void setPreferenceEntries() {
            clearAllEntries();
            String[] entryValues;
            int[] entryValuesInt;
            switch (getEnabledNetworkType()) {
                case ENABLED_NETWORKS_CDMA_CHOICES:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.enabled_networks_cdma_values);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 4) {
                        throw new IllegalArgumentException(
                                "ENABLED_NETWORKS_CDMA_CHOICES index error.");
                    }
                    add5gEntry(addNrToLteNetworkType(entryValuesInt[0]));
                    addLteEntry(entryValuesInt[0]);
                    add3gEntry(entryValuesInt[1]);
                    add1xEntry(entryValuesInt[2]);
                    addGlobalEntry(entryValuesInt[3]);
                    break;
                case ENABLED_NETWORKS_CDMA_NO_LTE_CHOICES:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.enabled_networks_cdma_no_lte_values);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 2) {
                        throw new IllegalArgumentException(
                                "ENABLED_NETWORKS_CDMA_NO_LTE_CHOICES index error.");
                    }
                    add3gEntry(entryValuesInt[0]);
                    add1xEntry(entryValuesInt[1]);
                    break;
                case ENABLED_NETWORKS_CDMA_ONLY_LTE_CHOICES:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.enabled_networks_cdma_only_lte_values);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 2) {
                        throw new IllegalArgumentException(
                                "ENABLED_NETWORKS_CDMA_ONLY_LTE_CHOICES index error.");
                    }
                    addLteEntry(entryValuesInt[0]);
                    addGlobalEntry(entryValuesInt[1]);
                    break;
                case ENABLED_NETWORKS_TDSCDMA_CHOICES:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.enabled_networks_tdscdma_values);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 3) {
                        throw new IllegalArgumentException(
                                "ENABLED_NETWORKS_TDSCDMA_CHOICES index error.");
                    }
                    add5gEntry(addNrToLteNetworkType(entryValuesInt[0]));
                    addLteEntry(entryValuesInt[0]);
                    add3gEntry(entryValuesInt[1]);
                    add2gEntry(entryValuesInt[2]);
                    break;
                case ENABLED_NETWORKS_EXCEPT_GSM_LTE_CHOICES:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.enabled_networks_except_gsm_lte_values);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 1) {
                        throw new IllegalArgumentException(
                                "ENABLED_NETWORKS_EXCEPT_GSM_LTE_CHOICES index error.");
                    }
                    add3gEntry(entryValuesInt[0]);
                    break;
                case ENABLED_NETWORKS_EXCEPT_GSM_4G_CHOICES:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.enabled_networks_except_gsm_values);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 2) {
                        throw new IllegalArgumentException(
                                "ENABLED_NETWORKS_EXCEPT_GSM_4G_CHOICES index error.");
                    }
                    add5gEntry(addNrToLteNetworkType(entryValuesInt[0]));
                    add4gEntry(entryValuesInt[0]);
                    add3gEntry(entryValuesInt[1]);
                    break;
                case ENABLED_NETWORKS_EXCEPT_GSM_CHOICES:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.enabled_networks_except_gsm_values);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 2) {
                        throw new IllegalArgumentException(
                                "ENABLED_NETWORKS_EXCEPT_GSM_CHOICES index error.");
                    }
                    add5gEntry(addNrToLteNetworkType(entryValuesInt[0]));
                    addLteEntry(entryValuesInt[0]);
                    add3gEntry(entryValuesInt[1]);
                    break;
                case ENABLED_NETWORKS_EXCEPT_LTE_CHOICES:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.enabled_networks_except_lte_values);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 2) {
                        throw new IllegalArgumentException(
                                "ENABLED_NETWORKS_EXCEPT_LTE_CHOICES index error.");
                    }
                    add3gEntry(entryValuesInt[0]);
                    add2gEntry(entryValuesInt[1]);
                    break;
                case ENABLED_NETWORKS_4G_CHOICES:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.enabled_networks_values);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 3) {
                        throw new IllegalArgumentException(
                                "ENABLED_NETWORKS_4G_CHOICES index error.");
                    }
                    add5gEntry(addNrToLteNetworkType(
                            entryValuesInt[0]));
                    add4gEntry(entryValuesInt[0]);
                    add3gEntry(entryValuesInt[1]);
                    add2gEntry(entryValuesInt[2]);
                    break;
                case ENABLED_NETWORKS_CHOICES:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.enabled_networks_values);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 3) {
                        throw new IllegalArgumentException("ENABLED_NETWORKS_CHOICES index error.");
                    }
                    add5gEntry(addNrToLteNetworkType(entryValuesInt[0]));
                    addLteEntry(entryValuesInt[0]);
                    add3gEntry(entryValuesInt[1]);
                    add2gEntry(entryValuesInt[2]);
                    break;
                case PREFERRED_NETWORK_MODE_CHOICES_WORLD_MODE:
                    entryValues = mContext.getResources().getStringArray(
                            R.array.preferred_network_mode_values_world_mode);
                    entryValuesInt = Stream.of(entryValues).mapToInt(Integer::parseInt).toArray();
                    if (entryValuesInt.length < 3) {
                        throw new IllegalArgumentException(
                                "PREFERRED_NETWORK_MODE_CHOICES_WORLD_MODE index error.");
                    }
                    addGlobalEntry(entryValuesInt[0]);
                    addCustomEntry(mContext.getString(R.string.network_world_mode_cdma_lte),
                            entryValuesInt[1]);
                    addCustomEntry(mContext.getString(R.string.network_world_mode_gsm_lte),
                            entryValuesInt[2]);
                    break;
                default:
                    throw new IllegalArgumentException("Not supported enabled network types.");
            }
        }

        private int getPreferredNetworkMode() {
            int networkMode = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                    TelephonyManager.DEFAULT_PREFERRED_NETWORK_MODE);
            if (!showNrList()) {
                Log.d(LOG_TAG, "Network mode :" + networkMode + " reduce NR");
                networkMode = reduceNrToLteNetworkType(networkMode);
            }
            Log.d(LOG_TAG, "getPreferredNetworkMode: " + networkMode);
            return networkMode;
        }

        private EnabledNetworks getEnabledNetworkType() {
            EnabledNetworks enabledNetworkType = EnabledNetworks.ENABLED_NETWORKS_UNKNOWN;
            final int phoneType = mTelephonyManager.getPhoneType();
            final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);

            if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                final int lteForced = android.provider.Settings.Global.getInt(
                        mContext.getContentResolver(),
                        android.provider.Settings.Global.LTE_SERVICE_FORCED + mSubId,
                        0);
                final int settingsNetworkMode = getPreferredNetworkMode();
                if (mTelephonyManager.isLteCdmaEvdoGsmWcdmaEnabled()) {
                    if (lteForced == 0) {
                        enabledNetworkType = EnabledNetworks.ENABLED_NETWORKS_CDMA_CHOICES;
                    } else {
                        switch (settingsNetworkMode) {
                            case TelephonyManagerConstants.NETWORK_MODE_CDMA_EVDO:
                            case TelephonyManagerConstants.NETWORK_MODE_CDMA_NO_EVDO:
                            case TelephonyManagerConstants.NETWORK_MODE_EVDO_NO_CDMA:
                                enabledNetworkType =
                                        EnabledNetworks.ENABLED_NETWORKS_CDMA_NO_LTE_CHOICES;
                                break;
                            case TelephonyManagerConstants.NETWORK_MODE_GLOBAL:
                            case TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                            case TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                            case TelephonyManagerConstants.NETWORK_MODE_LTE_ONLY:
                                enabledNetworkType =
                                        EnabledNetworks.ENABLED_NETWORKS_CDMA_ONLY_LTE_CHOICES;
                                break;
                            default:
                                enabledNetworkType = EnabledNetworks.ENABLED_NETWORKS_CDMA_CHOICES;
                                break;
                        }
                    }
                }
            } else if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
                if (MobileNetworkUtils.isTdscdmaSupported(mContext, mSubId)) {
                    enabledNetworkType = EnabledNetworks.ENABLED_NETWORKS_TDSCDMA_CHOICES;
                } else if (carrierConfig != null
                        && !carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)
                        && !carrierConfig.getBoolean(CarrierConfigManager.KEY_LTE_ENABLED_BOOL)) {
                    enabledNetworkType = EnabledNetworks.ENABLED_NETWORKS_EXCEPT_GSM_LTE_CHOICES;
                } else if (carrierConfig != null
                        && !carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)) {
                    enabledNetworkType = mShow4gForLTE
                            ? EnabledNetworks.ENABLED_NETWORKS_EXCEPT_GSM_4G_CHOICES
                            : EnabledNetworks.ENABLED_NETWORKS_EXCEPT_GSM_CHOICES;
                } else if (carrierConfig != null
                        && !carrierConfig.getBoolean(CarrierConfigManager.KEY_LTE_ENABLED_BOOL)) {
                    enabledNetworkType = EnabledNetworks.ENABLED_NETWORKS_EXCEPT_LTE_CHOICES;
                } else if (mIsGlobalCdma) {
                    enabledNetworkType = EnabledNetworks.ENABLED_NETWORKS_CDMA_CHOICES;
                } else {
                    enabledNetworkType = mShow4gForLTE ? EnabledNetworks.ENABLED_NETWORKS_4G_CHOICES
                            : EnabledNetworks.ENABLED_NETWORKS_CHOICES;
                }
            }
            //TODO(b/117881708): figure out what world mode is, then we can optimize code. Otherwise
            // I prefer to keep this old code
            if (MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                enabledNetworkType = EnabledNetworks.PREFERRED_NETWORK_MODE_CHOICES_WORLD_MODE;
            }

            Log.d(LOG_TAG, "enabledNetworkType: " + enabledNetworkType);
            return enabledNetworkType;
        }

        /**
         * Sets the display string for the network mode choice and selects the corresponding item
         *
         * @param networkMode the current network mode. The current mode might not be an option in
         *                   the choice list. The nearest choice is selected instead
         */
        void setPreferenceValueAndSummary(int networkMode) {
            setSelectedEntry(networkMode);
            switch (networkMode) {
                case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_WCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM:
                    setSelectedEntry(
                            TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA);
                    setSummary(R.string.network_3G);
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_WCDMA_ONLY:
                case TelephonyManagerConstants.NETWORK_MODE_GSM_UMTS:
                case TelephonyManagerConstants.NETWORK_MODE_WCDMA_PREF:
                    if (!mIsGlobalCdma) {
                        setSelectedEntry(TelephonyManagerConstants.NETWORK_MODE_WCDMA_PREF);
                        setSummary(R.string.network_3G);
                    } else {
                        setSelectedEntry(
                                TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA);
                        setSummary(R.string.network_global);
                    }
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_GSM_ONLY:
                    if (!mIsGlobalCdma) {
                        setSelectedEntry(TelephonyManagerConstants.NETWORK_MODE_GSM_ONLY);
                        setSummary(R.string.network_2G);
                    } else {
                        setSelectedEntry(
                                TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA);
                        setSummary(R.string.network_global);
                    }
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                    if (MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                        setSummary(
                                R.string.preferred_network_mode_lte_gsm_umts_summary);
                        break;
                    }
                case TelephonyManagerConstants.NETWORK_MODE_LTE_ONLY:
                case TelephonyManagerConstants.NETWORK_MODE_LTE_WCDMA:
                    if (!mIsGlobalCdma) {
                        setSelectedEntry(
                                TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA);
                        if (is5gEntryDisplayed()) {
                            setSummary(mShow4gForLTE
                                    ? R.string.network_4G_pure : R.string.network_lte_pure);
                        } else {
                            setSummary(mShow4gForLTE
                                    ? R.string.network_4G : R.string.network_lte);
                        }
                    } else {
                        setSelectedEntry(
                                TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA);
                        setSummary(R.string.network_global);
                    }
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                    if (MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                        setSummary(
                                R.string.preferred_network_mode_lte_cdma_summary);
                    } else {
                        setSelectedEntry(
                                TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO);
                        setSummary(is5gEntryDisplayed()
                                ? R.string.network_lte_pure : R.string.network_lte);
                    }
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                    setSelectedEntry(
                            TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA);
                    setSummary(R.string.network_3G);
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_CDMA_EVDO:
                case TelephonyManagerConstants.NETWORK_MODE_EVDO_NO_CDMA:
                case TelephonyManagerConstants.NETWORK_MODE_GLOBAL:
                    setSelectedEntry(TelephonyManagerConstants.NETWORK_MODE_CDMA_EVDO);
                    setSummary(R.string.network_3G);
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_CDMA_NO_EVDO:
                    setSelectedEntry(TelephonyManagerConstants.NETWORK_MODE_CDMA_NO_EVDO);
                    setSummary(R.string.network_1x);
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_ONLY:
                    setSelectedEntry(TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_ONLY);
                    setSummary(R.string.network_3G);
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
                case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    if (MobileNetworkUtils.isTdscdmaSupported(mContext, mSubId)) {
                        setSelectedEntry(TelephonyManagerConstants
                                .NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA);
                        setSummary(is5gEntryDisplayed()
                                ? R.string.network_lte_pure : R.string.network_lte);
                    } else {
                        setSelectedEntry(
                                TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA);
                        if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA
                                || mIsGlobalCdma
                                || MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                            setSummary(R.string.network_global);
                        } else {
                            if (is5gEntryDisplayed()) {
                                setSummary(mShow4gForLTE
                                        ? R.string.network_4G_pure : R.string.network_lte_pure);
                            } else {
                                setSummary(mShow4gForLTE
                                        ? R.string.network_4G : R.string.network_lte);
                            }
                        }
                    }
                    break;

                case TelephonyManagerConstants.NETWORK_MODE_NR_ONLY:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_WCDMA:
                    setSelectedEntry(
                            TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA);
                    setSummary(mContext.getString(R.string.network_5G)
                            + mContext.getString(R.string.network_recommended));
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_WCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                    setSelectedEntry(TelephonyManagerConstants
                            .NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA);
                    setSummary(mContext.getString(R.string.network_5G)
                            + mContext.getString(R.string.network_recommended));
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO:
                    setSelectedEntry(TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO);
                    setSummary(mContext.getString(R.string.network_5G)
                            + mContext.getString(R.string.network_recommended));
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA:
                    setSelectedEntry(
                            TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA);
                    if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA
                            || mIsGlobalCdma
                            || MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                        setSummary(R.string.network_global);
                    } else {
                        setSummary(mContext.getString(R.string.network_5G)
                                + mContext.getString(R.string.network_recommended));
                    }
                    break;
                default:
                    setSummary(
                            mContext.getString(R.string.mobile_network_mode_error, networkMode));
            }
        }

        /**
         * Transform LTE network mode to 5G network mode.
         *
         * @param networkType an LTE network mode without 5G.
         * @return the corresponding network mode with 5G.
         */
        private static int addNrToLteNetworkType(int networkType) {
            switch(networkType) {
                case TelephonyManagerConstants.NETWORK_MODE_LTE_ONLY:
                    return TelephonyManagerConstants.NETWORK_MODE_NR_LTE;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO:
                    return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_WCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_WCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM:
                    return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_WCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                    return TelephonyManagerConstants
                            .NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
                default:
                    return networkType; // not LTE
            }
        }

        /**
         * Transform NR5G network mode to LTE network mode.
         *
         * @param networkType an 5G network mode.
         * @return the corresponding network mode without 5G.
         */
        private static int reduceNrToLteNetworkType(int networkType) {
            switch(networkType) {
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE:
                    return TelephonyManagerConstants.NETWORK_MODE_LTE_ONLY;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO:
                    return TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_WCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_LTE_WCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM:
                    return TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_WCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA:
                    return TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                    return TelephonyManagerConstants
                            .NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA;
                default:
                    return networkType; // do nothing
            }
        }

        private void setPreferenceValueAndSummary() {
            setPreferenceValueAndSummary(getPreferredNetworkMode());
        }

        private boolean checkSupportedRadioBitmask(long supportedRadioBitmask, long targetBitmask) {
            return (targetBitmask & supportedRadioBitmask) > 0;
        }

        /**
         * Add 5G option. Only show the UI when device supported 5G and allowed 5G.
         */
        private void add5gEntry(int value) {
            boolean isNRValue = value >= TelephonyManagerConstants.NETWORK_MODE_NR_ONLY;
            if (showNrList() && isNRValue) {
                mEntries.add(mContext.getString(R.string.network_5G)
                        + mContext.getString(R.string.network_recommended));
                mEntriesValue.add(value);
                mIs5gEntryDisplayed = true;
            } else {
                mIs5gEntryDisplayed = false;
                Log.d(LOG_TAG, "Hide 5G option. "
                        + " supported5GRadioAccessFamily: " + mSupported5gRadioAccessFamily
                        + " allowed5GNetworkType: " + mAllowed5gNetworkType
                        + " isNRValue: " + isNRValue);
            }
        }

        private void addGlobalEntry(int value) {
            Log.d(LOG_TAG, "addGlobalEntry. "
                    + " supported5GRadioAccessFamily: " + mSupported5gRadioAccessFamily
                    + " allowed5GNetworkType: " + mAllowed5gNetworkType);
            mEntries.add(mContext.getString(R.string.network_global));
            if (showNrList()) {
                value = addNrToLteNetworkType(value);
            }
            mEntriesValue.add(value);
        }

        private boolean showNrList() {
            return mSupported5gRadioAccessFamily && mAllowed5gNetworkType;
        }

        /**
         * Add LTE entry. If device supported 5G, show "LTE" instead of "LTE (recommended)".
         */
        private void addLteEntry(int value) {
            if (showNrList()) {
                mEntries.add(mContext.getString(R.string.network_lte_pure));
            } else {
                mEntries.add(mContext.getString(R.string.network_lte));
            }
            mEntriesValue.add(value);
        }

        /**
         * Add 4G entry. If device supported 5G, show "4G" instead of "4G (recommended)".
         */
        private void add4gEntry(int value) {
            if (showNrList()) {
                mEntries.add(mContext.getString(R.string.network_4G_pure));
            } else {
                mEntries.add(mContext.getString(R.string.network_4G));
            }
            mEntriesValue.add(value);
        }

        private void add3gEntry(int value) {
            mEntries.add(mContext.getString(R.string.network_3G));
            mEntriesValue.add(value);
        }

        private void add2gEntry(int value) {
            mEntries.add(mContext.getString(R.string.network_2G));
            mEntriesValue.add(value);
        }

        private void add1xEntry(int value) {
            mEntries.add(mContext.getString(R.string.network_1x));
            mEntriesValue.add(value);
        }

        private void addCustomEntry(String name, int value) {
            mEntries.add(name);
            mEntriesValue.add(value);
        }

        private String[] getEntries() {
            return mEntries.toArray(new String[0]);
        }

        private void clearAllEntries() {
            mEntries.clear();
            mEntriesValue.clear();
        }

        private String[] getEntryValues() {
            final Integer [] intArr = mEntriesValue.toArray(new Integer[0]);
            return Arrays.stream(intArr)
                    .map(String::valueOf)
                    .toArray(String[]::new);
        }

        private int getSelectedEntryValue() {
            return mSelectedEntry;
        }

        private void setSelectedEntry(int value) {
            boolean isInEntriesValue = mEntriesValue.stream()
                    .anyMatch(v -> v == value);

            if (isInEntriesValue) {
                mSelectedEntry = value;
            } else if (mEntriesValue.size() > 0) {
                // if the value isn't in entriesValue, select on the first one.
                mSelectedEntry = mEntriesValue.get(0);
            } else {
                Log.e(LOG_TAG, "entriesValue is empty");
            }
        }

        private String getSummary() {
            return mSummary;
        }

        private void setSummary(int summaryResId) {
            setSummary(mContext.getString(summaryResId));
        }

        private void setSummary(String summary) {
            this.mSummary = summary;
        }

        private boolean is5gEntryDisplayed() {
            return mIs5gEntryDisplayed;
        }

    }
}
