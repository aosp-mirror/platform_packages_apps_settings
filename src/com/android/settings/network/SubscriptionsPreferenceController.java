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

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.provider.Settings;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
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
import com.android.settings.network.telephony.DataConnectivityListener;
import com.android.settings.network.telephony.MobileNetworkActivity;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.SignalStrengthListener;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.net.SignalStrengthUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This manages a set of Preferences it places into a PreferenceGroup owned by some parent
 * controller class - one for each available subscription. This controller is only considered
 * available if there are 2 or more subscriptions.
 */
public class SubscriptionsPreferenceController extends AbstractPreferenceController implements
        LifecycleObserver, SubscriptionsChangeListener.SubscriptionsChangeListenerClient,
        MobileDataEnabledListener.Client, DataConnectivityListener.Client,
        SignalStrengthListener.Callback {
    private static final String TAG = "SubscriptionsPrefCntrlr";

    private UpdateListener mUpdateListener;
    private String mPreferenceGroupKey;
    private PreferenceGroup mPreferenceGroup;
    private SubscriptionManager mManager;
    private ConnectivityManager mConnectivityManager;
    private SubscriptionsChangeListener mSubscriptionsListener;
    private MobileDataEnabledListener mDataEnabledListener;
    private DataConnectivityListener mConnectivityListener;
    private SignalStrengthListener mSignalStrengthListener;


    // Map of subscription id to Preference
    private Map<Integer, Preference> mSubscriptionPreferences;
    private int mStartOrder;

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
        mManager = context.getSystemService(SubscriptionManager.class);
        mConnectivityManager = mContext.getSystemService(ConnectivityManager.class);
        mSubscriptionPreferences = new ArrayMap<>();
        mSubscriptionsListener = new SubscriptionsChangeListener(context, this);
        mDataEnabledListener = new MobileDataEnabledListener(context, this);
        mConnectivityListener = new DataConnectivityListener(context, this);
        mSignalStrengthListener = new SignalStrengthListener(context, this);
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mSubscriptionsListener.start();
        mDataEnabledListener.start(SubscriptionManager.getDefaultDataSubscriptionId());
        mConnectivityListener.start();
        mSignalStrengthListener.resume();
        update();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mSubscriptionsListener.stop();
        mDataEnabledListener.stop();
        mConnectivityListener.stop();
        mSignalStrengthListener.pause();
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
            for (Preference pref : mSubscriptionPreferences.values()) {
                mPreferenceGroup.removePreference(pref);
            }
            mSubscriptionPreferences.clear();
            mSignalStrengthListener.updateSubscriptionIds(Collections.emptySet());
            mUpdateListener.onChildrenUpdated();
            return;
        }

        final Map<Integer, Preference> existingPrefs = mSubscriptionPreferences;
        mSubscriptionPreferences = new ArrayMap<>();

        int order = mStartOrder;
        final Set<Integer> activeSubIds = new ArraySet<>();
        final int dataDefaultSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        for (SubscriptionInfo info : SubscriptionUtil.getActiveSubscriptions(mManager)) {
            final int subId = info.getSubscriptionId();
            // Avoid from showing subscription(SIM)s which has been marked as hidden
            // For example, only one subscription will be shown when there're multiple
            // subscriptions with same group UUID.
            if (!canSubscriptionBeDisplayed(mContext, subId)) {
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
                final Intent intent = new Intent(mContext, MobileNetworkActivity.class);
                intent.putExtra(Settings.EXTRA_SUB_ID, subId);
                mContext.startActivity(intent);
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
        pref.setIcon(getIcon(level, numLevels, showCutOut));
    }

    @VisibleForTesting
    Drawable getIcon(int level, int numLevels, boolean cutOut) {
        return MobileNetworkUtils.getSignalStrengthIcon(mContext, level, numLevels,
                NO_CELL_DATA_TYPE_ICON, cutOut);
    }

    private boolean activeNetworkIsCellular() {
        final Network activeNetwork = mConnectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }
        final NetworkCapabilities networkCapabilities = mConnectivityManager.getNetworkCapabilities(
                activeNetwork);
        if (networkCapabilities == null) {
            return false;
        }
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
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
        final int callsDefaultSubId = SubscriptionManager.getDefaultVoiceSubscriptionId();
        final int smsDefaultSubId = SubscriptionManager.getDefaultSmsSubscriptionId();

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
            if (dataEnabled && activeNetworkIsCellular()) {
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
     * @return true if there are at least 2 available subscriptions.
     */
    @Override
    public boolean isAvailable() {
        if (mSubscriptionsListener.isAirplaneModeOn()) {
            return false;
        }
        List<SubscriptionInfo> subInfoList = SubscriptionUtil.getActiveSubscriptions(mManager);
        if (subInfoList == null) {
            return false;
        }
        return subInfoList.stream()
                // Avoid from showing subscription(SIM)s which has been marked as hidden
                // For example, only one subscription will be shown when there're multiple
                // subscriptions with same group UUID.
                .filter(subInfo ->
                        canSubscriptionBeDisplayed(mContext, subInfo.getSubscriptionId()))
                .count() >= 2;
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
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
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

    @VisibleForTesting
    boolean canSubscriptionBeDisplayed(Context context, int subId) {
        return (SubscriptionUtil.getAvailableSubscription(context,
                ProxySubscriptionManager.getInstance(context), subId) != null);
    }
}
