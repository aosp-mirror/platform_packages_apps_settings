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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class ZenModeBypassingAppsPreferenceControllerTest {

    private ZenModeBypassingAppsPreferenceController mController;

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mController = new ZenModeBypassingAppsPreferenceController(mContext, mock(Lifecycle.class));
        ReflectionHelpers.setField(mController, "mNotificationBackend", mBackend);
    }

    @Test
    public void testIsAvailable() {
        when(mBackend.getNumAppsBypassingDnd(anyInt())).thenReturn(5);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testNotAvailable() {
        when(mBackend.getNumAppsBypassingDnd(anyInt())).thenReturn(0);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testHasSummary() {
        assertThat(mController.getSummary()).isNotNull();
    }
}
