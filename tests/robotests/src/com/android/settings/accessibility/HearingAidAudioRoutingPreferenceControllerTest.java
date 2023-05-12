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

package com.android.settings.accessibility;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.util.FeatureFlagUtils;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link HearingAidAudioRoutingPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class HearingAidAudioRoutingPreferenceControllerTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private HearingAidAudioRoutingPreferenceController mController;

    @Before
    public void setUp() {
        mController = new HearingAidAudioRoutingPreferenceController(mContext, "test_key");
    }

    @Test
    public void getAvailabilityStatus_audioRoutingNotSupported_returnUnsupported() {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING, false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_audioRoutingNotSupported_available() {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING, true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
