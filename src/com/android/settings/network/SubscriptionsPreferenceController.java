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

package com.android.settings.network;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.android.settings.network.MobileIconGroupExtKt.getSummaryForSub;
import static com.android.settings.network.MobileIconGroupExtKt.maybeToHtml;
import static com.android.settings.network.telephony.MobileNetworkUtils.NO_CELL_DATA_TYPE_ICON;
import static com.android.settingslib.mobile.MobileMappings.getIconKey;
import static com.android.settingslib.mobile.MobileMappings.mapIconSets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.UserManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.network.telephony.DataConnectivityListener;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.SignalStrengthListener;
import com.android.settings.network.telephony.TelephonyDisplayInfoListener;
import com.android.settings.widget.MutableGearPreference;
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.settingslib.SignalIcon.MobileIconGroup;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.mobile.MobileMappings;
import com.android.settingslib.mobile.MobileMappings.Config;
import com.android.settingslib.mobile.TelephonyIcons;
import com.android.settingslib.net.SignalStrengthUtil;
import com.android.wifitrackerlib.WifiEntry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This controller manages preference with data subscription information and make its state
 * display on preference.
 */
public class SubscriptionsPreferenceController extends AbstractPreferenceController implements
        LifecycleObserver, SubscriptionsChangeListener.SubscriptionsChangeListenerClient,
        MobileDataEnabledListener.Client, DataConnectivityListener.Client,
        SignalStrengthListener.Callback, TelephonyDisplayInfoListener.Callback,
        TelephonyCallback.CarrierNetworkListener {
    private static final String TAG = "SubscriptionsPrefCntrlr";

    private UpdateListener mUpdateListener;
    private String mPreferenceGroupKey;
    private PreferenceGroup mPreferenceGroup;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private SubscriptionsChangeListener mSubscriptionsListener;
    private MobileDataEnabledListener mDataEnabledListener;
    private DataConnectivityListener mConnectivityListener;
    private SignalStrengthListener mSignalStrengthListener;
    private TelephonyDisplayInfoListener mTelephonyDisplayInfoListener;
    private WifiPickerTrackerHelper mWifiPickerTrackerHelper;
    private final WifiManager mWifiManager;
    private boolean mCarrierNetworkChangeMode;

    @VisibleForTesting
    final BroadcastReceiver mConnectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                mConfig = mSubsPrefCtrlInjector.getConfig(mContext);
                update();
            } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                update();
            }
        }
    };

    // Map of subscription id to Preference
    private Map<Integer, Preference> mSubscriptionPreferences;
    private int mStartOrder;
    private MutableGearPreference mSubsGearPref;
    private Config mConfig = null;
    private SubsPrefCtrlInjector mSubsPrefCtrlInjector;
    private TelephonyDisplayInfo mTelephonyDisplayInfo =
            new TelephonyDisplayInfo(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE);

    /**
     * This interface lets a parent of this class know that some change happened - this could
     * either be because overall availability changed, or because we've added/removed/updated some
     * preferences.
     */
    public interface UpdateListener {
        void onChildrenUpdated();
    }

    /**
     * @param context            the context for the UI where we're placing these preferences
     * @param lifecycle          for listening to lifecycle events for the UI
     * @param updateListener     called to let our parent controller know that our availability has
     *                           changed, or that one or more of the preferences we've placed in the
     *                           PreferenceGroup has changed
     * @param preferenceGroupKey the key used to lookup the PreferenceGroup where Preferences will
     *                           be placed
     * @param startOrder         the order that should be given to the first Preference placed into
     *                           the PreferenceGroup; the second will use startOrder+1, third will
     *                           use startOrder+2, etc. - this is useful for when the parent wants
     *                           to have other preferences in the same PreferenceGroup and wants
     *                           a specific ordering relative to this controller's prefs.
     */
    public SubscriptionsPreferenceController(Context context, Lifecycle lifecycle,
            UpdateListener updateListener, String preferenceGroupKey, int startOrder) {
        super(context);
        mUpdateListener = updateListener;
        mPreferenceGroupKey = preferenceGroupKey;
        mStartOrder = startOrder;
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mWifiManager = context.getSystemService(WifiManager.class);
        mSubscriptionPreferences = new ArrayMap<>();
        mSubscriptionsListener = new SubscriptionsChangeListener(context, this);
        mDataEnabledListener = new MobileDataEnabledListener(context, this);
        mConnectivityListener = new DataConnectivityListener(context, this);
        mSignalStrengthListener = new SignalStrengthListener(context, this);
        mTelephonyDisplayInfoListener = new TelephonyDisplayInfoListener(context, this);
        lifecycle.addObserver(this);
        mSubsPrefCtrlInjector = createSubsPrefCtrlInjector();
        mConfig = mSubsPrefCtrlInjector.getConfig(mContext);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mContext.registerReceiver(mConnectionChangeReceiver, filter);
    }

    private void unRegisterReceiver() {
        if (mConnectionChangeReceiver != null) {
            mContext.unregisterReceiver(mConnectionChangeReceiver);
        }
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mSubscriptionsListener.start();
        mDataEnabledListener.start(mSubsPrefCtrlInjector.getDefaultDataSubscriptionId());
        mConnectivityListener.start();
        mSignalStrengthListener.resume();
        mTelephonyDisplayInfoListener.resume();
        registerReceiver();
        update();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mSubscriptionsListener.stop();
        mDataEnabledListener.stop();
        mConnectivityListener.stop();
        mSignalStrengthListener.pause();
        mTelephonyDisplayInfoListener.pause();
        unRegisterReceiver();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceGroup = screen.findPreference(mPreferenceGroupKey);
        update();
    }

    private void update() {
        if (mPreferenceGroup == null) {
            return;
        }
        if (!isAvailable()) {
            if (mSubsGearPref != null) {
                mPreferenceGroup.removePreference(mSubsGearPref);
            }
            for (Preference pref : mSubscriptionPreferences.values()) {
                mPreferenceGroup.removePreference(pref);
            }

            mSubscriptionPreferences.clear();
            mSignalStrengthListener.updateSubscriptionIds(Collections.emptySet());
            mTelephonyDisplayInfoListener.updateSubscriptionIds(Collections.emptySet());
            mUpdateListener.onChildrenUpdated();
            return;
        }

        // Prefer using the currently active sub
        SubscriptionInfo subInfoCandidate = mSubscriptionManager.getActiveSubscriptionInfo(
                SubscriptionManager.getActiveDataSubscriptionId());
        SubscriptionInfo subInfo = mSubscriptionManager.isSubscriptionVisible(subInfoCandidate)
                ? subInfoCandidate : mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (subInfo == null) {
            mPreferenceGroup.removeAll();
            return;
        }
        if (mSubsGearPref == null) {
            mPreferenceGroup.removeAll();
            mSubsGearPref = new MutableGearPreference(mContext, null);
            mSubsGearPref.setOnPreferenceClickListener(preference -> {
                connectCarrierNetwork();
                return true;
            });
        }

        mSubsGearPref.setOnGearClickListener(p ->
                MobileNetworkUtils.launchMobileNetworkSettings(mContext, subInfo));

        if (!(mContext.getSystemService(UserManager.class)).isAdminUser()) {
            mSubsGearPref.setGearEnabled(false);
        }

        mSubsGearPref.setTitle(SubscriptionUtil.getUniqueSubscriptionDisplayName(
                subInfo, mContext));
        mSubsGearPref.setOrder(mStartOrder);
        mSubsGearPref.setSummary(getMobilePreferenceSummary(subInfo.getSubscriptionId()));
        mSubsGearPref.setIcon(getIcon(subInfo.getSubscriptionId()));
        mPreferenceGroup.addPreference(mSubsGearPref);

        final Set<Integer> activeDataSubIds = new ArraySet<>();
        activeDataSubIds.add(subInfo.getSubscriptionId());
        mSignalStrengthListener.updateSubscriptionIds(activeDataSubIds);
        mTelephonyDisplayInfoListener.updateSubscriptionIds(activeDataSubIds);
        mUpdateListener.onChildrenUpdated();
    }

    /**@return {@code true} if subId is the default data sub. **/
    private boolean isDds(int subId) {
        SubscriptionInfo info = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        return info != null && info.getSubscriptionId() == subId;
    }

    private CharSequence getMobilePreferenceSummary(int subId) {
        final TelephonyManager tmForSubId = mTelephonyManager.createForSubscriptionId(subId);
        boolean isDds = isDds(subId);
        if (!tmForSubId.isDataEnabled() && isDds) {
            return mContext.getString(R.string.mobile_data_off_summary);
        }
        final ServiceState serviceState = tmForSubId.getServiceState();
        final NetworkRegistrationInfo regInfo = (serviceState == null)
                ? null
                : serviceState.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_PS,
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN);

        final boolean isDataInService = (regInfo == null)
                ? false
                : regInfo.isRegistered();
        final boolean isCarrierNetworkActive = isCarrierNetworkActive();
        String result = mSubsPrefCtrlInjector.getNetworkType(mContext, mConfig,
                mTelephonyDisplayInfo, subId, isCarrierNetworkActive, mCarrierNetworkChangeMode);
        if (mSubsPrefCtrlInjector.isActiveCellularNetwork(mContext) || isCarrierNetworkActive) {
            String connectionState = mContext.getString(isDds
                    ? R.string.mobile_data_connection_active
                    : R.string.mobile_data_temp_connection_active);
            if (result.isEmpty()) {
                return connectionState;
            } else {
                result = mContext.getString(
                        R.string.preference_summary_default_combination, connectionState, result);
            }
        } else if (!isDataInService) {
            return mContext.getString(R.string.mobile_data_no_connection);
        }
        return maybeToHtml(result);
    }

    @VisibleForTesting
    Drawable getIcon(int subId) {
        final TelephonyManager tmForSubId = mTelephonyManager.createForSubscriptionId(subId);
        final SignalStrength strength = tmForSubId.getSignalStrength();
        int level = (strength == null) ? 0 : strength.getLevel();
        int numLevels = SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
        boolean isCarrierNetworkActive = isCarrierNetworkActive();
        if (isCarrierNetworkActive) {
            level = getCarrierNetworkLevel();
            numLevels = WifiEntry.WIFI_LEVEL_MAX + 1;
        } else if (shouldInflateSignalStrength(subId)) {
            level += 1;
            numLevels += 1;
        }

        Drawable icon = mContext.getDrawable(R.drawable.ic_signal_strength_zero_bar_no_internet);

        final ServiceState serviceState = tmForSubId.getServiceState();
        final NetworkRegistrationInfo regInfo = (serviceState == null)
                ? null
                : serviceState.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_PS,
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN);

        final boolean isDataInService = (regInfo == null)
                ? false
                : regInfo.isRegistered();
        final boolean isVoiceInService = (serviceState == null)
                ? false
                : (serviceState.getState() == ServiceState.STATE_IN_SERVICE);
        final boolean isDataEnabled = tmForSubId.isDataEnabled()
                // non-Dds but auto data switch feature is enabled
                || (!isDds(subId) && tmForSubId.isMobileDataPolicyEnabled(
                        TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH));
        if (isDataInService || isVoiceInService || isCarrierNetworkActive) {
            icon = mSubsPrefCtrlInjector.getIcon(mContext, level, numLevels, !isDataEnabled,
                    mCarrierNetworkChangeMode);
        }

        final boolean isActiveCellularNetwork =
                mSubsPrefCtrlInjector.isActiveCellularNetwork(mContext);
        if (isActiveCellularNetwork || isCarrierNetworkActive) {
            icon.setTint(Utils.getColorAccentDefaultColor(mContext));
        }

        return icon;
    }

    @VisibleForTesting
    boolean shouldInflateSignalStrength(int subId) {
        return SignalStrengthUtil.shouldInflateSignalStrength(mContext, subId);
    }

    /**
     * The summary can have either 1 or 2 lines depending on which services (calls, SMS, data) this
     * subscription is the default for.
     *
     * If this subscription is the default for calls and/or SMS, we add a line to show that.
     *
     * If this subscription is the default for data, we add a line with detail about
     * whether the data connection is active.
     *
     * If a subscription isn't the default for anything, we just say it is available.
     */
    protected String getSummary(int subId, boolean isDefaultForData) {
        final int callsDefaultSubId = mSubsPrefCtrlInjector.getDefaultVoiceSubscriptionId();
        final int smsDefaultSubId = mSubsPrefCtrlInjector.getDefaultSmsSubscriptionId();

        String line1 = null;
        if (subId == callsDefaultSubId && subId == smsDefaultSubId) {
            line1 = mContext.getString(R.string.default_for_calls_and_sms);
        } else if (subId == callsDefaultSubId) {
            line1 = mContext.getString(R.string.default_for_calls);
        } else if (subId == smsDefaultSubId) {
            line1 = mContext.getString(R.string.default_for_sms);
        }

        String line2 = null;
        if (isDefaultForData) {
            final TelephonyManager telMgrForSub = mContext.getSystemService(
                    TelephonyManager.class).createForSubscriptionId(subId);
            final boolean dataEnabled = telMgrForSub.isDataEnabled();
            if (dataEnabled && mSubsPrefCtrlInjector.isActiveCellularNetwork(mContext)) {
                line2 = mContext.getString(R.string.mobile_data_active);
            } else if (!dataEnabled) {
                line2 = mContext.getString(R.string.mobile_data_off);
            } else {
                line2 = mContext.getString(R.string.default_for_mobile_data);
            }
        }

        if (line1 != null && line2 != null) {
            return String.join(System.lineSeparator(), line1, line2);
        } else if (line1 != null) {
            return line1;
        } else if (line2 != null) {
            return line2;
        } else {
            return mContext.getString(R.string.subscription_available);
        }
    }

    /**
     * @return true if there is at least 1 available subscription.
     */
    @Override
    public boolean isAvailable() {
        if (mSubscriptionsListener.isAirplaneModeOn()
                && (!mWifiManager.isWifiEnabled() || !isCarrierNetworkActive())) {
            return false;
        }
        List<SubscriptionInfo> subInfoList =
                SubscriptionUtil.getActiveSubscriptions(mSubscriptionManager);
        if (subInfoList == null) {
            return false;
        }

        return subInfoList.stream()
                // Avoid from showing subscription(SIM)s which has been marked as hidden
                // For example, only one subscription will be shown when there're multiple
                // subscriptions with same group UUID.
                .filter(subInfo ->
                        mSubsPrefCtrlInjector.canSubscriptionBeDisplayed(mContext,
                                subInfo.getSubscriptionId()))
                .count() >= 1;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        update();
    }

    @Override
    public void onSubscriptionsChanged() {
        // See if we need to change which sub id we're using to listen for enabled/disabled changes.
        int defaultDataSubId = mSubsPrefCtrlInjector.getDefaultDataSubscriptionId();
        if (defaultDataSubId != mDataEnabledListener.getSubId()) {
            mDataEnabledListener.stop();
            mDataEnabledListener.start(defaultDataSubId);
        }
        update();
    }

    @Override
    public void onMobileDataEnabledChange() {
        update();
    }

    @Override
    public void onDataConnectivityChange() {
        update();
    }

    @Override
    public void onSignalStrengthChanged() {
        update();
    }

    @Override
    public void onTelephonyDisplayInfoChanged(int subId,
            TelephonyDisplayInfo telephonyDisplayInfo) {
        if (subId != mSubsPrefCtrlInjector.getDefaultDataSubscriptionId()) {
            return;
        }
        mTelephonyDisplayInfo = telephonyDisplayInfo;
        update();
    }

    @Override
    public void onCarrierNetworkChange(boolean active) {
        mCarrierNetworkChangeMode = active;
        update();
    }

    public void setWifiPickerTrackerHelper(WifiPickerTrackerHelper helper) {
        mWifiPickerTrackerHelper = helper;
    }

    @VisibleForTesting
    public void connectCarrierNetwork() {
        if (!MobileNetworkUtils.isMobileDataEnabled(mContext)) {
            return;
        }
        if (mWifiPickerTrackerHelper != null) {
            mWifiPickerTrackerHelper.connectCarrierNetwork(null /* ConnectCallback */);
        }
    }

    SubsPrefCtrlInjector createSubsPrefCtrlInjector() {
        return new SubsPrefCtrlInjector();
    }

    boolean isCarrierNetworkActive() {
        return mWifiPickerTrackerHelper != null
                && mWifiPickerTrackerHelper.isCarrierNetworkActive();
    }

    private int getCarrierNetworkLevel() {
        if (mWifiPickerTrackerHelper == null) return WifiEntry.WIFI_LEVEL_MIN;
        return mWifiPickerTrackerHelper.getCarrierNetworkLevel();
    }

    /**
     * To inject necessary data from each static api.
     */
    @VisibleForTesting
    public static class SubsPrefCtrlInjector {
        /**
         * Uses to inject function and value for class and test class.
         */
        public boolean canSubscriptionBeDisplayed(Context context, int subId) {
            return (SubscriptionUtil.getAvailableSubscriptionBySubIdAndShowingForUser(context,
                    ProxySubscriptionManager.getInstance(context), subId) != null);
        }

        /**
         * Check SIM be able to display on UI.
         */
        public int getDefaultSmsSubscriptionId() {
            return SubscriptionManager.getDefaultSmsSubscriptionId();
        }

        /**
         * Gets default voice subscription ID.
         */
        public int getDefaultVoiceSubscriptionId() {
            return SubscriptionManager.getDefaultVoiceSubscriptionId();
        }

        /**
         * Gets default data subscription ID.
         */
        public int getDefaultDataSubscriptionId() {
            return SubscriptionManager.getDefaultDataSubscriptionId();
        }

        /**
         * Confirms the current network is cellular and active.
         */
        public boolean isActiveCellularNetwork(Context context) {
            return MobileNetworkUtils.activeNetworkIsCellular(context);
        }

        /**
         * Gets config for carrier customization.
         */
        public Config getConfig(Context context) {
            return MobileMappings.Config.readConfig(context);
        }

        /**
         * Gets current network type of Carrier Wi-Fi Network or Cellular.
         */
        public String getNetworkType(Context context, Config config,
                TelephonyDisplayInfo telephonyDisplayInfo, int subId, boolean isCarrierWifiNetwork,
                boolean carrierNetworkChanged) {
            MobileIconGroup iconGroup = null;
            if (isCarrierWifiNetwork) {
                iconGroup = TelephonyIcons.CARRIER_MERGED_WIFI;
            } else if (carrierNetworkChanged) {
                iconGroup = TelephonyIcons.CARRIER_NETWORK_CHANGE;
            } else {
                String iconKey = getIconKey(telephonyDisplayInfo);
                iconGroup = mapIconSets(config).get(iconKey);
            }

            if (iconGroup == null) {
                Log.d(TAG, "Can not get the network's icon and description.");
                return "";
            }

            return getSummaryForSub(iconGroup, context, subId);
        }

        /**
         * Gets signal icon with different signal level.
         */
        public Drawable getIcon(Context context, int level, int numLevels, boolean cutOut,
                boolean carrierNetworkChanged) {
            return MobileNetworkUtils.getSignalStrengthIcon(context, level, numLevels,
                    NO_CELL_DATA_TYPE_ICON, cutOut, carrierNetworkChanged);
        }
    }
}
