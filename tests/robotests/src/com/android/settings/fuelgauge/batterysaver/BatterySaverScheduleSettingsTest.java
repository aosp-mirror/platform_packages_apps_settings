package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Pair;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class BatterySaverScheduleSettingsTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private BatterySaverScheduleSettings mBatterySaverScheduleSettings;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mBatterySaverScheduleSettings = new BatterySaverScheduleSettings();
        mBatterySaverScheduleSettings.onAttach(mContext);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;

        setSchedule(PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE, 1);
        mBatterySaverScheduleSettings.onResume();
    }

    @Test
    public void onPause_withNoScheduleType_logExpectedData() {
        setSchedule(PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE, 0);

        mBatterySaverScheduleSettings.onPause();

        verifySchedule(SettingsEnums.BATTERY_SAVER_SCHEDULE_TYPE_NO_SCHEDULE,
                /* schedulePercentage= */ -1);
    }

    @Test
    public void onPause_withRoutineScheduleType_logExpectedData() {
        setSchedule(PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC, 0);

        mBatterySaverScheduleSettings.onPause();

        verifySchedule(SettingsEnums.BATTERY_SAVER_SCHEDULE_TYPE_BASED_ON_ROUTINE,
                /* schedulePercentage= */ -1);
    }

    @Test
    public void onPause_withPercentageScheduleType_logExpectedData() {
        int expectedPercentage = 10;
        setSchedule(PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE, expectedPercentage);

        mBatterySaverScheduleSettings.onPause();

        verifySchedule(SettingsEnums.BATTERY_SAVER_SCHEDULE_TYPE_BASED_ON_PERCENTAGE,
                expectedPercentage);
    }

    @Test
    public void onPause_scheduleTypeAndPercentageAreNotChanged_notLogAnyData() {
        mBatterySaverScheduleSettings.onResume();
        mBatterySaverScheduleSettings.onPause();

        waitAWhile();
        verifyNoMoreInteractions(mMetricsFeatureProvider);
    }

    @Test
    public void onPause_multipleScheduleTypeChanges_logLastChangedData() {
        int expectedPercentage = 10;
        setSchedule(PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE, 0);
        setSchedule(PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC, 0);
        setSchedule(PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE, expectedPercentage);

        mBatterySaverScheduleSettings.onPause();

        verifySchedule(SettingsEnums.BATTERY_SAVER_SCHEDULE_TYPE_BASED_ON_PERCENTAGE,
                expectedPercentage);
    }

    private void setSchedule(int scheduleType, int schedulePercentage) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AUTOMATIC_POWER_SAVE_MODE, scheduleType);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, schedulePercentage);
    }

    private void verifySchedule(int scheduleType, int schedulePercentage) {
        waitAWhile();
        verify(mMetricsFeatureProvider).action(mContext, SettingsEnums.FUELGAUGE_BATTERY_SAVER,
                Pair.create(SettingsEnums.FIELD_BATTERY_SAVER_SCHEDULE_TYPE,
                        scheduleType),
                Pair.create(SettingsEnums.FIELD_BATTERY_SAVER_PERCENTAGE_VALUE,
                        schedulePercentage));
    }

    private void waitAWhile() {
        try {
            Thread.sleep(200);
        } catch (Exception e) {
        }
    }
}
