/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryOptimizationPreferenceControllerTest {

    private static final String PKG_IN_WHITELIST = "com.pkg.in.whitelist";
    private static final String PKG_NOT_IN_WHITELIST = "com.pkg.not.in.whitelist";
    private static final String KEY_OPTIMIZATION = "battery_optimization";
    private static final String KEY_OTHER = "other";
    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private DashboardFragment mFragment;
    @Mock
    private TestPowerWhitelistBackend mBackend;

    private BatteryOptimizationPreferenceController mController;
    private Preference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        doReturn(false).when(mBackend).isWhitelisted(PKG_NOT_IN_WHITELIST);
        doReturn(true).when(mBackend).isWhitelisted(PKG_IN_WHITELIST);

        mPreference = new SwitchPreference(mContext);
        mController = spy(new BatteryOptimizationPreferenceController(mSettingsActivity, mFragment,
                PKG_NOT_IN_WHITELIST, mBackend));
    }

    @Test
    public void testHandlePreferenceTreeClick_OptimizationPreference_HandleClick() {
        mPreference.setKey(KEY_OPTIMIZATION);

        final boolean handled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(handled).isTrue();
        verify(mSettingsActivity).startActivity(any(Intent.class));
    }

    @Test
    public void testHandlePreferenceTreeClick_OtherPreference_NotHandleClick() {
        mPreference.setKey(KEY_OTHER);

        final boolean handled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(handled).isFalse();
        verify(mSettingsActivity, never()).startActivity(any(Intent.class));
    }

    @Test
    public void testUpdateState_appInWhitelist_showSummaryNotOptimized() {
        BatteryOptimizationPreferenceController controller =
                new BatteryOptimizationPreferenceController(mSettingsActivity, mFragment,
                        PKG_IN_WHITELIST, mBackend);

        controller.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(mContext.getString(R.string.high_power_on));
    }

    @Test
    public void testUpdateState_appNotInWhitelist_showSummaryOptimized() {
        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(mContext.getString(R.string.high_power_off));
    }

    /**
     * Create this test class so we could mock it
     */
    public static class TestPowerWhitelistBackend extends PowerWhitelistBackend {

        public TestPowerWhitelistBackend(Context context) {
            super(context);
        }

        @Override
        public void refreshList() {
            // Do nothing so we could mock it without error
        }
    }
}
