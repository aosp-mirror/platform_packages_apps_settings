package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.hardware.display.ColorDisplayManager;
import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class})
public class NightDisplayPreferenceControllerTest {

  @After
  public void tearDown() {
    SettingsShadowResources.reset();
  }

  @Test
  public void nightDisplaySuggestion_isNotCompleted_ifAutoModeDisabled() {
    final Application context = RuntimeEnvironment.application;
    context.getSystemService(ColorDisplayManager.class)
        .setNightDisplayAutoMode(ColorDisplayManager.AUTO_MODE_DISABLED);
    assertThat(NightDisplayPreferenceController.isSuggestionComplete(context)).isFalse();
  }

  @Test
  public void nightDisplaySuggestion_isCompleted_ifAutoModeCustom() {
    final Application context = RuntimeEnvironment.application;
    context.getSystemService(ColorDisplayManager.class)
        .setNightDisplayAutoMode(ColorDisplayManager.AUTO_MODE_CUSTOM_TIME);
    assertThat(NightDisplayPreferenceController.isSuggestionComplete(context)).isTrue();
  }

  @Test
  public void nightDisplaySuggestion_isCompleted_ifAutoModeTwilight() {
    final Application context = RuntimeEnvironment.application;
    context.getSystemService(ColorDisplayManager.class)
        .setNightDisplayAutoMode(ColorDisplayManager.AUTO_MODE_TWILIGHT);
    assertThat(NightDisplayPreferenceController.isSuggestionComplete(context)).isTrue();
  }

  @Test
  public void nightDisplaySuggestion_isCompleted_ifSuggestionDisabled() {
    final Application context = RuntimeEnvironment.application;
    context.getSystemService(ColorDisplayManager.class)
        .setNightDisplayAutoMode(ColorDisplayManager.AUTO_MODE_DISABLED);
    SettingsShadowResources.overrideResource(R.bool.config_night_light_suggestion_enabled, false);
    assertThat(NightDisplayPreferenceController.isSuggestionComplete(context)).isTrue();
  }
}
