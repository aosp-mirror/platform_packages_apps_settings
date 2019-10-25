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

import static com.android.settings.network.telephony.MobileNetworkActivity.MOBILE_SETTINGS_TAG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.View;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.view.menu.ContextMenuBuilder;
import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.development.featureflags.FeatureFlagPersistent;
import com.android.settings.network.SubscriptionUtil;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

@RunWith(RobolectricTestRunner.class)
public class MobileNetworkActivityTest {

    private static final int CURRENT_SUB_ID = 3;
    private static final int PREV_SUB_ID = 1;

    private Context mContext;
    private MobileNetworkActivity mMobileNetworkActivity;
    private List<SubscriptionInfo> mSubscriptionInfos;
    private Fragment mShowFragment;
    private Fragment mHideFragment;

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private SubscriptionInfo mSubscriptionInfo2;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private BottomNavigationView mBottomNavigationView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mMobileNetworkActivity = spy(new MobileNetworkActivity());
        mSubscriptionInfos = new ArrayList<>();
        mShowFragment = new Fragment();
        mHideFragment = new Fragment();
        mMobileNetworkActivity.mSubscriptionInfos = mSubscriptionInfos;
        mMobileNetworkActivity.mSubscriptionManager = mSubscriptionManager;
        when(mSubscriptionInfo.getSubscriptionId()).thenReturn(PREV_SUB_ID);
        when(mSubscriptionInfo2.getSubscriptionId()).thenReturn(CURRENT_SUB_ID);

