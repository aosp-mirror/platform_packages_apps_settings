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

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.deviceinfo.storage.StorageEntry;
import com.android.settings.deviceinfo.storage.StorageRenameFragment;
import com.android.settings.deviceinfo.storage.StorageUtils;
import com.android.settings.deviceinfo.storage.StorageUtils.MountTask;
import com.android.settings.deviceinfo.storage.StorageUtils.UnmountTask;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;
import com.android.settingslib.core.lifecycle.events.OnOptionsItemSelected;
import com.android.settingslib.core.lifecycle.events.OnPrepareOptionsMenu;

import java.util.Objects;

/**
 * Handles the option menu on the Storage settings.
 */
public class VolumeOptionMenuController implements LifecycleObserver, OnCreateOptionsMenu,
        OnPrepareOptionsMenu, OnOptionsItemSelected {

    private static final String TAG = "VolumeOptionMenuController";
    private final Context mContext;
    private final Fragment mFragment;
    private final PackageManager mPackageManager;
    @VisibleForTesting
    MenuItem mRename;
    @VisibleForTesting
    MenuItem mMount;
    @VisibleForTesting
    MenuItem mUnmount;
    @VisibleForTesting
    MenuItem mFormat;
    @VisibleForTesting
    MenuItem mFormatAsPortable;
    @VisibleForTesting
    MenuItem mFormatAsInternal;
    @VisibleForTesting
    MenuItem mMigrate;
    @VisibleForTesting
    MenuItem mFree;
    @VisibleForTesting
    MenuItem mForget;
    private StorageEntry mStorageEntry;

    public VolumeOptionMenuController(Context context, Fragment parent, StorageEntry storageEntry) {
        mContext = context;
        mFragment = parent;
        mPackageManager = context.getPackageManager();
        mStorageEntry = storageEntry;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.storage_volume, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mRename = menu.findItem(R.id.storage_rename);
        mMount = menu.findItem(R.id.storage_mount);
        mUnmount = menu.findItem(R.id.storage_unmount);
        mFormat = menu.findItem(R.id.storage_format);
        mFormatAsPortable = menu.findItem(R.id.storage_format_as_portable);
        mFormatAsInternal = menu.findItem(R.id.storage_format_as_internal);
        mMigrate = menu.findItem(R.id.storage_migrate);
        mFree = menu.findItem(R.id.storage_free);
        mForget = menu.findItem(R.id.storage_forget);

        updateOptionsMenu();
    }

    private void updateOptionsMenu() {
        if (mRename == null || mMount == null || mUnmount == null || mFormat == null
                || mFormatAsPortable == null || mFormatAsInternal == null || mMigrate == null
                || mFree == null || mForget == null) {
            Log.d(TAG, "Menu items are not available");
            return;
        }

        mRename.setVisible(false);
        mMount.setVisible(false);
        mUnmount.setVisible(false);
        mFormat.setVisible(false);
        mFormatAsPortable.setVisible(false);
        mFormatAsInternal.setVisible(false);
        mMigrate.setVisible(false);
        mFree.setVisible(false);
        mForget.setVisible(false);

        if (mStorageEntry.isDiskInfoUnsupported()) {
            mFormat.setVisible(true);
            return;
        }
        if (mStorageEntry.isVolumeRecordMissed()) {
            mForget.setVisible(true);
            return;
        }
        if (mStorageEntry.isUnmounted()) {
            mMount.setVisible(true);
            return;
        }
        if (!mStorageEntry.isMounted()) {
            return;
        }

        if (mStorageEntry.isPrivate()) {
            if (!mStorageEntry.isDefaultInternalStorage()) {
                mRename.setVisible(true);
                mFormatAsPortable.setVisible(true);
            }

            // Only offer to migrate when not current storage.
            final VolumeInfo primaryVolumeInfo = mPackageManager.getPrimaryStorageCurrentVolume();
            final VolumeInfo selectedVolumeInfo = mStorageEntry.getVolumeInfo();
            mMigrate.setVisible(primaryVolumeInfo != null
                    && primaryVolumeInfo.getType() == VolumeInfo.TYPE_PRIVATE
                    && !Objects.equals(selectedVolumeInfo, primaryVolumeInfo)
                    && primaryVolumeInfo.isMountedWritable());
            return;
        }

        if (mStorageEntry.isPublic()) {
            mRename.setVisible(true);
            mUnmount.setVisible(true);
            mFormatAsInternal.setVisible(true);
            return;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (!mFragment.isAdded()) {
            return false;
        }

        final int menuId = menuItem.getItemId();
        if (menuId == R.id.storage_mount) {
            if (mStorageEntry.isUnmounted()) {
                new MountTask(mFragment.getActivity(), mStorageEntry.getVolumeInfo()).execute();
                return true;
            }
            return false;
        }
        if (menuId == R.id.storage_unmount) {
            if (mStorageEntry.isMounted()) {
                if (mStorageEntry.isPublic()) {
                    new UnmountTask(mFragment.getActivity(),
                            mStorageEntry.getVolumeInfo()).execute();
                    return true;
                }
                if (mStorageEntry.isPrivate() && !mStorageEntry.isDefaultInternalStorage()) {
                    final Bundle args = new Bundle();
                    args.putString(VolumeInfo.EXTRA_VOLUME_ID, mStorageEntry.getId());
                    new SubSettingLauncher(mContext)
                            .setDestination(PrivateVolumeUnmount.class.getCanonicalName())
                            .setTitleRes(R.string.storage_menu_unmount)
                            .setSourceMetricsCategory(SettingsEnums.DEVICEINFO_STORAGE)
                            .setArguments(args)
                            .launch();
                    return true;
                }
            }
            return false;
        }
        if (menuId == R.id.storage_rename) {
            if ((mStorageEntry.isPrivate() && !mStorageEntry.isDefaultInternalStorage())
                    ||  mStorageEntry.isPublic()) {
                StorageRenameFragment.show(mFragment, mStorageEntry.getVolumeInfo());
                return true;
            }
            return false;
        }
        if (menuId == R.id.storage_format) {
            if (mStorageEntry.isDiskInfoUnsupported() || mStorageEntry.isPublic()) {
                StorageWizardFormatConfirm.showPublic(mFragment.getActivity(),
                        mStorageEntry.getDiskId());
                return true;
            }
            return false;
        }
        if (menuId == R.id.storage_format_as_portable) {
            if (mStorageEntry.isPrivate()) {
                boolean mIsPermittedToAdopt = UserManager.get(mContext).isAdminUser()
                    && !ActivityManager.isUserAMonkey();

                if(!mIsPermittedToAdopt){
                    //Notify guest users as to why formatting is disallowed
                    Toast.makeText(mFragment.getActivity(),
                                 R.string.storage_wizard_guest,Toast.LENGTH_LONG).show();
                    (mFragment.getActivity()).finish();
                    return false;
                }
                final Bundle args = new Bundle();
                args.putString(VolumeInfo.EXTRA_VOLUME_ID, mStorageEntry.getId());
                new SubSettingLauncher(mContext)
                        .setDestination(PrivateVolumeFormat.class.getCanonicalName())
                        .setTitleRes(R.string.storage_menu_format)
                        .setSourceMetricsCategory(SettingsEnums.DEVICEINFO_STORAGE)
                        .setArguments(args)
                        .launch();
                return true;
            }
            return false;
        }
        if (menuId == R.id.storage_format_as_internal) {
            if (mStorageEntry.isPublic()) {
                final Intent intent = new Intent(mFragment.getActivity(), StorageWizardInit.class);
                intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mStorageEntry.getId());
                mContext.startActivity(intent);
                return true;
            }
            return false;
        }
        if (menuId == R.id.storage_migrate) {
            if (mStorageEntry.isPrivate()) {
                final Intent intent = new Intent(mContext, StorageWizardMigrateConfirm.class);
                intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mStorageEntry.getId());
                mContext.startActivity(intent);
                return true;
            }
            return false;
        }
        if (menuId == R.id.storage_forget) {
            if (mStorageEntry.isVolumeRecordMissed()) {
                StorageUtils.launchForgetMissingVolumeRecordFragment(mContext, mStorageEntry);
                return true;
            }
            return false;
        }
        return false;
    }

    public void setSelectedStorageEntry(StorageEntry storageEntry) {
        mStorageEntry = storageEntry;

        updateOptionsMenu();
    }
}