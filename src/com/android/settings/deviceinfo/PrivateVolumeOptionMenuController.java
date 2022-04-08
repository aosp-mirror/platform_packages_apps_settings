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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.storage.VolumeInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;
import com.android.settingslib.core.lifecycle.events.OnOptionsItemSelected;
import com.android.settingslib.core.lifecycle.events.OnPrepareOptionsMenu;

import java.util.Objects;

/**
 * Handles the option menu on the Storage settings.
 */
public class PrivateVolumeOptionMenuController implements LifecycleObserver, OnCreateOptionsMenu,
        OnPrepareOptionsMenu, OnOptionsItemSelected {
    private static final int OPTIONS_MENU_MIGRATE_DATA = 100;

    private Context mContext;
    private VolumeInfo mVolumeInfo;
    private PackageManager mPm;

    public PrivateVolumeOptionMenuController(
            Context context, VolumeInfo volumeInfo, PackageManager packageManager) {
        mContext = context;
        mVolumeInfo = volumeInfo;
        mPm = packageManager;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        menu.add(Menu.NONE, OPTIONS_MENU_MIGRATE_DATA, 0, R.string.storage_menu_migrate);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mVolumeInfo == null) {
            return;
        }

        // Only offer to migrate when not current storage
        final VolumeInfo privateVol = mPm.getPrimaryStorageCurrentVolume();
        final MenuItem migrate = menu.findItem(OPTIONS_MENU_MIGRATE_DATA);
        if (migrate != null) {
            migrate.setVisible((privateVol != null)
                    && (privateVol.getType() == VolumeInfo.TYPE_PRIVATE)
                    && !Objects.equals(mVolumeInfo, privateVol)
                    && privateVol.isMountedWritable());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == OPTIONS_MENU_MIGRATE_DATA) {
            final Intent intent = new Intent(mContext, StorageWizardMigrateConfirm.class);
            intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mVolumeInfo.getId());
            mContext.startActivity(intent);
            return true;
        }
        return false;
    }
}
