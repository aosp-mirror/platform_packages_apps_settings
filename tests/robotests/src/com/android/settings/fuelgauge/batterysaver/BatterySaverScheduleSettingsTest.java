package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowInteractionJankMonitor.class})
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
        int expectedPercentage = 0;
        setSchedule(PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE, expectedPercentage);

        mBatterySaverScheduleSettings.onPause();

        verifySchedule("key_battery_saver_no_schedule", expectedPercentage);
    }

    @Test
    public void onPause_withPercentageScheduleType_logExpectedData() {
        int expectedPercentage = 10;
        setSchedule(PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE, expectedPercentage);

        mBatterySaverScheduleSettings.onPause();

        verifySchedule("key_battery_saver_percentage", expectedPercentage);
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

        verifySchedule("key_battery_saver_percentage", expectedPercentage);
    }

    @Test
    public void getMetricsCategory_returnExpectedResult() {
        assertThat(mBatterySaverScheduleSettings.getMetricsCategory())
                .isEqualTo(SettingsEnums.FUELGAUGE_BATTERY_SAVER_SCHEDULE);
    }

    private void setSchedule(int scheduleType, int schedulePercentage) {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.AUTOMATIC_POWER_SAVE_MODE,
                scheduleType);
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                schedulePercentage);
    }

    private void verifySchedule(String scheduleTypeKey, int schedulePercentage) {
        waitAWhile();
        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_SAVER,
                        SettingsEnums.FIELD_BATTERY_SAVER_SCHEDULE_TYPE,
                        SettingsEnums.FIELD_BATTERY_SAVER_PERCENTAGE_VALUE,
                        scheduleTypeKey,
                        schedulePercentage);
    }

    private void waitAWhile() {
        try {
            Thread.sleep(200);
        } catch (Exception e) {
        }
    }
}
