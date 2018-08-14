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

package com.android.settings.development.featureflags;

import static android.arch.lifecycle.Lifecycle.Event.ON_START;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class FeatureFlagPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private FeatureFlagsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = new FeatureFlagsPreferenceController(mContext, mLifecycle);
        when(mScreen.getContext()).thenReturn(mContext);
        mController.displayPreference(mScreen);
    }

    @Test
    public void verifyConstants() {
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.getPreferenceKey()).isNull();
    }

    @Test
    public void onStart_shouldRefreshFeatureFlags() {
        mLifecycle.handleLifecycleEvent(ON_START);

        verify(mScreen).removeAll();
        verify(mScreen, atLeastOnce()).addPreference(any(FeatureFlagPreference.class));
    }
}