        doReturn(mSubscriptionManager).when(mMobileNetworkActivity).getSystemService(
                SubscriptionManager.class);
        doReturn(mTelephonyManager).when(mMobileNetworkActivity).getSystemService(
                TelephonyManager.class);
        doReturn(mBottomNavigationView).when(mMobileNetworkActivity).findViewById(R.id.bottom_nav);
        doReturn(mFragmentManager).when(mMobileNetworkActivity).getSupportFragmentManager();
        doReturn(mFragmentTransaction).when(mFragmentManager).beginTransaction();
        doReturn(mHideFragment).when(mFragmentManager).findFragmentByTag(
                MOBILE_SETTINGS_TAG + PREV_SUB_ID);
        doReturn(mShowFragment).when(mFragmentManager).findFragmentByTag(
                MOBILE_SETTINGS_TAG + CURRENT_SUB_ID);
    }

    @After
    public void tearDown() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(null);
    }

    @Test
    public void updateBottomNavigationView_oneSubscription_shouldBeGone() {
        mSubscriptionInfos.add(mSubscriptionInfo);
        doReturn(mSubscriptionInfos).when(mSubscriptionManager).getActiveSubscriptionInfoList(
                eq(true));

        mMobileNetworkActivity.updateBottomNavigationView();

        verify(mBottomNavigationView).setVisibility(View.GONE);
    }

    @Test
    public void updateBottomNavigationView_twoSubscription_updateMenu() {
        final Menu menu = new ContextMenuBuilder(mContext);
        mSubscriptionInfos.add(mSubscriptionInfo);
        mSubscriptionInfos.add(mSubscriptionInfo);
        doReturn(mSubscriptionInfos).when(mSubscriptionManager).getActiveSubscriptionInfoList(
                eq(true));
        doReturn(menu).when(mBottomNavigationView).getMenu();

        mMobileNetworkActivity.updateBottomNavigationView();

        assertThat(menu.size()).isEqualTo(2);
    }

    @Test
    public void switchFragment_newFragment_replaceIt() {
        mMobileNetworkActivity.mCurSubscriptionId = PREV_SUB_ID;

        mMobileNetworkActivity.switchFragment(mShowFragment, CURRENT_SUB_ID);

        verify(mFragmentTransaction).replace(R.id.main_content, mShowFragment,
                MOBILE_SETTINGS_TAG + CURRENT_SUB_ID);
    }

    @Test
    public void phoneChangeReceiver_ignoresStickyBroadcastFromBeforeRegistering() {
        Activity activity = Robolectric.setupActivity(Activity.class);
        MobileNetworkActivity.PhoneChangeReceiver.Client client = mock(
                MobileNetworkActivity.PhoneChangeReceiver.Client.class);
        MobileNetworkActivity.PhoneChangeReceiver receiver =
                new MobileNetworkActivity.PhoneChangeReceiver(activity, client);
        Intent intent = new Intent(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        activity.sendStickyBroadcast(intent);

        receiver.register();
        verify(client, never()).onPhoneChange();

        activity.sendStickyBroadcast(intent);
        verify(client, times(1)).onPhoneChange();
    }

    @Test
    public void phoneChangeReceiver_ignoresCarrierConfigChangeForWrongSubscriptionId() {
        Activity activity = Robolectric.setupActivity(Activity.class);

        MobileNetworkActivity.PhoneChangeReceiver.Client client = mock(
                MobileNetworkActivity.PhoneChangeReceiver.Client.class);
        doReturn(2).when(client).getSubscriptionId();

        MobileNetworkActivity.PhoneChangeReceiver receiver =
                new MobileNetworkActivity.PhoneChangeReceiver(activity, client);

        receiver.register();

        Intent intent = new Intent(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        intent.putExtra(CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX, 3);
        activity.sendBroadcast(intent);
        verify(client, never()).onPhoneChange();
    }

    @Test
    public void phoneChangeReceiver_dispatchesCarrierConfigChangeForCorrectSubscriptionId() {
        Activity activity = Robolectric.setupActivity(Activity.class);

        MobileNetworkActivity.PhoneChangeReceiver.Client client = mock(
                MobileNetworkActivity.PhoneChangeReceiver.Client.class);
        doReturn(2).when(client).getSubscriptionId();

        MobileNetworkActivity.PhoneChangeReceiver receiver =
                new MobileNetworkActivity.PhoneChangeReceiver(activity, client);

        receiver.register();

        Intent intent = new Intent(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        intent.putExtra(CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX, 2);
        activity.sendBroadcast(intent);
        verify(client).onPhoneChange();
    }


    @Test
    public void getSubscriptionId_hasIntent_getIdFromIntent() {
        final Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_SUB_ID, CURRENT_SUB_ID);
        doReturn(intent).when(mMobileNetworkActivity).getIntent();
        mSubscriptionInfos.add(mSubscriptionInfo);
        mSubscriptionInfos.add(mSubscriptionInfo2);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(mSubscriptionInfos);
        doReturn(true).when(mSubscriptionManager).isActiveSubscriptionId(CURRENT_SUB_ID);

        assertThat(mMobileNetworkActivity.getSubscriptionId()).isEqualTo(CURRENT_SUB_ID);
    }

    @Test
    public void getSubscriptionId_noIntent_firstIdInList() {
        doReturn(null).when(mMobileNetworkActivity).getIntent();
        mSubscriptionInfos.add(mSubscriptionInfo);
        mSubscriptionInfos.add(mSubscriptionInfo2);

        assertThat(mMobileNetworkActivity.getSubscriptionId()).isEqualTo(PREV_SUB_ID);
    }

    @Test
    public void onSaveInstanceState_saveCurrentSubId() {
        mMobileNetworkActivity = Robolectric.buildActivity(MobileNetworkActivity.class).get();
        mMobileNetworkActivity.mCurSubscriptionId = PREV_SUB_ID;
        final Bundle bundle = new Bundle();

        mMobileNetworkActivity.saveInstanceState(bundle);

        assertThat(bundle.getInt(Settings.EXTRA_SUB_ID)).isEqualTo(PREV_SUB_ID);
    }

    @Test
    public void onNewIntent_newSubscriptionId_fragmentReplaced() {
        FeatureFlagPersistent.setEnabled(mContext, FeatureFlags.NETWORK_INTERNET_V2, true);

        mSubscriptionInfos.add(mSubscriptionInfo);
        mSubscriptionInfos.add(mSubscriptionInfo2);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(mSubscriptionInfos);
        mMobileNetworkActivity.mCurSubscriptionId = PREV_SUB_ID;

        final Intent newIntent = new Intent();
        newIntent.putExtra(Settings.EXTRA_SUB_ID, CURRENT_SUB_ID);
        mMobileNetworkActivity.onNewIntent(newIntent);
        assertThat(mMobileNetworkActivity.mCurSubscriptionId).isEqualTo(CURRENT_SUB_ID);
    }
}
