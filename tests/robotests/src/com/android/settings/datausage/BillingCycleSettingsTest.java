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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.NetworkPolicyManager;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.NetworkPolicyEditor;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class BillingCycleSettingsTest {

    private static final int LIMIT_BYTES = 123;

    @Mock
    private BillingCycleSettings mMockBillingCycleSettings;
    private BillingCycleSettings.ConfirmLimitFragment mConfirmLimitFragment;
    @Mock
    private PreferenceManager mMockPreferenceManager;
    @Mock
    private NetworkPolicyEditor mNetworkPolicyEditor;
    @Mock
    private NetworkPolicyManager mNetworkPolicyManager;
    @Mock
    private PackageManager mMockPackageManager;

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

    private SharedPreferences mSharedPreferences;

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

        assertThat(mSharedPreferences.getBoolean(BillingCycleSettings.KEY_SET_DATA_LIMIT, true))
            .isFalse();
        verify(mMockBillingCycleSettings, never()).setPolicyLimitBytes(anyLong());
    }

    @Test
    public void testDataUsageLimit_shouldBeSetOnConfirmation() {
        mConfirmLimitFragment.onClick(null, DialogInterface.BUTTON_POSITIVE);

        assertThat(mSharedPreferences.getBoolean(BillingCycleSettings.KEY_SET_DATA_LIMIT, false))
            .isTrue();
        verify(mMockBillingCycleSettings).setPolicyLimitBytes(LIMIT_BYTES);
    }

    @Test
    public void testDataUsageSummary_shouldBeNull() {
        final BillingCycleSettings billingCycleSettings = spy(new BillingCycleSettings());
        when(billingCycleSettings.getContext()).thenReturn(mContext);
        billingCycleSettings.setUpForTest(mNetworkPolicyEditor, mBillingCycle,
                mDataLimit, mDataWarning, mEnableDataLimit, mEnableDataWarning);

        doReturn("some-string").when(billingCycleSettings).getString(anyInt(), anyInt());
        when(mNetworkPolicyEditor.getPolicyCycleDay(any())).thenReturn(CYCLE_NONE + 1);
        when(mNetworkPolicyEditor.getPolicyLimitBytes(any())).thenReturn(2000L);
        when(mNetworkPolicyEditor.getPolicyWarningBytes(any())).thenReturn(1000L);

        billingCycleSettings.updatePrefs();

        verify(mBillingCycle).setSummary(null);
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    @Ignore
    public void onCreate_emptyArguments_shouldSetDefaultNetworkTemplate() {
        final BillingCycleSettings billingCycleSettings = spy(new BillingCycleSettings());
        when(billingCycleSettings.getContext()).thenReturn(mContext);
        when(billingCycleSettings.getArguments()).thenReturn(Bundle.EMPTY);
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(billingCycleSettings.getActivity()).thenReturn(activity);
        final Resources.Theme theme = mContext.getTheme();
        when(activity.getTheme()).thenReturn(theme);
        doNothing().when(billingCycleSettings)
            .onCreatePreferences(any(Bundle.class), nullable(String.class));
        when(mContext.getSystemService(Context.NETWORK_POLICY_SERVICE))
            .thenReturn(mNetworkPolicyManager);
        when(mContext.getPackageManager()).thenReturn(mMockPackageManager);
        when(mMockPackageManager.hasSystemFeature(any())).thenReturn(true);
        final SwitchPreference preference = mock(SwitchPreference.class);
        when(billingCycleSettings.findPreference(anyString())).thenReturn(preference);

        billingCycleSettings.onCreate(Bundle.EMPTY);

        assertThat(billingCycleSettings.mNetworkTemplate).isNotNull();
    }
}
