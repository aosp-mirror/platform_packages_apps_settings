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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.TelephonyIntents;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ActiveSubsciptionsListenerTest {

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo1;
    @Mock
    private SubscriptionInfo mSubscriptionInfo2;

    private Context mContext;
    private ActiveSubsciptionsListener mListener;
    private List<SubscriptionInfo> mActiveSubscriptions;
    private BroadcastReceiver mSubscriptionChangeReceiver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        mActiveSubscriptions = new ArrayList<SubscriptionInfo>();
    }

    @Test
    public void constructor_noListeningWasSetup() {
        mListener = spy(new ActiveSubsciptionsListener(mContext) {
            public void onChanged() {}
        });
        verify(mSubscriptionManager, never()).addOnSubscriptionsChangedListener(any());
        verify(mContext, never()).registerReceiver(any(), any());
        verify(mListener, never()).onChanged();
    }

    @Test
    public void start_onChangedShouldAlwaysBeCalled() {
        mListener = spy(new ActiveSubsciptionsListener(mContext) {
            public void onChanged() {}
        });
        mSubscriptionChangeReceiver = spy(mListener.mSubscriptionChangeReceiver);
        when(mSubscriptionChangeReceiver.isInitialStickyBroadcast()).thenReturn(false);

        mActiveSubscriptions.add(mSubscriptionInfo1);
        mActiveSubscriptions.add(mSubscriptionInfo2);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(mActiveSubscriptions);

        final Intent intentSubscription =
                new Intent(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        final Intent intentRadioTech =
                new Intent(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);

        mSubscriptionChangeReceiver.onReceive(mContext, intentSubscription);
        mSubscriptionChangeReceiver.onReceive(mContext, intentRadioTech);
        verify(mListener, never()).onChanged();

        mListener.start();

        mSubscriptionChangeReceiver.onReceive(mContext, intentSubscription);
        verify(mListener, atLeastOnce()).onChanged();

        mSubscriptionChangeReceiver.onReceive(mContext, intentRadioTech);
        verify(mListener, times(1)).onChanged();

        mListener.stop();

        mContext.sendStickyBroadcast(intentSubscription);
        mContext.sendStickyBroadcast(intentRadioTech);
        verify(mListener, times(1)).onChanged();
    }

    @Test
    public void start_alwaysFetchAndCacheResult() {
        mListener = spy(new ActiveSubsciptionsListener(mContext) {
            public void onChanged() {}
        });
        mActiveSubscriptions.add(mSubscriptionInfo1);
        mActiveSubscriptions.add(mSubscriptionInfo2);

        mListener.start();

        List<SubscriptionInfo> subInfoList = null;
        int numberOfAccess = 0;
        for (int numberOfSubInfo = mActiveSubscriptions.size(); numberOfSubInfo >= 0;
                numberOfSubInfo--) {
            if (mActiveSubscriptions.size() > numberOfSubInfo) {
                mActiveSubscriptions.remove(numberOfSubInfo);
            }
            when(mSubscriptionManager.getActiveSubscriptionInfoList())
                    .thenReturn(mActiveSubscriptions);

            // fetch twice and test if they generated access to SubscriptionManager only once
            subInfoList = mListener.getActiveSubscriptionsInfo();
            subInfoList = mListener.getActiveSubscriptionsInfo();

            numberOfAccess++;
            verify(mSubscriptionManager, times(numberOfAccess)).getActiveSubscriptionInfoList();

            mListener.clearCache();
        }

        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(null);

        // fetch twice and test if they generated access to SubscriptionManager only once
        subInfoList = mListener.getActiveSubscriptionsInfo();
        subInfoList = mListener.getActiveSubscriptionsInfo();

        numberOfAccess++;
        verify(mSubscriptionManager, times(numberOfAccess)).getActiveSubscriptionInfoList();
    }
}
