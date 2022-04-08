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

package com.android.settings.users;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_FOR_USER;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@Ignore
public class MultiUserFooterPreferenceControllerTest {

    private Context mContext;
    private MultiUserFooterPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new MultiUserFooterPreferenceController(mContext, "footer");
    }

    @Test
    public void getAvailabilityStatus_multiUserOff_shouldReturnEnabled() {
        mController.mUserCaps.mEnabled = true;
        mController.mUserCaps.mUserSwitcherEnabled = false;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_multiUserOn_shouldReturnDisabled() {
        mController.mUserCaps.mEnabled = true;
        mController.mUserCaps.mUserSwitcherEnabled = true;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
    }
}
