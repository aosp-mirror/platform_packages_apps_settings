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

import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.core.SettingsBaseActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MobileSettingsActivity extends SettingsBaseActivity {

    @VisibleForTesting
    static final String MOBILE_SETTINGS_TAG = "mobile_settings:";
    public static final String KEY_SUBSCRIPTION_ID = "key_subscription_id";
    public static final String KEY_CUR_SUBSCRIPTION_ID = "key_cur_subscription_id";

    private SubscriptionManager mSubscriptionManager;
    @VisibleForTesting
    Integer mCurSubscriptionId;
    @VisibleForTesting
    List<SubscriptionInfo> mSubscriptionInfos;

    private final SubscriptionManager.OnSubscriptionsChangedListener
            mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            updateSubscriptions(null);
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //TODO(b/114749736): update fragment by new intent, or at least make sure this page shows
        // current tab for sim card
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mobile_settings_container);
        setActionBar(findViewById(R.id.action_bar));
        mSubscriptionManager = getSystemService(SubscriptionManager.class);
        mSubscriptionInfos = mSubscriptionManager.getActiveSubscriptionInfoList();
        mCurSubscriptionId = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_CUR_SUBSCRIPTION_ID)
                : null;

        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);

        updateSubscriptions(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        saveInstanceState(outState);
    }

    @VisibleForTesting
    void saveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_CUR_SUBSCRIPTION_ID, mCurSubscriptionId);
    }

    @VisibleForTesting
    void updateSubscriptions(Bundle savedInstanceState) {
        //TODO(b/114749736): Sort it by phoneId
        mSubscriptionInfos = mSubscriptionManager.getActiveSubscriptionInfoList();
        final int subId = CollectionUtils.isEmpty(mSubscriptionInfos)
                ? SubscriptionManager.INVALID_SUBSCRIPTION_ID
                : mSubscriptionInfos.get(0).getSubscriptionId();

        updateBottomNavigationView();

        if (savedInstanceState == null) {
            switchFragment(new MobileNetworkFragment(), subId);
        }
    }

    @VisibleForTesting
    void updateBottomNavigationView() {
        final BottomNavigationView navigation = findViewById(R.id.bottom_nav);

        if (CollectionUtils.size(mSubscriptionInfos) <= 1) {
            navigation.setVisibility(View.GONE);
        } else {
            final Menu menu = navigation.getMenu();
            menu.clear();
            for (int i = 0, size = mSubscriptionInfos.size(); i < size; i++) {
                final SubscriptionInfo subscriptionInfo = mSubscriptionInfos.get(i);
                menu.add(0, subscriptionInfo.getSubscriptionId(), i,
                        subscriptionInfo.getDisplayName());
            }
            navigation.setOnNavigationItemSelectedListener(item -> {
                switchFragment(new MobileNetworkFragment(), item.getItemId());
                return true;
            });
        }
    }

    @VisibleForTesting
    void switchFragment(Fragment fragment, int subscriptionId) {
        if (mCurSubscriptionId != null && subscriptionId == mCurSubscriptionId) {
            return;
        }
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_SUBSCRIPTION_ID, subscriptionId);

        if (mCurSubscriptionId != null) {
            final Fragment hideFragment = fragmentManager.findFragmentByTag(
                    buildFragmentTag(mCurSubscriptionId));
            if (hideFragment != null) {
                fragmentTransaction.hide(hideFragment);
            }
        }

        Fragment showFragment = fragmentManager.findFragmentByTag(buildFragmentTag(subscriptionId));
        if (showFragment == null) {
            fragment.setArguments(bundle);
            fragmentTransaction.add(R.id.main_content, fragment, buildFragmentTag(subscriptionId));
        } else {
            showFragment.setArguments(bundle);
            fragmentTransaction.show(showFragment);
        }
        fragmentTransaction.commit();
        mCurSubscriptionId = subscriptionId;
    }

    private String buildFragmentTag(int subscriptionId) {
        return MOBILE_SETTINGS_TAG + subscriptionId;
    }
}