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

import static android.telephony.SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
import static android.telephony.SignalStrength.SIGNAL_STRENGTH_GOOD;
import static android.telephony.SignalStrength.SIGNAL_STRENGTH_GREAT;
import static android.telephony.SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
import static android.telephony.SignalStrength.SIGNAL_STRENGTH_POOR;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
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
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.provider.Settings;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSubscriptionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowSubscriptionManager.class)
public class SubscriptionsPreferenceControllerTest {
    private static final String KEY = "preference_group";

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private Network mActiveNetwork;
    @Mock
    private NetworkCapabilities mCapabilities;
    @Mock
    private Drawable mSignalStrengthIcon;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private SubscriptionsPreferenceController mController;
    private int mOnChildUpdatedCount;
    private SubscriptionsPreferenceController.UpdateListener mUpdateListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mConnectivityManager.getActiveNetwork()).thenReturn(mActiveNetwork);
        when(mConnectivityManager.getNetworkCapabilities(mActiveNetwork)).thenReturn(mCapabilities);
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(mTelephonyManager);
        when(mScreen.findPreference(eq(KEY))).thenReturn(mPreferenceCategory);
        when(mPreferenceCategory.getContext()).thenReturn(mContext);
        mOnChildUpdatedCount = 0;
        mUpdateListener = () -> mOnChildUpdatedCount++;

        mController = spy(
                new SubscriptionsPreferenceController(mContext, mLifecycle, mUpdateListener,
                        KEY, 5));
        doReturn(true).when(mController).canSubscriptionBeDisplayed(any(), anyInt());
        doReturn(mSignalStrengthIcon).when(mController).getIcon(anyInt(), anyInt(), anyBoolean());
    }

    @After
    public void tearDown() {
        SubscriptionUtil.setActiveSubscriptionsForTesting(null);
    }

    @Test
    public void isAvailable_oneSubscription_availableFalse() {
        setupMockSubscriptions(1);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_twoSubscriptions_availableTrue() {
        setupMockSubscriptions(2);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_fiveSubscriptions_availableTrue() {
        setupMockSubscriptions(5);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_airplaneModeOn_availableFalse() {
        setupMockSubscriptions(2);
        assertThat(mController.isAvailable()).isTrue();
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onAirplaneModeChanged_airplaneModeTurnedOn_eventFired() {
        setupMockSubscriptions(2);
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
        setupMockSubscriptions(2);
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
        final List<SubscriptionInfo> subs = setupMockSubscriptions(2);
        SubscriptionUtil.setActiveSubscriptionsForTesting(subs.subList(0, 1));
        mController.onResume();
        mController.displayPreference(mScreen);
        assertThat(mController.isAvailable()).isFalse();

        final int updateCountBeforeSubscriptionChange = mOnChildUpdatedCount;
        SubscriptionUtil.setActiveSubscriptionsForTesting(subs);
        mController.onSubscriptionsChanged();
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mOnChildUpdatedCount).isEqualTo(updateCountBeforeSubscriptionChange + 1);
    }

    @Test
    public void onSubscriptionsChanged_countBecameOne_eventFiredAndPrefsRemoved() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(2);
        mController.onResume();
        mController.displayPreference(mScreen);
        assertThat(mController.isAvailable()).isTrue();
        verify(mPreferenceCategory, times(2)).addPreference(any(Preference.class));

        final int updateCountBeforeSubscriptionChange = mOnChildUpdatedCount;
        SubscriptionUtil.setActiveSubscriptionsForTesting(subs.subList(0, 1));
        mController.onSubscriptionsChanged();
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mOnChildUpdatedCount).isEqualTo(updateCountBeforeSubscriptionChange + 1);

        verify(mPreferenceCategory, times(2)).removePreference(any(Preference.class));
    }

    @Test
    public void onSubscriptionsChanged_subscriptionReplaced_preferencesChanged() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(3);

        // Start out with only sub1 and sub2.
        SubscriptionUtil.setActiveSubscriptionsForTesting(subs.subList(0, 2));
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
        SubscriptionUtil.setActiveSubscriptionsForTesting(Arrays.asList(subs.get(0), subs.get(2)));
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
    private void runPreferenceClickTest(final int subscriptionCount, final int selectedPrefIndex) {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(subscriptionCount);
        final ArgumentCaptor<Preference> prefCaptor = ArgumentCaptor.forClass(Preference.class);
        mController.displayPreference(mScreen);
        verify(mPreferenceCategory, times(subscriptionCount)).addPreference(prefCaptor.capture());
        final List<Preference> prefs = prefCaptor.getAllValues();
        final Preference pref = prefs.get(selectedPrefIndex);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(intentCaptor.capture());
        pref.getOnPreferenceClickListener().onPreferenceClick(pref);
        final Intent intent = intentCaptor.getValue();
        assertThat(intent).isNotNull();
        assertThat(intent.hasExtra(Settings.EXTRA_SUB_ID)).isTrue();
        final int subIdFromIntent = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                INVALID_SUBSCRIPTION_ID);
        assertThat(subIdFromIntent).isEqualTo(
                subs.get(selectedPrefIndex).getSubscriptionId());
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

    @Test
    public void getSummary_twoSubsOneDefaultForEverythingDataActive() {
        setupMockSubscriptions(2);

        ShadowSubscriptionManager.setDefaultSmsSubscriptionId(11);
        ShadowSubscriptionManager.setDefaultVoiceSubscriptionId(11);
        when(mTelephonyManager.isDataEnabled()).thenReturn(true);
        when(mCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true);

        assertThat(mController.getSummary(11, true)).isEqualTo(
                mContext.getString(R.string.default_for_calls_and_sms) + System.lineSeparator()
                        + mContext.getString(R.string.mobile_data_active));

        assertThat(mController.getSummary(22, false)).isEqualTo(
                mContext.getString(R.string.subscription_available));
    }

    @Test
    public void getSummary_twoSubsOneDefaultForEverythingDataNotActive() {
        setupMockSubscriptions(2, 1, true);

        ShadowSubscriptionManager.setDefaultSmsSubscriptionId(1);
        ShadowSubscriptionManager.setDefaultVoiceSubscriptionId(1);

        assertThat(mController.getSummary(1, true)).isEqualTo(
                mContext.getString(R.string.default_for_calls_and_sms) + System.lineSeparator()
                        + mContext.getString(R.string.default_for_mobile_data));

        assertThat(mController.getSummary(2, false)).isEqualTo(
                mContext.getString(R.string.subscription_available));
    }

    @Test
    public void getSummary_twoSubsOneDefaultForEverythingDataDisabled() {
        setupMockSubscriptions(2);

        ShadowSubscriptionManager.setDefaultVoiceSubscriptionId(1);
        ShadowSubscriptionManager.setDefaultSmsSubscriptionId(1);

        assertThat(mController.getSummary(1, true)).isEqualTo(
                mContext.getString(R.string.default_for_calls_and_sms) + System.lineSeparator()
                        + mContext.getString(R.string.mobile_data_off));

        assertThat(mController.getSummary(2, false)).isEqualTo(
                mContext.getString(R.string.subscription_available));
    }

    @Test
    public void getSummary_twoSubsOneForCallsAndDataOneForSms() {
        setupMockSubscriptions(2, 1, true);

        ShadowSubscriptionManager.setDefaultSmsSubscriptionId(2);
        ShadowSubscriptionManager.setDefaultVoiceSubscriptionId(1);

        assertThat(mController.getSummary(1, true)).isEqualTo(
                mContext.getString(R.string.default_for_calls) + System.lineSeparator()
                        + mContext.getString(R.string.default_for_mobile_data));

        assertThat(mController.getSummary(2, false)).isEqualTo(
                mContext.getString(R.string.default_for_sms));
    }

    @Test
    public void setIcon_nullStrength_noCrash() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(2);
        setMockSubSignalStrength(subs, 0, -1);
        final Preference pref = mock(Preference.class);

        mController.setIcon(pref, 1, true /* isDefaultForData */);
        verify(mController).getIcon(eq(0), eq(NUM_SIGNAL_STRENGTH_BINS), eq(true));
    }

    @Test
    public void setIcon_noSignal_correctLevels() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(2, 1, true);
        setMockSubSignalStrength(subs, 0, SIGNAL_STRENGTH_NONE_OR_UNKNOWN);
        setMockSubSignalStrength(subs, 1, SIGNAL_STRENGTH_NONE_OR_UNKNOWN);
        setMockSubDataEnabled(subs, 0, true);
        final Preference pref = mock(Preference.class);

        mController.setIcon(pref, 1, true /* isDefaultForData */);
        verify(mController).getIcon(eq(0), eq(NUM_SIGNAL_STRENGTH_BINS), eq(false));

        mController.setIcon(pref, 2, false /* isDefaultForData */);
        verify(mController).getIcon(eq(0), eq(NUM_SIGNAL_STRENGTH_BINS), eq(true));
    }

    @Test
    public void setIcon_noSignal_withInflation_correctLevels() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(2, 1, true);
        setMockSubSignalStrength(subs, 0, SIGNAL_STRENGTH_NONE_OR_UNKNOWN);
        setMockSubSignalStrength(subs, 1, SIGNAL_STRENGTH_NONE_OR_UNKNOWN);
        final Preference pref = mock(Preference.class);
        doReturn(true).when(mController).shouldInflateSignalStrength(anyInt());

        mController.setIcon(pref, 1, true /* isDefaultForData */);
        verify(mController).getIcon(eq(1), eq(NUM_SIGNAL_STRENGTH_BINS + 1), eq(false));

        mController.setIcon(pref, 2, false /* isDefaultForData */);
        verify(mController).getIcon(eq(1), eq(NUM_SIGNAL_STRENGTH_BINS + 1), eq(true));
    }

    @Test
    public void setIcon_greatSignal_correctLevels() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(2, 1, true);
        setMockSubSignalStrength(subs, 0, SIGNAL_STRENGTH_GREAT);
        setMockSubSignalStrength(subs, 1, SIGNAL_STRENGTH_GREAT);
        final Preference pref = mock(Preference.class);

        mController.setIcon(pref, 1, true /* isDefaultForData */);
        verify(mController).getIcon(eq(4), eq(NUM_SIGNAL_STRENGTH_BINS), eq(false));

        mController.setIcon(pref, 2, false /* isDefaultForData */);
        verify(mController).getIcon(eq(4), eq(NUM_SIGNAL_STRENGTH_BINS), eq(true));
    }

    @Test
    public void onSignalStrengthChanged_subTwoGoesFromGoodToGreat_correctLevels() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(2);
        setMockSubSignalStrength(subs, 0, SIGNAL_STRENGTH_POOR);
        setMockSubSignalStrength(subs, 1, SIGNAL_STRENGTH_GOOD);

        mController.onResume();
        mController.displayPreference(mScreen);

        // Now change the signal strength for the 2nd subscription from Good to Great
        setMockSubSignalStrength(subs, 1, SIGNAL_STRENGTH_GREAT);
        mController.onSignalStrengthChanged();

        final ArgumentCaptor<Integer> level = ArgumentCaptor.forClass(Integer.class);
        verify(mController, times(4)).getIcon(level.capture(), eq(NUM_SIGNAL_STRENGTH_BINS),
                eq(true));
        assertThat(level.getAllValues().get(0)).isEqualTo(1);
        assertThat(level.getAllValues().get(1)).isEqualTo(3); // sub2, first time
        assertThat(level.getAllValues().get(2)).isEqualTo(1);
        assertThat(level.getAllValues().get(3)).isEqualTo(4); // sub2, after change
    }

    @Test
    public void displayPreference_mobileDataOff_bothSubsHaveCutOut() {
        setupMockSubscriptions(2, 1, false);

        mController.onResume();
        mController.displayPreference(mScreen);

        verify(mController, times(2)).getIcon(eq(SIGNAL_STRENGTH_GOOD),
                eq(NUM_SIGNAL_STRENGTH_BINS), eq(true));
    }

    @Test
    public void displayPreference_mobileDataOn_onlyNonDefaultSubHasCutOut() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(2, 1, true);
        setMockSubSignalStrength(subs, 1, SIGNAL_STRENGTH_POOR);

        mController.onResume();
        mController.displayPreference(mScreen);

        verify(mController).getIcon(eq(SIGNAL_STRENGTH_GOOD), eq(NUM_SIGNAL_STRENGTH_BINS),
                eq(false));
        verify(mController).getIcon(eq(SIGNAL_STRENGTH_POOR), eq(NUM_SIGNAL_STRENGTH_BINS),
                eq(true));
    }

    @Test
    public void displayPreference_subscriptionsWithSameGroupUUID_onlyOneWillBeSeen() {
        doReturn(false).when(mController).canSubscriptionBeDisplayed(any(), eq(3));
        final List<SubscriptionInfo> subs = setupMockSubscriptions(3);
        SubscriptionUtil.setActiveSubscriptionsForTesting(subs.subList(0, 3));

        mController.onResume();
        mController.displayPreference(mScreen);

        verify(mPreferenceCategory, times(2)).addPreference(any(Preference.class));
    }

    @Test
    public void onMobileDataEnabledChange_mobileDataTurnedOff_bothSubsHaveCutOut() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(2, 1, true);

        mController.onResume();
        mController.displayPreference(mScreen);

        setMockSubDataEnabled(subs, 0, false);
        mController.onMobileDataEnabledChange();

        final ArgumentCaptor<Boolean> cutOutCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(mController, times(4)).getIcon(eq(SIGNAL_STRENGTH_GOOD),
                eq(NUM_SIGNAL_STRENGTH_BINS), cutOutCaptor.capture());
        assertThat(cutOutCaptor.getAllValues().get(0)).isEqualTo(false); // sub1, first time
        assertThat(cutOutCaptor.getAllValues().get(1)).isEqualTo(true);
        assertThat(cutOutCaptor.getAllValues().get(2)).isEqualTo(true); // sub1, second time
        assertThat(cutOutCaptor.getAllValues().get(3)).isEqualTo(true);
    }

    private List<SubscriptionInfo> setupMockSubscriptions(int count) {
        return setupMockSubscriptions(count, 0, true);
    }

    /** Helper method to setup several mock active subscriptions. The generated subscription id's
     * start at 1.
     *
     * @param count How many subscriptions to create
     * @param defaultDataSubId The subscription id of the default data subscription - pass
     *                         INVALID_SUBSCRIPTION_ID if there should not be one
     * @param mobileDataEnabled Whether mobile data should be considered enabled for the default
     *                          data subscription
     */
    private List<SubscriptionInfo> setupMockSubscriptions(int count, int defaultDataSubId,
            boolean mobileDataEnabled) {
        if (defaultDataSubId != INVALID_SUBSCRIPTION_ID) {
            ShadowSubscriptionManager.setDefaultDataSubscriptionId(defaultDataSubId);
        }
        final ArrayList<SubscriptionInfo> infos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int subscriptionId = i + 1;
            final SubscriptionInfo info = mock(SubscriptionInfo.class);
            final TelephonyManager mgrForSub = mock(TelephonyManager.class);
            final SignalStrength signalStrength = mock(SignalStrength.class);

            if (subscriptionId == defaultDataSubId) {
                when(mgrForSub.isDataEnabled()).thenReturn(mobileDataEnabled);
            }
            when(info.getSubscriptionId()).thenReturn(i + 1);
            when(info.getDisplayName()).thenReturn("sub" + (i + 1));
            doReturn(mgrForSub).when(mTelephonyManager).createForSubscriptionId(eq(subscriptionId));
            when(mgrForSub.getSignalStrength()).thenReturn(signalStrength);
            when(signalStrength.getLevel()).thenReturn(SIGNAL_STRENGTH_GOOD);

            infos.add(info);
        }
        SubscriptionUtil.setActiveSubscriptionsForTesting(infos);
        return infos;
    }

    /**
     * Helper method to set the signal strength returned for a mock subscription
     * @param subs The list of subscriptions
     * @param index The index in of the subscription in |subs| to change
     * @param level The signal strength level to return for the subscription. Pass -1 to force
     *              return of a null SignalStrength object for the subscription.
     */
    private void setMockSubSignalStrength(List<SubscriptionInfo> subs, int index, int level) {
        final TelephonyManager mgrForSub =
                mTelephonyManager.createForSubscriptionId(subs.get(index).getSubscriptionId());
        if (level == -1) {
            when(mgrForSub.getSignalStrength()).thenReturn(null);
        } else {
            final SignalStrength signalStrength = mgrForSub.getSignalStrength();
            when(signalStrength.getLevel()).thenReturn(level);
        }
    }

    private void setMockSubDataEnabled(List<SubscriptionInfo> subs, int index, boolean enabled) {
        final TelephonyManager mgrForSub =
                mTelephonyManager.createForSubscriptionId(subs.get(index).getSubscriptionId());
        when(mgrForSub.isDataEnabled()).thenReturn(enabled);
    }
}
