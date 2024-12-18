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

package com.android.settings.communal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.UserHandle;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class, ShadowUserManager.class})
public class CommunalPreferenceControllerTest {
    private ShadowUserManager mShadowUserManager;
    private CommunalPreferenceController mController;

    private static final String PREF_KEY = "test_key";

    @Before
    public void setup() {
        final Context context = spy(RuntimeEnvironment.application);
        mShadowUserManager = ShadowUserManager.getShadow();
        doReturn(context).when(context).createContextAsUser(any(UserHandle.class), anyInt());
        mController = new CommunalPreferenceController(context, PREF_KEY);
    }

    @Test
    public void isAvailable_communalEnabled_shouldBeTrueForDockUser() {
        setCommunalEnabled(true);
        mShadowUserManager.setUserForeground(true);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_communalEnabled_shouldBeFalseForNonDockUser() {
        setCommunalEnabled(true);
        mShadowUserManager.setUserForeground(false);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_communalDisabled_shouldBeFalseForDockUser() {
        setCommunalEnabled(false);
        mShadowUserManager.setUserForeground(true);
        assertFalse(mController.isAvailable());
    }

    private void setCommunalEnabled(boolean enabled) {
        SettingsShadowResources.overrideResource(R.bool.config_show_communal_settings, enabled);
    }
}
