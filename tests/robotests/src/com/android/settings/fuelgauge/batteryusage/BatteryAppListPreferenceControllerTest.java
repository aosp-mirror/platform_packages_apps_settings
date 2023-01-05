/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Process;
import android.os.UserManager;
import android.text.format.DateUtils;

import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryAppListPreferenceControllerTest {

    private static final String KEY_APP_LIST = "app_list";

    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private PreferenceGroup mAppListGroup;
    @Mock
    private InstrumentedPreferenceFragment mFragment;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private BatteryEntry mBatteryEntry;

    private Context mContext;
    private PowerGaugePreference mPreference;
    private BatteryAppListPreferenceController mPreferenceController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mContext = spy(RuntimeEnvironment.application);
        final Resources resources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(resources);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});
        when(mFeatureFactory.powerUsageFeatureProvider.getHideApplicationSummary(mContext))
                .thenReturn(new String[]{"com.android.googlequicksearchbox"});

        mPreference = new PowerGaugePreference(mContext);

        mPreferenceController = new BatteryAppListPreferenceController(mContext, KEY_APP_LIST, null,
                mSettingsActivity, mFragment);
        mPreferenceController.mBatteryUtils = mBatteryUtils;
        mPreferenceController.mAppListGroup = mAppListGroup;

        BatteryAppListPreferenceController.sConfig =
                new BatteryAppListPreferenceController.Config() {
                    @Override
                    public boolean shouldShowBatteryAttributionList(Context context) {
                        return true;
                    }
                };
    }

    @Test
    public void testSetUsageSummary_timeLessThanOneMinute_doNotSetSummary() {
        when(mBatteryEntry.getTimeInForegroundMs()).thenReturn(59 * DateUtils.SECOND_IN_MILLIS);

        mPreferenceController.setUsageSummary(mPreference, mBatteryEntry);
        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void testSetUsageSummary_systemProcessUid_doNotSetSummary() {
        when(mBatteryEntry.getTimeInForegroundMs()).thenReturn(DateUtils.MINUTE_IN_MILLIS);
        when(mBatteryEntry.getUid()).thenReturn(Process.SYSTEM_UID);

        mPreferenceController.setUsageSummary(mPreference, mBatteryEntry);
        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void testSetUsageSummary_timeMoreThanOneMinute_normalApp_setScreenSummary() {
        when(mBatteryEntry.getTimeInForegroundMs()).thenReturn(2 * DateUtils.MINUTE_IN_MILLIS);
        doReturn(mContext.getText(R.string.battery_used_for)).when(mFragment).getText(
                R.string.battery_used_for);
        doReturn(mContext).when(mFragment).getContext();

        mPreferenceController.setUsageSummary(mPreference, mBatteryEntry);

        assertThat(mPreference.getSummary().toString()).isEqualTo("Used for 2 min");
    }

    @Test
    public void testSetUsageSummary_timeMoreThanOneMinute_GoogleApp_shouldNotSetScreenSummary() {
        when(mBatteryEntry.getTimeInForegroundMs()).thenReturn(2 * DateUtils.MINUTE_IN_MILLIS);
        when(mBatteryEntry.getDefaultPackageName())
                .thenReturn("com.android.googlequicksearchbox");
        doReturn(mContext.getText(R.string.battery_used_for)).when(mFragment).getText(
                R.string.battery_used_for);
        doReturn(mContext).when(mFragment).getContext();

        mPreferenceController.setUsageSummary(mPreference, mBatteryEntry);

        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void testSetUsageSummary_timeMoreThanOneMinute_hiddenApp_setUsedSummary() {
        when(mBatteryEntry.getTimeInForegroundMs()).thenReturn(2 * DateUtils.MINUTE_IN_MILLIS);
        when(mBatteryEntry.isHidden()).thenReturn(true);

        doReturn(mContext).when(mFragment).getContext();

        mPreferenceController.setUsageSummary(mPreference, mBatteryEntry);

        assertThat(mPreference.getSummary().toString()).isEqualTo("2 min");
    }

    @Test
    public void testNeverUseFakeData() {
        assertThat(BatteryAppListPreferenceController.USE_FAKE_DATA).isFalse();
    }
}
