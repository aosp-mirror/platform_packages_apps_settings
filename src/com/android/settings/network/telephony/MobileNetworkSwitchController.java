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

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.widget.LayoutPreference;

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

        mSwitchBar.getSwitch().setOnBeforeCheckedChangeListener((toggleSwitch, isChecked) -> {
            // TODO b/135222940: re-evaluate whether to use
            // mSubscriptionManager#isSubscriptionEnabled
            if (mSubscriptionManager.isActiveSubscriptionId(mSubId) != isChecked
                    && (!mSubscriptionManager.setSubscriptionEnabled(mSubId, isChecked))) {
                return true;
            }
            return false;
        });
        update();
    }

    private void update() {
        if (mSwitchBar == null) {
            return;
        }

        SubscriptionInfo subInfo = null;
        for (SubscriptionInfo info : SubscriptionUtil.getAvailableSubscriptions(mContext)) {
            if (info.getSubscriptionId() == mSubId) {
                subInfo = info;
                break;
            }
        }

        // For eSIM, we always want the toggle. If telephony stack support disabling a pSIM
        // directly, we show the toggle.
        if (subInfo == null || (!subInfo.isEmbedded() && !SubscriptionUtil.showToggleForPhysicalSim(
                mSubscriptionManager))) {
            mSwitchBar.hide();
        } else {
            mSwitchBar.show();
            mSwitchBar.setCheckedInternal(mSubscriptionManager.isActiveSubscriptionId(mSubId));
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;

    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {}

    @Override
    public void onSubscriptionsChanged() {
        update();
    }
}
