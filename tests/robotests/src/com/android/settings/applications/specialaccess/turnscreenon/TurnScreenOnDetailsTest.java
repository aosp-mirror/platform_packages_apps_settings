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

package com.android.settings.applications.specialaccess.turnscreenon;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.MODE_ERRORED;
import static android.app.AppOpsManager.OP_TURN_SCREEN_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TurnScreenOnDetailsTest {

    private static final int UID = 0;
    private static final String PACKAGE_NAME = "com.android.fake.package";

    @Mock
    private AppOpsManager mAppOpsManager;



    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void isTurnScreenOnAllowed_appOpErrored_shouldReturnFalse() {
        when(mAppOpsManager.checkOpNoThrow(eq(OP_TURN_SCREEN_ON), eq(UID),
                eq(PACKAGE_NAME))).thenReturn(MODE_ERRORED);

        boolean isAllowed = TurnScreenOnDetails.isTurnScreenOnAllowed(mAppOpsManager, UID,
                PACKAGE_NAME);

        assertThat(isAllowed).isFalse();
    }

    @Test
    public void isTurnScreenOnAllowed_appOpAllowed_shouldReturnTrue() {
        when(mAppOpsManager.checkOpNoThrow(eq(OP_TURN_SCREEN_ON), eq(UID),
                eq(PACKAGE_NAME))).thenReturn(MODE_ALLOWED);

        boolean isAllowed = TurnScreenOnDetails.isTurnScreenOnAllowed(mAppOpsManager, UID,
                PACKAGE_NAME);

        assertThat(isAllowed).isTrue();
    }
}
