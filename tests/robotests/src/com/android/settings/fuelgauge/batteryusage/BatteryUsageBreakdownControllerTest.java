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
import android.content.ContentValues;
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
    private static final String PREF_KEY2 = "pref_key2";
    private static final String PREF_SUMMARY = "fake preference summary";

    @Mock private InstrumentedPreferenceFragment mFragment;
    @Mock private SettingsActivity mSettingsActivity;
    @Mock private PreferenceGroup mAppListPreferenceGroup;
    @Mock private Drawable mDrawable;
    @Mock private BatteryHistEntry mBatteryHistEntry;
    @Mock private AnomalyAppItemPreference mAnomalyAppItemPreference;

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
                .getHideApplicationSet();
        mBatteryUsageBreakdownController = createController();
        mBatteryUsageBreakdownController.mAppListPreferenceGroup = mAppListPreferenceGroup;
        mBatteryDiffEntry =
                new BatteryDiffEntry(
                        mContext,
                        /* uid= */ 0L,
                        /* userId= */ 0L,
                        /* key= */ "key",
                        /* isHidden= */ false,
                        /* componentId= */ -1,
                        /* legacyPackageName= */ null,
                        /* legacyLabel= */ null,
                        /* consumerType= */ ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                        /* foregroundUsageTimeInMs= */ 1,
                        /* foregroundServiceUsageTimeInMs= */ 2,
                        /* backgroundUsageTimeInMs= */ 3,
                        /* screenOnTimeInMs= */ 0,
                        /* consumePower= */ 3,
                        /* foregroundUsageConsumePower= */ 0,
                        /* foregroundServiceUsageConsumePower= */ 1,
                        /* backgroundUsageConsumePower= */ 2,
                        /* cachedUsageConsumePower= */ 0);
        mBatteryDiffEntry = spy(mBatteryDiffEntry);
        mBatteryUsageBreakdownController.mBatteryDiffData =
                new BatteryDiffData(
                        mContext,
                        /* startTimestamp= */ 0L,
                        /* endTimestamp= */ 0L,
                        /* startBatteryLevel= */ 0,
                        /* endBatteryLevel= */ 0,
                        /* screenOnTime= */ 0L,
                        Arrays.asList(mBatteryDiffEntry),
                        Arrays.asList(),
                        Set.of(),
                        Set.of(),
                        /* isAccumulated= */ false);
        BatteryDiffEntry.clearCache();
        // Adds fake testing data.
        BatteryDiffEntry.sResourceCache.put(
                "fakeBatteryDiffEntryKey",
                new BatteryEntry.NameAndIcon("fakeName", /* icon= */ null, /* iconId= */ 1));
        doReturn(mAnomalyAppItemPreference).when(mAppListPreferenceGroup).findPreference(PREF_KEY);
    }

    @Test
    public void onDestroy_clearPreferenceCacheAndPreferenceGroupRemoveAll() {
        // Ensures the testing environment is correct.
        mBatteryUsageBreakdownController.mPreferenceCache.put(PREF_KEY, mAnomalyAppItemPreference);
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
        doReturn(PREF_KEY).when(mBatteryDiffEntry).getKey();
        doReturn(null).when(mAppListPreferenceGroup).findPreference(PREF_KEY);
        doReturn(false).when(mBatteryDiffEntry).validForRestriction();

        mBatteryUsageBreakdownController.addAllPreferences();

        // Verifies the preference cache.
        final PowerGaugePreference pref =
                (PowerGaugePreference)
                        mBatteryUsageBreakdownController.mPreferenceCache.get(PREF_KEY);
        assertThat(pref).isNotNull();
        // Verifies the added preference configuration.
        verify(mAppListPreferenceGroup).addPreference(pref);
        assertThat(pref.getKey()).isEqualTo(PREF_KEY);
        assertThat(pref.getTitle().toString()).isEqualTo(appLabel);
        assertThat(pref.getIcon()).isEqualTo(mDrawable);
        assertThat(pref.getOrder()).isEqualTo(1);
        assertThat(pref.getBatteryDiffEntry()).isSameInstanceAs(mBatteryDiffEntry);
        assertThat(pref.isSingleLineTitle()).isTrue();
        assertThat(pref.isSelectable()).isFalse();
    }

    @Test
    public void addPreferenceToScreen_alreadyInScreen_notAddPreferenceAgain() {
        final String appLabel = "fake app label";
        doReturn(1).when(mAppListPreferenceGroup).getPreferenceCount();
        doReturn(mDrawable).when(mBatteryDiffEntry).getAppIcon();
        doReturn(appLabel).when(mBatteryDiffEntry).getAppLabel();
        doReturn(PREF_KEY).when(mBatteryDiffEntry).getKey();

        mBatteryUsageBreakdownController.addAllPreferences();

        verify(mAppListPreferenceGroup, never()).addPreference(any());
    }

    @Test
    public void removeAndCacheAllUnusedPreferences_removePref_buildCacheAndRemoveAllPreference() {
        doReturn(1).when(mAppListPreferenceGroup).getPreferenceCount();
        doReturn(mAnomalyAppItemPreference).when(mAppListPreferenceGroup).getPreference(0);
        doReturn(PREF_KEY2).when(mBatteryHistEntry).getKey();
        doReturn(PREF_KEY).when(mAnomalyAppItemPreference).getKey();
        // Ensures the testing data is correct.
        assertThat(mBatteryUsageBreakdownController.mPreferenceCache).isEmpty();

        mBatteryUsageBreakdownController.removeAndCacheAllUnusedPreferences();

        assertThat(mBatteryUsageBreakdownController.mPreferenceCache.get(PREF_KEY))
                .isEqualTo(mAnomalyAppItemPreference);
        verify(mAppListPreferenceGroup).removePreference(mAnomalyAppItemPreference);
    }

    @Test
    public void removeAndCacheAllUnusedPreferences_keepPref_KeepAllPreference() {
        doReturn(1).when(mAppListPreferenceGroup).getPreferenceCount();
        doReturn(mAnomalyAppItemPreference).when(mAppListPreferenceGroup).getPreference(0);
        doReturn(PREF_KEY).when(mBatteryDiffEntry).getKey();
        doReturn(PREF_KEY).when(mAnomalyAppItemPreference).getKey();
        // Ensures the testing data is correct.
        assertThat(mBatteryUsageBreakdownController.mPreferenceCache).isEmpty();

        mBatteryUsageBreakdownController.removeAndCacheAllUnusedPreferences();

        verify(mAppListPreferenceGroup, never()).removePreference(any());
        assertThat(mBatteryUsageBreakdownController.mPreferenceCache).isEmpty();
    }

    @Test
    public void handlePreferenceTreeClick_notPowerGaugePreference_returnFalse() {
        assertThat(
                        mBatteryUsageBreakdownController.handlePreferenceTreeClick(
                                mAppListPreferenceGroup))
                .isFalse();

        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_APP_ITEM);
        verify(mMetricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_BATTERY_USAGE_SYSTEM_ITEM);
    }

    @Test
    public void handlePreferenceTreeClick_forAppEntry_returnTrue() {
        mBatteryDiffEntry.mConsumerType = ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY;
        doReturn(mBatteryDiffEntry).when(mAnomalyAppItemPreference).getBatteryDiffEntry();

        assertThat(
                        mBatteryUsageBreakdownController.handlePreferenceTreeClick(
                                mAnomalyAppItemPreference))
                .isTrue();
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
        mBatteryDiffEntry.mConsumerType = ConvertUtils.CONSUMER_TYPE_UID_BATTERY;
        doReturn(mBatteryDiffEntry).when(mAnomalyAppItemPreference).getBatteryDiffEntry();

        assertThat(
                        mBatteryUsageBreakdownController.handlePreferenceTreeClick(
                                mAnomalyAppItemPreference))
                .isTrue();
        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.OPEN_BATTERY_USAGE,
                        SettingsEnums.ACTION_BATTERY_USAGE_APP_ITEM,
                        SettingsEnums.OPEN_BATTERY_USAGE,
                        /* package name */ "none",
                        /* percentage of total */ 100);
    }

    @Test
    public void setPreferencePercent_lessThanThreshold_expectedFormat() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        final BatteryDiffEntry batteryDiffEntry =
                createBatteryDiffEntry(
                        /* isSystem= */ true,
                        /* screenOnTimeInMs= */ 0,
                        /* foregroundUsageTimeInMs= */ 0,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ 0);
        batteryDiffEntry.mConsumePower = 0.8;
        batteryDiffEntry.setTotalConsumePower(100);
        mBatteryUsageBreakdownController.mPercentLessThanThresholdText = "< 1%";

        mBatteryUsageBreakdownController.setPreferencePercentage(pref, batteryDiffEntry);

        assertThat(pref.getPercentage()).isEqualTo("< 1%");
    }

    @Test
    public void setPreferencePercent_greaterThanThreshold_expectedFormat() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        final BatteryDiffEntry batteryDiffEntry =
                createBatteryDiffEntry(
                        /* isSystem= */ true,
                        /* screenOnTimeInMs= */ 0,
                        /* foregroundUsageTimeInMs= */ 0,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ 0);
        batteryDiffEntry.mConsumePower = 16;
        batteryDiffEntry.setTotalConsumePower(100);
        mBatteryUsageBreakdownController.mPercentLessThanThresholdText = "< 1%";

        mBatteryUsageBreakdownController.setPreferencePercentage(pref, batteryDiffEntry);

        assertThat(pref.getPercentage()).isEqualTo("16%");
    }

    @Test
    public void setPreferenceSummary_systemEntryTotalUsageTimeIsZero_emptySummary() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref,
                createBatteryDiffEntry(
                        /* isSystem= */ true,
                        /* screenOnTimeInMs= */ 0,
                        /* foregroundUsageTimeInMs= */ 0,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ 0));
        assertThat(pref.getSummary().toString().isEmpty()).isTrue();
    }

    @Test
    public void setPreferenceSummary_systemEntryTotalUsageTimeLessThanAMinute_expectedSummary() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref,
                createBatteryDiffEntry(
                        /* isSystem= */ true,
                        /* screenOnTimeInMs= */ 0,
                        /* foregroundUsageTimeInMs= */ DateUtils.MINUTE_IN_MILLIS - 1,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ 0));
        assertThat(pref.getSummary().toString()).isEqualTo("Total: less than a min");
    }

    @Test
    public void setPreferenceSummary_systemEntryTotalUsageTimeGreaterThanAMinute_expectedSummary() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref,
                createBatteryDiffEntry(
                        /* isSystem= */ true,
                        /* screenOnTimeInMs= */ 0,
                        /* foregroundUsageTimeInMs= */ DateUtils.MINUTE_IN_MILLIS * 2,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ 0));
        assertThat(pref.getSummary().toString()).isEqualTo("Total: 2 min");
    }

    @Test
    public void setPreferenceSummary_appEntryAllTimesAreZero_emptySummary() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref,
                createBatteryDiffEntry(
                        /* isSystem= */ false,
                        /* screenOnTimeInMs= */ 0,
                        /* foregroundUsageTimeInMs= */ 0,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ 0));
        assertThat(pref.getSummary().toString().isEmpty()).isTrue();
    }

    @Test
    public void setPreferenceSummary_appEntryBackgroundUsageTimeOnly_expectedSummary() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref,
                createBatteryDiffEntry(
                        /* isSystem= */ false,
                        /* screenOnTimeInMs= */ 0,
                        /* foregroundUsageTimeInMs= */ 0,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ DateUtils.MINUTE_IN_MILLIS));
        assertThat(pref.getSummary().toString()).isEqualTo("Background: 1 min");
    }

    @Test
    public void setPreferenceSummary_appEntryWithFGSTime_expectedSummary() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref,
                createBatteryDiffEntry(
                        /* isSystem= */ false,
                        /* screenOnTimeInMs= */ 0,
                        /* foregroundUsageTimeInMs= */ 0,
                        /* foregroundServiceUsageTimeInMs= */ DateUtils.MINUTE_IN_MILLIS / 2,
                        /* backgroundUsageTimeInMs= */ DateUtils.MINUTE_IN_MILLIS / 2));
        assertThat(pref.getSummary().toString()).isEqualTo("Background: 1 min");
    }

    @Test
    public void setPreferenceSummary_appEntryScreenOnTimeOnly_expectedSummary() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref,
                createBatteryDiffEntry(
                        /* isSystem= */ false,
                        /* screenOnTimeInMs= */ DateUtils.MINUTE_IN_MILLIS,
                        /* foregroundUsageTimeInMs= */ 0,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ 0));
        assertThat(pref.getSummary().toString()).isEqualTo("Screen time: 1 min");
    }

    @Test
    public void setPreferenceSummary_appEntryAllTimesLessThanAMinute_expectedSummary() {
        final PowerGaugePreference pref = new PowerGaugePreference(mContext);
        pref.setSummary(PREF_SUMMARY);

        mBatteryUsageBreakdownController.setPreferenceSummary(
                pref,
                createBatteryDiffEntry(
                        /* isSystem= */ false,
                        /* screenOnTimeInMs= */ DateUtils.MINUTE_IN_MILLIS - 1,
                        /* foregroundUsageTimeInMs= */ DateUtils.MINUTE_IN_MILLIS - 1,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ DateUtils.MINUTE_IN_MILLIS - 1));
        assertThat(pref.getSummary().toString())
                .isEqualTo("Screen time: less than a min\nBackground: less than a min");
    }

    private BatteryDiffEntry createBatteryDiffEntry(
            boolean isSystem,
            long screenOnTimeInMs,
            long foregroundUsageTimeInMs,
            long foregroundServiceUsageTimeInMs,
            long backgroundUsageTimeInMs) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(
                BatteryHistEntry.KEY_CONSUMER_TYPE,
                Integer.valueOf(
                        isSystem
                                ? ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY
                                : ConvertUtils.CONSUMER_TYPE_UID_BATTERY));
        contentValues.put(BatteryHistEntry.KEY_USER_ID, Integer.valueOf(1001));
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(contentValues);
        return new BatteryDiffEntry(
                mContext,
                batteryHistEntry.mUid,
                batteryHistEntry.mUserId,
                batteryHistEntry.getKey(),
                batteryHistEntry.mIsHidden,
                batteryHistEntry.mDrainType,
                batteryHistEntry.mPackageName,
                batteryHistEntry.mAppLabel,
                batteryHistEntry.mConsumerType,
                foregroundUsageTimeInMs,
                backgroundUsageTimeInMs,
                foregroundServiceUsageTimeInMs,
                screenOnTimeInMs,
                /* consumePower= */ 0,
                /* foregroundUsageConsumePower= */ 0,
                /* foregroundServiceUsageConsumePower= */ 0,
                /* backgroundUsageConsumePower= */ 0,
                /* cachedUsageConsumePower= */ 0);
    }

    private BatteryUsageBreakdownController createController() {
        final BatteryUsageBreakdownController controller =
                new BatteryUsageBreakdownController(
                        mContext, /* lifecycle= */ null, mSettingsActivity, mFragment);
        controller.mPrefContext = mContext;
        return controller;
    }
}
