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
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;

/**
 * Preference controller for "Enabled network mode"
 */
public class EnabledNetworkModePreferenceController extends
        TelephonyBasePreferenceController implements
        ListPreference.OnPreferenceChangeListener, LifecycleObserver {

    private CarrierConfigManager mCarrierConfigManager;
    private ContentObserver mPreferredNetworkModeObserver;
    private TelephonyManager mTelephonyManager;
    private boolean mIsGlobalCdma;
    @VisibleForTesting
    boolean mShow4GForLTE;
    private Preference mPreference;

    public EnabledNetworkModePreferenceController(Context context, String key) {
        super(context, key);
        mCarrierConfigManager = context.getSystemService(CarrierConfigManager.class);
        mPreferredNetworkModeObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                if (mPreference != null) {
                    updateState(mPreference);
                }
            }
        };
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        boolean visible;
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);
        final TelephonyManager telephonyManager = TelephonyManager
                .from(mContext).createForSubscriptionId(subId);
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
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.PREFERRED_NETWORK_MODE + mSubId), true,
                mPreferredNetworkModeObserver);
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mPreferredNetworkModeObserver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ListPreference listPreference = (ListPreference) preference;
        final int networkMode = getPreferredNetworkMode();
        updatePreferenceEntries(listPreference);
        updatePreferenceValueAndSummary(listPreference, networkMode);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        final int settingsMode = Integer.parseInt((String) object);

        if (mTelephonyManager.setPreferredNetworkType(mSubId, settingsMode)) {
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                    settingsMode);
            updatePreferenceValueAndSummary((ListPreference) preference, settingsMode);
            return true;
        }

        return false;
    }

    public void init(Lifecycle lifecycle, int subId) {
        mSubId = subId;
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        mTelephonyManager = TelephonyManager.from(mContext).createForSubscriptionId(mSubId);

        final boolean isLteOnCdma =
                mTelephonyManager.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;
        mIsGlobalCdma = isLteOnCdma
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
        mShow4GForLTE = carrierConfig != null
                ? carrierConfig.getBoolean(
                CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL)
                : false;
        lifecycle.addObserver(this);
    }

    private int getPreferredNetworkMode() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                Phone.PREFERRED_NT_MODE);
    }

    private void updatePreferenceEntries(ListPreference preference) {
        final int phoneType = mTelephonyManager.getPhoneType();
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(mSubId);
        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            final int lteForced = android.provider.Settings.Global.getInt(
                    mContext.getContentResolver(),
                    android.provider.Settings.Global.LTE_SERVICE_FORCED + mSubId,
                    0);
            final boolean isLteOnCdma = mTelephonyManager.getLteOnCdmaMode()
                    == PhoneConstants.LTE_ON_CDMA_TRUE;
            final int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mContext.getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mSubId,
                    Phone.PREFERRED_NT_MODE);
            if (isLteOnCdma) {
                if (lteForced == 0) {
                    preference.setEntries(
                            R.array.enabled_networks_cdma_choices);
                    preference.setEntryValues(
                            R.array.enabled_networks_cdma_values);
                } else {
                    switch (settingsNetworkMode) {
                        case TelephonyManager.NETWORK_MODE_CDMA_EVDO:
                        case TelephonyManager.NETWORK_MODE_CDMA_NO_EVDO:
                        case TelephonyManager.NETWORK_MODE_EVDO_NO_CDMA:
                            preference.setEntries(
                                    R.array.enabled_networks_cdma_no_lte_choices);
                            preference.setEntryValues(
                                    R.array.enabled_networks_cdma_no_lte_values);
                            break;
                        case TelephonyManager.NETWORK_MODE_GLOBAL:
                        case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO:
                        case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                        case TelephonyManager.NETWORK_MODE_LTE_ONLY:
                            preference.setEntries(
                                    R.array.enabled_networks_cdma_only_lte_choices);
                            preference.setEntryValues(
                                    R.array.enabled_networks_cdma_only_lte_values);
                            break;
                        default:
                            preference.setEntries(
                                    R.array.enabled_networks_cdma_choices);
                            preference.setEntryValues(
                                    R.array.enabled_networks_cdma_values);
                            break;
                    }
                }
            }
        } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
            if (MobileNetworkUtils.isTdscdmaSupported(mContext, mSubId)) {
                preference.setEntries(
                        R.array.enabled_networks_tdscdma_choices);
                preference.setEntryValues(
                        R.array.enabled_networks_tdscdma_values);
            } else if (carrierConfig != null
                    && !carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)
                    && !carrierConfig.getBoolean(CarrierConfigManager.KEY_LTE_ENABLED_BOOL)) {
                preference.setEntries(R.array.enabled_networks_except_gsm_lte_choices);
                preference.setEntryValues(R.array.enabled_networks_except_gsm_lte_values);
            } else if (carrierConfig != null
                    && !carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)) {
                int select = mShow4GForLTE
                        ? R.array.enabled_networks_except_gsm_4g_choices
                        : R.array.enabled_networks_except_gsm_choices;
                preference.setEntries(select);
                preference.setEntryValues(
                        R.array.enabled_networks_except_gsm_values);
            } else if (carrierConfig != null
                    && !carrierConfig.getBoolean(CarrierConfigManager.KEY_LTE_ENABLED_BOOL)) {
                preference.setEntries(
                        R.array.enabled_networks_except_lte_choices);
                preference.setEntryValues(
                        R.array.enabled_networks_except_lte_values);
            } else if (mIsGlobalCdma) {
                preference.setEntries(R.array.enabled_networks_cdma_choices);
                preference.setEntryValues(R.array.enabled_networks_cdma_values);
            } else {
                int select = mShow4GForLTE ? R.array.enabled_networks_4g_choices
                        : R.array.enabled_networks_choices;
                preference.setEntries(select);
                preference.setEntryValues(R.array.enabled_networks_values);
            }
        }
        //TODO(b/117881708): figure out what world mode is, then we can optimize code. Otherwise
        // I prefer to keep this old code
        if (MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
            preference.setEntries(
                    R.array.preferred_network_mode_choices_world_mode);
            preference.setEntryValues(
                    R.array.preferred_network_mode_values_world_mode);
        }
    }

    private void updatePreferenceValueAndSummary(ListPreference preference, int networkMode) {
        preference.setValue(Integer.toString(networkMode));
        switch (networkMode) {
            case TelephonyManager.NETWORK_MODE_TDSCDMA_WCDMA:
            case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA:
            case TelephonyManager.NETWORK_MODE_TDSCDMA_GSM:
                preference.setValue(
                        Integer.toString(TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA));
                preference.setSummary(R.string.network_3G);
                break;
            case TelephonyManager.NETWORK_MODE_WCDMA_ONLY:
            case TelephonyManager.NETWORK_MODE_GSM_UMTS:
            case TelephonyManager.NETWORK_MODE_WCDMA_PREF:
                if (!mIsGlobalCdma) {
                    preference.setValue(Integer.toString(TelephonyManager.NETWORK_MODE_WCDMA_PREF));
                    preference.setSummary(R.string.network_3G);
                } else {
                    preference.setValue(Integer.toString(TelephonyManager
                            .NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    preference.setSummary(R.string.network_global);
                }
                break;
            case TelephonyManager.NETWORK_MODE_GSM_ONLY:
                if (!mIsGlobalCdma) {
                    preference.setValue(
                            Integer.toString(TelephonyManager.NETWORK_MODE_GSM_ONLY));
                    preference.setSummary(R.string.network_2G);
                } else {
                    preference.setValue(
                            Integer.toString(TelephonyManager
                                    .NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    preference.setSummary(R.string.network_global);
                }
                break;
            case TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA:
                if (MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                    preference.setSummary(
                            R.string.preferred_network_mode_lte_gsm_umts_summary);
                    break;
                }
            case TelephonyManager.NETWORK_MODE_LTE_ONLY:
            case TelephonyManager.NETWORK_MODE_LTE_WCDMA:
                if (!mIsGlobalCdma) {
                    preference.setValue(
                            Integer.toString(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA));
                    preference.setSummary(
                            mShow4GForLTE ? R.string.network_4G : R.string.network_lte);
                } else {
                    preference.setValue(
                            Integer.toString(TelephonyManager
                                    .NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    preference.setSummary(R.string.network_global);
                }
                break;
            case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO:
                if (MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                    preference.setSummary(
                            R.string.preferred_network_mode_lte_cdma_summary);
                } else {
                    preference.setValue(
                            Integer.toString(TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO));
                    preference.setSummary(R.string.network_lte);
                }
                break;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                preference.setValue(Integer.toString(TelephonyManager
                        .NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA));
                preference.setSummary(R.string.network_3G);
                break;
            case TelephonyManager.NETWORK_MODE_CDMA_EVDO:
            case TelephonyManager.NETWORK_MODE_EVDO_NO_CDMA:
            case TelephonyManager.NETWORK_MODE_GLOBAL:
                preference.setValue(
                        Integer.toString(TelephonyManager.NETWORK_MODE_CDMA_EVDO));
                preference.setSummary(R.string.network_3G);
                break;
            case TelephonyManager.NETWORK_MODE_CDMA_NO_EVDO:
                preference.setValue(
                        Integer.toString(TelephonyManager.NETWORK_MODE_CDMA_NO_EVDO));
                preference.setSummary(R.string.network_1x);
                break;
            case TelephonyManager.NETWORK_MODE_TDSCDMA_ONLY:
                preference.setValue(
                        Integer.toString(TelephonyManager.NETWORK_MODE_TDSCDMA_ONLY));
                preference.setSummary(R.string.network_3G);
                break;
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM:
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA:
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA:
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_WCDMA:
            case TelephonyManager.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
            case TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                if (MobileNetworkUtils.isTdscdmaSupported(mContext, mSubId)) {
                    preference.setValue(
                            Integer.toString(TelephonyManager
                                    .NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA));
                    preference.setSummary(R.string.network_lte);
                } else {
                    preference.setValue(
                            Integer.toString(TelephonyManager
                                    .NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    if (mTelephonyManager.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA
                            || mIsGlobalCdma
                            || MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                        preference.setSummary(R.string.network_global);
                    } else {
                        preference.setSummary(mShow4GForLTE
                                ? R.string.network_4G : R.string.network_lte);
                    }
                }
                break;
            default:
                preference.setSummary(
                        mContext.getString(R.string.mobile_network_mode_error, networkMode));
        }
    }
}
