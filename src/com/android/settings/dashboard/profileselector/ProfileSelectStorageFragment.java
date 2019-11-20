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
 * limitations under the License.
 */

package com.android.settings.dashboard.profileselector;

import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.VolumeInfo;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.deviceinfo.StorageProfileFragment;

/**
 * Storage Setting page for personal/managed profile.
 */
public class ProfileSelectStorageFragment extends ProfileSelectFragment {
    @Override
    public Fragment[] getFragments() {

        final Bundle storageBundle = new Bundle();
        storageBundle.putString(VolumeInfo.EXTRA_VOLUME_ID, VolumeInfo.ID_PRIVATE_INTERNAL);
        storageBundle.putInt(EXTRA_PROFILE, ProfileSelectFragment.PERSONAL);

        final Fragment storageDashboardFragment = new StorageDashboardFragment();
        storageDashboardFragment.setArguments(storageBundle);

        UserInfo targetUser = null;
        for (UserInfo info : UserManager.get(getActivity()).getUsers()) {
            if (!info.isPrimary()) {
                targetUser = info;
                break;
            }
        }
        if (targetUser != null) {
            storageBundle.putInt(StorageProfileFragment.USER_ID_EXTRA, targetUser.id);
        }
        final Fragment storageProfileFragment = new StorageProfileFragment();
        storageProfileFragment.setArguments(storageBundle);

        return new Fragment[]{
                storageDashboardFragment,
                storageProfileFragment
        };
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.storage_summary_donut;
    }
}

