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

package com.android.settings.communal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.Utils;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class CommunalPreferenceControllerTest {
    @Mock
    private UserManager mUserManager;

    private Context mContext;
    private Resources mResources;
    private CommunalPreferenceController mController;

    private static final String PREF_KEY = "test_key";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());

        mController = new CommunalPreferenceController(mContext, PREF_KEY);

        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
    }

    @Test
    public void isAvailable_communalEnabled_shouldBeTrueForDockUser() {
        setCommunalEnabled(true);
        when(Utils.canCurrentUserDream(mContext)).thenReturn(true);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_communalEnabled_shouldBeFalseForNonDockUser() {
        setCommunalEnabled(true);
        when(Utils.canCurrentUserDream(mContext)).thenReturn(false);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_communalDisabled_shouldBeFalseForDockUser() {
        setCommunalEnabled(false);
        when(Utils.canCurrentUserDream(mContext)).thenReturn(true);
        assertFalse(mController.isAvailable());
    }

    private void setCommunalEnabled(boolean enabled) {
        final int boolId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "bool",
                "config_show_communal_settings");
        when(mResources.getBoolean(boolId)).thenReturn(enabled);
    }
}
