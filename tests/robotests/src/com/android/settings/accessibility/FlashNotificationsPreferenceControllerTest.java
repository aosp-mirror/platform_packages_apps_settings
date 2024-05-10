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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.ShadowFlashNotificationsUtils.setFlashNotificationsState;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowFlashNotificationsUtils.class)
public class FlashNotificationsPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "preference_key";

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private FlashNotificationsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new FlashNotificationsPreferenceController(mContext, PREFERENCE_KEY);
    }

    @After
    public void tearDown() {
        ShadowFlashNotificationsUtils.reset();
    }

    @Test
    public void getAvailabilityStatus() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getSummary_stateOff_assertOff() {
        setFlashNotificationsState(FlashNotificationsUtil.State.OFF);

        assertThat(mController.getSummary().toString()).isEqualTo(mContext.getString(
                R.string.flash_notifications_summary_off));
    }

    @Test
    public void getSummary_stateCamera_assertCamera() {
        setFlashNotificationsState(FlashNotificationsUtil.State.CAMERA);

        assertThat(mController.getSummary().toString()).isEqualTo(mContext.getString(
                R.string.flash_notifications_summary_on_camera));
    }

    @Test
    public void getSummary_stateScreen_assertScreen() {
        setFlashNotificationsState(FlashNotificationsUtil.State.SCREEN);

        assertThat(mController.getSummary().toString()).isEqualTo(mContext.getString(
                R.string.flash_notifications_summary_on_screen));
    }

    @Test
    public void getSummary_stateCameraScreen_assertCameraScreen() {
        setFlashNotificationsState(FlashNotificationsUtil.State.CAMERA_SCREEN);

        assertThat(mController.getSummary().toString()).isEqualTo(mContext.getString(
                R.string.flash_notifications_summary_on_camera_and_screen));
    }
}
