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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MobileDataSliceTest {

    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;

    private Context mContext;
    private MobileDataSlice mMobileDataSlice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(anyInt());
        doReturn(SUB_ID).when(mSubscriptionInfo).getSubscriptionId();
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mMobileDataSlice = spy(new MobileDataSlice(mContext));
    }

    @Test
    public void getSlice_shouldHaveTitleAndToggle() {
        final Slice mobileData = mMobileDataSlice.getSlice();

        final SliceMetadata metadata = SliceMetadata.from(mContext, mobileData);
        assertThat(metadata.getTitle())
                .isEqualTo(mContext.getString(R.string.mobile_data_settings_title));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        final IconCompat expectedToggleIcon = IconCompat.createWithResource(mContext,
                R.drawable.ic_network_cell);
        assertThat(primaryAction.getIcon().toString()).isEqualTo(expectedToggleIcon.toString());
    }

    @Test
    public void handleUriChange_turnedOn_updatesMobileData() {
        doReturn(false).when(mMobileDataSlice).isAirplaneModeEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        final Intent intent = mMobileDataSlice.getIntent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, true);

        mMobileDataSlice.onNotifyChange(intent);

        verify(mTelephonyManager)
                .setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, true);
    }

    @Test
    public void handleUriChange_turnedOff_updatesMobileData() {
        doReturn(false).when(mMobileDataSlice).isAirplaneModeEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        final Intent intent = mMobileDataSlice.getIntent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, false);

        mMobileDataSlice.onNotifyChange(intent);

        verify(mTelephonyManager)
                .setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, false);
    }

    @Test
    public void handleUriChange_turnedOff_airplaneModeOn_mobileDataDoesNotUpdate() {
        doReturn(true).when(mMobileDataSlice).isAirplaneModeEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        final Intent intent = mMobileDataSlice.getIntent();
        intent.putExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE, false);

        mMobileDataSlice.onNotifyChange(intent);

        verify(mTelephonyManager, times(0))
                .setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, true);
    }

    @Test
    public void isAirplaneModeEnabled_correctlyReturnsTrue() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);

        final boolean isAirplaneModeEnabled = mMobileDataSlice.isAirplaneModeEnabled();

        assertThat(isAirplaneModeEnabled).isTrue();
    }

    @Test
    public void isAirplaneModeEnabled_correctlyReturnsFalse() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

        final boolean isAirplaneModeEnabled = mMobileDataSlice.isAirplaneModeEnabled();

        assertThat(isAirplaneModeEnabled).isFalse();
    }

    @Test
    public void isMobileDataEnabled_mobileDataEnabled() {
        final boolean seed = true;
        doReturn(seed).when(mTelephonyManager).isDataEnabled();

        final boolean isMobileDataEnabled = mMobileDataSlice.isMobileDataEnabled();

        assertThat(isMobileDataEnabled).isEqualTo(seed);
    }

    @Test
    public void isMobileDataAvailable_noSubscriptions_slicePrimaryActionIsEmpty() {
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(new ArrayList<>());
        final Slice mobileData = mMobileDataSlice.getSlice();

        assertThat(mobileData).isNull();
    }

    @Test
    public void isMobileDataAvailable_nullSubscriptions_slicePrimaryActionIsEmpty() {
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(null);
        final Slice mobileData = mMobileDataSlice.getSlice();

        assertThat(mobileData).isNull();
    }

    @Test
    public void airplaneModeEnabled_slicePrimaryActionIsEmpty() {
        doReturn(true).when(mMobileDataSlice).isAirplaneModeEnabled();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(SUB_ID);
        final Slice mobileData = mMobileDataSlice.getSlice();

        assertThat(mobileData).isNull();
    }
}
