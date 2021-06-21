/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.notification;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_FOR_USER;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserHandle;

import com.android.settings.testutils.shadow.ShadowAudioHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(shadows = ShadowAudioHelper.class)
@RunWith(RobolectricTestRunner.class)
public class WorkSoundsPreferenceControllerTest {

    private Context mContext;
    private WorkSoundsPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new WorkSoundsPreferenceController(mContext, "test_key");
    }

    @After
    public void tearDown() {
        ShadowAudioHelper.reset();
    }

    @Test
    public void getAvailabilityStatus_supportWorkProfileSound_shouldReturnAvailable() {
        ShadowAudioHelper.setIsSingleVolume(false);
        ShadowAudioHelper.setManagedProfileId(UserHandle.USER_CURRENT);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notSupportWorkProfileSound_shouldReturnDisabled() {
        ShadowAudioHelper.setIsSingleVolume(true);
        ShadowAudioHelper.setManagedProfileId(UserHandle.USER_NULL);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
    }
}
