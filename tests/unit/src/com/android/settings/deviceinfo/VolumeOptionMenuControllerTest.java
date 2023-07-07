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

package com.android.settings.deviceinfo;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.view.Menu;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.deviceinfo.storage.StorageEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class VolumeOptionMenuControllerTest {

    private static final String INTERNAL_VOLUME_ID = "1";
    private static final String EXTERNAL_VOLUME_ID = "2";
    private static final String DISK_ID = "3";
    private static final String VOLUME_RECORD_FSUUID = "volume_record_fsuuid";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Menu mMenu;
    @Mock private PackageManager mPackageManager;
    @Mock private VolumeInfo mExternalVolumeInfo;
    @Mock private VolumeInfo mInternalVolumeInfo;

    private Context mContext;
    private VolumeOptionMenuController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        when(mInternalVolumeInfo.getId()).thenReturn(INTERNAL_VOLUME_ID);
        when(mInternalVolumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mInternalVolumeInfo.getState()).thenReturn(VolumeInfo.STATE_MOUNTED);
        when(mInternalVolumeInfo.isMountedWritable()).thenReturn(true);
        when(mExternalVolumeInfo.getId()).thenReturn(EXTERNAL_VOLUME_ID);

        final StorageEntry selectedStorageEntry = new StorageEntry(mContext, mInternalVolumeInfo);
        mController = new VolumeOptionMenuController(mContext, mock(Fragment.class),
                selectedStorageEntry);
    }

    @Test
    public void onPrepareOptionsMenu_unSupportedDiskInfo_formatIsVisible() {
        final StorageEntry unsupportedStorageEntry =
                new StorageEntry(new DiskInfo(DISK_ID, 0 /* flags */));
        mController.setSelectedStorageEntry(unsupportedStorageEntry);

        mController.onPrepareOptionsMenu(mMenu);

        verify(mController.mFormat, atLeastOnce()).setVisible(true);
        verify(mController.mRename, never()).setVisible(true);
        verify(mController.mMount, never()).setVisible(true);
        verify(mController.mUnmount, never()).setVisible(true);
        verify(mController.mFormatAsPortable, never()).setVisible(true);
        verify(mController.mFormatAsInternal, never()).setVisible(true);
        verify(mController.mMigrate, never()).setVisible(true);
        verify(mController.mFree, never()).setVisible(true);
        verify(mController.mForget, never()).setVisible(true);
    }

    @Test
    public void onPrepareOptionsMenu_missingVolumeRecord_forgetIsVisible() {
        final StorageEntry missingStorageEntry =
                new StorageEntry(new VolumeRecord(0 /* type */, VOLUME_RECORD_FSUUID));
        mController.setSelectedStorageEntry(missingStorageEntry);

        mController.onPrepareOptionsMenu(mMenu);

        verify(mController.mForget, atLeastOnce()).setVisible(true);
        verify(mController.mRename, never()).setVisible(true);
        verify(mController.mMount, never()).setVisible(true);
        verify(mController.mUnmount, never()).setVisible(true);
        verify(mController.mFormat, never()).setVisible(true);
        verify(mController.mFormatAsPortable, never()).setVisible(true);
        verify(mController.mFormatAsInternal, never()).setVisible(true);
        verify(mController.mMigrate, never()).setVisible(true);
        verify(mController.mFree, never()).setVisible(true);
    }

    @Test
    public void onPrepareOptionsMenu_unmountedStorage_mountIsVisible() {
        when(mInternalVolumeInfo.getState()).thenReturn(VolumeInfo.STATE_UNMOUNTED);
        mController.setSelectedStorageEntry(new StorageEntry(mContext, mInternalVolumeInfo));

        mController.onPrepareOptionsMenu(mMenu);

        verify(mController.mMount, atLeastOnce()).setVisible(true);
        verify(mController.mRename, never()).setVisible(true);
        verify(mController.mUnmount, never()).setVisible(true);
        verify(mController.mFormat, never()).setVisible(true);
        verify(mController.mFormatAsPortable, never()).setVisible(true);
        verify(mController.mFormatAsInternal, never()).setVisible(true);
        verify(mController.mMigrate, never()).setVisible(true);
        verify(mController.mFree, never()).setVisible(true);
        verify(mController.mForget, never()).setVisible(true);
    }

    @Test
    public void onPrepareOptionsMenu_privateNotDefaultInternal_someMenusAreVisible() {
        mController.onPrepareOptionsMenu(mMenu);

        verify(mController.mRename, atLeastOnce()).setVisible(true);
        verify(mController.mFormatAsPortable, atLeastOnce()).setVisible(true);
        verify(mController.mMount, never()).setVisible(true);
        verify(mController.mFormat, never()).setVisible(true);
        verify(mController.mFormatAsInternal, never()).setVisible(true);
        verify(mController.mFree, never()).setVisible(true);
        verify(mController.mForget, never()).setVisible(true);
    }

    @Test
    public void onPrepareOptionsMenu_privateDefaultInternal_mostMenusAreNotVisible() {
        when(mInternalVolumeInfo.getId()).thenReturn(VolumeInfo.ID_PRIVATE_INTERNAL);
        when(mPackageManager.getPrimaryStorageCurrentVolume()).thenReturn(mInternalVolumeInfo);

        mController.onPrepareOptionsMenu(mMenu);

        verify(mController.mRename, never()).setVisible(true);
        verify(mController.mUnmount, never()).setVisible(true);
        verify(mController.mFormatAsPortable, never()).setVisible(true);
        verify(mController.mMount, never()).setVisible(true);
        verify(mController.mFormat, never()).setVisible(true);
        verify(mController.mFormatAsInternal, never()).setVisible(true);
        verify(mController.mFree, never()).setVisible(true);
        verify(mController.mForget, never()).setVisible(true);
    }

    @Test
    public void onPrepareOptionsMenu_publicStorage_someMenusArcVisible() {
        when(mExternalVolumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PUBLIC);
        when(mExternalVolumeInfo.getState()).thenReturn(VolumeInfo.STATE_MOUNTED);
        when(mExternalVolumeInfo.getDiskId()).thenReturn(DISK_ID);
        final DiskInfo externalDiskInfo = mock(DiskInfo.class);
        mController.setSelectedStorageEntry(new StorageEntry(mContext, mExternalVolumeInfo));

        mController.onPrepareOptionsMenu(mMenu);

        verify(mController.mRename, atLeastOnce()).setVisible(true);
        verify(mController.mUnmount, atLeastOnce()).setVisible(true);
        verify(mController.mFormatAsInternal, atLeastOnce()).setVisible(true);
        verify(mController.mFormatAsPortable, never()).setVisible(true);
        verify(mController.mFormat, never()).setVisible(true);
        verify(mController.mMount, never()).setVisible(true);
        verify(mController.mFree, never()).setVisible(true);
        verify(mController.mForget, never()).setVisible(true);
    }

    @Test
    public void onPrepareOptionsMenu_noExternalStorage_migrateNotVisible() {
        when(mPackageManager.getPrimaryStorageCurrentVolume()).thenReturn(mInternalVolumeInfo);

        mController.onPrepareOptionsMenu(mMenu);

        verify(mController.mMigrate, atLeastOnce()).setVisible(false);
        verify(mController.mMigrate, never()).setVisible(true);
    }

    @Test
    public void onPrepareOptionsMenu_externalPrimaryStorageAvailable_migrateIsVisible() {
        when(mExternalVolumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mExternalVolumeInfo.isMountedWritable()).thenReturn(true);
        when(mPackageManager.getPrimaryStorageCurrentVolume()).thenReturn(mExternalVolumeInfo);

        mController.onPrepareOptionsMenu(mMenu);

        verify(mController.mMigrate, atLeastOnce()).setVisible(true);
    }

    @Test
    public void onPrepareOptionsMenu_externalUnmounted_migrateIsVisible() {
        when(mExternalVolumeInfo.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mExternalVolumeInfo.isMountedWritable()).thenReturn(false);
        when(mPackageManager.getPrimaryStorageCurrentVolume()).thenReturn(mExternalVolumeInfo);

        mController.onPrepareOptionsMenu(mMenu);

        verify(mController.mMigrate, atLeastOnce()).setVisible(false);
        verify(mController.mMigrate, never()).setVisible(true);
    }
}
