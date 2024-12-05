/*
 * Copyright (C) 2024 The Android Open Source Project
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
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settingslib.widget.IllustrationPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DoubleTapPowerIllustrationPreferenceControllerTest {

    private static final String KEY = "gesture_double_tap_power_video";
    private Application mContext;
    private IllustrationPreference mPreference;
    private DoubleTapPowerIllustrationPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPreference = new IllustrationPreference(mContext);
        mController = new DoubleTapPowerIllustrationPreferenceController(mContext, KEY);

        PreferenceScreen mScreen = mock(PreferenceScreen.class);
        when(mScreen.findPreference(KEY)).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void updateState_setDoubleTapPowerForCamera_showsCameraIllustration() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForCameraLaunch(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.getLottieAnimationResId()).isEqualTo(R.drawable.quickly_open_camera);
    }

    @Test
    public void updateState_setDoubleTapPowerForWallet_showsWalletIllustration() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForWalletLaunch(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.getLottieAnimationResId())
                .isEqualTo(R.drawable.double_tap_power_for_wallet);
    }
}
