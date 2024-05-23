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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ContrastPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class ContrastPreferenceControllerTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    private ContrastPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new ContrastPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void getAvailabilityStatus_flagsEnabled_shouldReturnAvailable() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_COLOR_CONTRAST_CONTROL);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagsDisabled_shouldReturnUnsupported() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_COLOR_CONTRAST_CONTROL);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }
}
