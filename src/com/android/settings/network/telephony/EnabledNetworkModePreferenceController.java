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

import static com.android.settings.network.telephony.EnabledNetworkModePreferenceControllerHelperKt.setAllowedNetworkTypes;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.ListPreference;
import androidx.preference.ListPreferenceDialogFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.network.telephony.NetworkModeChoicesProto.EnabledNetworks;
import com.android.settings.network.telephony.NetworkModeChoicesProto.UiOptions;
import com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Preference controller for "Enabled network mode"
 */
public class EnabledNetworkModePreferenceController extends
        TelephonyBasePreferenceController implements
        ListPreference.OnPreferenceChangeListener, LifecycleObserver,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private static final String LOG_TAG = "EnabledNetworkMode";
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private TelephonyManager mTelephonyManager;
    private CarrierConfigCache mCarrierConfigCache;
    private PreferenceEntriesBuilder mBuilder;
    private SubscriptionsChangeListener mSubscriptionsListener;
    private int mCallState = TelephonyManager.CALL_STATE_IDLE;
    private PhoneCallStateTelephonyCallback mTelephonyCallback;
    private FragmentManager mFragmentManager;
    private LifecycleOwner mViewLifecycleOwner;

    public EnabledNetworkModePreferenceController(Context context, String key) {
        super(context, key);
        mSubscriptionsListener = new SubscriptionsChangeListener(context, this);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
        if (mTelephonyCallback == null) {
            mTelephonyCallback = new PhoneCallStateTelephonyCallback();
        }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        boolean visible;

        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(subId);
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            visible = false;
        } else if (carrierConfig == null
                || !CarrierConfigManager.isConfigForIdentifiedCarrier(carrierConfig)) {
            visible = false;
        } else if (carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)
                || carrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL)) {
            visible = false;
        } else if (carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            visible = false;
        } else if (!isCallStateIdle()) {
            return AVAILABLE_UNSEARCHABLE;
        } else {
            visible = true;
        }

        return visible ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    protected boolean isCallStateIdle() {
        return mCallState == TelephonyManager.CALL_STATE_IDLE;
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mSubscriptionsListener.start();
        if (mAllowedNetworkTypesListener == null || mTelephonyCallback == null) {
            return;
        }
        mAllowedNetworkTypesListener.register(mContext, mSubId);
        mTelephonyCallback.register(mTelephonyManager, mSubId);
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mSubscriptionsListener.stop();
        if (mAllowedNetworkTypesListener == null || mTelephonyCallback == null) {
            return;
        }
        mAllowedNetworkTypesListener.unregister(mContext, mSubId);
        mTelephonyCallback.unregister();
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

        if (mBuilder == null) {
            return;
        }

        final ListPreference listPreference = (ListPreference) preference;

        mBuilder.setPreferenceEntries();
        mBuilder.setPreferenceValueAndSummary();

        listPreference.setEntries(mBuilder.getEntries());
        listPreference.setEntryValues(mBuilder.getEntryValues());
        listPreference.setValue(Integer.toString(mBuilder.getSelectedEntryValue()));
        listPreference.setSummary(mBuilder.getSummary());
        boolean listPreferenceEnabled = isCallStateIdle();
        listPreference.setEnabled(listPreferenceEnabled);
        if (!listPreferenceEnabled) {
            // If dialog is already opened when ListPreference disabled, dismiss them.
            for (Fragment fragment : mFragmentManager.getFragments()) {
                if (fragment instanceof ListPreferenceDialogFragmentCompat) {
                    ((ListPreferenceDialogFragmentCompat) fragment).dismiss();
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object object) {
        final int newPreferredNetworkMode = Integer.parseInt((String) object);
        final ListPreference listPreference = (ListPreference) preference;
        mBuilder.setPreferenceValueAndSummary(newPreferredNetworkMode);
        listPreference.setValue(Integer.toString(mBuilder.getSelectedEntryValue()));
        listPreference.setSummary(mBuilder.getSummary());

        setAllowedNetworkTypes(mTelephonyManager, mViewLifecycleOwner, newPreferredNetworkMode);
        return true;
    }

    void init(int subId, FragmentManager fragmentManager) {
        mSubId = subId;
        mFragmentManager = fragmentManager;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
        mBuilder = new PreferenceEntriesBuilder(mContext, mSubId);

        if (mAllowedNetworkTypesListener == null) {
            mAllowedNetworkTypesListener = new AllowedNetworkTypesListener(
                    mContext.getMainExecutor());
            mAllowedNetworkTypesListener.setAllowedNetworkTypesListener(
                    () -> {
                        mBuilder.updateConfig();
                        updatePreference();
                    });
        }
    }

    @Override
    public void onViewCreated(@NonNull LifecycleOwner viewLifecycleOwner) {
        mViewLifecycleOwner = viewLifecycleOwner;
    }

    private void updatePreference() {
        if (mPreferenceScreen != null) {
            displayPreference(mPreferenceScreen);
        }
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    private final class PreferenceEntriesBuilder {
        private CarrierConfigCache mCarrierConfigCache;
        private Context mContext;
        private TelephonyManager mTelephonyManager;

        private boolean mAllowed5gNetworkType;
        private boolean mIsGlobalCdma;
        private boolean mIs5gEntryDisplayed;
        private boolean mShow4gForLTE;
        private boolean mSupported5gRadioAccessFamily;
        private boolean mDisplay2gOptions;
        private boolean mDisplay3gOptions;
        private boolean mLteEnabled;
        private int mSelectedEntry;
        private int mSubId;
        private String mSummary;

        private List<String> mEntries = new ArrayList<>();
        private List<Integer> mEntriesValue = new ArrayList<>();

        PreferenceEntriesBuilder(Context context, int subId) {
            this.mContext = context;
            this.mSubId = subId;
            mCarrierConfigCache = CarrierConfigCache.getInstance(context);
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                    .createForSubscriptionId(mSubId);
            updateConfig();
        }

        public void updateConfig() {
            mTelephonyManager = mTelephonyManager.createForSubscriptionId(mSubId);
            final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(mSubId);
            final boolean flagHidePrefer3gItem = Flags.hidePrefer3gItem();
            mAllowed5gNetworkType = checkSupportedRadioBitmask(
                    mTelephonyManager.getAllowedNetworkTypesForReason(
                            TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER),
                    TelephonyManager.NETWORK_TYPE_BITMASK_NR);
            mSupported5gRadioAccessFamily = checkSupportedRadioBitmask(
                    mTelephonyManager.getSupportedRadioAccessFamily(),
                    TelephonyManager.NETWORK_TYPE_BITMASK_NR);
            if (carrierConfig != null) {
                mIsGlobalCdma = mTelephonyManager.isLteCdmaEvdoGsmWcdmaEnabled()
                        && carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
                mShow4gForLTE = carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL);
                mDisplay2gOptions = carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_PREFER_2G_BOOL);

                if (flagHidePrefer3gItem) {
                    mDisplay3gOptions = carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_PREFER_3G_VISIBILITY_BOOL);
                } else {
                    mDisplay3gOptions = getResourcesForSubId().getBoolean(
                            R.bool.config_display_network_mode_3g_option);

                    int[] carriersWithout3gMenu = getResourcesForSubId().getIntArray(
                            R.array.network_mode_3g_deprecated_carrier_id);
                    if ((carriersWithout3gMenu != null) && (carriersWithout3gMenu.length > 0)) {
                        SubscriptionManager sm = mContext.getSystemService(
                                SubscriptionManager.class);
                        SubscriptionInfo subInfo = sm.getActiveSubscriptionInfo(mSubId);
                        if (subInfo != null) {
                            int carrierId = subInfo.getCarrierId();

                            for (int idx = 0; idx < carriersWithout3gMenu.length; idx++) {
                                if (carrierId == carriersWithout3gMenu[idx]) {
                                    mDisplay3gOptions = false;
                                    break;
                                }
                            }
                        }
                    }
                }

                mLteEnabled = carrierConfig.getBoolean(CarrierConfigManager.KEY_LTE_ENABLED_BOOL);
            }
            Log.d(LOG_TAG, "PreferenceEntriesBuilder: subId" + mSubId
                    + " ,Supported5gRadioAccessFamily :" + mSupported5gRadioAccessFamily
                    + " ,mAllowed5gNetworkType :" + mAllowed5gNetworkType
                    + " ,IsGlobalCdma :" + mIsGlobalCdma
                    + " ,Display2gOptions:" + mDisplay2gOptions
                    + " ,Display3gOptions:" + mDisplay3gOptions
                    + " ,Display4gOptions" + mLteEnabled
                    + " ,Show4gForLTE :" + mShow4gForLTE);
        }

        void setPreferenceEntries() {
            mTelephonyManager = mTelephonyManager.createForSubscriptionId(mSubId);

            clearAllEntries();
            UiOptions.Builder uiOptions = UiOptions.newBuilder();
            uiOptions.setType(getEnabledNetworkType());
            switch (uiOptions.getType()) {
                case ENABLED_NETWORKS_CDMA_CHOICES:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_cdma_values)
                            .addFormat(UiOptions.PresentFormat.add5gAndLteEntry)
                            .addFormat(UiOptions.PresentFormat.add3gEntry)
                            .addFormat(UiOptions.PresentFormat.add1xEntry)
                            .addFormat(UiOptions.PresentFormat.addGlobalEntry);
                    break;
                case ENABLED_NETWORKS_CDMA_NO_LTE_CHOICES:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_cdma_no_lte_values)
                            .addFormat(UiOptions.PresentFormat.add3gEntry)
                            .addFormat(UiOptions.PresentFormat.add1xEntry);
                    break;
                case ENABLED_NETWORKS_CDMA_ONLY_LTE_CHOICES:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_cdma_only_lte_values)
                            .addFormat(UiOptions.PresentFormat.addLteEntry)
                            .addFormat(UiOptions.PresentFormat.addGlobalEntry);
                    break;
                case ENABLED_NETWORKS_TDSCDMA_CHOICES:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_tdscdma_values)
                            .addFormat(UiOptions.PresentFormat.add5gAndLteEntry)
                            .addFormat(UiOptions.PresentFormat.add3gEntry)
                            .addFormat(UiOptions.PresentFormat.add2gEntry);
                    break;
                case ENABLED_NETWORKS_EXCEPT_GSM_LTE_CHOICES:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_except_gsm_lte_values)
                            .addFormat(UiOptions.PresentFormat.add3gEntry);
                    break;
                case ENABLED_NETWORKS_EXCEPT_GSM_4G_CHOICES:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_except_gsm_values)
                            .addFormat(UiOptions.PresentFormat.add5gAnd4gEntry)
                            .addFormat(UiOptions.PresentFormat.add3gEntry);
                    break;
                case ENABLED_NETWORKS_EXCEPT_GSM_CHOICES:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_except_gsm_values)
                            .addFormat(UiOptions.PresentFormat.add5gAndLteEntry)
                            .addFormat(UiOptions.PresentFormat.add3gEntry);
                    break;
                case ENABLED_NETWORKS_EXCEPT_LTE_CHOICES:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_except_lte_values)
                            .addFormat(UiOptions.PresentFormat.add3gEntry)
                            .addFormat(UiOptions.PresentFormat.add2gEntry);
                    break;
                case ENABLED_NETWORKS_4G_CHOICES:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_values)
                            .addFormat(UiOptions.PresentFormat.add5gAnd4gEntry)
                            .addFormat(UiOptions.PresentFormat.add3gEntry)
                            .addFormat(UiOptions.PresentFormat.add2gEntry);
                    break;
                case ENABLED_NETWORKS_CHOICES:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_values)
                            .addFormat(UiOptions.PresentFormat.add5gAndLteEntry)
                            .addFormat(UiOptions.PresentFormat.add3gEntry)
                            .addFormat(UiOptions.PresentFormat.add2gEntry);
                    break;
                case PREFERRED_NETWORK_MODE_CHOICES_WORLD_MODE:
                    uiOptions = uiOptions
                            .setChoices(R.array.preferred_network_mode_values_world_mode)
                            .addFormat(UiOptions.PresentFormat.addGlobalEntry)
                            .addFormat(UiOptions.PresentFormat.addWorldModeCdmaEntry)
                            .addFormat(UiOptions.PresentFormat.addWorldModeGsmEntry);
                    break;
                case ENABLED_NETWORKS_4G_CHOICES_EXCEPT_GSM_3G:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_except_gsm_3g_values)
                            .addFormat(UiOptions.PresentFormat.add5gAnd4gEntry);
                    break;
                case ENABLED_NETWORKS_CHOICES_EXCEPT_GSM_3G:
                    uiOptions = uiOptions
                            .setChoices(R.array.enabled_networks_values)
                            .addFormat(UiOptions.PresentFormat.add5gAndLteEntry);
                    break;
                default:
                    throw new IllegalArgumentException("Not supported enabled network types.");
            }

            String[] entryValues = getResourcesForSubId().getStringArray(uiOptions.getChoices());
            final int[] entryValuesInt = Stream.of(entryValues)
                    .mapToInt(Integer::parseInt).toArray();
            final List<UiOptions.PresentFormat> formatList = uiOptions.getFormatList();
            if (entryValuesInt.length < formatList.size()) {
                throw new IllegalArgumentException(
                        uiOptions.getType().name() + " index error.");
            }
            // Compose options based on given values and formats.
            IntStream.range(0, formatList.size()).forEach(entryIndex -> {
                switch (formatList.get(entryIndex)) {
                    case add1xEntry:
                        if (mDisplay2gOptions) {
                            add1xEntry(entryValuesInt[entryIndex]);
                        }
                        break;
                    case add2gEntry:
                        if (mDisplay2gOptions) {
                            add2gEntry(entryValuesInt[entryIndex]);
                        }
                        break;
                    case add3gEntry:
                        if (mDisplay3gOptions) {
                            add3gEntry(entryValuesInt[entryIndex]);
                        }
                        break;
                    case addGlobalEntry:
                        addGlobalEntry(entryValuesInt[entryIndex]);
                        break;
                    case addWorldModeCdmaEntry:
                        addCustomEntry(
                                getResourcesForSubId().getString(
                                        R.string.network_world_mode_cdma_lte),
                                entryValuesInt[entryIndex]);
                        break;
                    case addWorldModeGsmEntry:
                        addCustomEntry(
                                getResourcesForSubId().getString(
                                        R.string.network_world_mode_gsm_lte),
                                entryValuesInt[entryIndex]);
                        break;
                    case add4gEntry:
                        add4gEntry(entryValuesInt[entryIndex]);
                        break;
                    case addLteEntry:
                        addLteEntry(entryValuesInt[entryIndex]);
                        break;
                    case add5gEntry:
                        add5gEntry(addNrToLteNetworkType(entryValuesInt[entryIndex]));
                        break;
                    case add5gAnd4gEntry:
                        add5gEntry(addNrToLteNetworkType(entryValuesInt[entryIndex]));
                        add4gEntry(entryValuesInt[entryIndex]);
                        break;
                    case add5gAndLteEntry:
                        add5gEntry(addNrToLteNetworkType(entryValuesInt[entryIndex]));
                        addLteEntry(entryValuesInt[entryIndex]);
                        break;
                    default:
                        throw new IllegalArgumentException("Not supported ui options format.");
                }
            });
        }

        private int getPreferredNetworkMode() {
            int networkMode = MobileNetworkUtils.getNetworkTypeFromRaf(
                    (int) mTelephonyManager.getAllowedNetworkTypesForReason(
                            TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER));
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
                } else if (!mDisplay2gOptions && !mDisplay3gOptions) {
                    enabledNetworkType = mShow4gForLTE
                            ? EnabledNetworks.ENABLED_NETWORKS_4G_CHOICES_EXCEPT_GSM_3G
                            : EnabledNetworks.ENABLED_NETWORKS_CHOICES_EXCEPT_GSM_3G;
                } else if (!mDisplay2gOptions && !mLteEnabled) {
                    enabledNetworkType = EnabledNetworks.ENABLED_NETWORKS_EXCEPT_GSM_LTE_CHOICES;
                } else if (!mDisplay2gOptions) {
                    enabledNetworkType = mShow4gForLTE
                            ? EnabledNetworks.ENABLED_NETWORKS_EXCEPT_GSM_4G_CHOICES
                            : EnabledNetworks.ENABLED_NETWORKS_EXCEPT_GSM_CHOICES;
                } else if (!mLteEnabled) {
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

            if (phoneType == TelephonyManager.PHONE_TYPE_NONE) {
                Log.d(LOG_TAG, "phoneType: PHONE_TYPE_NONE");
                enabledNetworkType = mShow4gForLTE
                        ? EnabledNetworks.ENABLED_NETWORKS_4G_CHOICES_EXCEPT_GSM_3G
                        : EnabledNetworks.ENABLED_NETWORKS_CHOICES_EXCEPT_GSM_3G;
            }

            Log.d(LOG_TAG, "enabledNetworkType: " + enabledNetworkType);
            return enabledNetworkType;
        }

        /**
         * Sets the display string for the network mode choice and selects the corresponding item
         *
         * @param networkMode the current network mode. The current mode might not be an option in
         *                    the choice list. The nearest choice is selected instead
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
                    setSummary(getResourcesForSubId().getString(R.string.network_5G_recommended));
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_WCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA:
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                    setSelectedEntry(TelephonyManagerConstants
                            .NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA);
                    setSummary(getResourcesForSubId().getString(R.string.network_5G_recommended));
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO:
                    setSelectedEntry(TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO);
                    setSummary(getResourcesForSubId().getString(R.string.network_5G_recommended));
                    break;
                case TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA:
                    setSelectedEntry(
                            TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA);
                    if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA
                            || mIsGlobalCdma
                            || MobileNetworkUtils.isWorldMode(mContext, mSubId)) {
                        setSummary(R.string.network_global);
                    } else {
                        setSummary(getResourcesForSubId().getString(
                                R.string.network_5G_recommended));
                    }
                    break;
                default:
                    setSummary(
                            getResourcesForSubId().getString(
                                    R.string.mobile_network_mode_error, networkMode));
            }
        }

        /**
         * Transform LTE network mode to 5G network mode.
         *
         * @param networkType an LTE network mode without 5G.
         * @return the corresponding network mode with 5G.
         */
        private int addNrToLteNetworkType(int networkType) {
            switch (networkType) {
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
        private int reduceNrToLteNetworkType(int networkType) {
            switch (networkType) {
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
                mEntries.add(getResourcesForSubId().getString(R.string.network_5G_recommended));
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
            mEntries.add(getResourcesForSubId().getString(R.string.network_global));
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
                mEntries.add(getResourcesForSubId().getString(R.string.network_lte_pure));
            } else {
                mEntries.add(getResourcesForSubId().getString(R.string.network_lte));
            }
            mEntriesValue.add(value);
        }

        /**
         * Add 4G entry. If device supported 5G, show "4G" instead of "4G (recommended)".
         */
        private void add4gEntry(int value) {
            if (showNrList()) {
                mEntries.add(getResourcesForSubId().getString(R.string.network_4G_pure));
            } else {
                mEntries.add(getResourcesForSubId().getString(R.string.network_4G));
            }
            mEntriesValue.add(value);
        }

        private void add3gEntry(int value) {
            mEntries.add(getResourcesForSubId().getString(R.string.network_3G));
            mEntriesValue.add(value);
        }

        private void add2gEntry(int value) {
            mEntries.add(getResourcesForSubId().getString(R.string.network_2G));
            mEntriesValue.add(value);
        }

        private void add1xEntry(int value) {
            mEntries.add(getResourcesForSubId().getString(R.string.network_1x));
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
            final Integer[] intArr = mEntriesValue.toArray(new Integer[0]);
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
            setSummary(getResourcesForSubId().getString(summaryResId));
        }

        private void setSummary(String summary) {
            this.mSummary = summary;
        }

        private boolean is5gEntryDisplayed() {
            return mIs5gEntryDisplayed;
        }

    }

    @VisibleForTesting
    class PhoneCallStateTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {

        private TelephonyManager mTelephonyManager;

        @Override
        public void onCallStateChanged(int state) {
            Log.d(LOG_TAG, "onCallStateChanged:" + state);
            mCallState = state;
            mBuilder.updateConfig();
            updatePreference();
        }

        public void register(TelephonyManager telephonyManager, int subId) {
            mTelephonyManager = telephonyManager;

            // assign current call state so that it helps to show correct preference state even
            // before first onCallStateChanged() by initial registration.
            if (Flags.enforceTelephonyFeatureMappingForPublicApis()) {
                try {
                    mCallState = mTelephonyManager.getCallState(subId);
                } catch (UnsupportedOperationException e) {
                    // Device doesn't support FEATURE_TELEPHONY_CALLING
                    mCallState = TelephonyManager.CALL_STATE_IDLE;
                }
            } else {
                mCallState = mTelephonyManager.getCallState(subId);
            }
            mTelephonyManager.registerTelephonyCallback(
                    mContext.getMainExecutor(), mTelephonyCallback);
        }

        public void unregister() {
            mCallState = TelephonyManager.CALL_STATE_IDLE;
            if (mTelephonyManager != null) {
                mTelephonyManager.unregisterTelephonyCallback(this);
            }
        }
    }

    @VisibleForTesting
    PhoneCallStateTelephonyCallback getTelephonyCallback() {
        return mTelephonyCallback;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }

    @Override
    public void onSubscriptionsChanged() {
        if (mBuilder != null) {
            mBuilder.updateConfig();
        }
    }
}
