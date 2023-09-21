/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.vrlistener;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import com.android.settings.testutils.shadow.ShadowActivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowActivityManager.class,
})
public class VrListenerScreenPreferenceControllerTest {

    private Context mContext;
    private VrListenerScreenPreferenceController mController;
    private ShadowActivityManager mActivityManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new VrListenerScreenPreferenceController(mContext, "key");
        mActivityManager = Shadow.extract(mContext.getSystemService(Context.ACTIVITY_SERVICE));
    }

    @Test
    public void getAvailability_byDefault_searchable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_lowMemory_unavailable() {
        mActivityManager.setIsLowRamDevice(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailability_disabled_unavailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
