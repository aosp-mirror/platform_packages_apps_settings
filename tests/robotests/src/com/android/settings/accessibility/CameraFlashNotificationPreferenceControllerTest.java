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

import static com.android.settings.accessibility.FlashNotificationsUtil.SETTING_KEY_CAMERA_FLASH_NOTIFICATION;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

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
public class CameraFlashNotificationPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "preference_key";

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private CameraFlashNotificationPreferenceController mController;
    private ContentResolver mContentResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContentResolver = mContext.getContentResolver();
        mController = new CameraFlashNotificationPreferenceController(mContext, PREFERENCE_KEY);
    }

    @After
    public void tearDown() {
        ShadowFlashNotificationsUtils.reset();
    }

    @Test
    public void getAvailabilityStatus_torchAvailable_assertAvailable() {
        ShadowFlashNotificationsUtils.setIsTorchAvailable(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_torchUnavailable_assertUnsupportedOnDevice() {
        ShadowFlashNotificationsUtils.setIsTorchAvailable(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isChecked_setOff_assertFalse() {
        Settings.System.putInt(mContentResolver, SETTING_KEY_CAMERA_FLASH_NOTIFICATION, 0);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_setOn_assertTrue() {
        Settings.System.putInt(mContentResolver, SETTING_KEY_CAMERA_FLASH_NOTIFICATION, 1);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_setTrue_assertNotZero() {
        mController.setChecked(true);
        assertThat(Settings.System.getInt(mContentResolver, SETTING_KEY_CAMERA_FLASH_NOTIFICATION,
                0)).isNotEqualTo(0);
    }

    @Test
    public void setChecked_setFalse_assertNotOne() {
        mController.setChecked(false);
        assertThat(Settings.System.getInt(mContentResolver, SETTING_KEY_CAMERA_FLASH_NOTIFICATION,
                1)).isNotEqualTo(1);
    }

    @Test
    public void getSliceHighlightMenuRes() {
        mController.getSliceHighlightMenuRes();
        assertThat(mController.getSliceHighlightMenuRes())
                .isEqualTo(R.string.menu_key_accessibility);
    }
}
