package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Global;

import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatterySaverScheduleSeekBarControllerTest {

    private Context mContext;
    private ContentResolver mResolver;
    private BatterySaverScheduleSeekBarController mController;
    @Mock
    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new BatterySaverScheduleSeekBarController(mContext);
        mResolver = mContext.getContentResolver();
    }

    @Test
    public void onPreferenceChange_updatesSettingsGlobal() {
        Settings.Global.putInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 5);
        mController.onPreferenceChange(mController.mSeekBarPreference, 10);
        assertThat(Settings.Global.getInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, -1))
                .isEqualTo(50);

        assertThat(mController.mSeekBarPreference.getTitle()).isEqualTo("50%");
    }

    @Test
    public void updateSeekBar_routineMode_hasCorrectProperties() {
        Settings.Global.putInt(mResolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC);
        mController.updateSeekBar();
        assertThat(mController.mSeekBarPreference.isVisible()).isFalse();
    }

    @Test
    public void updateSeekBar_percentageMode_hasCorrectProperties() {
        Settings.Global.putInt(mResolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        Settings.Global.putInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 5);
        mController.updateSeekBar();
        assertThat(mController.mSeekBarPreference.isVisible()).isTrue();
    }

    @Test
    public void updateSeekBar_noneMode_hasCorrectProperties() {
        Settings.Global.putInt(mResolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        Settings.Global.putInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
        mController.updateSeekBar();
        assertThat(mController.mSeekBarPreference.isVisible()).isFalse();
    }

    @Test
    public void addToScreen_addsToEnd() {
        Settings.Global.putInt(mResolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        Settings.Global.putInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 15);
        mController.addToScreen(mScreen);
        assertThat(mController.mSeekBarPreference.getOrder()).isEqualTo(100);
    }

}
