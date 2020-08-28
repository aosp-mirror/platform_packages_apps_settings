package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatterySaverScheduleRadioButtonsControllerTest {
    private Context mContext;
    private ContentResolver mResolver;
    private BatterySaverScheduleRadioButtonsController mController;
    private BatterySaverScheduleSeekBarController mSeekBarController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSeekBarController = new BatterySaverScheduleSeekBarController(mContext);
        mController = new BatterySaverScheduleRadioButtonsController(
                mContext, mSeekBarController);
        mResolver = mContext.getContentResolver();
    }

    @Test
    public void getDefaultKey_routine_returnsCorrectValue() {
        Settings.Global.putInt(mResolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC);
        assertThat(mController.getDefaultKey())
                .isEqualTo(BatterySaverScheduleRadioButtonsController.KEY_ROUTINE);
    }

    @Test
    public void getDefaultKey_automatic_returnsCorrectValue() {
        Settings.Global.putInt(mResolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        Settings.Global.putInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 5);
        assertThat(mController.getDefaultKey())
                .isEqualTo(BatterySaverScheduleRadioButtonsController.KEY_PERCENTAGE);
    }

    @Test
    public void getDefaultKey_none_returnsCorrectValue() {
        Settings.Global.putInt(mResolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        Settings.Global.putInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
        assertThat(mController.getDefaultKey())
                .isEqualTo(BatterySaverScheduleRadioButtonsController.KEY_NO_SCHEDULE);
    }

    @Test
    public void setDefaultKey_any_defaultsToNoScheduleIfWarningNotSeen() {
        Secure.putString(
                mContext.getContentResolver(), Secure.LOW_POWER_WARNING_ACKNOWLEDGED, "null");
        mController.setDefaultKey(BatterySaverScheduleRadioButtonsController.KEY_ROUTINE);
        assertThat(mController.getDefaultKey())
                .isEqualTo(BatterySaverScheduleRadioButtonsController.KEY_NO_SCHEDULE);
    }

    @Test
    public void setDefaultKey_percentage_shouldSuppressNotification() {
        Secure.putInt(
                mContext.getContentResolver(), Secure.LOW_POWER_WARNING_ACKNOWLEDGED, 1);
        Settings.Global.putInt(mResolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        Settings.Global.putInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 5);
        mController.setDefaultKey(BatterySaverScheduleRadioButtonsController.KEY_PERCENTAGE);

        final int result = Settings.Secure.getInt(mResolver,
                Secure.SUPPRESS_AUTO_BATTERY_SAVER_SUGGESTION, 0);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void setDefaultKey_routine_shouldSuppressNotification() {
        Secure.putInt(
                mContext.getContentResolver(), Secure.LOW_POWER_WARNING_ACKNOWLEDGED, 1);
        Settings.Global.putInt(mResolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC);
        mController.setDefaultKey(BatterySaverScheduleRadioButtonsController.KEY_ROUTINE);

        final int result = Settings.Secure.getInt(mResolver,
                Secure.SUPPRESS_AUTO_BATTERY_SAVER_SUGGESTION, 0);
        assertThat(result).isEqualTo(1);
    }
}
