/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.core;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.core.BasePreferenceController.DISABLED_FOR_USER;
import static com.android.settings.core.BasePreferenceController.DISABLED_UNSUPPORTED;
import static com.android.settings.core.BasePreferenceController.UNAVAILABLE_UNKNOWN;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.android.settings.TestConfig;
import com.android.settings.slices.SliceData;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BasePreferenceControllerTest {

    @Mock
    private BasePreferenceController mPreferenceController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }


    @Test(expected = IllegalArgumentException.class)
    public void newController_noKey_shouldCrash() {
        new BasePreferenceController(RuntimeEnvironment.application, null /* key */) {
            @Override
            public int getAvailabilityStatus() {
                return AVAILABLE;
            }
        };
    }

    @Test
    public void isAvailable_availableStatusAvailable_returnsTrue() {
        when(mPreferenceController.getAvailabilityStatus()).thenReturn(AVAILABLE);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_availableStatusUnsupported_returnsFalse() {
        when(mPreferenceController.getAvailabilityStatus()).thenReturn(DISABLED_UNSUPPORTED);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_availableStatusDisabled_returnsFalse() {
        when(mPreferenceController.getAvailabilityStatus()).thenReturn(DISABLED_FOR_USER);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_availableStatusBlockedDependent_returnsFalse() {
        when(mPreferenceController.getAvailabilityStatus()).thenReturn(DISABLED_DEPENDENT_SETTING);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_availableStatusUnavailable_returnsFalse() {
        when(mPreferenceController.getAvailabilityStatus()).thenReturn(UNAVAILABLE_UNKNOWN);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void isSupported_availableStatusAvailable_returnsTrue() {
        when(mPreferenceController.getAvailabilityStatus()).thenReturn(AVAILABLE);

        assertThat(mPreferenceController.isSupported()).isTrue();
    }

    @Test
    public void isSupported_availableStatusUnsupported_returnsFalse() {
        when(mPreferenceController.getAvailabilityStatus()).thenReturn(DISABLED_UNSUPPORTED);

        assertThat(mPreferenceController.isSupported()).isFalse();
    }

    @Test
    public void isSupported_availableStatusDisabled_returnsTrue() {
        when(mPreferenceController.getAvailabilityStatus()).thenReturn(DISABLED_FOR_USER);

        assertThat(mPreferenceController.isSupported()).isTrue();
    }

    @Test
    public void isSupported_availableStatusDependentSetting_returnsTrue() {
        when(mPreferenceController.getAvailabilityStatus()).thenReturn(DISABLED_DEPENDENT_SETTING);

        assertThat(mPreferenceController.isSupported()).isTrue();
    }

    @Test
    public void isSupported_availableStatusUnavailable_returnsTrue() {
        when(mPreferenceController.getAvailabilityStatus()).thenReturn(UNAVAILABLE_UNKNOWN);

        assertThat(mPreferenceController.isSupported()).isTrue();
    }

    @Test
    public void getSliceType_shouldReturnIntent() {
        assertThat(mPreferenceController.getSliceType()).isEqualTo(SliceData.SliceType.INTENT);
    }
}