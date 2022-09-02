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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.SubscriptionUtil;
import com.android.settings.widget.SettingsMainSwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class MobileNetworkSwitchControllerTest {
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscription;

    private PreferenceScreen mScreen;
    private PreferenceManager mPreferenceManager;
    private SettingsMainSwitchPreference mSwitchBar;
    private Context mContext;
    private MobileNetworkSwitchController mController;
    private int mSubId = 123;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
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

        final String key = "prefKey";
        mController = new MobileNetworkSwitchController(mContext, key);
        mController.init(mSubscription.getSubscriptionId());

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mSwitchBar = new SettingsMainSwitchPreference(mContext);
        mSwitchBar.setKey(key);
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
    @Ignore
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
    @Ignore
    public void displayPreference_oneEnabledSubscription_switchBarNotHidden() {
        doReturn(true).when(mSubscriptionManager).isActiveSubscriptionId(mSubId);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mSubscription));
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
    }

    @Test
    @UiThreadTest
    @Ignore
    public void displayPreference_oneDisabledSubscription_switchBarNotHidden() {
        doReturn(false).when(mSubscriptionManager).isActiveSubscriptionId(mSubId);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mSubscription));

        mController.displayPreference(mScreen);

        assertThat(mSwitchBar.isShowing()).isTrue();
    }

    @Test
    @UiThreadTest
    @Ignore
    public void displayPreference_subscriptionEnabled_switchIsOn() {
        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(true);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();
    }

    @Test
    @UiThreadTest
    @Ignore
    public void displayPreference_subscriptionDisabled_switchIsOff() {
        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isFalse();
    }

    @Test
    @UiThreadTest
    @Ignore
    public void switchChangeListener_fromEnabledToDisabled_setSubscriptionEnabledCalledCorrectly() {
        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(true);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(intentCaptor.capture());
        mSwitchBar.setChecked(false);
        Bundle extra = intentCaptor.getValue().getExtras();

        verify(mContext, times(1)).startActivity(any());
        assertThat(extra.getInt(ToggleSubscriptionDialogActivity.ARG_SUB_ID)).isEqualTo(mSubId);
        assertThat(extra.getBoolean(ToggleSubscriptionDialogActivity.ARG_enable))
                .isEqualTo(false);
    }

    @Test
    @UiThreadTest
    @Ignore
    public void switchChangeListener_fromEnabledToDisabled_setSubscriptionEnabledFailed() {
        when(mSubscriptionManager.setSubscriptionEnabled(eq(mSubId), anyBoolean()))
                .thenReturn(false);
        when(mSubscriptionManager.isActiveSubscriptionId(mSubId)).thenReturn(true);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(intentCaptor.capture());
        mSwitchBar.setChecked(false);
        Bundle extra = intentCaptor.getValue().getExtras();

        verify(mContext, times(1)).startActivity(any());
        assertThat(extra.getInt(ToggleSubscriptionDialogActivity.ARG_SUB_ID)).isEqualTo(mSubId);
        assertThat(extra.getBoolean(ToggleSubscriptionDialogActivity.ARG_enable))
                .isEqualTo(false);
        assertThat(mSwitchBar.isChecked()).isTrue();
    }

    @Test
    @UiThreadTest
    @Ignore
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
}
