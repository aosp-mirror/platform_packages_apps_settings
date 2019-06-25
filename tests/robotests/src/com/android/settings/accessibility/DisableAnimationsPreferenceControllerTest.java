/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.DisableAnimationsPreferenceController.ANIMATION_OFF_VALUE;
import static com.android.settings.accessibility.DisableAnimationsPreferenceController.ANIMATION_ON_VALUE;
import static com.android.settings.accessibility.DisableAnimationsPreferenceController.TOGGLE_ANIMATION_TARGETS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DisableAnimationsPreferenceControllerTest {

    private Context mContext;
    private SwitchPreference mPreference;
    private DisableAnimationsPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = new DisableAnimationsPreferenceController(mContext, "disable_animation");
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isChecked_enabledAnimation_shouldReturnFalse() {
        for (String animationPreference : TOGGLE_ANIMATION_TARGETS) {
            Settings.Global.putString(mContext.getContentResolver(), animationPreference,
                    ANIMATION_ON_VALUE);
        }

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void isChecked_disabledAnimation_shouldReturnTrue() {
        for (String animationPreference : TOGGLE_ANIMATION_TARGETS) {
            Settings.Global.putString(mContext.getContentResolver(), animationPreference,
                    ANIMATION_OFF_VALUE);
        }

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void setChecked_disabledAnimation_shouldDisableAnimationTargets() {
        mController.setChecked(true);

        for (String animationSetting : TOGGLE_ANIMATION_TARGETS) {
            assertThat(Settings.Global.getString(mContext.getContentResolver(), animationSetting))
                    .isEqualTo(ANIMATION_OFF_VALUE);
        }
    }

    @Test
    public void setChecked_enabledAnimation_shouldEnableAnimationTargets() {
        mController.setChecked(false);

        for (String animationSetting : TOGGLE_ANIMATION_TARGETS) {
            assertThat(Settings.Global.getString(mContext.getContentResolver(), animationSetting))
                    .isEqualTo(ANIMATION_ON_VALUE);
        }
    }
}
