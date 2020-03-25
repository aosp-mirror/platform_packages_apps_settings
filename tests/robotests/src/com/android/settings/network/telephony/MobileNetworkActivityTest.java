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

import static androidx.lifecycle.Lifecycle.State;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.network.ProxySubscriptionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowSubscriptionManager;
import org.robolectric.shadows.ShadowSubscriptionManager.SubscriptionInfoBuilder;

@RunWith(AndroidJUnit4.class)
public class MobileNetworkActivityTest {

    private static final int CURRENT_SUB_ID = 3;
    private static final int PREV_SUB_ID = 1;

    private Context mContext;
    private ShadowContextImpl mShadowContextImpl;
    private Intent mTestIntent;

    @Mock
    private UserManager mUserManager;
    @Mock
    private TelephonyManager mTelephonyManager;

    private ShadowSubscriptionManager mSubscriptionManager;
    private SubscriptionInfo mSubscriptionInfo1;
    private SubscriptionInfo mSubscriptionInfo2;

    private ActivityScenario<MobileNetworkActivity> mMobileNetworkActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        mShadowContextImpl = Shadow.extract(RuntimeEnvironment.application.getBaseContext());

        mShadowContextImpl.setSystemService(Context.USER_SERVICE, mUserManager);
        doReturn(true).when(mUserManager).isAdminUser();

        mShadowContextImpl.setSystemService(Context.TELEPHONY_SERVICE, mTelephonyManager);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(anyInt());

        mTestIntent = new Intent(mContext, MockMobileNetworkActivity.class);

        mSubscriptionManager = shadowOf(mContext.getSystemService(SubscriptionManager.class));
        mSubscriptionInfo1 = SubscriptionInfoBuilder.newBuilder()
                .setId(PREV_SUB_ID).buildSubscriptionInfo();
        mSubscriptionInfo2 = SubscriptionInfoBuilder.newBuilder()
                .setId(CURRENT_SUB_ID).buildSubscriptionInfo();
    }

    @After
    public void cleanUp() {
        if (mMobileNetworkActivity != null) {
            mMobileNetworkActivity.close();
        }
    }

    private static class MockMobileNetworkActivity extends MobileNetworkActivity {
        private MockMobileNetworkActivity() {
            super();
        }

        private SubscriptionInfo mSubscriptionInFragment;

        @Override
        ProxySubscriptionManager getProxySubscriptionManager() {
            if (mProxySubscriptionMgr == null) {
                mProxySubscriptionMgr = mock(ProxySubscriptionManager.class);
            }
            return mProxySubscriptionMgr;
        }

        @Override
        void registerActiveSubscriptionsListener() {
            onChanged();
        }

        @Override
        void switchFragment(SubscriptionInfo subInfo) {
            mSubscriptionInFragment = subInfo;
        }
    }

    private ActivityScenario<MobileNetworkActivity> createTargetActivity(Intent activityIntent) {
        return ActivityScenario.launch(activityIntent);
    }

    @Test
    public void updateBottomNavigationView_oneSubscription_shouldNotCrash() {
        mSubscriptionManager.setActiveSubscriptionInfos(mSubscriptionInfo1);

        mMobileNetworkActivity = createTargetActivity(mTestIntent);

        mMobileNetworkActivity.moveToState(State.STARTED);
    }

    @Test
    public void updateBottomNavigationView_twoSubscription_shouldNotCrash() {
        mSubscriptionManager.setActiveSubscriptionInfos(mSubscriptionInfo1, mSubscriptionInfo2);

        mMobileNetworkActivity = createTargetActivity(mTestIntent);

        mMobileNetworkActivity.moveToState(State.STARTED);
    }

    @Test
    public void switchFragment_switchBetweenTwoSubscriptions() {
        mSubscriptionManager.setActiveSubscriptionInfos(mSubscriptionInfo1, mSubscriptionInfo2);

        mTestIntent.putExtra(Settings.EXTRA_SUB_ID, PREV_SUB_ID);
        mMobileNetworkActivity = createTargetActivity(mTestIntent);

        mMobileNetworkActivity.moveToState(State.STARTED);

        mMobileNetworkActivity.onActivity(activity -> {
            final MockMobileNetworkActivity mockActivity = (MockMobileNetworkActivity) activity;
            mockActivity.switchFragment(mSubscriptionInfo1);
            assertThat(mockActivity.mSubscriptionInFragment).isEqualTo(mSubscriptionInfo1);
        });
    }

    @Test
    public void switchFragment_subscriptionsUpdate_notifyByIntent() {
        mSubscriptionManager.setActiveSubscriptionInfos(mSubscriptionInfo1, mSubscriptionInfo2);

        mTestIntent.putExtra(Settings.EXTRA_SUB_ID, PREV_SUB_ID);
        mMobileNetworkActivity = createTargetActivity(mTestIntent);

        mMobileNetworkActivity.moveToState(State.STARTED);

        mMobileNetworkActivity.onActivity(activity -> {
            final MockMobileNetworkActivity mockActivity = (MockMobileNetworkActivity) activity;
            mockActivity.switchFragment(mSubscriptionInfo1);
            assertThat(mockActivity.mSubscriptionInFragment).isEqualTo(mSubscriptionInfo1);

            mContext.sendBroadcast(new Intent(
                    CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED), null);

            mockActivity.switchFragment(mSubscriptionInfo2);
            assertThat(mockActivity.mSubscriptionInFragment).isEqualTo(mSubscriptionInfo2);

            mContext.sendBroadcast(new Intent(
                    TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED), null);

            mockActivity.switchFragment(mSubscriptionInfo1);
            assertThat(mockActivity.mSubscriptionInFragment).isEqualTo(mSubscriptionInfo1);
        });
    }

    @Test
    public void onSaveInstanceState_saveCurrentSubId() {
        mSubscriptionManager.setActiveSubscriptionInfos(mSubscriptionInfo1, mSubscriptionInfo2);

        mTestIntent.putExtra(Settings.EXTRA_SUB_ID, PREV_SUB_ID);
        mMobileNetworkActivity = createTargetActivity(mTestIntent);

        mMobileNetworkActivity.moveToState(State.STARTED);

        mMobileNetworkActivity.onActivity(activity -> {
            final Bundle bundle = new Bundle();
            activity.saveInstanceState(bundle);
            assertThat(bundle.getInt(Settings.EXTRA_SUB_ID)).isEqualTo(PREV_SUB_ID);
        });
    }
}
