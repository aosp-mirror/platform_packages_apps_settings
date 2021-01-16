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

import static com.android.settings.network.telephony.MobileNetworkUtils.NO_CELL_DATA_TYPE_ICON;
import static com.android.settingslib.mobile.MobileMappings.getIconKey;
import static com.android.settingslib.mobile.MobileMappings.mapIconSets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.ArraySet;

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
import com.android.settings.network.telephony.MobileNetworkActivity;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.SignalStrengthListener;
import com.android.settings.network.telephony.TelephonyDisplayInfoListener;
import com.android.settings.widget.GearPreference;
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.mobile.MobileMappings;
import com.android.settingslib.mobile.MobileMappings.Config;
import com.android.settingslib.net.SignalStrengthUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * If the provider model is not enabled, this controller manages a set of Preferences it places into
 * a PreferenceGroup owned by some parent
 * controller class - one for each available subscription. This controller is only considered
 * available if there are 2 or more subscriptions.
 *
 * If the provider model is enabled, this controller manages preference with data subscription
 * information and make its state display on preference.
 * TODO this class will clean up the multiple subscriptions functionality after the provider
 * model is released.
 */
public class SubscriptionsPreferenceController extends AbstractPreferenceController implements
        LifecycleObserver, SubscriptionsChangeListener.SubscriptionsChangeListenerClient,
        MobileDataEnabledListener.Client, DataConnectivityListener.Client,
        SignalStrengthListener.Callback, TelephonyDisplayInfoListener.Callback {
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

    @VisibleForTesting
    final BroadcastReceiver mDataSubscriptionChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                mConfig = mSubsPrefCtrlInjector.getConfig(mContext);
                update();
            }
        }
    };

    // Map of subscription id to Preference
    private Map<Integer, Preference> mSubscriptionPreferences;
    private int mStartOrder;
    private GearPreference mSubsGearPref;
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

    private void registerDataSubscriptionChangedReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mContext.registerReceiver(mDataSubscriptionChangedReceiver, filter);
    }

    private void unRegisterDataSubscriptionChangedReceiver() {
        if (mDataSubscriptionChangedReceiver != null) {
            mContext.unregisterReceiver(mDataSubscriptionChangedReceiver);
        }

    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mSubscriptionsListener.start();
        mDataEnabledListener.start(mSubsPrefCtrlInjector.getDefaultDataSubscriptionId());
        mConnectivityListener.start();
        mSignalStrengthListener.resume();
        mTelephonyDisplayInfoListener.resume();
        registerDataSubscriptionChangedReceiver();
        update();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mSubscriptionsListener.stop();
        mDataEnabledListener.stop();
        mConnectivityListener.stop();
        mSignalStrengthListener.pause();
        mTelephonyDisplayInfoListener.pause();
        unRegisterDataSubscriptionChangedReceiver();
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

        if (mSubsPrefCtrlInjector.isProviderModelEnabled(mContext)) {
            updateForProvider();
        } else {
            updateForBase();
        }
    }

    private void updateForProvider() {
        SubscriptionInfo subInfo = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (subInfo == null) {
            mPreferenceGroup.removeAll();
            return;
        }
        if (mSubsGearPref == null) {
            mPreferenceGroup.removeAll();
            mSubsGearPref = new GearPreference(mContext, null);
            mSubsGearPref.setOnPreferenceClickListener(preference -> {
                connectCarrierNetwork();
                return true;
            });
            mSubsGearPref.setOnGearClickListener(p ->
                    startMobileNetworkActivity(mContext, subInfo.getSubscriptionId()));
        }

        mSubsGearPref.setTitle(subInfo.getDisplayName());
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

    private String getMobilePreferenceSummary(int subId) {
        String result = mSubsPrefCtrlInjector.getNetworkType(
                mContext, mConfig, mTelephonyDisplayInfo, subId);
        if (!result.isEmpty() && mSubsPrefCtrlInjector.isActiveCellularNetwork(mContext)) {
            result = mContext.getString(R.string.preference_summary_default_combination,
                    mContext.getString(R.string.mobile_data_connection_active), result);
        }
        return result;
    }

    private Drawable getIcon(int subId) {
        final TelephonyManager tmForSubId = mTelephonyManager.createForSubscriptionId(subId);
        final SignalStrength strength = tmForSubId.getSignalStrength();
        int level = (strength == null) ? 0 : strength.getLevel();

        int numLevels = SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
        if (shouldInflateSignalStrength(subId)) {
            level += 1;
            numLevels += 1;
        }

        final boolean isMobileDataOn = tmForSubId.isDataEnabled();
        final boolean isActiveCellularNetwork =
                mSubsPrefCtrlInjector.isActiveCellularNetwork(mContext);
        final boolean isMobileDataAccessible = tmForSubId.getDataState()
                == TelephonyManager.DATA_CONNECTED;
        final ServiceState serviceState = tmForSubId.getServiceState();
        final boolean isVoiceOutOfService = (serviceState == null)
                ? true
                : (serviceState.getState() == ServiceState.STATE_OUT_OF_SERVICE);

        Drawable icon = mSubsPrefCtrlInjector.getIcon(mContext, level, numLevels, false);

        if (isActiveCellularNetwork) {
            icon.setTint(Utils.getColorAccentDefaultColor(mContext));
            return icon;
        }
        if ((isMobileDataOn && isMobileDataAccessible)
                || (!isMobileDataOn && !isVoiceOutOfService)) {
            return icon;
        }

        icon = mContext.getDrawable(R.drawable.ic_signal_strength_zero_bar_no_internet);
        return icon;
    }

    private void updateForBase() {
        final Map<Integer, Preference> existingPrefs = mSubscriptionPreferences;
        mSubscriptionPreferences = new ArrayMap<>();

        int order = mStartOrder;
        final Set<Integer> activeSubIds = new ArraySet<>();
        final int dataDefaultSubId = mSubsPrefCtrlInjector.getDefaultDataSubscriptionId();
        for (SubscriptionInfo info :
                SubscriptionUtil.getActiveSubscriptions(mSubscriptionManager)) {
            final int subId = info.getSubscriptionId();
            // Avoid from showing subscription(SIM)s which has been marked as hidden
            // For example, only one subscription will be shown when there're multiple
            // subscriptions with same group UUID.
            if (!mSubsPrefCtrlInjector.canSubscriptionBeDisplayed(mContext, subId)) {
                continue;
            }
            activeSubIds.add(subId);
            Preference pref = existingPrefs.remove(subId);
            if (pref == null) {
                pref = new Preference(mPreferenceGroup.getContext());
                mPreferenceGroup.addPreference(pref);
            }
            pref.setTitle(info.getDisplayName());
            final boolean isDefaultForData = (subId == dataDefaultSubId);
            pref.setSummary(getSummary(subId, isDefaultForData));
            setIcon(pref, subId, isDefaultForData);
            pref.setOrder(order++);

            pref.setOnPreferenceClickListener(clickedPref -> {
                startMobileNetworkActivity(mContext, subId);
                return true;
            });

            mSubscriptionPreferences.put(subId, pref);
        }
        mSignalStrengthListener.updateSubscriptionIds(activeSubIds);

        // Remove any old preferences that no longer map to a subscription.
        for (Preference pref : existingPrefs.values()) {
            mPreferenceGroup.removePreference(pref);
        }
        mUpdateListener.onChildrenUpdated();
    }

    private static void startMobileNetworkActivity(Context context, int subId) {
        final Intent intent = new Intent(context, MobileNetworkActivity.class);
        intent.putExtra(Settings.EXTRA_SUB_ID, subId);
        context.startActivity(intent);
    }

    @VisibleForTesting
    boolean shouldInflateSignalStrength(int subId) {
        return SignalStrengthUtil.shouldInflateSignalStrength(mContext, subId);
    }

    @VisibleForTesting
    void setIcon(Preference pref, int subId, boolean isDefaultForData) {
        final TelephonyManager mgr = mContext.getSystemService(
                TelephonyManager.class).createForSubscriptionId(subId);
        final SignalStrength strength = mgr.getSignalStrength();
        int level = (strength == null) ? 0 : strength.getLevel();
        int numLevels = SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
        if (shouldInflateSignalStrength(subId)) {
            level += 1;
            numLevels += 1;
        }

        final boolean showCutOut = !isDefaultForData || !mgr.isDataEnabled();
        pref.setIcon(mSubsPrefCtrlInjector.getIcon(mContext, level, numLevels, showCutOut));
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
     * @return true if there are at least 2 available subscriptions,
     * or if there is at least 1 available subscription for provider model.
     */
    @Override
    public boolean isAvailable() {
        if (mSubscriptionsListener.isAirplaneModeOn()) {
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
                .count() >= (mSubsPrefCtrlInjector.isProviderModelEnabled(mContext) ? 1 : 2);
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
    public void onTelephonyDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo) {
        mTelephonyDisplayInfo = telephonyDisplayInfo;
        update();
    }

    @VisibleForTesting
    boolean canSubscriptionBeDisplayed(Context context, int subId) {
        return (SubscriptionUtil.getAvailableSubscription(context,
                ProxySubscriptionManager.getInstance(context), subId) != null);
    }

    public void setWifiPickerTrackerHelper(WifiPickerTrackerHelper helper) {
        mWifiPickerTrackerHelper = helper;
    }

    @VisibleForTesting
    public void connectCarrierNetwork() {
        if (mTelephonyManager == null || !mTelephonyManager.isDataEnabled()) {
            return;
        }
        if (mWifiPickerTrackerHelper != null) {
            mWifiPickerTrackerHelper.connectCarrierNetwork(null /* ConnectCallback */);
        }
    }

    SubsPrefCtrlInjector createSubsPrefCtrlInjector() {
        return new SubsPrefCtrlInjector();
    }

    /**
     * To inject necessary data from each static api.
     */
    @VisibleForTesting
    public static class SubsPrefCtrlInjector {
        /**
         * Use to inject function and value for class and test class.
         */
        public boolean canSubscriptionBeDisplayed(Context context, int subId) {
            return (SubscriptionUtil.getAvailableSubscription(context,
                    ProxySubscriptionManager.getInstance(context), subId) != null);
        }

        /**
         * Check SIM be able to display on UI.
         */
        public int getDefaultSmsSubscriptionId() {
            return SubscriptionManager.getDefaultSmsSubscriptionId();
        }

        /**
         * Get default voice subscription ID.
         */
        public int getDefaultVoiceSubscriptionId() {
            return SubscriptionManager.getDefaultVoiceSubscriptionId();
        }

        /**
         * Get default data subscription ID.
         */
        public int getDefaultDataSubscriptionId() {
            return SubscriptionManager.getDefaultDataSubscriptionId();
        }

        /**
         * Confirm the current network is cellular and active.
         */
        public boolean isActiveCellularNetwork(Context context) {
            return MobileNetworkUtils.activeNetworkIsCellular(context);
        }

        /**
         * Confirm the flag of Provider Model switch is turned on or not.
         */
        public boolean isProviderModelEnabled(Context context) {
            return Utils.isProviderModelEnabled(context);
        }

        /**
         * Get config for carrier customization.
         */
        public Config getConfig(Context context) {
            return MobileMappings.Config.readConfig(context);
        }

        /**
         * Get current mobile network type.
         */
        public String getNetworkType(Context context, Config config,
                TelephonyDisplayInfo telephonyDisplayInfo, int subId) {
            String iconKey = getIconKey(telephonyDisplayInfo);
            int resId = mapIconSets(config).get(iconKey).dataContentDescription;
            return resId != 0
                ? SubscriptionManager.getResourcesForSubId(context, subId).getString(resId) : "";
        }

        /**
         * Get signal icon with different signal level.
         */
        public Drawable getIcon(Context context, int level, int numLevels, boolean cutOut) {
            return MobileNetworkUtils.getSignalStrengthIcon(context, level, numLevels,
                    NO_CELL_DATA_TYPE_ICON, cutOut);
        }
    }
}