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

import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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
import android.telephony.SubscriptionManager;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class SubscriptionsPreferenceControllerTest {
    private static final String KEY = "preference_group";

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private SubscriptionManager mSubscriptionManager;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private SubscriptionsPreferenceController mController;
    private int mOnChildUpdatedCount;
    private SubscriptionsPreferenceController.UpdateListener mUpdateListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(Robolectric.setupActivity(Activity.class));
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mScreen.findPreference(eq(KEY))).thenReturn(mPreferenceCategory);
        when(mPreferenceCategory.getContext()).thenReturn(mContext);
        mOnChildUpdatedCount = 0;
        mUpdateListener = () -> mOnChildUpdatedCount++;

        mController = new SubscriptionsPreferenceController(mContext, mLifecycle, mUpdateListener,
                KEY, 5);
    }

    @After
    public void tearDown() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(null);
    }

    @Test
    public void isAvailable_oneSubscription_availableFalse() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(
                Arrays.asList(mock(SubscriptionInfo.class)));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_twoSubscriptions_availableTrue() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(
                Arrays.asList(mock(SubscriptionInfo.class), mock(SubscriptionInfo.class)));
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_fiveSubscriptions_availableTrue() {
        final ArrayList<SubscriptionInfo> subs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            subs.add(mock(SubscriptionInfo.class));
        }
        SubscriptionUtil.setAvailableSubscriptionsForTesting(subs);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_airplaneModeOn_availableFalse() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(
                Arrays.asList(mock(SubscriptionInfo.class), mock(SubscriptionInfo.class)));
        assertThat(mController.isAvailable()).isTrue();
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onAirplaneModeChanged_airplaneModeTurnedOn_eventFired() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(
                Arrays.asList(mock(SubscriptionInfo.class), mock(SubscriptionInfo.class)));
        mController.onResume();
        mController.displayPreference(mScreen);
        assertThat(mController.isAvailable()).isTrue();

        final int updateCountBeforeModeChange = mOnChildUpdatedCount;
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        mController.onAirplaneModeChanged(true);
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mOnChildUpdatedCount).isEqualTo(updateCountBeforeModeChange + 1);
    }

    @Test
    public void onAirplaneModeChanged_airplaneModeTurnedOff_eventFired() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(
                Arrays.asList(mock(SubscriptionInfo.class), mock(SubscriptionInfo.class)));
        mController.onResume();
        mController.displayPreference(mScreen);
        assertThat(mController.isAvailable()).isFalse();

        final int updateCountBeforeModeChange = mOnChildUpdatedCount;
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        mController.onAirplaneModeChanged(false);
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mOnChildUpdatedCount).isEqualTo(updateCountBeforeModeChange + 1);
    }

    @Test
    public void onSubscriptionsChanged_countBecameTwo_eventFired() {
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo sub2 = mock(SubscriptionInfo.class);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.onResume();
        mController.displayPreference(mScreen);
        assertThat(mController.isAvailable()).isFalse();

        final int updateCountBeforeSubscriptionChange = mOnChildUpdatedCount;
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mController.onSubscriptionsChanged();
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mOnChildUpdatedCount).isEqualTo(updateCountBeforeSubscriptionChange + 1);
    }

    @Test
    public void onSubscriptionsChanged_countBecameOne_eventFired() {
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo sub2 = mock(SubscriptionInfo.class);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mController.onResume();
        mController.displayPreference(mScreen);
        assertThat(mController.isAvailable()).isTrue();

        final int updateCountBeforeSubscriptionChange = mOnChildUpdatedCount;
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.onSubscriptionsChanged();
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mOnChildUpdatedCount).isEqualTo(updateCountBeforeSubscriptionChange + 1);
    }


    @Test
    public void onSubscriptionsChanged_subscriptionReplaced_preferencesChanged() {
        final SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo sub2 = mock(SubscriptionInfo.class);
        final SubscriptionInfo sub3 = mock(SubscriptionInfo.class);
        when(sub1.getDisplayName()).thenReturn("sub1");
        when(sub2.getDisplayName()).thenReturn("sub2");
        when(sub3.getDisplayName()).thenReturn("sub3");
        when(sub1.getSubscriptionId()).thenReturn(1);
        when(sub2.getSubscriptionId()).thenReturn(2);
        when(sub3.getSubscriptionId()).thenReturn(3);

        // Start out with only sub1 and sub2.
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mController.onResume();
        mController.displayPreference(mScreen);
        final ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        verify(mPreferenceCategory, times(2)).addPreference(captor.capture());
        assertThat(captor.getAllValues().size()).isEqualTo(2);
        assertThat(captor.getAllValues().get(0).getTitle()).isEqualTo("sub1");
        assertThat(captor.getAllValues().get(1).getTitle()).isEqualTo("sub2");

        // Now replace sub2 with sub3, and make sure the old preference was removed and the new
        // preference was added.
        final int updateCountBeforeSubscriptionChange = mOnChildUpdatedCount;
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub3));
        mController.onSubscriptionsChanged();
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mOnChildUpdatedCount).isEqualTo(updateCountBeforeSubscriptionChange + 1);

        verify(mPreferenceCategory).removePreference(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("sub2");
        verify(mPreferenceCategory, times(3)).addPreference(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("sub3");
    }


    /**
     * Helper to create a specified number of subscriptions, display them, and then click on one and
     * verify that the intent fires and has the right subscription id extra.
     *
     * @param subscriptionCount the number of subscriptions
     * @param selectedPrefIndex index of the subscription to click on
     */
    private void runPreferenceClickTest(int subscriptionCount, int selectedPrefIndex) {
        final ArrayList<SubscriptionInfo> subscriptions = new ArrayList<>();
        for (int i = 0; i < subscriptionCount; i++) {
            final SubscriptionInfo sub = mock(SubscriptionInfo.class);
            doReturn(i + 1).when(sub).getSubscriptionId();
            subscriptions.add(sub);
        }
        SubscriptionUtil.setAvailableSubscriptionsForTesting(subscriptions);
        mController.displayPreference(mScreen);
        final ArgumentCaptor<Preference> prefCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mPreferenceCategory, times(subscriptionCount)).addPreference(prefCaptor.capture());
        final List<Preference> prefs = prefCaptor.getAllValues();
        final Preference pref = prefs.get(selectedPrefIndex);
        pref.getOnPreferenceClickListener().onPreferenceClick(pref);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        final Intent intent = intentCaptor.getValue();
        assertThat(intent).isNotNull();
        assertThat(intent.hasExtra(Settings.EXTRA_SUB_ID)).isTrue();
        final int subIdFromIntent = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                INVALID_SUBSCRIPTION_ID);
        assertThat(subIdFromIntent).isEqualTo(
                subscriptions.get(selectedPrefIndex).getSubscriptionId());
    }

    @Test
    public void twoPreferences_firstPreferenceClicked_correctIntentFires() {
        runPreferenceClickTest(2, 0);
    }

    @Test
    public void twoPreferences_secondPreferenceClicked_correctIntentFires() {
        runPreferenceClickTest(2, 1);
    }

    @Test
    public void threePreferences_secondPreferenceClicked_correctIntentFires() {
        runPreferenceClickTest(3, 1);
    }

    @Test
    public void threePreferences_thirdPreferenceClicked_correctIntentFires() {
        runPreferenceClickTest(3, 2);
    }
}
