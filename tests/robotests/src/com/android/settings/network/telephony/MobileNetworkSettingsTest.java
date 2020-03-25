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

import static com.android.settings.network.telephony.MobileNetworkSettings.REQUEST_CODE_DELETE_SUBSCRIPTION;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.NetworkPolicyManager;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.datausage.DataUsageSummaryPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowEntityHeaderController.class)
public class MobileNetworkSettingsTest {
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private NetworkStatsManager mNetworkStatsManager;
    @Mock
    private NetworkPolicyManager mNetworkPolicyManager;
    @Mock
    private FragmentActivity mActivity;

    private Context mContext;
    private MobileNetworkSettings mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(NetworkStatsManager.class)).thenReturn(mNetworkStatsManager);
        ShadowEntityHeaderController.setUseMock(mock(EntityHeaderController.class));

        mFragment = spy(new MobileNetworkSettings());
        final Bundle args = new Bundle();
        final int subscriptionId = 1234;
        args.putInt(Settings.EXTRA_SUB_ID, subscriptionId);
        mFragment.setArguments(args);
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.isFinishing()).thenReturn(false);
        when(mActivity.getSystemService(NetworkPolicyManager.class)).thenReturn(
                mNetworkPolicyManager);
    }

    @Test
    public void createPreferenceControllers_createsDataUsageSummaryController() {
        final List<AbstractPreferenceController> controllers =
                mFragment.createPreferenceControllers(mContext);
        assertThat(controllers.stream().filter(
                c -> c.getClass().equals(DataUsageSummaryPreferenceController.class))
                .count())
                .isEqualTo(1);
    }

    @Test
    public void onActivityResult_noActivity_noCrash() {
        when(mFragment.getActivity()).thenReturn(null);
        // this should not crash
        mFragment.onActivityResult(REQUEST_CODE_DELETE_SUBSCRIPTION, Activity.RESULT_OK, null);
    }

    @Test
    public void onActivityResult_deleteSubscription_activityFinishes() {
        mFragment.onActivityResult(REQUEST_CODE_DELETE_SUBSCRIPTION, Activity.RESULT_OK, null);
        verify(mActivity).finish();
    }

    @Test
    public void isPageSearchEnabled_adminUser_shouldReturnTrue() {
        final UserManager userManager = mock(UserManager.class);
        when(mContext.getSystemService(UserManager.class)).thenReturn(userManager);
        when(userManager.isAdminUser()).thenReturn(true);
        final BaseSearchIndexProvider provider =
                (BaseSearchIndexProvider) mFragment.SEARCH_INDEX_DATA_PROVIDER;

        final Object obj = ReflectionHelpers.callInstanceMethod(provider, "isPageSearchEnabled",
                ReflectionHelpers.ClassParameter.from(Context.class, mContext));
        final boolean isEnabled = (Boolean) obj;

        assertThat(isEnabled).isTrue();
    }

    @Test
    public void isPageSearchEnabled_nonAdminUser_shouldReturnFalse() {
        final UserManager userManager = mock(UserManager.class);
        when(mContext.getSystemService(UserManager.class)).thenReturn(userManager);
        when(userManager.isAdminUser()).thenReturn(false);
        final BaseSearchIndexProvider provider =
                (BaseSearchIndexProvider) mFragment.SEARCH_INDEX_DATA_PROVIDER;

        final Object obj = ReflectionHelpers.callInstanceMethod(provider, "isPageSearchEnabled",
                ReflectionHelpers.ClassParameter.from(Context.class, mContext));
        final boolean isEnabled = (Boolean) obj;

        assertThat(isEnabled).isFalse();
    }
}
