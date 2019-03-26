/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.pm.ResolveInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class LiveCaptionPreferenceControllerTest {

    private LiveCaptionPreferenceController mController;

    @Before
    public void setUp() {
        mController = new LiveCaptionPreferenceController(RuntimeEnvironment.application,
                "test_key");
    }

    @Test
    public void getAvailabilityStatus_canResolveIntent_shouldReturnAvailable() {
        final ShadowPackageManager pm = Shadows.shadowOf(
                RuntimeEnvironment.application.getPackageManager());
        pm.addResolveInfoForIntent(LiveCaptionPreferenceController.LIVE_CAPTION_INTENT,
                new ResolveInfo());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noResolveIntent_shouldReturnUnavailable() {
        final ShadowPackageManager pm = Shadows.shadowOf(
                RuntimeEnvironment.application.getPackageManager());
        pm.setResolveInfosForIntent(LiveCaptionPreferenceController.LIVE_CAPTION_INTENT,
                Collections.emptyList());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}