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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import android.platform.test.flag.junit.SetFlagsRule;

import com.android.server.notification.Flags;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

// TODO(b/367455695): remove test when feature flag is cleaned
@RunWith(RobolectricTestRunner.class)
public class LockScreenNotificationsPreferencePageControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "lock_screen_notifications_page";

    private LockScreenNotificationsPreferencePageController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new LockScreenNotificationsPreferencePageController(
                RuntimeEnvironment.application,
                PREFERENCE_KEY);
    }

    @Test
    public void isAvailable_flagEnabled_shouldReturnTrue() {
        mSetFlagsRule.enableFlags(Flags.FLAG_NOTIFICATION_LOCK_SCREEN_SETTINGS);
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isAvailable_flagDisabled_shouldReturnFalse() {
        mSetFlagsRule.disableFlags(Flags.FLAG_NOTIFICATION_LOCK_SCREEN_SETTINGS);
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

}
