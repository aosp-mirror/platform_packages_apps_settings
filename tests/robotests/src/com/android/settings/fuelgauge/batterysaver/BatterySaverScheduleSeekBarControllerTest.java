package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.widget.SeekBar;

import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowInteractionJankMonitor.class})
public class BatterySaverScheduleSeekBarControllerTest {

    private Context mContext;
    private ContentResolver mResolver;
    private BatterySaverScheduleSeekBarController mController;
    @Mock private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new BatterySaverScheduleSeekBarController(mContext);
        mResolver = mContext.getContentResolver();
        mController.mSeekBarPreference = spy(mController.mSeekBarPreference);
    }

    @Test
    public void onPreferenceChange_withoutOnStopTrackingTouch_updatesTitleAndDescriptionOnly() {
        final CharSequence expectedTitle = "50%";
        setTriggerLevel(5);

        mController.onPreferenceChange(mController.mSeekBarPreference, 10);

        assertThat(getTriggerLevel()).isEqualTo(5);
        assertThat(mController.mSeekBarPreference.getTitle()).isEqualTo(expectedTitle);
        verify(mController.mSeekBarPreference).overrideSeekBarStateDescription(expectedTitle);
    }

    @Test
    public void onPreferenceChange_withOnStopTrackingTouch_updatesSettingsGlobal() {
        final CharSequence expectedTitle = "50%";
        setTriggerLevel(5);

        mController.onPreferenceChange(mController.mSeekBarPreference, 10);
        mController.onStopTrackingTouch(new SeekBar(mContext));

        assertThat(getTriggerLevel()).isEqualTo(50);
        assertThat(mController.mSeekBarPreference.getTitle()).isEqualTo(expectedTitle);
        verify(mController.mSeekBarPreference).overrideSeekBarStateDescription(expectedTitle);
    }

    @Test
    public void onStopTrackingTouch_invalidValue_noUpdates() {
        setTriggerLevel(5);

        mController.mPercentage = 0;
        mController.onStopTrackingTouch(new SeekBar(mContext));

        assertThat(getTriggerLevel()).isEqualTo(5);
    }

    @Test
    public void updateSeekBar_routineMode_hasCorrectProperties() {
        Settings.Global.putInt(
                mResolver,
                Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC);

        mController.updateSeekBar();

        assertThat(mController.mSeekBarPreference.isVisible()).isFalse();
        verify(mController.mSeekBarPreference, never()).overrideSeekBarStateDescription(any());
    }

    @Test
    public void updateSeekBar_percentageMode_hasCorrectProperties() {
        final CharSequence expectedTitle = "10%";
        Settings.Global.putInt(
                mResolver,
                Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        setTriggerLevel(10);

        mController.updateSeekBar();

        assertThat(mController.mSeekBarPreference.isVisible()).isTrue();
        assertThat(mController.mSeekBarPreference.getTitle()).isEqualTo(expectedTitle);
        verify(mController.mSeekBarPreference).overrideSeekBarStateDescription(expectedTitle);
    }

    @Test
    public void updateSeekBar_noneMode_hasCorrectProperties() {
        Settings.Global.putInt(
                mResolver,
                Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        setTriggerLevel(0);

        mController.updateSeekBar();

        assertThat(mController.mSeekBarPreference.isVisible()).isFalse();
        verify(mController.mSeekBarPreference, never()).overrideSeekBarStateDescription(any());
    }

    @Test
    public void addToScreen_addsToEnd() {
        Settings.Global.putInt(
                mResolver,
                Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        setTriggerLevel(15);

        mController.addToScreen(mScreen);

        assertThat(mController.mSeekBarPreference.getOrder()).isEqualTo(100);
    }

    private void setTriggerLevel(int level) {
        Settings.Global.putInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, level);
    }

    private int getTriggerLevel() {
        return Settings.Global.getInt(mResolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, -1);
    }
}
