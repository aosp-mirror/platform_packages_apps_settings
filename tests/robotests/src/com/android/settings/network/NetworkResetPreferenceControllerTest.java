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

package com.android.settings.network;


import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class NetworkResetPreferenceControllerTest {

    @Mock
    private NetworkResetRestrictionChecker mRestrictionChecker;
    private Context mContext;
    private NetworkResetPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new NetworkResetPreferenceController(mContext);
        ReflectionHelpers.setField(mController, "mRestrictionChecker", mRestrictionChecker);
    }

    @Test
    public void testIsAvailable_shouldReturnTrueWhenNoUserRestriction() {
        when(mRestrictionChecker.isRestrictionEnforcedByAdmin()).thenReturn(true);

        when(mRestrictionChecker.hasUserRestriction()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();

        when(mRestrictionChecker.hasUserRestriction()).thenReturn(false);

        assertThat(mController.isAvailable()).isTrue();
        verify(mRestrictionChecker, never()).isRestrictionEnforcedByAdmin();
    }
}
