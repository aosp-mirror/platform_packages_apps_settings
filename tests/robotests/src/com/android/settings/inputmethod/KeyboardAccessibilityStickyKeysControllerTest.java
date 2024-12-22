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

import android.content.Context;
import android.hardware.input.InputSettings;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.keyboard.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class KeyboardAccessibilityStickyKeysControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private Context mContext;
    private KeyboardAccessibilityStickyKeysController mKeyboardAccessibilityStickyKeysController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mKeyboardAccessibilityStickyKeysController = new KeyboardAccessibilityStickyKeysController(
                mContext,
                "accessibility_sticky_keys");
    }

    @Test
    @EnableFlags(Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED)
    public void getAvailabilityStatus_flagIsEnabled_isAvailable() {
        assertThat(mKeyboardAccessibilityStickyKeysController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED)
    public void getAvailabilityStatus_flagIsDisabled_notSupport() {
        assertThat(mKeyboardAccessibilityStickyKeysController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void setChecked_true_updateStickyKeyValue() {
        mKeyboardAccessibilityStickyKeysController.setChecked(true);
        boolean isEnabled = InputSettings.isAccessibilityStickyKeysEnabled(mContext);

        assertThat(isEnabled).isTrue();
    }

    @Test
    public void setChecked_false_updateStickyKeyValue() {
        mKeyboardAccessibilityStickyKeysController.setChecked(false);
        boolean isEnabled = InputSettings.isAccessibilityStickyKeysEnabled(mContext);

        assertThat(isEnabled).isFalse();
    }
}
