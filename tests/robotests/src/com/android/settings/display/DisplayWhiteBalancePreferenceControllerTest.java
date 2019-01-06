package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.Secure;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
    SettingsShadowResources.class
})
public class DisplayWhiteBalancePreferenceControllerTest {

  private Context mContext;
  private DisplayWhiteBalancePreferenceController mController;

  @After
  public void tearDown() {
    SettingsShadowResources.reset();
  }

  @Before
  public void setUp() {
    mContext = RuntimeEnvironment.application;
    mController = new DisplayWhiteBalancePreferenceController(mContext, "display_white_balance");
  }

  @Test
  public void isAvailable_configuredAvailable() {
    SettingsShadowResources.overrideResource(
        com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    assertThat(mController.isAvailable()).isTrue();
  }

  @Test
  public void isAvailable_configuredUnavailable() {
    SettingsShadowResources.overrideResource(
        com.android.internal.R.bool.config_displayWhiteBalanceAvailable, false);
    assertThat(mController.isAvailable()).isFalse();
  }

  @Test
  public void setChecked_true() {
    mController.setChecked(true);
    assertThat(Settings.Secure
        .getInt(mContext.getContentResolver(), Secure.DISPLAY_WHITE_BALANCE_ENABLED, 0) == 1)
        .isTrue();
  }

  @Test
  public void setChecked_false() {
    mController.setChecked(false);
    assertThat(Settings.Secure
        .getInt(mContext.getContentResolver(), Secure.DISPLAY_WHITE_BALANCE_ENABLED, 0) == 1)
        .isFalse();
  }

  @Test
  public void isChecked_true() {
    Settings.Secure.putInt(mContext.getContentResolver(), Secure.DISPLAY_WHITE_BALANCE_ENABLED, 1);
    assertThat(mController.isChecked()).isTrue();
  }

  @Test
  public void isChecked_false() {
    Settings.Secure.putInt(mContext.getContentResolver(), Secure.DISPLAY_WHITE_BALANCE_ENABLED, 0);
    assertThat(mController.isChecked()).isFalse();
  }
}
