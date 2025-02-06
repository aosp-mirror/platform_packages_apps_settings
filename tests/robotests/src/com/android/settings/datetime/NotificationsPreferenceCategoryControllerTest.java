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

package com.android.settings.datetime;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.server.flags.Flags;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class NotificationsPreferenceCategoryControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();


    private NotificationsPreferenceCategoryController mController;
    @Mock
    private AbstractPreferenceController mChildController;

    @Before
    @EnableFlags({Flags.FLAG_DATETIME_NOTIFICATIONS})
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = RuntimeEnvironment.getApplication();

        mController = new NotificationsPreferenceCategoryController(context, "test_key");
        mController.addChildController(mChildController);
    }

    @Test
    @DisableFlags({Flags.FLAG_DATETIME_NOTIFICATIONS})
    public void getAvailabilityStatus_featureDisabled() {
        when(mChildController.isAvailable()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags({Flags.FLAG_DATETIME_NOTIFICATIONS})
    public void getAvailabilityStatus_featureEnabled() {
        when(mChildController.isAvailable()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);

        when(mChildController.isAvailable()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
