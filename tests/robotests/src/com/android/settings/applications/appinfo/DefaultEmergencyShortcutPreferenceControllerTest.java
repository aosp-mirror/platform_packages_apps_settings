/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import android.app.role.RoleControllerManager;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class DefaultEmergencyShortcutPreferenceControllerTest {

    @Mock
    private RoleControllerManager mRoleControllerManager;

    private DefaultEmergencyShortcutPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication.getInstance().setSystemService(Context.ROLE_CONTROLLER_SERVICE,
                mRoleControllerManager);
        mController = new DefaultEmergencyShortcutPreferenceController(
                RuntimeEnvironment.application, "Package1");
    }

    @Test
    public void getPreferenceKey_shouldReturnDefaultEmergency() {
        assertThat(mController.getPreferenceKey()).isEqualTo("default_emergency_app");
    }
}
