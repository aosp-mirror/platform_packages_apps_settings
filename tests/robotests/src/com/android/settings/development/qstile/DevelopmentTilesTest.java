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

package com.android.settings.development.qstile;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.service.quicksettings.Tile;

import com.android.settingslib.development.DevelopmentSettingsEnabler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowUserManager;

@RunWith(RobolectricTestRunner.class)
public class DevelopmentTilesTest {

    @Mock
    private Tile mTile;
    @Mock
    private PackageManager mPackageManager;

    private DevelopmentTiles mService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mService = spy(Robolectric.setupService(DevelopmentTiles.ShowLayout.class));
        final ShadowUserManager um = Shadows.shadowOf(
                RuntimeEnvironment.application.getSystemService(UserManager.class));
        um.setIsAdminUser(true);
        doReturn(mTile).when(mService).getQsTile();
    }

    @Test
    public void refresh_devOptionIsDisabled_shouldResetTileValue() {
        final ComponentName cn = new ComponentName(
                mService.getPackageName(), mService.getClass().getName());
        doReturn(mPackageManager).when(mService).getPackageManager();

        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(mService, false);
        mService.setIsEnabled(true);

        mService.refresh();

        verify(mPackageManager).setComponentEnabledSetting(cn,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        assertThat(mService.isEnabled()).isFalse();
    }
}
