package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.content.ComponentName;
import android.provider.Settings.Secure;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.Settings.NightDisplaySuggestionActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {
        SettingsShadowResources.class
})
public class NightDisplayPreferenceControllerTest {

  private NightDisplayPreferenceController mPreferenceController;

  @Before
  public void setUp() {
    mPreferenceController = new NightDisplayPreferenceController(RuntimeEnvironment.application);
  }

  @After
  public void tearDown() {
    mPreferenceController = null;
    SettingsShadowResources.reset();
  }

  @Test
  public void nightDisplaySuggestion_isNotCompleted_ifAutoModeDisabled() {
    final Application context = RuntimeEnvironment.application;
    Secure.putInt(context.getContentResolver(),
        Secure.NIGHT_DISPLAY_AUTO_MODE, ColorDisplayController.AUTO_MODE_DISABLED);
    final ComponentName componentName =
        new ComponentName(context, NightDisplaySuggestionActivity.class);
    assertThat(mPreferenceController.isSuggestionComplete(context)).isFalse();
  }

  @Test
  public void nightDisplaySuggestion_isCompleted_ifAutoModeCustom() {
    final Application context = RuntimeEnvironment.application;
    Secure.putInt(context.getContentResolver(),
        Secure.NIGHT_DISPLAY_AUTO_MODE, ColorDisplayController.AUTO_MODE_CUSTOM);
    final ComponentName componentName =
        new ComponentName(context, NightDisplaySuggestionActivity.class);
    assertThat(mPreferenceController.isSuggestionComplete(context)).isTrue();
  }

  @Test
  public void nightDisplaySuggestion_isCompleted_ifAutoModeTwilight() {
    final Application context = RuntimeEnvironment.application;
    Secure.putInt(context.getContentResolver(),
        Secure.NIGHT_DISPLAY_AUTO_MODE, ColorDisplayController.AUTO_MODE_TWILIGHT);
    final ComponentName componentName =
        new ComponentName(context, NightDisplaySuggestionActivity.class);
    assertThat(mPreferenceController.isSuggestionComplete(context)).isTrue();
  }

  @Test
  public void nightDisplaySuggestion_isCompleted_ifDisabled() {
    final Application context = RuntimeEnvironment.application;
    Secure.putInt(context.getContentResolver(),
            Secure.NIGHT_DISPLAY_AUTO_MODE, ColorDisplayController.AUTO_MODE_DISABLED);
    SettingsShadowResources.overrideResource(R.bool.config_night_light_suggestion_enabled, false);

    final ComponentName componentName =
            new ComponentName(context, NightDisplaySuggestionActivity.class);
    assertThat(mPreferenceController.isSuggestionComplete(context)).isTrue();
  }
}
