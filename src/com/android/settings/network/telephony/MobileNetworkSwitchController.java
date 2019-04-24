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

package com.android.settings.network.telephony;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.FeatureFlags;
import com.android.settings.development.featureflags.FeatureFlagPersistent;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

/** This controls a switch to allow enabling/disabling a mobile network */
public class MobileNetworkSwitchController extends BasePreferenceController implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient, LifecycleObserver {
    private static final String TAG = "MobileNetworkSwitchCtrl";
    private SwitchBar mSwitchBar;
    private int mSubId;
    private SubscriptionsChangeListener mChangeListener;
    private SubscriptionManager mSubscriptionManager;

    public MobileNetworkSwitchController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mChangeListener = new SubscriptionsChangeListener(context, this);
    }

    public void init(Lifecycle lifecycle, int subId) {
        lifecycle.addObserver(this);
        mSubId = subId;
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
        final LayoutPreference pref = screen.findPreference(mPreferenceKey);
        mSwitchBar = pref.findViewById(R.id.switch_bar);
        mSwitchBar.setSwitchBarText(R.string.mobile_network_use_sim_on,
                R.string.mobile_network_use_sim_off);

        mSwitchBar.addOnSwitchChangeListener((switchView, isChecked) -> {
            if (mSubscriptionManager.isSubscriptionEnabled(mSubId) != isChecked
                    && (!mSubscriptionManager.setSubscriptionEnabled(mSubId, isChecked))) {
                mSwitchBar.setChecked(!isChecked);
            }
        });
        update();
    }

    private void update() {
        if (mSwitchBar == null) {
            return;
        }
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(
                mContext);
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID ||
                mSubscriptionManager.isSubscriptionEnabled(mSubId) && subs.size() < 2) {
            mSwitchBar.hide();
            return;
        }

        for (SubscriptionInfo info : subs) {
            if (info.getSubscriptionId() == mSubId) {
                mSwitchBar.show();
                mSwitchBar.setChecked(mSubscriptionManager.isSubscriptionEnabled(mSubId));
                return;
            }
        }
        // This subscription was not found in the available list.
        mSwitchBar.hide();
    }

    @Override
    public int getAvailabilityStatus() {
        if (FeatureFlagPersistent.isEnabled(mContext, FeatureFlags.NETWORK_INTERNET_V2)) {
            return AVAILABLE_UNSEARCHABLE;
        } else {
            return CONDITIONALLY_UNAVAILABLE;
        }
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {}

    @Override
    public void onSubscriptionsChanged() {
        update();
    }
}
