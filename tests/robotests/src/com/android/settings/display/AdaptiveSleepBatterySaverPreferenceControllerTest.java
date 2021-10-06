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

package com.android.settings.display;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AdaptiveSleepBatterySaverPreferenceControllerTest {
    private Context mContext;
    private AdaptiveSleepBatterySaverPreferenceController mController;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(getApplicationContext());

        doReturn(mPackageManager).when(mContext).getPackageManager();
        when(mPackageManager.getAttentionServicePackageName()).thenReturn("some.package");
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_GRANTED);

        mController = spy(new AdaptiveSleepBatterySaverPreferenceController(mContext));
        when(mController.isPowerSaveMode()).thenReturn(false);
    }

    @Test
    public void addToScreen_normalCase_hidePreference() {
        mController.addToScreen(mScreen);

        assertThat(mController.mPreference.isVisible()).isFalse();
    }

    @Test
    public void addToScreen_powerSaveEnabled_showPreference() {
        when(mController.isPowerSaveMode()).thenReturn(true);

        mController.addToScreen(mScreen);

        assertThat(mController.mPreference.isVisible()).isTrue();
    }
}
