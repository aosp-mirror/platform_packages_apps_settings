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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.LocaleList;
import android.text.format.DateUtils;

import androidx.preference.PreferenceGroup;

import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class BatteryUsageBreakdownControllerTest {
    private static final String PREF_KEY = "pref_key";
    private static final String PREF_SUMMARY = "fake preference summary";

    @Mock
    private InstrumentedPreferenceFragment mFragment;
    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private PreferenceGroup mAppListPreferenceGroup;
    @Mock
    private Drawable mDrawable;
    @Mock
    private BatteryHistEntry mBatteryHistEntry;
    @Mock
    private PowerGaugePreference mPowerGaugePreference;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryDiffEntry mBatteryDiffEntry;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private BatteryUsageBreakdownController mBatteryUsageBreakdownController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(new Locale("en_US"));
        org.robolectric.shadows.ShadowSettings.set24HourTimeFormat(false);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;
        mContext = spy(RuntimeEnvironment.application);
        final Resources resources = spy(mContext.getResources());
        resources.getConfiguration().setLocales(new LocaleList(new Locale("en_US")));
        doReturn(resources).when(mContext).getResources();
        doReturn(Set.of("com.android.gms.persistent"))
                .when(mFeatureFactory.powerUsageFeatureProvider)
                .getHideApplicationSet(mContext);
        mBatteryUsageBreakdownController = createController();
        mBatteryUsageBreakdownController.mAppListPreferenceGroup = mAppListPreferenceGroup;
        mBatteryDiffEntry = new BatteryDiffEntry(
                mContext,
                /*foregroundUsageTimeInMs=*/ 1,
                /*backgroundUsageTimeInMs=*/ 2,
                /*screenOnTimeInMs=*/ 0,
                /*consumePower=*/ 3,
                /*foregroundUsageConsumePower=*/ 0,
                /*foregroundServiceUsageConsumePower=*/ 1,
                /*backgroundUsageConsumePower=*/ 2,
                /*cachedUsageConsumePower=*/ 0,
                mBatteryHistEntry);
        mBatteryDiffEntry = spy(mBatteryDiffEntry);
        mBatteryUsageBreakdownController.mBatteryDiffData =
                new BatteryDiffData(Arrays.asList(mBatteryDiffEntry), Arrays.asList());
        mBatteryUsageBreakdownController.mBatteryDiffData.setTotalConsumePower();
        mBatteryUsageBreakdownController.mBatteryDiffData.sortEntries();
        // Adds fake testing data.
        BatteryDiffEntry.sResourceCache.put(
                "fakeBatteryDiffEntryKey",
                new BatteryEntry.NameAndIcon("fakeName", /*icon=*/ null, /*iconId=*/ 1));
    }

    @Test
    public void onDestroy_clearPreferenceCacheAndPreferenceGroupRemoveAll() {
        // Ensures the testing environment is correct.
        mBatteryUsageBreakdownController.mPreferenceCache.put(
                PREF_KEY, mPowerGaugePreference);
        assertThat(mBatteryUsageBreakdownController.mPreferenceCache).hasSize(1);

        mBatteryUsageBreakdownController.onDestroy();

        assertThat(mBatteryUsageBreakdownController.mPreferenceCache).isEmpty();
    }

    @Test
    public void onDestroy_removeAllPreferenceFromPreferenceGroup() {
        mBatteryUsageBreakdownController.onDestroy();
        verify(mAppListPreferenceGroup).removeAll();
    }

    @Test
    public void addAllPreferences_addAllPreferences() {
        final String appLabel = "fake app label";
        doReturn(1).when(mAppListPreferenceGroup).getPreferenceCount();
        doReturn(mDrawable).when(mBatteryDiffEntry).getAppIcon();
        doReturn(appLabel).when(mBatteryDiffEntry).getAppLabel();
        doReturn(PREF_KEY).when(mBatteryHistEntry).getKey();
        doReturn(null).when(mAppListPreferenceGroup).findPreference(PREF_KEY);
        doReturn(false).when(mBatteryDiffEntry).validForRestriction();

        mBatteryUsageBreakdownController.addAllPreferences();

        // Verifies the preference cache.
        final PowerGaugePreference pref =
                (PowerGaugePreference) mBatteryUsageBreakdownController.mPreferenceCache
                        .get(PREF_KEY);
        assertThat(pref).isNotNull();
        // Verifies the added preference configuration.
        verify(mAppListPreferenceGroup).addPreference(pref);
        assertThat(pref.getKey()).isEqualTo(PREF_KEY);
        assertThat(pref.getTitle().toString()).isEqualTo(appLabel);
        assertThat(pref.getIcon()).isEqualTo(mDrawable);
        assertThat(pref.getOrder()).isEqualTo(1);
        assertThat(pref.getBatteryDiffEntry()).isSameInstanceAs(mBatteryDiffEntry);
        assertThat(pref.isSingleLineTitle()).isTrue();
        assertThat(pref.isEnabled()).isFalse();
    }

    @Test
    public void addPreferenceToScreen_alreadyInScreen_notAddPreferenceAgain() {
        final String appLabel = "fake app label";
        doReturn(1).when(mAppListPreferenceGroup).getPreferenceCount();
        doReturn(mDrawable).when(mBatteryDiffEntry).getAppIcon();
        doReturn(appLabel).when(mBatteryDiffEntry).getAppLabel();
        doReturn(PREF_KEY).when(mBatteryHistEntry).getKey();
        doReturn(mPowerGaugePreference).when(mAppListPreferenceGroup).findPreference(PREF_KEY);

        mBatteryUsageBreakdownController.addAllPreferences();

        verify(mAppListPreferenceGroup, never()).addPreference(any());
    }

    @Test
    public void removeAndCacheAllPreferences_buildCacheAndRemoveAllPreference() {
        doReturn(1).when(mAppListPreferenceGroup).getPreferenceCount();
        doReturn(mPowerGaugePreference).when(mAppListPreferenceGroup).getPreference(0);
        doReturn(PREF_KEY).when(mBatteryHistEntry).getKey();
        doReturn(PREF_KEY).when(mPowerGaugePreference).getKey();
        doReturn(mPowerGaugePreference).when(mAppListPreferenceGroup).findPreference(PREF_KEY);
        // Ensures the testing data is correct.
        assertThat(mBatteryUsageBreakdownController.mPreferenceCache).isEmpty();

        mBatteryUsageBreakdownController.removeAndCacheAllPreferences();

        assertThat(mBatteryUsageBreakdownController.mPreferenceCache.get(PREF_KEY))
                .isEqualTo(mPowerGaugePreference);
        verify(mAppListPreferenceGroup).removeAll();
    }

    @Test
    public void handlePreferenceTreeClick_notPowerGaugePreference_returnFalse() {
        assertThat(mBatteryUsageBreakdownController
                .handlePreferenceTreeClick(mAppListPreferenceGroup)).isFalse();

        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_APP_ITEM);
        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_SYSTEM_ITEM);
    }

    @Test
    public void handlePreferenceTreeClick_forAppEntry_returnTrue() {
        doReturn(false).when(mBatteryHistEntry).isAppEntry();
        doReturn(mBatteryDiffEntry).when(mPowerGaugePreference).getBatteryDiffEntry();

        assertThat(mBatteryUsageBreakdownController.handlePreferenceTreeClick(
                mPowerGaugePreference)).isTrue();
        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.OPEN_BATTERY_USAGE,
                        SettingsEnums.ACTION_BATTERY_USAGE_SYSTEM_ITEM,
                        SettingsEnums.OPEN_BATTERY_USAGE,
                        /* package name */ "none",
                        /* percentage of total */ 100);
    }

    @Test
    public void handlePreferenceTreeClick_forSystemEntry_returnTrue() {
        doReturn(true).when(mBatteryHistEntry).isAppEntry();
        doReturn(mBatteryDiffEntry).when(mPowerGaugePreference).getBatteryDiffEntry();

        assertThat(mBatteryUsageBreakdownController.handlePreferenceTreeClick(
                mPowerGaugePreference)).isTrue();
        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.OPEN_BATTERY_USAGE,
                        SettingsEnums.ACTION_BATTERY_USAGE_APP_ITEM,
                        SettingsEnums.OPEN_BATTERY_USAGE,
                        /* package name */ "none",
                        /* percentage of total */ 100);
    }

    @Test
    public void setPreferenceSummary_setNullContentIfTotalUsageTimeIsZero() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref, createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ 0,
                        /*backgroundUsageTimeInMs=*/ 0));
        assertThat(pref.getSummary()).isNull();
    }

    @Test
    public void setPreferenceSummary_setBackgroundUsageTimeOnly() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref, createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ 0,
                        /*backgroundUsageTimeInMs=*/ DateUtils.MINUTE_IN_MILLIS));
        assertThat(pref.getSummary().toString()).isEqualTo("Background: 1 min");
    }

    @Test
    public void setPreferenceSummary_setTotalUsageTimeLessThanAMinute() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref, createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ 100,
                        /*backgroundUsageTimeInMs=*/ 200));
        assertThat(pref.getSummary().toString()).isEqualTo("Total: less than a min");
    }

    @Test
    public void setPreferenceSummary_setTotalTimeIfBackgroundTimeLessThanAMinute() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref, createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ DateUtils.MINUTE_IN_MILLIS,
                        /*backgroundUsageTimeInMs=*/ 200));
        assertThat(pref.getSummary().toString())
                .isEqualTo("Total: 1 min\nBackground: less than a min");
    }

    @Test
    public void setPreferenceSummary_setTotalAndBackgroundUsageTime() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref, createBatteryDiffEntry(
                        /*foregroundUsageTimeInMs=*/ DateUtils.MINUTE_IN_MILLIS,
                        /*backgroundUsageTimeInMs=*/ DateUtils.MINUTE_IN_MILLIS));
        assertThat(pref.getSummary().toString()).isEqualTo("Total: 2 min\nBackground: 1 min");
    }

    private BatteryDiffEntry createBatteryDiffEntry(
            long foregroundUsageTimeInMs, long backgroundUsageTimeInMs) {
        return new BatteryDiffEntry(
                mContext, foregroundUsageTimeInMs, backgroundUsageTimeInMs, /*screenOnTimeInMs=*/ 0,
                /*consumePower=*/ 0, /*foregroundUsageConsumePower=*/ 0,
                /*foregroundServiceUsageConsumePower=*/ 0, /*backgroundUsageConsumePower=*/ 0,
                /*cachedUsageConsumePower=*/ 0, mBatteryHistEntry);
    }

    private BatteryUsageBreakdownController createController() {
        final BatteryUsageBreakdownController controller =
                new BatteryUsageBreakdownController(
                        mContext, /*lifecycle=*/ null, mSettingsActivity, mFragment);
        controller.mPrefContext = mContext;
        return controller;
    }
}
