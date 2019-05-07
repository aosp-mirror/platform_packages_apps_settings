package com.android.settings.display;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.google.common.truth.Truth.assertThat;


import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.SettingsShadowResources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
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
  private ContentResolver mContentResolver;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Context mContext;
  @Mock
  private PreferenceScreen mScreen;
  @Mock
  private Preference mPreference;

  private final String PREFERENCE_KEY = "display_white_balance";

  @After
  public void tearDown() {
    SettingsShadowResources.reset();
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    mContentResolver = RuntimeEnvironment.application.getContentResolver();
    when(mContext.getContentResolver()).thenReturn(mContentResolver);
    when(mContext.getResources()).thenReturn(RuntimeEnvironment.application.getResources());
    when(mScreen.findPreference(PREFERENCE_KEY)).thenReturn(mPreference);

    mController = spy(new DisplayWhiteBalancePreferenceController(mContext, PREFERENCE_KEY));
    doReturn(mColorDisplayManager).when(mController).getColorDisplayManager();
  }

  @Test
  public void isAvailable() {
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

  @Test
  public void onStart_configuredUnavailable() {
    SettingsShadowResources.overrideResource(
        com.android.internal.R.bool.config_displayWhiteBalanceAvailable, false);
    mController.displayPreference(mScreen);
    mController.onStart();
    assertThat(mController.mContentObserver).isNull();
  }

  @Test
  public void onStart_configuredAvailable() {
    SettingsShadowResources.overrideResource(
        com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    when(mColorDisplayManager.getColorMode())
        .thenReturn(ColorDisplayManager.COLOR_MODE_NATURAL);
    toggleAccessibilityInversion(false);
    toggleAccessibilityDaltonizer(false);

    mController.displayPreference(mScreen);
    mController.onStart();
    assertThat(mController.mContentObserver).isNotNull();
  }

  @Test
  public void visibility_configuredAvailableAccessibilityToggled() {
    SettingsShadowResources.overrideResource(
        com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    mController.displayPreference(mScreen);

    // Accessibility features disabled
    toggleAccessibilityInversion(false);
    reset(mPreference);
    mController.updateVisibility();
    verify(mPreference).setVisible(true);

    toggleAccessibilityDaltonizer(false);
    reset(mPreference);
    mController.updateVisibility();
    verify(mPreference).setVisible(true);

    // Accessibility features enabled one by one
    toggleAccessibilityInversion(true);
    mController.updateVisibility();
    verify(mPreference).setVisible(false);

    toggleAccessibilityDaltonizer(true);
    reset(mPreference);
    mController.updateVisibility();
    verify(mPreference).setVisible(false);

    // Accessibility features disabled one by one
    toggleAccessibilityInversion(false);
    reset(mPreference);
    mController.updateVisibility();
    // Daltonizer is still enabled, so we expect the preference to still be invisible
    verify(mPreference).setVisible(false);

    // Now both a11y features are disabled, so we expect the preference to become visible
    toggleAccessibilityDaltonizer(false);
    mController.updateVisibility();
    verify(mPreference).setVisible(true);
  }

  @Test
  public void visibility_configuredAvailableColorModeChanged() {
    SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    mController.displayPreference(mScreen);

    // Non-Saturated color mode selected
    when(mColorDisplayManager.getColorMode()).thenReturn(ColorDisplayManager.COLOR_MODE_NATURAL);
    reset(mPreference);
    mController.updateVisibility();
    verify(mPreference).setVisible(true);

    // Saturated color mode selected
    when(mColorDisplayManager.getColorMode()).thenReturn(ColorDisplayManager.COLOR_MODE_SATURATED);
    mController.updateVisibility();
    verify(mPreference).setVisible(false);

    // Switch back to non-Saturated color mode
    when(mColorDisplayManager.getColorMode()).thenReturn(ColorDisplayManager.COLOR_MODE_NATURAL);
    reset(mPreference);
    mController.updateVisibility();
    verify(mPreference).setVisible(true);
  }

  private void toggleAccessibilityInversion(boolean enable) {
    Settings.Secure.putInt(mContentResolver,
        Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, enable ? 1 : 0);
  }

  private void toggleAccessibilityDaltonizer(boolean enable) {
    Settings.Secure.putInt(mContentResolver,
        Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, enable ? 1 : 0);
  }
}
