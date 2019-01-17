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
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.settings.R;
import com.android.settings.network.telephony.MobileNetworkActivity;
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
    private PreferenceScreen mScreen;

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
        mScreen = screen;
    }

    @Override
    public CharSequence getSummary() {
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(
                mSubscriptionManager);
        if (subs.isEmpty()) {
            return mContext.getResources().getString(R.string.mobile_network_summary_add_a_network);
        } else if (subs.size() == 1) {
            return subs.get(0).getDisplayName();
        } else {
            final int count = subs.size();
            return mContext.getResources().getQuantityString(R.plurals.mobile_network_summary_count,
                    count, count);
        }
    }

    private void update() {
        if (mScreen != null) {
            final Preference preference = mScreen.findPreference(getPreferenceKey());
            refreshSummary(preference);
            final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(
                    mSubscriptionManager);

            preference.setOnPreferenceClickListener(null);
            preference.setFragment(null);
            if (subs.size() == 0) {
                preference.setOnPreferenceClickListener((Preference pref) -> {
                    // TODO(asargent) - need to get correct intent to fire here
                    return true;
                });
            } else if (subs.size() == 1) {
                preference.setOnPreferenceClickListener((Preference pref) -> {
                    final Intent intent = new Intent(mContext, MobileNetworkActivity.class);
                    mContext.startActivity(intent);
                    return true;
                });
            } else {
                preference.setFragment(MobileNetworkListFragment.class.getCanonicalName());
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }

    @Override
    public void onSubscriptionsChanged() {
        update();
    }
}
