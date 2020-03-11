/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.display;

import android.content.Context;
import androidx.fragment.app.Fragment;
import com.android.settings.display.darkmode.DarkModePreference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(RobolectricTestRunner.class)
public class DarkUIPreferenceControllerTest {

    private DarkUIPreferenceController mController;
    private Context mContext;
    @Mock
    private Fragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = spy(new DarkUIPreferenceController(mContext, "dark_ui_mode"));
        mController.setParentFragment(mFragment);
        mController.mPreference = new DarkModePreference(mContext, null /* AttributeSet attrs */);
        mController.onStart();
    }

    @Test
    public void batterySaverToggles_disabledStateUpdates() {
        doReturn(true).when(mController).isPowerSaveMode();
        mController.updateEnabledStateIfNeeded();
        assertThat(mController.mPreference.isEnabled()).isFalse();

        doReturn(false).when(mController).isPowerSaveMode();
        mController.updateEnabledStateIfNeeded();
        assertThat(mController.mPreference.isEnabled()).isTrue();

        doReturn(true).when(mController).isPowerSaveMode();
        mController.updateEnabledStateIfNeeded();
        assertThat(mController.mPreference.isEnabled()).isFalse();
    }
}
