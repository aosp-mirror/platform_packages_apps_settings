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

import static android.service.notification.Adjustment.KEY_IMPORTANCE;
import static android.service.notification.Adjustment.KEY_TYPE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.app.INotificationManager;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BundlePreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    BundlePreferenceController mController;
    @Mock
    INotificationManager mInm;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSetFlagsRule.enableFlags(
                android.service.notification.Flags.FLAG_NOTIFICATION_CLASSIFICATION,
                Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI);
        mController = new BundlePreferenceController(mContext, PREFERENCE_KEY);
        mController.mBackend.setNm(mInm);
    }

    @Test
    public void isAvailable_flagEnabledNasSupports_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_flagEnabledNasDoesNotSupport_shouldReturnFalse() throws Exception {
        when(mInm.getUnsupportedAdjustmentTypes()).thenReturn(List.of(KEY_TYPE));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_flagDisabledNasSupports_shouldReturnFalse() {
        mSetFlagsRule.disableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getSummary() throws Exception {
        when(mInm.getAllowedAssistantAdjustments(any())).thenReturn(List.of(KEY_TYPE));
        assertThat(mController.getSummary()).isEqualTo("On");

        when(mInm.getAllowedAssistantAdjustments(any())).thenReturn(List.of(KEY_IMPORTANCE));
        assertThat(mController.getSummary()).isEqualTo("Off");
    }
}
