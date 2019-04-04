package com.android.settings.display;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static com.google.common.truth.Truth.assertThat;

import android.hardware.display.ColorDisplayManager;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
    SettingsShadowResources.class
})
public class DisplayWhiteBalancePreferenceControllerTest {

  private DisplayWhiteBalancePreferenceController mController;

  @Mock
  private ColorDisplayManager mColorDisplayManager;

  @After
  public void tearDown() {
    SettingsShadowResources.reset();
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mController = spy(new DisplayWhiteBalancePreferenceController(RuntimeEnvironment.application,
        "display_white_balance"));
    doReturn(mColorDisplayManager).when(mController).getColorDisplayManager();
  }

  @Test
  public void isAvailable_configuredAvailable() {
    SettingsShadowResources.overrideResource(
        com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    when(mColorDisplayManager.getColorMode())
        .thenReturn(ColorDisplayManager.COLOR_MODE_NATURAL);
    assertThat(mController.isAvailable()).isTrue();
  }

  @Test
  public void isAvailable_configuredUnavailable() {
    SettingsShadowResources.overrideResource(
        com.android.internal.R.bool.config_displayWhiteBalanceAvailable, false);
    when(mColorDisplayManager.getColorMode())
        .thenReturn(ColorDisplayManager.COLOR_MODE_SATURATED);
    assertThat(mController.isAvailable()).isFalse();

    SettingsShadowResources.overrideResource(
        com.android.internal.R.bool.config_displayWhiteBalanceAvailable, false);
    when(mColorDisplayManager.getColorMode())
        .thenReturn(ColorDisplayManager.COLOR_MODE_NATURAL);
    assertThat(mController.isAvailable()).isFalse();

    SettingsShadowResources.overrideResource(
        com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    when(mColorDisplayManager.getColorMode())
        .thenReturn(ColorDisplayManager.COLOR_MODE_SATURATED);
    assertThat(mController.isAvailable()).isFalse();
  }

  @Test
  public void setChecked_true_setSuccessfully() {
    when(mColorDisplayManager.setDisplayWhiteBalanceEnabled(true)).thenReturn(true);
    assertThat(mController.setChecked(true)).isTrue();
  }

  @Test
  public void setChecked_false_setSuccessfully() {
    when(mColorDisplayManager.setDisplayWhiteBalanceEnabled(false)).thenReturn(true);
    assertThat(mController.setChecked(false)).isTrue();
  }

  @Test
  public void isChecked_true() {
    when(mColorDisplayManager.isDisplayWhiteBalanceEnabled()).thenReturn(true);
    assertThat(mController.isChecked()).isTrue();
  }

  @Test
  public void isChecked_false() {
    when(mColorDisplayManager.isDisplayWhiteBalanceEnabled()).thenReturn(false);
    assertThat(mController.isChecked()).isFalse();
  }
}
