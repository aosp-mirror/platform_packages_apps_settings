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

package com.android.settings.development.quarantine;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.pm.Flags;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class QuarantinedAppsPreferenceControllerTest {

    private static final String PREF_KEY = "quarantined_apps";

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private Context mContext;
    private QuarantinedAppsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new QuarantinedAppsPreferenceController(mContext, PREF_KEY);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_QUARANTINED_ENABLED)
    public void testAvailabilityStatus_flagEnabled() {
        assertEquals(mController.getAvailabilityStatus(), AVAILABLE);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_QUARANTINED_ENABLED)
    public void testAvailabilityStatus_flagDisabled() {
        assertEquals(mController.getAvailabilityStatus(), CONDITIONALLY_UNAVAILABLE);
    }
}
