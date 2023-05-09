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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.SubscriptionUtil;
import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DefaultSubscriptionControllerTest {

    private static final String SUB_ID_1 = "1";
    private static final String SUB_ID_2 = "2";
    private static final String SUB_ID_3 = "3";
    private static final String DISPLAY_NAME_1 = "Sub 1";
    private static final String DISPLAY_NAME_2 = "Sub 2";
    private static final String DISPLAY_NAME_3 = "Sub 3";
    private static final String SUB_MCC_1 = "123";
    private static final String SUB_MNC_1 = "456";
    private static final String SUB_MCC_2 = "223";
    private static final String SUB_MNC_2 = "456";
    private static final String SUB_MCC_3 = "323";
    private static final String SUB_MNC_3 = "456";
    private static final String SUB_COUNTRY_ISO_1 = "Sub 1";
    private static final String SUB_COUNTRY_ISO_2 = "Sub 2";
    private static final String SUB_COUNTRY_ISO_3 = "Sub 3";

    @Mock
    private SubscriptionManager mSubMgr;
    @Mock
    private TelecomManager mTelecomManager;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private LifecycleOwner mLifecycleOwner;
    @Mock
    private SubscriptionInfoEntity mSubInfo1;
    @Mock
    private SubscriptionInfoEntity mSubInfo2;
    @Mock
    private SubscriptionInfoEntity mSubInfo3;

    private LifecycleRegistry mLifecycleRegistry;
    private PreferenceScreen mScreen;
    private PreferenceManager mPreferenceManager;
    private ListPreference mListPreference;
    private Context mContext;
    private TestDefaultSubscriptionController mController;
    private List<SubscriptionInfoEntity> mSubscriptionInfoEntityList = new ArrayList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubMgr);
        when(mContext.getSystemService(TelecomManager.class)).thenReturn(mTelecomManager);

        final String key = "prefkey";
        mController = new TestDefaultSubscriptionController(mContext, key, mLifecycle,
                mLifecycleOwner);
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mListPreference = new ListPreference(mContext);
        mListPreference.setKey(key);
        mScreen.addPreference(mListPreference);
        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
    }

    @After
    public void tearDown() {
        SubscriptionUtil.setActiveSubscriptionsForTesting(null);
    }

    private SubscriptionInfoEntity setupSubscriptionInfoEntity(
            String subId, String displayName, String mcc, String mnc, String countryIso) {
        return new SubscriptionInfoEntity(subId, 1, 1, displayName, displayName, 0, mcc, mnc,
                countryIso, false, 1, TelephonyManager.DEFAULT_PORT_INDEX, false, null,
                SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM, displayName, false, "1234567890",
                true, false, true, true, true, true, false);
    }

    @Test
    public void getAvailabilityStatus_twoSubscriptions_isAvailable() {
        SubscriptionUtil.setActiveSubscriptionsForTesting(Arrays.asList(
                createMockSub(1, "sub1"),
                createMockSub(2, "sub2")));
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getSummary_singleSub() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1,
                SUB_COUNTRY_ISO_1);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setDefaultSubscription(Integer.parseInt(mSubInfo1.subId));
        mController.displayPreference(mScreen);

        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);

        assertThat(mListPreference.getSummary().toString()).isEqualTo(SUB_ID_1);
    }

    @Test
    public void getSummary_twoSubs() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1,
                SUB_COUNTRY_ISO_1);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, DISPLAY_NAME_2, SUB_MCC_2, SUB_MNC_2,
                SUB_COUNTRY_ISO_2);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.setDefaultSubscription(Integer.parseInt(mSubInfo1.subId));
        mController.displayPreference(mScreen);

        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);

        assertThat(mListPreference.getSummary().toString()).isEqualTo(SUB_ID_1);
    }

    @Test
    public void onPreferenceChange_prefChangedToSub2_callbackCalledCorrectly() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1,
                SUB_COUNTRY_ISO_1);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, DISPLAY_NAME_2, SUB_MCC_2, SUB_MNC_2,
                SUB_COUNTRY_ISO_2);
        mController.setDefaultSubscription(Integer.parseInt(mSubInfo1.subId));
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);

        mController.displayPreference(mScreen);
        mListPreference.setValue("222");
        mController.onPreferenceChange(mListPreference, "222");
        assertThat(mController.getDefaultSubscriptionId()).isEqualTo(222);
    }

    @Test
    public void onPreferenceChange_prefChangedToAlwaysAsk_callbackCalledCorrectly() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1,
                SUB_COUNTRY_ISO_1);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, DISPLAY_NAME_2, SUB_MCC_2, SUB_MNC_2,
                SUB_COUNTRY_ISO_2);
        mController.setDefaultSubscription(Integer.parseInt(mSubInfo1.subId));
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);

        mController.displayPreference(mScreen);
        mListPreference.setValue(Integer.toString(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        mController.onPreferenceChange(mListPreference,
                Integer.toString(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        assertThat(mController.getDefaultSubscriptionId()).isEqualTo(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    @Test
    public void onPreferenceChange_prefBecomesAvailable_onPreferenceChangeCallbackNotNull() {
        // Start with only one sub active, so the pref is not available
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1,
                SUB_COUNTRY_ISO_1);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, DISPLAY_NAME_2, SUB_MCC_2, SUB_MNC_2,
                SUB_COUNTRY_ISO_2);
        mController.setDefaultSubscription(Integer.parseInt(mSubInfo1.subId));
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setDefaultSubscription(Integer.parseInt(mSubInfo1.subId));
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);
        mController.displayPreference(mScreen);
        assertThat(mController.isAvailable()).isTrue();

        // Now make two subs be active - the pref should become available, and the
        // onPreferenceChange callback should be properly wired up.
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);

        assertThat(mController.isAvailable()).isTrue();
        mListPreference.callChangeListener(SUB_ID_2);
        assertThat(mController.getDefaultSubscriptionId()).isEqualTo(2);
    }

    @Test
    public void onSubscriptionsChanged_twoSubscriptionsDefaultChanges_selectedEntryGetsUpdated() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1,
                SUB_COUNTRY_ISO_1);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, DISPLAY_NAME_2, SUB_MCC_2, SUB_MNC_2,
                SUB_COUNTRY_ISO_2);
        mController.setDefaultSubscription(Integer.parseInt(mSubInfo1.subId));
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);

        mController.displayPreference(mScreen);
        assertThat(mListPreference.getEntry()).isEqualTo(DISPLAY_NAME_1);
        assertThat(mListPreference.getValue()).isEqualTo(SUB_ID_1);

        mController.setDefaultSubscription(Integer.parseInt(mSubInfo2.subId));
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);
        assertThat(mListPreference.getEntry()).isEqualTo(DISPLAY_NAME_2);
        assertThat(mListPreference.getValue()).isEqualTo(mSubInfo2.subId);
    }

    @Test
    public void onSubscriptionsChanged_goFromTwoSubscriptionsToOne_prefDisappears() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1,
                SUB_COUNTRY_ISO_1);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, DISPLAY_NAME_2, SUB_MCC_2, SUB_MNC_2,
                SUB_COUNTRY_ISO_2);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.setDefaultSubscription(Integer.parseInt(mSubInfo1.subId));
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);
        mController.displayPreference(mScreen);

        mController.displayPreference(mScreen);
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mListPreference.isVisible()).isTrue();
        assertThat(mListPreference.isEnabled()).isTrue();

        mSubscriptionInfoEntityList.remove(mSubInfo2);
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mListPreference.isVisible()).isTrue();
        assertThat(mListPreference.isEnabled()).isFalse();
    }

    @Test
    @UiThreadTest
    public void onSubscriptionsChanged_goFromOneSubscriptionToTwo_prefAppears() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1,
                SUB_COUNTRY_ISO_1);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, DISPLAY_NAME_2, SUB_MCC_2, SUB_MNC_2,
                SUB_COUNTRY_ISO_2);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setDefaultSubscription(Integer.parseInt(mSubInfo1.subId));
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);
        mController.displayPreference(mScreen);
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mListPreference.isVisible()).isTrue();
        assertThat(mListPreference.isEnabled()).isFalse();

        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mListPreference.isVisible()).isTrue();
        assertThat(mListPreference.isEnabled()).isTrue();
    }

    @Test
    public void onSubscriptionsChanged_goFromTwoToThreeSubscriptions_listGetsUpdated() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1,
                SUB_COUNTRY_ISO_1);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, DISPLAY_NAME_2, SUB_MCC_2, SUB_MNC_2,
                SUB_COUNTRY_ISO_2);
        mSubInfo3 = setupSubscriptionInfoEntity(SUB_ID_3, DISPLAY_NAME_3, SUB_MCC_3, SUB_MNC_3,
                SUB_COUNTRY_ISO_3);
        mController.setDefaultSubscription(Integer.parseInt(mSubInfo1.subId));
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);

        mController.displayPreference(mScreen);
        assertThat(mListPreference.getEntries().length).isEqualTo(3);

        mSubscriptionInfoEntityList.add(mSubInfo3);
        mController.onActiveSubInfoChanged(mSubscriptionInfoEntityList);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mListPreference.isVisible()).isTrue();
        final CharSequence[] entries = mListPreference.getEntries();
        final CharSequence[] entryValues = mListPreference.getEntryValues();
        assertThat(entries.length).isEqualTo(4);
        assertThat(entries[0].toString()).isEqualTo(DISPLAY_NAME_1);
        assertThat(entries[1].toString()).isEqualTo(DISPLAY_NAME_2);
        assertThat(entries[2].toString()).isEqualTo(DISPLAY_NAME_3);
        assertThat(entries[3].toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "calls_and_sms_ask_every_time"));
        assertThat(entryValues[0].toString()).isEqualTo(SUB_ID_1);
        assertThat(entryValues[1].toString()).isEqualTo(SUB_ID_2);
        assertThat(entryValues[2].toString()).isEqualTo(SUB_ID_3);
        assertThat(entryValues[3].toString()).isEqualTo(
                Integer.toString(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }

    private SubscriptionInfo createMockSub(int id, String displayName) {
        final SubscriptionInfo sub = mock(SubscriptionInfo.class);
        when(sub.getSubscriptionId()).thenReturn(id);
        when(sub.getDisplayName()).thenReturn(displayName);
        return sub;
    }

    private static class TestDefaultSubscriptionController extends DefaultSubscriptionController {
        int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        TestDefaultSubscriptionController(Context context, String preferenceKey,
                Lifecycle lifecycle, LifecycleOwner lifecycleOwner) {
            super(context, preferenceKey, lifecycle, lifecycleOwner);
        }

        @Override
        protected int getDefaultSubscriptionId() {
            return mSubId;
        }

        @Override
        protected void setDefaultSubscription(int subscriptionId) {
            mSubId = subscriptionId;
        }

        @Override
        public CharSequence getSummary() {
            return String.valueOf(mSubId);
        }
    }
}
