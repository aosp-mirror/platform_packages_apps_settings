/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.network.SubscriptionUtil;
import com.android.settings.widget.SettingsMainSwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.concurrent.Executor;

public class MobileNetworkSwitchControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscription;
    @Mock
    private TelephonyManager mTelephonyManager;

    private PreferenceScreen mScreen;
    private PreferenceManager mPreferenceManager;
    private SettingsMainSwitchPreference mSwitchBar;
    private Context mContext;
    private MobileNetworkSwitchController mController;
    private int mSubId = 123;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mSubscriptionManager.setSubscriptionEnabled(eq(mSubId), anyBoolean()))
                .thenReturn(true);

        when(mSubscription.isEmbedded()).thenReturn(true);
        when(mSubscription.getSubscriptionId()).thenReturn(mSubId);
        // Most tests want to have 2 available subscriptions so that the switch bar will show.
        final SubscriptionInfo sub2 = mock(SubscriptionInfo.class);
        when(sub2.getSubscriptionId()).thenReturn(456);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mSubscription, sub2));

        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(mSubId))
                .thenReturn(mTelephonyManager);

        final String key = "prefKey";
        mController = new MobileNetworkSwitchController(mContext, key);
        mController.init(mSubscription.getSubscriptionId());

        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mSwitchBar = new SettingsMainSwitchPreference(mContext);
        mSwitchBar.setKey(key);
        mSwitchBar.setTitle("123");
        mScreen.addPreference(mSwitchBar);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(mSwitchBar.getLayoutResource(),
                new LinearLayout(mContext), false);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(view);
        mSwitchBar.onBindViewHolder(holder);
    }

    @After
    public void cleanUp() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(null);
    }

    @Test
    @UiThreadTest
    public void isAvailable_pSIM_isNotAvailable() {
        when(mSubscription.isEmbedded()).thenReturn(false);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isFalse();

        when(mSubscriptionManager.canDisablePhysicalSubscription()).thenReturn(true);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
    }

    @Test
    @UiThreadTest
    public void displayPreference_oneEnabledSubscription_switchBarNotHidden() {
        doReturn(true).when(mSubscriptionManager).isActiveSubscriptionId(mSubId);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mSubscription));
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
    }

    @Test
    @UiThreadTest
    public void displayPreference_oneDisabledSubscription_switchBarNotHidden() {
        doReturn(false).when(mSubscriptionManager).isActiveSubscriptionId(mSubId);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mSubscription));

        mController.displayPreference(mScreen);

        assertThat(mSwitchBar.isShowing()).isTrue();
    }

    @Test
    @UiThreadTest
    public void displayPreference_subscriptionEnabled_switchIsOn() {
        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(true);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();
    }

    @Test
    @UiThreadTest
    public void displayPreference_subscriptionDisabled_switchIsOff() {
        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isFalse();
    }

    @Test
    @UiThreadTest
    public void switchChangeListener_fromEnabledToDisabled_setSubscriptionEnabledCalledCorrectly() {
        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(true);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(intentCaptor.capture());

        // set switch off then should start a Activity.
        mSwitchBar.setChecked(false);

        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(false);
        // Simulate action of back from previous activity.
        mController.displayPreference(mScreen);
        Bundle extra = intentCaptor.getValue().getExtras();

        verify(mContext, times(1)).startActivity(any());
        assertThat(extra.getInt(ToggleSubscriptionDialogActivity.ARG_SUB_ID)).isEqualTo(mSubId);
        assertThat(extra.getBoolean(ToggleSubscriptionDialogActivity.ARG_enable))
                .isEqualTo(false);
        assertThat(mSwitchBar.isChecked()).isFalse();
    }

    @Test
    @UiThreadTest
    public void switchChangeListener_fromEnabledToDisabled_setSubscriptionEnabledFailed() {
        when(mSubscriptionManager.setSubscriptionEnabled(eq(mSubId), anyBoolean()))
                .thenReturn(false);
        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(true);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(intentCaptor.capture());

        // set switch off then should start a Activity.
        mSwitchBar.setChecked(false);

        // Simulate action of back from previous activity.
        mController.displayPreference(mScreen);
        Bundle extra = intentCaptor.getValue().getExtras();

        verify(mContext, times(1)).startActivity(any());
        assertThat(extra.getInt(ToggleSubscriptionDialogActivity.ARG_SUB_ID)).isEqualTo(mSubId);
        assertThat(extra.getBoolean(ToggleSubscriptionDialogActivity.ARG_enable))
                .isEqualTo(false);
        assertThat(mSwitchBar.isChecked()).isTrue();
    }

    @Test
    @UiThreadTest
    public void switchChangeListener_fromDisabledToEnabled_setSubscriptionEnabledCalledCorrectly() {
        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(false);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isFalse();

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(intentCaptor.capture());
        mSwitchBar.setChecked(true);
        Bundle extra = intentCaptor.getValue().getExtras();

        verify(mContext, times(1)).startActivity(any());
        assertThat(extra.getInt(ToggleSubscriptionDialogActivity.ARG_SUB_ID)).isEqualTo(mSubId);
        assertThat(extra.getBoolean(ToggleSubscriptionDialogActivity.ARG_enable)).isEqualTo(true);
    }
    @Test
    @UiThreadTest
    public void onResumeAndonPause_registerAndUnregisterTelephonyCallback() {
        mController.onResume();

        verify(mTelephonyManager)
                .registerTelephonyCallback(any(Executor.class), any(TelephonyCallback.class));

        mController.onPause();
        verify(mTelephonyManager)
                .unregisterTelephonyCallback(any(TelephonyCallback.class));
    }

    @Test
    @UiThreadTest
    public void onPause_doNotRegisterAndUnregisterTelephonyCallback() {
        mController.onPause();
        verify(mTelephonyManager, times(0))
                .unregisterTelephonyCallback(any(TelephonyCallback.class));
    }
}
