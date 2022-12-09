/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.IllustrationPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LongPressPowerIllustrationPreferenceControllerTest {

    private Application mContext;
    private IllustrationPreference mPreference;
    private LongPressPowerIllustrationPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPreference = new IllustrationPreference(mContext);
        mController = new LongPressPowerIllustrationPreferenceController(mContext, "test_key");

        PreferenceScreen mScreen = mock(PreferenceScreen.class);
        when(mScreen.findPreference("test_key")).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void updateState_longPressPowerForPowerMenu_showsPowerMenuAnimation() {
        PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.getLottieAnimationResId())
                .isEqualTo(R.raw.lottie_long_press_power_for_power_menu);
    }

    @Test
    public void updateState_longPressPowerForAssistant_showsAssistantAnimation() {
        PowerMenuSettingsUtils.setLongPressPowerForAssistant(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.getLottieAnimationResId())
                .isEqualTo(R.raw.lottie_long_press_power_for_assistant);
    }
}
