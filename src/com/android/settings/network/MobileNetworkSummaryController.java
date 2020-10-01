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

package com.android.settings.network;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.euicc.EuiccManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.telephony.MobileNetworkActivity;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.AddPreference;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.List;

public class MobileNetworkSummaryController extends AbstractPreferenceController implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient, LifecycleObserver,
        PreferenceControllerMixin {
    private static final String TAG = "MobileNetSummaryCtlr";

    private static final String KEY = "mobile_network_list";

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private SubscriptionManager mSubscriptionManager;
    private UserManager mUserManager;
    private SubscriptionsChangeListener mChangeListener;
    private AddPreference mPreference;

    /**
     * This controls the summary text and click behavior of the "Mobile network" item on the
     * Network & internet page. There are 3 separate cases depending on the number of mobile network
     * subscriptions:
     * <ul>
     * <li>No subscription: click action begins a UI flow to add a network subscription, and
     * the summary text indicates this</li>
     *
     * <li>One subscription: click action takes you to details for that one network, and
     * the summary text is the network name</li>
     *
     * <li>More than one subscription: click action takes you to a page listing the subscriptions,
     * and the summary text gives the count of SIMs</li>
     * </ul>
     */
    public MobileNetworkSummaryController(Context context, Lifecycle lifecycle) {
        super(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(mContext).getMetricsFeatureProvider();
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mUserManager = context.getSystemService(UserManager.class);
        if (lifecycle != null) {
          mChangeListener = new SubscriptionsChangeListener(context, this);
          lifecycle.addObserver(this);
        }
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mChangeListener.start();
        update();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mChangeListener.stop();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public CharSequence getSummary() {
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(
                mContext);
        if (subs.isEmpty()) {
            if (MobileNetworkUtils.showEuiccSettings(mContext)) {
                return mContext.getResources().getString(
                        R.string.mobile_network_summary_add_a_network);
            }
            return null;
        } else if (subs.size() == 1) {
            final SubscriptionInfo info = subs.get(0);
            final int subId = info.getSubscriptionId();
            if (!info.isEmbedded() && !mSubscriptionManager.isActiveSubscriptionId(subId)
                    && !SubscriptionUtil.showToggleForPhysicalSim(mSubscriptionManager)) {
                return mContext.getString(R.string.mobile_network_tap_to_activate,
                        SubscriptionUtil.getDisplayName(info));
            } else {
                return subs.get(0).getDisplayName();
            }
        } else {
            final int count = subs.size();
            return mContext.getResources().getQuantityString(R.plurals.mobile_network_summary_count,
                    count, count);
        }
    }

    private void startAddSimFlow() {
        final Intent intent = new Intent(EuiccManager.ACTION_PROVISION_EMBEDDED_SUBSCRIPTION);
        intent.putExtra(EuiccManager.EXTRA_FORCE_PROVISION, true);
        mContext.startActivity(intent);
    }

    private void update() {
        if (mPreference == null || mPreference.isDisabledByAdmin()) {
            return;
        }
        refreshSummary(mPreference);
        mPreference.setOnPreferenceClickListener(null);
        mPreference.setOnAddClickListener(null);
        mPreference.setFragment(null);
        mPreference.setEnabled(!mChangeListener.isAirplaneModeOn());

        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(
                mContext);

        if (subs.isEmpty()) {
            if (MobileNetworkUtils.showEuiccSettings(mContext)) {
                mPreference.setOnPreferenceClickListener((Preference pref) -> {
                    mMetricsFeatureProvider.logClickedPreference(pref,
                            pref.getExtras().getInt(DashboardFragment.CATEGORY));
                    startAddSimFlow();
                    return true;
                });
            } else {
                mPreference.setEnabled(false);
            }
        } else {
            // We have one or more existing subscriptions, so we want the plus button if eSIM is
            // supported.
            if (MobileNetworkUtils.showEuiccSettings(mContext)) {
                mPreference.setAddWidgetEnabled(!mChangeListener.isAirplaneModeOn());
                mPreference.setOnAddClickListener(p -> {
                    mMetricsFeatureProvider.logClickedPreference(p,
                            p.getExtras().getInt(DashboardFragment.CATEGORY));
                    startAddSimFlow();
                });
            }

            if (subs.size() == 1) {
                mPreference.setOnPreferenceClickListener((Preference pref) -> {
                    mMetricsFeatureProvider.logClickedPreference(pref,
                            pref.getExtras().getInt(DashboardFragment.CATEGORY));
                    final SubscriptionInfo info = subs.get(0);
                    final int subId = info.getSubscriptionId();
                    if (!info.isEmbedded() && !mSubscriptionManager.isActiveSubscriptionId(subId)
                            && !SubscriptionUtil.showToggleForPhysicalSim(mSubscriptionManager)) {
                        mSubscriptionManager.setSubscriptionEnabled(subId, true);
                    } else {
                        final Intent intent = new Intent(mContext, MobileNetworkActivity.class);
                        intent.putExtra(Settings.EXTRA_SUB_ID, subs.get(0).getSubscriptionId());
                        mContext.startActivity(intent);
                    }
                    return true;
                });
            } else {
                mPreference.setFragment(MobileNetworkListFragment.class.getCanonicalName());
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isWifiOnly(mContext) && mUserManager.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        update();
    }

    @Override
    public void onSubscriptionsChanged() {
        refreshSummary(mPreference);
        update();
    }
}
