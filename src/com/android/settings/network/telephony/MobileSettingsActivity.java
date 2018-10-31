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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.core.SettingsBaseActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MobileSettingsActivity extends SettingsBaseActivity {

    private static final String TAG = "MobileSettingsActivity";
    @VisibleForTesting
    static final String MOBILE_SETTINGS_TAG = "mobile_settings:";
    @VisibleForTesting
    static final int SUB_ID_NULL = Integer.MIN_VALUE;

    @VisibleForTesting
    SubscriptionManager mSubscriptionManager;
    @VisibleForTesting
    int mCurSubscriptionId;
    @VisibleForTesting
    List<SubscriptionInfo> mSubscriptionInfos = new ArrayList<>();
    private PhoneChangeReceiver mPhoneChangeReceiver;

    private final SubscriptionManager.OnSubscriptionsChangedListener
            mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (!Objects.equals(mSubscriptionInfos,
                    mSubscriptionManager.getActiveSubscriptionInfoList())) {
                updateSubscriptions(null);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mobile_settings_container);
        setActionBar(findViewById(R.id.mobile_action_bar));
        mPhoneChangeReceiver = new PhoneChangeReceiver();
        mSubscriptionManager = getSystemService(SubscriptionManager.class);
        mSubscriptionInfos = mSubscriptionManager.getActiveSubscriptionInfoList();
        mCurSubscriptionId = savedInstanceState != null
                ? savedInstanceState.getInt(Settings.EXTRA_SUB_ID, SUB_ID_NULL)
                : SUB_ID_NULL;

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        updateSubscriptions(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final IntentFilter intentFilter = new IntentFilter(
                TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        registerReceiver(mPhoneChangeReceiver, intentFilter);
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mPhoneChangeReceiver);
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
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

    @VisibleForTesting
    void updateSubscriptions(Bundle savedInstanceState) {
        mSubscriptionInfos = mSubscriptionManager.getActiveSubscriptionInfoList();

        updateBottomNavigationView();

        if (savedInstanceState == null) {
            switchFragment(new MobileNetworkFragment(), getSubscriptionId());
        }
    }

    /**
     * Get the current subId to display. First check whether intent has {@link
     * Settings#EXTRA_SUB_ID}. If not, just display first one in list
     * since it is already sorted by sim slot.
     */
    @VisibleForTesting
    int getSubscriptionId() {
        final Intent intent = getIntent();
        if (intent != null) {
            final int subId = intent.getIntExtra(Settings.EXTRA_SUB_ID, SUB_ID_NULL);
            if (subId != SUB_ID_NULL && mSubscriptionManager.isActiveSubscriptionId(subId)) {
                return subId;
            }
        }

        if (CollectionUtils.isEmpty(mSubscriptionInfos)) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        return mSubscriptionInfos.get(0).getSubscriptionId();
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
                        subscriptionInfo.getDisplayName())
                        .setIcon(R.drawable.ic_settings_sim);
            }
            navigation.setOnNavigationItemSelectedListener(item -> {
                switchFragment(new MobileNetworkFragment(), item.getItemId());
                return true;
            });
        }
    }

    @VisibleForTesting
    void switchFragment(Fragment fragment, int subscriptionId) {
        switchFragment(fragment, subscriptionId, false /* forceUpdate */);
    }

    @VisibleForTesting
    void switchFragment(Fragment fragment, int subscriptionId, boolean forceUpdate) {
        if (mCurSubscriptionId != SUB_ID_NULL && subscriptionId == mCurSubscriptionId
                && !forceUpdate) {
            return;
        }
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        final Bundle bundle = new Bundle();
        bundle.putInt(Settings.EXTRA_SUB_ID, subscriptionId);

        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.main_content, fragment,
                buildFragmentTag(subscriptionId));
        fragmentTransaction.commit();
        mCurSubscriptionId = subscriptionId;
    }

    private String buildFragmentTag(int subscriptionId) {
        return MOBILE_SETTINGS_TAG + subscriptionId;
    }

    private class PhoneChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // When the radio changes (ex: CDMA->GSM), refresh the fragment.
            // This is very rare to happen.
            if (mCurSubscriptionId != SUB_ID_NULL) {
                switchFragment(new MobileNetworkFragment(), mCurSubscriptionId,
                        true /* forceUpdate */);
            }
        }
    }
}