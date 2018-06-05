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

package com.android.settings.gestures;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class PreventRingingParentPreferenceControllerTest {

    @Mock
    private Resources mResources;

    private Context mContext;
    private PreventRingingParentPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getResources()).thenReturn(mResources);

        mController = new PreventRingingParentPreferenceController(mContext, "test_key");
    }

    @Test
    public void testIsAvailable_configIsTrue_shouldAvailableUnSearchable() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void testIsAvailable_configIsFalse_shouldReturnFalse() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }
}
