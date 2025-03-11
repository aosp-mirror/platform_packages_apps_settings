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
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.hardware.input.Flags;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link MouseReverseVerticalScrollingPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowSystemSettings.class,
})
public class MouseReverseVerticalScrollingPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "mouse_reverse_vertical_scrolling";
    private static final String SETTING_KEY = Settings.System.MOUSE_REVERSE_VERTICAL_SCROLLING;

    private Context mContext;
    private MouseReverseVerticalScrollingPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new MouseReverseVerticalScrollingPreferenceController(
                mContext, PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(Flags.FLAG_MOUSE_REVERSE_VERTICAL_SCROLLING)
    public void getAvailabilityStatus_expected() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_MOUSE_REVERSE_VERTICAL_SCROLLING)
    public void getAvailabilityStatus_flagIsDisabled_notSupport() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags(Flags.FLAG_MOUSE_REVERSE_VERTICAL_SCROLLING)
    public void setChecked_true_shouldReturn1() {
        mController.setChecked(true);

        boolean isEnabled = InputSettings.isMouseReverseVerticalScrollingEnabled(mContext);
        assertThat(isEnabled).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_MOUSE_REVERSE_VERTICAL_SCROLLING)
    public void setChecked_false_shouldReturn0() {
        mController.setChecked(false);

        boolean isEnabled = InputSettings.isMouseReverseVerticalScrollingEnabled(mContext);
        assertThat(isEnabled).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_MOUSE_REVERSE_VERTICAL_SCROLLING)
    public void isChecked_providerPutInt1_returnTrue() {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                1,
                UserHandle.USER_CURRENT);

        boolean result = mController.isChecked();

        assertThat(result).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_MOUSE_REVERSE_VERTICAL_SCROLLING)
    public void isChecked_providerPutInt0_returnFalse() {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                0,
                UserHandle.USER_CURRENT);

        boolean result = mController.isChecked();

        assertThat(result).isFalse();
    }
}
