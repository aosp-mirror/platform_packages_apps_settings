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

package com.android.settings.network;

import static com.android.settings.network.SubscriptionUtil.KEY_UNIQUE_SUBSCRIPTION_DISPLAYNAME;
import static com.android.settings.network.SubscriptionUtil.SUB_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class SubscriptionUtilTest {
    private static final int SUBID_1 = 1;
    private static final int SUBID_2 = 2;
    private static final int SUBID_3 = 3;
    private static final CharSequence CARRIER_1 = "carrier1";
    private static final CharSequence CARRIER_1_SPACE = " carrier1       ";
    private static final CharSequence CARRIER_2 = "carrier2";

    private Context mContext;
    private NetworkCapabilities mNetworkCapabilities;

    @Mock
    private SubscriptionManager mSubMgr;
    @Mock
    private TelephonyManager mTelMgr;
    @Mock
    private Resources mResources;
    @Mock private ConnectivityManager mConnectivityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubMgr);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelMgr);
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mTelMgr.getUiccSlotsInfo()).thenReturn(null);
    }

    @Ignore
    @Test
    public void getAvailableSubscriptions_nullInfoFromSubscriptionManager_nonNullResult() {
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(null);

        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);

        assertThat(subs).isNotNull();
        assertThat(subs).isEmpty();
    }

    @Test
    public void getAvailableSubscriptions_oneSubscription_oneResult() {
        final SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info));

        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);

        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(1);
    }

    @Ignore
    @Test
    public void getAvailableSubscriptions_twoSubscriptions_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info1, info2));

        // // TODO remove this line.
        // when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info1, info2));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mContext);

        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(2);
    }

    @Test
    public void getActiveSubscriptions_nullInfoFromSubscriptionManager_nonNullResult() {
        when(mSubMgr.getActiveSubscriptionInfoList()).thenReturn(null);

        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mSubMgr);

        assertThat(subs).isNotNull();
        assertThat(subs).isEmpty();
    }

    @Test
    public void getActiveSubscriptions_oneSubscription_oneResult() {
        final SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(mSubMgr.getActiveSubscriptionInfoList()).thenReturn(Arrays.asList(info));

        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mSubMgr);

        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(1);
    }

    @Test
    public void getActiveSubscriptions_twoSubscriptions_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(mSubMgr.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(info1, info2));

        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mSubMgr);

        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(2);
    }

    @Ignore
    @Test
    public void getUniqueDisplayNames_uniqueCarriers_originalUsed() {
        // Each subscription's default display name is unique.
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info2.getSubscriptionId()).thenReturn(SUBID_2);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(info2.getDisplayName()).thenReturn(CARRIER_2);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(info1, info2));
            // Arrays.asList(info2));

        // Each subscription has a unique last 4 digits of the phone number.
        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        TelephonyManager sub2Telmgr = mock(TelephonyManager.class);
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("1112223333");
        when(mSubMgr.getPhoneNumber(SUBID_2)).thenReturn("2223334444");
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);
        when(mTelMgr.createForSubscriptionId(SUBID_2)).thenReturn(sub2Telmgr);

        final Map<Integer, CharSequence> idNames =
                SubscriptionUtil.getUniqueSubscriptionDisplayNames(mContext);

        assertThat(idNames).isNotNull();
        assertThat(idNames).hasSize(2);
        assertEquals(CARRIER_1, idNames.get(SUBID_1));
        assertEquals(CARRIER_2, idNames.get(SUBID_2));
    }

    @Ignore
    @Test
    public void getUniqueDisplayNames_identicalCarriers_fourDigitsUsed() {
        // Both subscriptions have the same display name.
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info2.getSubscriptionId()).thenReturn(SUBID_2);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(info2.getDisplayName()).thenReturn(CARRIER_1);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(info1, info2));

        // Each subscription has a unique last 4 digits of the phone number.
        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        TelephonyManager sub2Telmgr = mock(TelephonyManager.class);
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("1112223333");
        when(mSubMgr.getPhoneNumber(SUBID_2)).thenReturn("2223334444");
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);
        when(mTelMgr.createForSubscriptionId(SUBID_2)).thenReturn(sub2Telmgr);

        final Map<Integer, CharSequence> idNames =
                SubscriptionUtil.getUniqueSubscriptionDisplayNames(mContext);

        assertThat(idNames).isNotNull();
        assertThat(idNames).hasSize(2);
        assertEquals(CARRIER_1 + " 3333", idNames.get(SUBID_1));
        assertEquals(CARRIER_1 + " 4444", idNames.get(SUBID_2));
    }

    @Ignore
    @Test
    public void getUniqueDisplayNames_identicalCarriersAfterTrim_fourDigitsUsed() {
        // Both subscriptions have the same display name.
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info2.getSubscriptionId()).thenReturn(SUBID_2);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(info2.getDisplayName()).thenReturn(CARRIER_1_SPACE);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(info1, info2));

        // Each subscription has a unique last 4 digits of the phone number.
        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        TelephonyManager sub2Telmgr = mock(TelephonyManager.class);
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("1112223333");
        when(mSubMgr.getPhoneNumber(SUBID_2)).thenReturn("2223334444");
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);
        when(mTelMgr.createForSubscriptionId(SUBID_2)).thenReturn(sub2Telmgr);

        final Map<Integer, CharSequence> idNames =
                SubscriptionUtil.getUniqueSubscriptionDisplayNames(mContext);

        assertThat(idNames).isNotNull();
        assertThat(idNames).hasSize(2);
        assertEquals(CARRIER_1 + " 3333", idNames.get(SUBID_1));
        assertEquals(CARRIER_1 + " 4444", idNames.get(SUBID_2));
    }

    @Ignore
    @Test
    public void getUniqueDisplayNames_phoneNumberBlocked_subscriptionIdFallback() {
        // Both subscriptions have the same display name.
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info2.getSubscriptionId()).thenReturn(SUBID_2);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(info2.getDisplayName()).thenReturn(CARRIER_1);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info1, info2));

        // The subscriptions' phone numbers cannot be revealed to the user.
        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        TelephonyManager sub2Telmgr = mock(TelephonyManager.class);
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("");
        when(mSubMgr.getPhoneNumber(SUBID_2)).thenReturn("");
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);
        when(mTelMgr.createForSubscriptionId(SUBID_2)).thenReturn(sub2Telmgr);

        final Map<Integer, CharSequence> idNames =
                SubscriptionUtil.getUniqueSubscriptionDisplayNames(mContext);

        assertThat(idNames).isNotNull();
        assertThat(idNames).hasSize(2);
        assertEquals(CARRIER_1 + " 1", idNames.get(SUBID_1));
        assertEquals(CARRIER_1 + " 2", idNames.get(SUBID_2));
    }

    @Ignore
    @Test
    public void getUniqueDisplayNames_phoneNumberIdentical_subscriptionIdFallback() {
        // TODO have three here from the same carrier
        // Both subscriptions have the same display name.
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info3 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info2.getSubscriptionId()).thenReturn(SUBID_2);
        when(info3.getSubscriptionId()).thenReturn(SUBID_3);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(info2.getDisplayName()).thenReturn(CARRIER_1);
        when(info3.getDisplayName()).thenReturn(CARRIER_1);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(info1, info2, info3));

        // Subscription 1 has a unique phone number, but subscriptions 2 and 3 share the same
        // last four digits.
        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        TelephonyManager sub2Telmgr = mock(TelephonyManager.class);
        TelephonyManager sub3Telmgr = mock(TelephonyManager.class);
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("1112223333");
        when(mSubMgr.getPhoneNumber(SUBID_2)).thenReturn("2223334444");
        when(mSubMgr.getPhoneNumber(SUBID_3)).thenReturn("5556664444");
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);
        when(mTelMgr.createForSubscriptionId(SUBID_2)).thenReturn(sub2Telmgr);
        when(mTelMgr.createForSubscriptionId(SUBID_3)).thenReturn(sub3Telmgr);

        final Map<Integer, CharSequence> idNames =
                SubscriptionUtil.getUniqueSubscriptionDisplayNames(mContext);

        assertThat(idNames).isNotNull();
        assertThat(idNames).hasSize(3);
        assertEquals(CARRIER_1 + " 3333", idNames.get(SUBID_1));
        assertEquals(CARRIER_1 + " 2", idNames.get(SUBID_2));
        assertEquals(CARRIER_1 + " 3", idNames.get(SUBID_3));
    }

    @Ignore
    @Test
    public void getUniqueDisplayName_onlyOneSubscription_correctNameReturned() {
        // Each subscription's default display name is unique.
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info1));
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("1112223333");

        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);

        final CharSequence name =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(SUBID_1, mContext);

        assertThat(name).isNotNull();
        assertEquals(CARRIER_1, name);
    }

    @Ignore
    @Test
    public void getUniqueDisplayName_identicalCarriers_correctNameReturned() {
        // Each subscription's default display name is unique.
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info2.getSubscriptionId()).thenReturn(SUBID_2);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(info2.getDisplayName()).thenReturn(CARRIER_1);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info1, info2));

        // Each subscription has a unique last 4 digits of the phone number.
        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        TelephonyManager sub2Telmgr = mock(TelephonyManager.class);
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("1112223333");
        when(mSubMgr.getPhoneNumber(SUBID_2)).thenReturn("2223334444");
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);
        when(mTelMgr.createForSubscriptionId(SUBID_2)).thenReturn(sub2Telmgr);

        final CharSequence name1 =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(SUBID_1, mContext);
        assertThat(name1).isNotNull();
        assertEquals(CARRIER_1 + " 3333", name1);
        final CharSequence name2 =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(SUBID_2, mContext);
        assertThat(name2).isNotNull();
        assertEquals(CARRIER_1 + " 4444", name2);
    }

    @Ignore
    @Test
    public void getUniqueDisplayName_phoneNumberIdentical_correctNameReturned() {
        // Each subscription's default display name is unique.
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info2.getSubscriptionId()).thenReturn(SUBID_2);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(info2.getDisplayName()).thenReturn(CARRIER_1);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info1, info2));

        // Both subscriptions have a the same 4 digits of the phone number.
        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        TelephonyManager sub2Telmgr = mock(TelephonyManager.class);
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("1112223333");
        when(mSubMgr.getPhoneNumber(SUBID_2)).thenReturn("2223334444");
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);
        when(mTelMgr.createForSubscriptionId(SUBID_2)).thenReturn(sub2Telmgr);

        final CharSequence name1 =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(SUBID_1, mContext);
        assertThat(name1).isNotNull();
        assertEquals(CARRIER_1 + " 1", name1);
        final CharSequence name2 =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(SUBID_2, mContext);
        assertThat(name2).isNotNull();
        assertEquals(CARRIER_1 + " 2", name2);
    }

    @Test
    public void getUniqueDisplayName_subscriptionNotActive_emptyString() {
        // Each subscription's default display name is unique.
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info1));

        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("1112223333");
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);

        // Subscription id is different than the one returned by the subscription manager.
        final CharSequence name =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(SUBID_2, mContext);

        assertThat(name).isNotNull();
        assertTrue(TextUtils.isEmpty(name));
    }

    @Ignore
    @Test
    public void getUniqueDisplayName_fullSubscriptionInfo_correctNameReturned() {
        // Each subscription's default display name is unique.
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(Arrays.asList(info1));

        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("1112223333");
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);

        final CharSequence name =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(info1, mContext);

        assertThat(name).isNotNull();
        assertEquals(CARRIER_1, name);
    }

    @Test
    public void getUniqueDisplayName_nullSubscriptionInfo_emptyStringReturned() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(mSubMgr.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(info1));

        TelephonyManager sub1Telmgr = mock(TelephonyManager.class);
        when(mSubMgr.getPhoneNumber(SUBID_1)).thenReturn("1112223333");
        when(mTelMgr.createForSubscriptionId(SUBID_1)).thenReturn(sub1Telmgr);

        SubscriptionInfo info2 = null;
        final CharSequence name =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(info2, mContext);

        assertThat(name).isNotNull();
        assertTrue(TextUtils.isEmpty(name));
    }

    @Test
    public void getUniqueDisplayName_hasRecord_useRecordBeTheResult() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info2.getSubscriptionId()).thenReturn(SUBID_2);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(info2.getDisplayName()).thenReturn(CARRIER_1);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(info1, info2));

        SharedPreferences sp = mock(SharedPreferences.class);
        when(mContext.getSharedPreferences(
                KEY_UNIQUE_SUBSCRIPTION_DISPLAYNAME, Context.MODE_PRIVATE)).thenReturn(sp);
        when(sp.getString(eq(SUB_ID + SUBID_1), anyString())).thenReturn(CARRIER_1 + " 6789");
        when(sp.getString(eq(SUB_ID + SUBID_2), anyString())).thenReturn(CARRIER_1 + " 4321");


        final CharSequence nameOfSub1 =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(info1, mContext);
        final CharSequence nameOfSub2 =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(info2, mContext);

        assertThat(nameOfSub1).isNotNull();
        assertThat(nameOfSub2).isNotNull();
        assertEquals(CARRIER_1 + " 6789", nameOfSub1.toString());
        assertEquals(CARRIER_1 + " 4321", nameOfSub2.toString());
    }

    @Test
    public void getUniqueDisplayName_hasRecordAndNameIsChanged_doesNotUseRecordBeTheResult() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUBID_1);
        when(info2.getSubscriptionId()).thenReturn(SUBID_2);
        when(info1.getDisplayName()).thenReturn(CARRIER_1);
        when(info2.getDisplayName()).thenReturn(CARRIER_2);
        when(mSubMgr.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(info1, info2));

        SharedPreferences sp = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(mContext.getSharedPreferences(
                KEY_UNIQUE_SUBSCRIPTION_DISPLAYNAME, Context.MODE_PRIVATE)).thenReturn(sp);
        when(sp.edit()).thenReturn(editor);
        when(editor.remove(anyString())).thenReturn(editor);

        when(sp.getString(eq(SUB_ID + SUBID_1), anyString())).thenReturn(CARRIER_1 + " 6789");
        when(sp.getString(eq(SUB_ID + SUBID_2), anyString())).thenReturn(CARRIER_1 + " 4321");


        final CharSequence nameOfSub1 =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(info1, mContext);
        final CharSequence nameOfSub2 =
                SubscriptionUtil.getUniqueSubscriptionDisplayName(info2, mContext);

        assertThat(nameOfSub1).isNotNull();
        assertThat(nameOfSub2).isNotNull();
        assertEquals(CARRIER_1 + " 6789", nameOfSub1.toString());
        assertEquals(CARRIER_2.toString(), nameOfSub2.toString());
    }

    @Test
    public void isInactiveInsertedPSim_nullSubInfo_doesNotCrash() {
        assertThat(SubscriptionUtil.isInactiveInsertedPSim(null)).isFalse();
    }

    @Test
    public void isSimHardwareVisible_configAsInvisible_returnFalse() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(R.bool.config_show_sim_info))
                .thenReturn(false);

        assertThat(SubscriptionUtil.isSimHardwareVisible(mContext)).isFalse();
    }

    @Test
    public void isSimHardwareVisible_configAsVisible_returnTrue() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(R.bool.config_show_sim_info))
                .thenReturn(true);

        assertTrue(SubscriptionUtil.isSimHardwareVisible(mContext));
    }

    @Test
    public void isValidCachedDisplayName_matchesRule1_returnTrue() {
        String originalName = "originalName";
        String cacheString = "originalName 1234";

        assertThat(SubscriptionUtil.isValidCachedDisplayName(cacheString, originalName)).isTrue();
    }

    @Test
    public void isValidCachedDisplayName_matchesRule2_returnTrue() {
        String originalName = "original Name";
        String cacheString = originalName + " " + 1234;

        assertThat(SubscriptionUtil.isValidCachedDisplayName(cacheString, originalName)).isTrue();
    }

    @Test
    public void isValidCachedDisplayName_nameIsEmpty1_returnFalse() {
        String originalName = "original Name";
        String cacheString = "";

        assertThat(SubscriptionUtil.isValidCachedDisplayName(cacheString, originalName)).isFalse();
    }

    @Test
    public void isValidCachedDisplayName_nameIsEmpty2_returnFalse() {
        String originalName = "";
        String cacheString = "originalName 1234";

        assertThat(SubscriptionUtil.isValidCachedDisplayName(cacheString, originalName)).isFalse();
    }

    @Test
    public void isValidCachedDisplayName_nameIsDifferent_returnFalse() {
        String originalName = "original Name";
        String cacheString = "originalName 1234";

        assertThat(SubscriptionUtil.isValidCachedDisplayName(cacheString, originalName)).isFalse();
    }

    @Test
    public void isValidCachedDisplayName_noNumber_returnFalse() {
        String originalName = "original Name";
        String cacheString = originalName;

        assertThat(SubscriptionUtil.isValidCachedDisplayName(cacheString, originalName)).isFalse();
    }

    @Test
    public void isValidCachedDisplayName_noSpace_returnFalse() {
        String originalName = "original Name";
        String cacheString = originalName;

        assertThat(SubscriptionUtil.isValidCachedDisplayName(cacheString, originalName)).isFalse();
    }

    @Test
    public void isConnectedToWifiOrDifferentSubId_hasWiFi_returnTrue() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        assertTrue(SubscriptionUtil.isConnectedToWifiOrDifferentSubId(mContext, SUBID_1));
    }

    @Test
    public void isConnectedToWifiOrDifferentSubId_noData_and_noWiFi_returnFalse() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH);

        assertFalse(SubscriptionUtil.isConnectedToWifiOrDifferentSubId(mContext, SUBID_1));
    }

    private void addNetworkTransportType(int networkType) {
        mNetworkCapabilities =
                new NetworkCapabilities.Builder().addTransportType(networkType).build();
        when(mConnectivityManager.getNetworkCapabilities(any())).thenReturn(mNetworkCapabilities);
    }
}
