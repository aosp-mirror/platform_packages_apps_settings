/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.media.RingtoneManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class NotificationRingtonePreferenceControllerTest {

    private NotificationRingtonePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new NotificationRingtonePreferenceController(RuntimeEnvironment.application);
    }

    @Test
    public void isAvailable_byDefault_isTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_whenNotVisible_isFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getRingtoneType_shouldReturnNotification() {
        assertThat(mController.getRingtoneType()).isEqualTo(RingtoneManager.TYPE_NOTIFICATION);
    }
}
