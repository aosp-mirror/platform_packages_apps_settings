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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class MobileNetworkSwitchControllerTest {
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private LayoutPreference mLayoutPreference;
    @Mock
    private SubscriptionInfo mSubscription;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private MobileNetworkSwitchController mController;
    private SwitchBar mSwitchBar;
    private int mSubId = 123;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mSubscriptionManager.setSubscriptionEnabled(eq(mSubId), anyBoolean()))
                .thenReturn(true);

        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);

        when(mSubscription.isEmbedded()).thenReturn(true);
        when(mSubscription.getSubscriptionId()).thenReturn(mSubId);
        // Most tests want to have 2 available subscriptions so that the switch bar will show.
        final SubscriptionInfo sub2 = mock(SubscriptionInfo.class);
        when(sub2.getSubscriptionId()).thenReturn(456);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mSubscription, sub2));

        final String key = "prefKey";
        mController = new MobileNetworkSwitchController(mContext, key);
        mController.init(mLifecycle, mSubscription.getSubscriptionId());

        mSwitchBar = new SwitchBar(mContext);
        when(mScreen.findPreference(key)).thenReturn(mLayoutPreference);
        when(mLayoutPreference.findViewById(R.id.switch_bar)).thenReturn(mSwitchBar);
    }

    @After
    public void cleanUp() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(null);
    }

    @Test
    public void isAvailable_pSIM_isNotAvailable() {
        when(mSubscription.isEmbedded()).thenReturn(false);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isFalse();
    }

    @Test
    public void displayPreference_oneEnabledSubscription_switchBarNotHidden() {
        doReturn(true).when(mSubscriptionManager).isSubscriptionEnabled(mSubId);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mSubscription));
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
    }

    @Test
    public void displayPreference_oneDisabledSubscription_switchBarNotHidden() {
        doReturn(false).when(mSubscriptionManager).isSubscriptionEnabled(mSubId);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mSubscription));
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
    }

    @Test
    public void displayPreference_subscriptionEnabled_switchIsOn() {
        when(mSubscriptionManager.isSubscriptionEnabled(mSubId)).thenReturn(true);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();
    }

    @Test
    public void displayPreference_subscriptionDisabled_switchIsOff() {
        when(mSubscriptionManager.isSubscriptionEnabled(mSubId)).thenReturn(false);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isFalse();
    }

    @Test
    public void switchChangeListener_fromEnabledToDisabled_setSubscriptionEnabledCalledCorrectly() {
        when(mSubscriptionManager.isSubscriptionEnabled(mSubId)).thenReturn(true);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();
        mSwitchBar.setChecked(false);
        verify(mSubscriptionManager).setSubscriptionEnabled(eq(mSubId), eq(false));
    }

    @Test
    public void switchChangeListener_fromEnabledToDisabled_setSubscriptionEnabledFailed() {
        when(mSubscriptionManager.setSubscriptionEnabled(eq(mSubId), anyBoolean()))
                .thenReturn(false);
        when(mSubscriptionManager.isSubscriptionEnabled(mSubId)).thenReturn(true);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();
        mSwitchBar.setChecked(false);
        verify(mSubscriptionManager).setSubscriptionEnabled(eq(mSubId), eq(false));
        assertThat(mSwitchBar.isChecked()).isTrue();
    }

    @Test
    public void switchChangeListener_fromDisabledToEnabled_setSubscriptionEnabledCalledCorrectly() {
        when(mSubscriptionManager.isSubscriptionEnabled(mSubId)).thenReturn(false);
        mController.displayPreference(mScreen);
        assertThat(mSwitchBar.isShowing()).isTrue();
        assertThat(mSwitchBar.isChecked()).isFalse();
        mSwitchBar.setChecked(true);
        verify(mSubscriptionManager).setSubscriptionEnabled(eq(mSubId), eq(true));
    }
}
