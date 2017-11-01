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

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BillingCycleSettingsTest {

    private static final int LIMIT_BYTES = 123;

    @Mock
    BillingCycleSettings mMockBillingCycleSettings;
    BillingCycleSettings.ConfirmLimitFragment mConfirmLimitFragment;
    @Mock
    PreferenceManager mMockPreferenceManager;
    SharedPreferences mSharedPreferences;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
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
}