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

package com.android.settings.privatespace.delete;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class DeletePrivateSpaceControllerTest {
    @Mock private Context mContext;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private Preference mPreference;
    private DeletePrivateSpaceController mDeletePrivateSpaceController;

    /** Required setup before a test. */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        final String preferenceKey = "private_space_delete";

        mPreference = new Preference(ApplicationProvider.getApplicationContext());
        mPreference.setKey(preferenceKey);

        mDeletePrivateSpaceController = new DeletePrivateSpaceController(mContext, preferenceKey);
    }

    /** Tests that the controller is available when private space flag is enabled. */
    @Test
    public void getAvailabilityStatus_whenPrivateFlagEnabled_returnsAvailable() {
        mSetFlagsRule.enableFlags(android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        assertThat(mDeletePrivateSpaceController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    /** Tests that the controller is not available when private space flag is disabled. */
    @Test
    public void getAvailabilityStatus_whenPrivateFlagDisabled_returnsUnsupportedOnDevice() {
        mSetFlagsRule.disableFlags(android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        assertThat(mDeletePrivateSpaceController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
