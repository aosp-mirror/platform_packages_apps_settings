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
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller for "Enabled network mode"
 */
public class EnabledNetworkModePreferenceController extends
        TelephonyBasePreferenceController implements
        ListPreference.OnPreferenceChangeListener, LifecycleObserver {

    private static final String LOG_TAG = "EnabledNetworkMode";
    private CarrierConfigManager mCarrierConfigManager;
    private ContentObserver mPreferredNetworkModeObserver;
    private TelephonyManager mTelephonyManager;
    private boolean mIsGlobalCdma;
    @VisibleForTesting
    boolean mShow4GForLTE;
    private Preference mPreference;
    @VisibleForTesting
    boolean mDisplay5gList = false;

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

        final long supportedRadioBitmask = mTelephonyManager.getSupportedRadioAccessFamily();
        mDisplay5gList = checkSupportedRadioBitmask(
                supportedRadioBitmask, mTelephonyManager.NETWORK_TYPE_BITMASK_NR);

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

        if (mDisplay5gList) {
            add5gListItem(preference);
        }
    }

    @VisibleForTesting
    boolean checkSupportedRadioBitmask(long supportedRadioBitmask, long targetBitmask) {
        Log.d(LOG_TAG, "supportedRadioBitmask: " + supportedRadioBitmask);
        if ((targetBitmask & supportedRadioBitmask) > 0) {
            return true;
        }
        return false;
    }

    /***
     * Preferred network list add 5G item.
     *
     * @string/enabled_networks_cdma_choices
     *         Before            |        After
     * @string/network_lte   , 8 |@string/network_5G + @string/network_recommended , 25
     * @string/network_3G    , 4 |@string/network_lte_pure, 8
     * @string/network_1x    , 5 |@string/network_3G      , 4
     * @string/network_global, 10|@string/network_1x      , 5
     *                           |@string/network_global  , 27
     *
     * @string/enabled_networks_cdma_only_lte_choices
     *         Before            |        After
     * @string/network_lte   , 8 |@string/network_5G + @string/network_recommended , 25
     * @string/network_global, 10|@string/network_lte_pure, 8
     *                           |@string/network_global  , 27
     *
     * @string/enabled_networks_tdscdma_choices
     *         Before         |        After
     * @string/network_lte, 22|@string/network_5G + @string/network_recommended , 33
     * @string/network_3G , 18|@string/network_lte_pure, 22
     * @string/network_2G , 1 |@string/network_3G      , 18
     *                        |@string/network_2G      , 1
     *
     * @string/enabled_networks_except_gsm_4g_choices
     *         Before         |        After
     * @string/network_4G , 9 |@string/network_5G + @string/network_recommended , 26
     * @string/network_3G , 0 |@string/network_4G_pure , 9
     *                        |@string/network_3G      , 0
     *
     * @string/enabled_networks_except_gsm_choices
     *         Before         |        After
     * @string/network_lte, 9 |@string/network_5G + @string/network_recommended , 26
     * @string/network_3G , 0 |@string/network_lte_pure, 9
     *                        |@string/network_3G      , 0
     *
     * @string/enabled_networks_4g_choices
     *         Before         |        After
     * @string/network_4G , 9 |@string/network_5G + @string/network_recommended , 26
     * @string/network_3G , 0 |@string/network_4G_pure , 9
     * @string/network_2G , 1 |@string/network_3G      , 0
     *                        |@string/network_2G      , 1
     *
     * @string/enabled_networks_choices
     *         Before         |        After
     * @string/network_lte, 9 |@string/network_5G + @string/network_recommended , 26
     * @string/network_3G , 0 |@string/network_lte_pure, 9
     * @string/network_2G , 1 |@string/network_3G      , 0
     *                        |@string/network_2G      , 1
     *
     * @string/preferred_network_mode_choices_world_mode
     *         Before         |        After
     * "Global"           , 10|@string/network_global  , 27
     * "LTE / CDMA"       , 8 |"LTE / CDMA"            , 8
     * "LTE / GSM / UMTS" , 9 |"LTE / GSM / UMTS"      , 9
     */
    @VisibleForTesting
    void add5gListItem(ListPreference preference) {
        final CharSequence[] oldEntries = preference.getEntries();
        final CharSequence[] oldEntryValues = preference.getEntryValues();
        List<CharSequence> newEntries = new ArrayList<>();
        List<CharSequence> newEntryValues = new ArrayList<>();

        CharSequence oldEntry;
        CharSequence oldEntryValue;
        CharSequence new5gEntry;
        CharSequence new5gEntryValue;

        for (int i = 0; i < oldEntries.length; i++) {
            oldEntry = oldEntries[i];
            oldEntryValue = oldEntryValues[i];
            new5gEntry = "";
            new5gEntryValue = "";

            if (mContext.getString(R.string.network_lte).equals(oldEntry)) {
                oldEntry = mContext.getString(R.string.network_lte_pure);
                new5gEntry = mContext.getString(R.string.network_5G)
                        + mContext.getString(R.string.network_recommended);
                new5gEntryValue = transformLteEntryValueTo5gEntryValue(oldEntryValue);
            } else if (mContext.getString(R.string.network_4G).equals(oldEntry)) {
                oldEntry = mContext.getString(R.string.network_4G_pure);
                new5gEntry = mContext.getString(R.string.network_5G)
                        + mContext.getString(R.string.network_recommended);
                new5gEntryValue = transformLteEntryValueTo5gEntryValue(oldEntryValue);
            } else if (mContext.getString(R.string.network_global).equals(oldEntry)) {
                //oldEntry: network_global
                //oldEntryValue: TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA
                oldEntryValue = Integer.toString(
                        TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA);
            }

            if (!TextUtils.isEmpty(new5gEntry)) {
                newEntries.add(new5gEntry);
                newEntryValues.add(new5gEntryValue);
            }
            newEntries.add(oldEntry);
            newEntryValues.add(oldEntryValue);
        }

        preference.setEntries(newEntries.toArray(new CharSequence[newEntries.size()]));
        preference.setEntryValues(newEntryValues.toArray(new CharSequence[newEntryValues.size()]));
    }

    /**
     * LTE network mode transform to 5G network mode.
     *
     * @param networkMode this is LTE network mode.
     * @return 5G network mode.
     */
    private CharSequence transformLteEntryValueTo5gEntryValue(CharSequence networkMode) {
        int networkModeInt = Integer.valueOf(networkMode.toString());
        return Integer.toString(addNrToNetworkType(networkModeInt));
    }

    private int addNrToNetworkType(int networkType) {
        long networkTypeBitmasks = RadioAccessFamily.getRafFromNetworkType(networkType);
        networkTypeBitmasks |= mTelephonyManager.NETWORK_TYPE_BITMASK_NR;
        return RadioAccessFamily.getNetworkTypeFromRaf((int) networkTypeBitmasks);
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
                    preference.setSummary(
                            mDisplay5gList ? R.string.network_lte_pure : R.string.network_lte);
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
                    preference.setSummary(
                            mDisplay5gList ? R.string.network_lte_pure : R.string.network_lte);
                } else {
                    preference.setValue(
                            Integer.toString(TelephonyManager
                                    .NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    if (mTelephonyManager.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA
                            || mIsGlobalCdma
                            || MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                        preference.setSummary(R.string.network_global);
                    } else {
                        if (mDisplay5gList) {
                            preference.setSummary(mShow4GForLTE
                                    ? R.string.network_4G_pure : R.string.network_lte_pure);
                        } else {
                            preference.setSummary(mShow4GForLTE
                                    ? R.string.network_4G : R.string.network_lte);
                        }
                    }
                }
                break;
            case TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO:
            case TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                preference.setValue(Integer.toString(networkMode));
                preference.setSummary(mContext.getString(R.string.network_5G)
                        + mContext.getString(R.string.network_recommended));
                break;
            case TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA:
                preference.setValue(
                        Integer.toString(TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA));
                if (mTelephonyManager.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA
                        || mIsGlobalCdma
                        || MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                    preference.setSummary(R.string.network_global);
                } else {
                    preference.setSummary(mContext.getString(R.string.network_5G)
                            + mContext.getString(R.string.network_recommended));
                }
                break;
            default:
                preference.setSummary(
                        mContext.getString(R.string.mobile_network_mode_error, networkMode));
        }
    }
}
