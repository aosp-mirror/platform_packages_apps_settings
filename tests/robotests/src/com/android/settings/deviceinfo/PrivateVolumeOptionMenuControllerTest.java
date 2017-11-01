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

package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.storage.VolumeInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.PackageManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PrivateVolumeOptionMenuControllerTest {
    @Mock
    private MenuItem mMigrateMenuItem;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Menu mMenu;
    @Mock
    private MenuInflater mMenuInflater;
    @Mock
    private PackageManagerWrapper mPm;
    @Mock
    private VolumeInfo mVolumeInfo;
    @Mock
    private VolumeInfo mPrimaryInfo;

    private PrivateVolumeOptionMenuController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mVolumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mPrimaryInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mMenu.findItem(anyInt())).thenReturn(mMigrateMenuItem);
        when(mMigrateMenuItem.getItemId()).thenReturn(100);

        mController = new PrivateVolumeOptionMenuController(
                RuntimeEnvironment.application, mPrimaryInfo, mPm);
    }

    @Test
    public void testMigrateDataMenuItemIsAdded() {
        mController.onCreateOptionsMenu(mMenu, mMenuInflater);

        verify(mMenu).add(Menu.NONE, 100, Menu.NONE, R.string.storage_menu_migrate);
    }

    @Test
    public void testMigrateDataIsNotVisibleNormally() {
        when(mPm.getPrimaryStorageCurrentVolume()).thenReturn(mPrimaryInfo);

        mController.onCreateOptionsMenu(mMenu, mMenuInflater);
        mController.onPrepareOptionsMenu(mMenu);

        verify(mMigrateMenuItem).setVisible(false);
    }

    @Test
    public void testMigrateDataIsVisibleWhenExternalVolumeIsPrimary() {
        when(mPm.getPrimaryStorageCurrentVolume()).thenReturn(mVolumeInfo);

        mController.onCreateOptionsMenu(mMenu, mMenuInflater);
        mController.onPrepareOptionsMenu(mMenu);

        verify(mMigrateMenuItem).setVisible(true);
    }

    @Test
    public void testMigrateDataGoesToMigrateWizard() {
        when(mPm.getPrimaryStorageCurrentVolume()).thenReturn(mVolumeInfo);

        mController.onCreateOptionsMenu(mMenu, mMenuInflater);
        mController.onPrepareOptionsMenu(mMenu);

        assertThat(mController.onOptionsItemSelected(mMigrateMenuItem)).isTrue();
        assertThat(ShadowApplication.getInstance()
                .getNextStartedActivity().getComponent().getClassName())
                .isEqualTo(StorageWizardMigrateConfirm.class.getName());
    }
}
