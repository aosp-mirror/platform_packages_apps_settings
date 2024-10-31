/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.input.InputSettings;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.keyboard.Flags;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
        ShadowAlertDialogCompat.class,
})
public class KeyboardAccessibilitySlowKeysControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String PREFERENCE_KEY = "keyboard_a11y_page_slow_keys";
    @Mock
    private Preference mPreference;
    private Context mContext;
    private KeyboardAccessibilitySlowKeysController mKeyboardAccessibilitySlowKeysController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mKeyboardAccessibilitySlowKeysController = new KeyboardAccessibilitySlowKeysController(
                mContext,
                PREFERENCE_KEY);
        when(mPreference.getKey()).thenReturn(PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED)
    public void getAvailabilityStatus_flagIsEnabled_isAvailable() {
        assertThat(mKeyboardAccessibilitySlowKeysController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED)
    public void getAvailabilityStatus_flagIsDisabled_notSupport() {
        assertThat(mKeyboardAccessibilitySlowKeysController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void setChecked_true_updateSlowKeyValue() {
        mKeyboardAccessibilitySlowKeysController.setChecked(true);
        boolean isEnabled = InputSettings.isAccessibilitySlowKeysEnabled(mContext);

        assertThat(isEnabled).isTrue();
    }

    @Test
    public void setChecked_false_updateSlowKeyValue() {
        mKeyboardAccessibilitySlowKeysController.setChecked(false);
        boolean isEnabled = InputSettings.isAccessibilitySlowKeysEnabled(mContext);

        assertThat(isEnabled).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_dialogShows() {
        mKeyboardAccessibilitySlowKeysController.handlePreferenceTreeClick(mPreference);

        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(alertDialog.isShowing()).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_performClickOn200_updatesSlowKeysThreshold() {
        mKeyboardAccessibilitySlowKeysController.handlePreferenceTreeClick(mPreference);
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        RadioGroup radioGroup = alertDialog.findViewById(R.id.input_setting_keys_value_group);
        radioGroup.check(R.id.input_setting_keys_value_200);

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        assertThat(alertDialog.isShowing()).isFalse();
        int threshold = InputSettings.getAccessibilitySlowKeysThreshold(mContext);
        assertThat(threshold).isEqualTo(200);
    }
}
