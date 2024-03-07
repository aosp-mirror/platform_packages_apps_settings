/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link TrackpadReverseScrollingPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowSystemSettings.class,
})
public class TrackpadReverseScrollingPreferenceControllerTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private static final String PREFERENCE_KEY = "trackpad_reverse_scrolling";
    private static final String SETTING_KEY = Settings.System.TOUCHPAD_NATURAL_SCROLLING;

    private Context mContext;
    private TrackpadReverseScrollingPreferenceController mController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new TrackpadReverseScrollingPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void getAvailabilityStatus_expected() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSliceHighlightMenuRes_expected() {
        assertThat(mController.getSliceHighlightMenuRes()).isEqualTo(R.string.menu_key_system);
    }

    @Test
    public void setChecked_true_shouldReturn0() {
        mController.setChecked(true);

        int result = Settings.System.getIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                0,
                UserHandle.USER_CURRENT);

        assertThat(result).isEqualTo(0);
        verify(mFeatureFactory.metricsFeatureProvider).action(
                any(),
                eq(SettingsEnums.ACTION_GESTURE_REVERSE_SCROLLING_CHANGED),
                eq(true));
    }

    @Test
    public void setChecked_false_shouldReturn1() {
        mController.setChecked(false);

        int result = Settings.System.getIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                0,
                UserHandle.USER_CURRENT);

        assertThat(result).isEqualTo(1);
        verify(mFeatureFactory.metricsFeatureProvider).action(
                any(),
                eq(SettingsEnums.ACTION_GESTURE_REVERSE_SCROLLING_CHANGED),
                eq(false));
    }

    @Test
    public void isChecked_providerPutInt1_returnFalse() {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                1,
                UserHandle.USER_CURRENT);

        boolean result = mController.isChecked();

        assertThat(result).isFalse();
    }

    @Test
    public void isChecked_providerPutInt0_returnTrue() {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                SETTING_KEY,
                0,
                UserHandle.USER_CURRENT);

        boolean result = mController.isChecked();

        assertThat(result).isTrue();
    }
}
