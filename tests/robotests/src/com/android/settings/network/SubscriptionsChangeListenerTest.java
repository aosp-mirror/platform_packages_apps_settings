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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.network.SubscriptionsChangeListener.SubscriptionsChangeListenerClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SubscriptionsChangeListenerTest {

    @Mock
    private SubscriptionsChangeListenerClient mClient;
    @Mock
    private SubscriptionManager mSubscriptionManager;

    private Context mContext;
    private SubscriptionsChangeListener mListener;
    private Uri mAirplaneModeUri;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);

        mAirplaneModeUri = Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON);
    }

    private void initListener(boolean alsoStart) {
        mListener = new SubscriptionsChangeListener(mContext, mClient);
        if (alsoStart) {
            mListener.start();
        }
    }

    @Test
    public void whenStartNotCalled_noListeningWasSetup() {
        final ContentResolver contentResolver = mock(ContentResolver.class);
        when(mContext.getContentResolver()).thenReturn(contentResolver);
        initListener(false);
        shadowMainLooper().idle();
        verify(contentResolver, never()).registerContentObserver(any(Uri.class), anyBoolean(),
                any(ContentObserver.class));
        verify(mSubscriptionManager, never()).addOnSubscriptionsChangedListener(any(), any());
        verify(mContext, never()).registerReceiver(any(), any());
    }

    @Test
    public void onSubscriptionsChangedEvent_subscriptionManagerFires_eventDeliveredToUs() {
        initListener(true);
        final ArgumentCaptor<SubscriptionManager.OnSubscriptionsChangedListener> captor =
                ArgumentCaptor.forClass(SubscriptionManager.OnSubscriptionsChangedListener.class);
        verify(mSubscriptionManager).addOnSubscriptionsChangedListener(any(), captor.capture());
        shadowMainLooper().idle();
        captor.getValue().onSubscriptionsChanged();
        verify(mClient).onSubscriptionsChanged();
    }

    @Test
    public void
    onSubscriptionsChangedEvent_ignoresStickyBroadcastFromBeforeRegistering() {
        final Intent intent = new Intent(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        mContext.sendStickyBroadcast(intent);

        initListener(true);
        shadowMainLooper().idle();
        verify(mClient, never()).onSubscriptionsChanged();

        mContext.sendStickyBroadcast(intent);
        shadowMainLooper().idle();
        verify(mClient, times(1)).onSubscriptionsChanged();
    }

    @Test
    public void onSubscriptionsChangedEvent_radioTechnologyChangedBroadcast_eventDeliveredToUs() {
        initListener(true);
        final ArgumentCaptor<BroadcastReceiver> broadcastReceiverCaptor =
                ArgumentCaptor.forClass(BroadcastReceiver.class);
        shadowMainLooper().idle();
        verify(mContext).registerReceiver(broadcastReceiverCaptor.capture(), any());
        broadcastReceiverCaptor.getValue().onReceive(mContext, null);
        shadowMainLooper().idle();
        verify(mClient).onSubscriptionsChanged();
    }

    @Test
    public void onAirplaneModeChangedEvent_becameTrue_eventFires() {
        initListener(true);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        mListener.onChange(false, mAirplaneModeUri);
        shadowMainLooper().idle();
        verify(mClient, atLeastOnce()).onAirplaneModeChanged(true);
        assertThat(mListener.isAirplaneModeOn()).isTrue();
    }

    @Test
    public void onAirplaneModeChangedEvent_becameFalse_eventFires() {
        initListener(true);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        mListener.onChange(false, mAirplaneModeUri);
        shadowMainLooper().idle();
        verify(mClient, atLeastOnce()).onAirplaneModeChanged(false);
        assertThat(mListener.isAirplaneModeOn()).isFalse();
    }

}
