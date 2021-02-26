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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.storage.VolumeInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class PrivateVolumeOptionMenuControllerTest {

    @Mock
    private MenuItem mMigrateMenuItem;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Menu mMenu;
    @Mock
    private MenuInflater mMenuInflater;
    @Mock
    private PackageManager mPm;
    @Mock
    private VolumeInfo mVolumeInfo;
    @Mock
    private VolumeInfo mPrimaryInfo;

    private Context mContext;
    private PrivateVolumeOptionMenuController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        when(mVolumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mVolumeInfo.isMountedWritable()).thenReturn(true);
        when(mPrimaryInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mMenu.findItem(anyInt())).thenReturn(mMigrateMenuItem);
        when(mMigrateMenuItem.getItemId()).thenReturn(100);

        mController = new PrivateVolumeOptionMenuController(mContext, mPrimaryInfo, mPm);
    }

    @Test
    public void testMigrateDataMenuItemIsAdded() {
        mController.onCreateOptionsMenu(mMenu, mMenuInflater);

        verify(mMenu).add(Menu.NONE, 100, Menu.NONE, ResourcesUtils.getResourcesId(
                mContext, "string", "storage_menu_migrate"));
    }

    @Test
    public void testMigrateDataIsNotVisibleNormally() {
        when(mPm.getPrimaryStorageCurrentVolume()).thenReturn(mPrimaryInfo);
        when(mPrimaryInfo.isMountedWritable()).thenReturn(true);

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
    public void testMigrateDataIsNotVisibleWhenExternalVolumeIsNotMounted() {
        when(mPm.getPrimaryStorageCurrentVolume()).thenReturn(mVolumeInfo);
        when(mVolumeInfo.isMountedWritable()).thenReturn(false);

        mController.onCreateOptionsMenu(mMenu, mMenuInflater);
        mController.onPrepareOptionsMenu(mMenu);

        verify(mMigrateMenuItem).setVisible(false);
    }

    @Test
    public void testMigrateDataGoesToMigrateWizard() {
        when(mPm.getPrimaryStorageCurrentVolume()).thenReturn(mVolumeInfo);
        doNothing().when(mContext).startActivity(any(Intent.class));

        mController.onCreateOptionsMenu(mMenu, mMenuInflater);
        mController.onPrepareOptionsMenu(mMenu);

        assertThat(mController.onOptionsItemSelected(mMigrateMenuItem)).isTrue();

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(
                Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        final Intent startedIntent = intentCaptor.getValue();
        assertThat(startedIntent.getComponent().getClassName())
                .isEqualTo(StorageWizardMigrateConfirm.class.getName());
    }
}
