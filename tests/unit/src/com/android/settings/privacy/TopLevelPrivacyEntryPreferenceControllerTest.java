/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.privacy;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.security.TopLevelSecurityEntryPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class TopLevelPrivacyEntryPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "top_level_privacy";

    private TopLevelPrivacyEntryPreferenceController mTopLevelPrivacyEntryPreferenceController;

    @Mock
    private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;

        mTopLevelPrivacyEntryPreferenceController =
                new TopLevelPrivacyEntryPreferenceController(
                        ApplicationProvider.getApplicationContext(), PREFERENCE_KEY);
    }

    @Test
    public void getAvailabilityStatus_whenSafetyCenterEnabled_returnsUnavailable() {
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(true);

        assertThat(mTopLevelPrivacyEntryPreferenceController.getAvailabilityStatus())
                .isEqualTo(TopLevelSecurityEntryPreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_whenSafetyCenterDisabled_returnsAvailable() {
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(false);

        assertThat(mTopLevelPrivacyEntryPreferenceController.getAvailabilityStatus())
                .isEqualTo(TopLevelSecurityEntryPreferenceController.AVAILABLE);
    }
}
