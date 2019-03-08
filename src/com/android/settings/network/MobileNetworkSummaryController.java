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

import static android.telephony.TelephonyManager.MultiSimVariants.UNKNOWN;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;

import com.android.settings.R;
import com.android.settings.network.telephony.MobileNetworkActivity;
import com.android.settings.widget.AddPreference;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

public class MobileNetworkSummaryController extends AbstractPreferenceController implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient, LifecycleObserver {
    private static final String TAG = "MobileNetSummaryCtlr";

    private static final String KEY = "mobile_network_list";

    private SubscriptionManager mSubscriptionManager;
    private SubscriptionsChangeListener mChangeListener;
    private TelephonyManager mTelephonyMgr;
    private EuiccManager mEuiccManager;
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
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mTelephonyMgr = mContext.getSystemService(TelephonyManager.class);
        mEuiccManager = mContext.getSystemService(EuiccManager.class);
        mChangeListener = new SubscriptionsChangeListener(context, this);
        lifecycle.addObserver(this);
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
                mSubscriptionManager);
        if (subs.isEmpty()) {
            if (mEuiccManager.isEnabled()) {
                return mContext.getResources().getString(
                        R.string.mobile_network_summary_add_a_network);
            }
            return null;
        } else if (subs.size() == 1) {
            return subs.get(0).getDisplayName();
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

    private boolean shouldShowAddButton() {
        // The add button should only show up if the device is in multi-sim mode and the eSIM
        // manager is enabled.
        return mTelephonyMgr.getMultiSimConfiguration() != UNKNOWN && mEuiccManager.isEnabled();
    }

    private void update() {
        if (mPreference == null) {
            return;
        }
        final boolean showAddButton = shouldShowAddButton();
        refreshSummary(mPreference);
        if (!showAddButton) {
            mPreference.setOnAddClickListener(null);
        } else {
            mPreference.setAddWidgetEnabled(!mChangeListener.isAirplaneModeOn());
            mPreference.setOnAddClickListener(p -> {
                startAddSimFlow();
            });
        }
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(
                mSubscriptionManager);
        mPreference.setOnPreferenceClickListener(null);
        mPreference.setFragment(null);
        mPreference.setEnabled(!mChangeListener.isAirplaneModeOn());
        if (subs.isEmpty()) {
            if (showAddButton) {
                mPreference.setEnabled(false);
            } else if (mEuiccManager.isEnabled()) {
                mPreference.setOnPreferenceClickListener((Preference pref) -> {
                    startAddSimFlow();
                    return true;
                });
            }
        } else if (subs.size() == 1) {
            mPreference.setOnPreferenceClickListener((Preference pref) -> {
                final Intent intent = new Intent(mContext, MobileNetworkActivity.class);
                mContext.startActivity(intent);
                return true;
            });
        } else {
            mPreference.setFragment(MobileNetworkListFragment.class.getCanonicalName());
        }
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isWifiOnly(mContext);
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
