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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.telephony.TelephonyIntents;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ActiveSubscriptionsListenerTest {
    private static final int SUB_ID1 = 3;
    private static final int SUB_ID2 = 7;

    private static final Intent INTENT_RADIO_TECHNOLOGY_CHANGED =
            new Intent(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);

    private static final Intent INTENT_MULTI_SIM_CONFIG_CHANGED =
            new Intent(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED);

    private static final Intent INTENT_CARRIER_CONFIG_CHANGED =
            new Intent(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);

    private Context mContext;

    @Mock
    private SubscriptionManager mSubscriptionManager;
    private List<SubscriptionInfo> mActiveSubscriptions;

    private ActiveSubscriptionsListenerImpl mListener;
    private BroadcastReceiver mReceiver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        doReturn(mSubscriptionManager).when(mSubscriptionManager).createForAllUserProfiles();

        mActiveSubscriptions = new ArrayList<SubscriptionInfo>();
        addMockSubscription(SUB_ID1);
        addMockSubscription(SUB_ID2);
        doReturn(mActiveSubscriptions).when(mSubscriptionManager).getActiveSubscriptionInfoList();

        mListener = spy(new ActiveSubscriptionsListenerImpl(Looper.getMainLooper(), mContext));
        doReturn(mSubscriptionManager).when(mListener).getSubscriptionManager();
        mReceiver = mListener.getSubscriptionChangeReceiver();
    }

    private void addMockSubscription(int subId) {
        SubscriptionInfo mockSubscriptionInfo = mock(SubscriptionInfo.class);
        doReturn(subId).when(mockSubscriptionInfo).getSubscriptionId();
        mActiveSubscriptions.add(mockSubscriptionInfo);
    }

    @After
    public void cleanUp() {
        mListener.close();
    }

    public class ActiveSubscriptionsListenerImpl extends ActiveSubscriptionsListener {
        public ActiveSubscriptionsListenerImpl(Looper looper, Context context) {
            super(looper, context);
        }

        @Override
        void registerForSubscriptionsChange() {}

        public void onChanged() {}
    }

    @Test
    public void constructor_noListeningWasSetup() {
        verify(mListener, never()).onChanged();
    }

    @Test
    public void start_configChangedIntent_onChangedShouldBeCalled() {
        mReceiver.onReceive(mContext, INTENT_RADIO_TECHNOLOGY_CHANGED);
        mReceiver.onReceive(mContext, INTENT_MULTI_SIM_CONFIG_CHANGED);
        verify(mListener, never()).onChanged();

        mListener.start();

        mReceiver.onReceive(mContext, INTENT_RADIO_TECHNOLOGY_CHANGED);
        verify(mListener, times(1)).onChanged();

        mReceiver.onReceive(mContext, INTENT_MULTI_SIM_CONFIG_CHANGED);
        verify(mListener, times(2)).onChanged();

        mListener.stop();

        mReceiver.onReceive(mContext, INTENT_RADIO_TECHNOLOGY_CHANGED);
        mReceiver.onReceive(mContext, INTENT_MULTI_SIM_CONFIG_CHANGED);
        verify(mListener, times(2)).onChanged();
    }

    @Test
    public void start_carrierConfigChangedIntent_onChangedWhenSubIdBeenCached() {
        mReceiver.onReceive(mContext, INTENT_CARRIER_CONFIG_CHANGED);
        verify(mListener, never()).onChanged();

        mListener.start();

        mListener.getActiveSubscriptionsInfo();

        mReceiver.onReceive(mContext, INTENT_CARRIER_CONFIG_CHANGED);
        verify(mListener, never()).onChanged();

        INTENT_CARRIER_CONFIG_CHANGED.putExtra(CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX,
                SUB_ID2);
        mReceiver.onReceive(mContext, INTENT_CARRIER_CONFIG_CHANGED);
        verify(mListener, times(1)).onChanged();

        mListener.stop();

        mReceiver.onReceive(mContext, INTENT_CARRIER_CONFIG_CHANGED);
        verify(mListener, times(1)).onChanged();
    }


    @Test
    public void start_alwaysFetchAndCacheResult() {
        mListener.start();

        List<SubscriptionInfo> subInfoList = null;
        int numberOfAccess = 0;

        for (int numberOfSubInfo = mActiveSubscriptions.size(); numberOfSubInfo >= 0;
                numberOfSubInfo--) {
            if (mActiveSubscriptions.size() > numberOfSubInfo) {
                mActiveSubscriptions.remove(numberOfSubInfo);
            }

            // fetch twice and test if they generated access to SubscriptionManager only once
            subInfoList = mListener.getActiveSubscriptionsInfo();
            subInfoList = mListener.getActiveSubscriptionsInfo();

            numberOfAccess++;
            verify(mSubscriptionManager, times(numberOfAccess)).getActiveSubscriptionInfoList();

            mListener.clearCache();
        }

        mActiveSubscriptions.clear();

        // fetch twice and test if they generated access to SubscriptionManager only once
        subInfoList = mListener.getActiveSubscriptionsInfo();
        subInfoList = mListener.getActiveSubscriptionsInfo();

        numberOfAccess++;
        verify(mSubscriptionManager, times(numberOfAccess)).getActiveSubscriptionInfoList();
    }
}
