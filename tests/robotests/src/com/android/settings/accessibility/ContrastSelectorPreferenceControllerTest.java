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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.UiModeManager;
import android.content.Context;
import android.widget.FrameLayout;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.Executor;

/** Tests for {@link ContrastSelectorPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class ContrastSelectorPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "color_contrast_selector";

    @Mock
    private UiModeManager mUiService;
    @Mock
    private Executor mExecutor;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private FrameLayout mFrameLayout;
    @Mock
    private LayoutPreference mLayoutPreference;
    private Context mContext;
    private ContrastSelectorPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getMainExecutor()).thenReturn(mExecutor);
        when(mContext.getSystemService(UiModeManager.class)).thenReturn(mUiService);
        mController = new ContrastSelectorPreferenceController(mContext, PREFERENCE_KEY);
        when(mScreen.findPreference(PREFERENCE_KEY)).thenReturn(mLayoutPreference);
        when(mLayoutPreference.findViewById(anyInt())).thenReturn(mFrameLayout);
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void onStart_shouldAddContrastListener() {
        mController.displayPreference(mScreen);
        mController.onStart();

        verify(mUiService).addContrastChangeListener(mExecutor, mController);
    }

    @Test
    public void onStop_shouldRemoveContrastListener() {
        mController.displayPreference(mScreen);
        mController.onStart();
        mController.onStop();

        verify(mUiService).removeContrastChangeListener(mController);
    }

    @Test
    public void displayPreference_shouldAddClickListener() {
        mController.displayPreference(mScreen);

        verify(mFrameLayout, times(3)).setOnClickListener(any());
    }

    @Test
    public void onContrastChanged_buttonShouldBeSelected() {
        mController.displayPreference(mScreen);
        mController.onContrastChanged(1);

        verify(mFrameLayout, times(2)).setSelected(true);
    }
}
