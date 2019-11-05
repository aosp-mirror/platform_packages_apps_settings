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

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.network.ProxySubscriptionManager;

import java.util.List;

/**
 * Activity for displaying MobileNetworkSettings
 */
public class MobileNetworkActivity extends SettingsBaseActivity
        implements ProxySubscriptionManager.OnActiveSubscriptionChangedListener {

    private static final String TAG = "MobileNetworkActivity";
    @VisibleForTesting
    static final String MOBILE_SETTINGS_TAG = "mobile_settings:";
    @VisibleForTesting
    static final int SUB_ID_NULL = Integer.MIN_VALUE;

    private ProxySubscriptionManager mProxySubscriptionMgr;
    private int mCurSubscriptionId;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        int updateSubscriptionIndex = SUB_ID_NULL;
        if (intent != null) {
            updateSubscriptionIndex = intent.getIntExtra(Settings.EXTRA_SUB_ID, SUB_ID_NULL);
        }

        mCurSubscriptionId = updateSubscriptionIndex;
        updateSubscriptions(getSubscription());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final UserManager userManager = this.getSystemService(UserManager.class);
        if (!userManager.isAdminUser()) {
            this.finish();
            return;
        }

        setContentView(R.layout.mobile_network_settings_container_v2);
        setActionBar(findViewById(R.id.mobile_action_bar));

        mProxySubscriptionMgr = ProxySubscriptionManager.getInstance(this);
        mProxySubscriptionMgr.setLifecycle(getLifecycle());
        mProxySubscriptionMgr.addActiveSubscriptionsListener(this);

        mCurSubscriptionId = savedInstanceState != null
                ? savedInstanceState.getInt(Settings.EXTRA_SUB_ID, SUB_ID_NULL)
                : SUB_ID_NULL;

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final SubscriptionInfo subscription = getSubscription();
        updateTitleAndNavigation(subscription);
    }

    /**
     * Implementation of ProxySubscriptionManager.OnActiveSubscriptionChangedListener
     */
    public void onChanged() {
        updateSubscriptions(getSubscription());
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateSubscriptions(getSubscription());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProxySubscriptionMgr.removeActiveSubscriptionsListener(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        saveInstanceState(outState);
    }

    @VisibleForTesting
    void saveInstanceState(@NonNull Bundle outState) {
        outState.putInt(Settings.EXTRA_SUB_ID, mCurSubscriptionId);
    }

    private void updateTitleAndNavigation(SubscriptionInfo subscription) {
        // Set the title to the name of the subscription. If we don't have subscription info, the
        // title will just default to the label for this activity that's already specified in
        // AndroidManifest.xml.
        if (subscription != null) {
            setTitle(subscription.getDisplayName());
        }
    }

    @VisibleForTesting
    void updateSubscriptions(SubscriptionInfo subscription) {
        if (subscription == null) {
            return;
        }
        final int subscriptionIndex = subscription.getSubscriptionId();

        updateTitleAndNavigation(subscription);
        switchFragment(subscription);

        mCurSubscriptionId = subscriptionIndex;
    }

    /**
     * Get the current subscription to display. First check whether intent has {@link
     * Settings#EXTRA_SUB_ID} and if so find the subscription with that id. If not, just return the
     * first one in the mSubscriptionInfos list since it is already sorted by sim slot.
     */
    @VisibleForTesting
    SubscriptionInfo getSubscription() {
        if (mCurSubscriptionId != SUB_ID_NULL) {
            final SubscriptionInfo subInfo =
                    mProxySubscriptionMgr.getActiveSubscriptionInfo(mCurSubscriptionId);
            if (subInfo != null) {
                return subInfo;
            }
        }
        final List<SubscriptionInfo> subInfos = mProxySubscriptionMgr.getActiveSubscriptionsInfo();
        if (CollectionUtils.isEmpty(subInfos)) {
            return null;
        }
        return subInfos.get(0);
    }

    @VisibleForTesting
    void switchFragment(SubscriptionInfo subInfo) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        final int subId = subInfo.getSubscriptionId();
        final Bundle bundle = new Bundle();
        bundle.putInt(Settings.EXTRA_SUB_ID, subId);

        final Fragment fragment = new MobileNetworkSettings();
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.main_content, fragment, buildFragmentTag(subId));
        fragmentTransaction.commit();
    }

    @VisibleForTesting
    String buildFragmentTag(int subscriptionId) {
        return MOBILE_SETTINGS_TAG + subscriptionId;
    }

    private boolean isSubscriptionChanged(int subscriptionId) {
        return (subscriptionId == SUB_ID_NULL) || (subscriptionId != mCurSubscriptionId);
    }
}
