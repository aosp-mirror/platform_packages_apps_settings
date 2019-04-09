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

import static android.provider.Settings.EXTRA_SUB_ID;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class MobileNetworkListControllerTest {
    @Mock
    TelephonyManager mTelephonyManager;
    @Mock
    EuiccManager mEuiccManager;

    @Mock
    private Lifecycle mLifecycle;

    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private MobileNetworkListController mController;
    private Preference mAddMorePreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(Robolectric.setupActivity(Activity.class));
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(EuiccManager.class)).thenReturn(mEuiccManager);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.EUICC_PROVISIONED, 1);
        when(mPreferenceScreen.getContext()).thenReturn(mContext);
        mAddMorePreference = new Preference(mContext);
        when(mPreferenceScreen.findPreference(MobileNetworkListController.KEY_ADD_MORE)).thenReturn(
                mAddMorePreference);
        mController = new MobileNetworkListController(mContext, mLifecycle);
    }

    @After
    public void tearDown() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(null);
    }

    @Test
    public void displayPreference_noSubscriptions_noCrash() {
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
    }

    @Test
    public void displayPreference_eSimNotSupported_addMoreLinkNotVisible() {
        when(mEuiccManager.isEnabled()).thenReturn(false);
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        assertThat(mAddMorePreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_eSimSupported_addMoreLinkIsVisible() {
        when(mEuiccManager.isEnabled()).thenReturn(true);
        when(mTelephonyManager.getNetworkCountryIso()).thenReturn("");
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        assertThat(mAddMorePreference.isVisible()).isTrue();
    }

    @Test
    public void displayPreference_twoSubscriptions_correctlySetup() {
        final SubscriptionInfo sub1 = createMockSubscription(1, "sub1");
        final SubscriptionInfo sub2 = createMockSubscription(2, "sub2");
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();

        // Check that the preferences get created with the correct titles.
        final ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(
                Preference.class);
        verify(mPreferenceScreen, times(2)).addPreference(preferenceCaptor.capture());
        final Preference pref1 = preferenceCaptor.getAllValues().get(0);
        final Preference pref2 = preferenceCaptor.getAllValues().get(1);
        assertThat(pref1.getTitle()).isEqualTo("sub1");
        assertThat(pref2.getTitle()).isEqualTo("sub2");

        // Check that the onclick listeners are setup to fire with the right subscription id.
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        pref1.getOnPreferenceClickListener().onPreferenceClick(pref1);
        pref2.getOnPreferenceClickListener().onPreferenceClick(pref2);
        verify(mContext, times(2)).startActivity(intentCaptor.capture());
        final Intent intent1 = intentCaptor.getAllValues().get(0);
        final Intent intent2 = intentCaptor.getAllValues().get(1);
        assertThat(intent1.getIntExtra(EXTRA_SUB_ID, INVALID_SUBSCRIPTION_ID)).isEqualTo(1);
        assertThat(intent2.getIntExtra(EXTRA_SUB_ID, INVALID_SUBSCRIPTION_ID)).isEqualTo(2);
    }

    @Test
    public void onSubscriptionsChanged_twoSubscriptionsOneChangesName_preferenceUpdated() {
        final SubscriptionInfo sub1 = createMockSubscription(1, "sub1");
        final SubscriptionInfo sub2 = createMockSubscription(2, "sub2");
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        final ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(
                Preference.class);
        verify(mPreferenceScreen, times(2)).addPreference(preferenceCaptor.capture());

        when(sub2.getDisplayName()).thenReturn("new name");
        mController.onSubscriptionsChanged();
        assertThat(preferenceCaptor.getAllValues().get(1).getTitle()).isEqualTo("new name");
    }

    @Test
    public void onSubscriptionsChanged_startWithThreeSubsAndRemoveOne_correctPreferenceRemoved() {
        final SubscriptionInfo sub1 = createMockSubscription(1, "sub1");
        final SubscriptionInfo sub2 = createMockSubscription(2, "sub2");
        final SubscriptionInfo sub3 = createMockSubscription(3, "sub3");
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2, sub3));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        final ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(
                Preference.class);
        verify(mPreferenceScreen, times(3)).addPreference(preferenceCaptor.capture());

        // remove sub2, and check that the second pref was removed from the screen
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub3));
        mController.onSubscriptionsChanged();
        final ArgumentCaptor<Preference> removedPrefCaptor = ArgumentCaptor.forClass(
                Preference.class);
        verify(mPreferenceScreen).removePreference(removedPrefCaptor.capture());
        assertThat(removedPrefCaptor.getValue().getTitle()).isEqualTo("sub2");
    }

    private SubscriptionInfo createMockSubscription(int id, String displayName) {
        final SubscriptionInfo sub = mock(SubscriptionInfo.class);
        when(sub.getSubscriptionId()).thenReturn(id);
        when(sub.getDisplayName()).thenReturn(displayName);
        return sub;
    }
}
