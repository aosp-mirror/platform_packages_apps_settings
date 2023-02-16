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

package com.android.settings.display;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@RunWith(AndroidJUnit4.class)
public class ScreenSaverPreferenceControllerTest {
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private final Resources mResources = mContext.getResources();
    @Mock
    private UserManager mUserManager;

    private ScreenSaverPreferenceController mController;

    private final String mPrefKey = "test_screensaver";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mController = new ScreenSaverPreferenceController(mContext, mPrefKey);

        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
    }

    @Test
    public void isAvailable_dreamsEnabledForAllUsers_shouldBeTrueForSystemUser() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsSupported)).thenReturn(true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsOnlyEnabledForDockUser))
                .thenReturn(false);
        when(mUserManager.isSystemUser()).thenReturn(true);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_dreamsEnabledForAllUsers_shouldBeTrueForNonSystemUser() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsSupported)).thenReturn(true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsOnlyEnabledForDockUser))
                .thenReturn(false);
        when(mUserManager.isSystemUser()).thenReturn(false);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_dreamsDisabled_shouldBeFalseForSystemUser() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsSupported)).thenReturn(false);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsOnlyEnabledForDockUser))
                .thenReturn(false);
        when(mUserManager.isSystemUser()).thenReturn(true);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_dreamsOnlyEnabledForSystemUser_shouldBeTrueForSystemUser() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsSupported)).thenReturn(true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsOnlyEnabledForDockUser))
                .thenReturn(true);
        when(mUserManager.isSystemUser()).thenReturn(true);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_dreamsOnlyEnabledForSystemUser_shouldBeFalseForNonSystemUser() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsSupported)).thenReturn(true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_dreamsOnlyEnabledForDockUser))
                .thenReturn(true);
        when(mUserManager.isSystemUser()).thenReturn(false);
        assertFalse(mController.isAvailable());
    }
}
