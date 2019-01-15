/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.datausage;

import static android.net.NetworkPolicy.CYCLE_NONE;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import android.util.FeatureFlagUtils;

import com.android.settings.core.FeatureFlags;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.NetworkPolicyEditor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class BillingCycleSettingsTest {

    private static final int LIMIT_BYTES = 123;

    @Mock
    BillingCycleSettings mMockBillingCycleSettings;
    BillingCycleSettings.ConfirmLimitFragment mConfirmLimitFragment;
    @Mock
    PreferenceManager mMockPreferenceManager;
    @Mock
    private NetworkPolicyEditor mNetworkPolicyEditor;

    private Context mContext;
    @Mock
    private Preference mBillingCycle;
    @Mock
    private Preference mDataWarning;
    @Mock
    private Preference mDataLimit;
    @Mock
    private SwitchPreference mEnableDataWarning;
    @Mock
    private SwitchPreference mEnableDataLimit;

    SharedPreferences mSharedPreferences;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mConfirmLimitFragment = new BillingCycleSettings.ConfirmLimitFragment();
        mConfirmLimitFragment.setTargetFragment(mMockBillingCycleSettings, 0);
        mSharedPreferences = RuntimeEnvironment.application.getSharedPreferences(
                "testSharedPreferences", Context.MODE_PRIVATE);
        when(mMockBillingCycleSettings.getPreferenceManager()).thenReturn(mMockPreferenceManager);
        when(mMockPreferenceManager.getSharedPreferences()).thenReturn(mSharedPreferences);
        final Bundle args = new Bundle();
        args.putLong(BillingCycleSettings.ConfirmLimitFragment.EXTRA_LIMIT_BYTES, LIMIT_BYTES);
        mConfirmLimitFragment.setArguments(args);
        mSharedPreferences.edit().putBoolean(
                BillingCycleSettings.KEY_SET_DATA_LIMIT, false).apply();
    }

    @Test
    public void testDataUsageLimit_shouldNotBeSetOnCancel() {
        mConfirmLimitFragment.onClick(null, DialogInterface.BUTTON_NEGATIVE);

        assertFalse(mSharedPreferences.getBoolean(BillingCycleSettings.KEY_SET_DATA_LIMIT, true));
        verify(mMockBillingCycleSettings, never()).setPolicyLimitBytes(anyLong());
    }

    @Test
    public void testDataUsageLimit_shouldBeSetOnConfirmation() {
        mConfirmLimitFragment.onClick(null, DialogInterface.BUTTON_POSITIVE);

        assertTrue(mSharedPreferences.getBoolean(BillingCycleSettings.KEY_SET_DATA_LIMIT, false));
        verify(mMockBillingCycleSettings).setPolicyLimitBytes(LIMIT_BYTES);
    }

    @Test
    public void testDataUsageSummary_shouldBeNullWithV2() {
        final BillingCycleSettings billingCycleSettings = spy(new BillingCycleSettings());
        when(billingCycleSettings.getContext()).thenReturn(mContext);
        billingCycleSettings.setUpForTest(mNetworkPolicyEditor, mBillingCycle,
                mDataLimit, mDataWarning, mEnableDataLimit, mEnableDataWarning);

        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.DATA_USAGE_SETTINGS_V2, true);

        doReturn("some-string").when(billingCycleSettings).getString(anyInt(), anyInt());
        when(mNetworkPolicyEditor.getPolicyCycleDay(anyObject())).thenReturn(CYCLE_NONE + 1);
        when(mNetworkPolicyEditor.getPolicyLimitBytes(anyObject())).thenReturn(2000L);
        when(mNetworkPolicyEditor.getPolicyWarningBytes(anyObject())).thenReturn(1000L);

        billingCycleSettings.updatePrefs();

        verify(mBillingCycle).setSummary(null);
    }
}