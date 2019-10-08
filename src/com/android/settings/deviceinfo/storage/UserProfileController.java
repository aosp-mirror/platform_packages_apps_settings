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

package com.android.settings.deviceinfo.storage;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.storage.VolumeInfo;
import android.util.SparseArray;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.Preconditions;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.deviceinfo.StorageItemPreference;
import com.android.settings.deviceinfo.StorageProfileFragment;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Defines a {@link AbstractPreferenceController} which handles a single profile of the primary
 * user.
 */
public class UserProfileController extends AbstractPreferenceController implements
        PreferenceControllerMixin, StorageAsyncLoader.ResultHandler,
        UserIconLoader.UserIconHandler {
    private static final String PREFERENCE_KEY_BASE = "pref_profile_";
    private StorageItemPreference mStoragePreference;
    private UserInfo mUser;
    private long mTotalSizeBytes;
    private final int mPreferenceOrder;

    public UserProfileController(Context context, UserInfo info, int preferenceOrder) {
        super(context);
        mUser = Preconditions.checkNotNull(info);
        mPreferenceOrder = preferenceOrder;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY_BASE + mUser.id;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mStoragePreference = new StorageItemPreference(screen.getContext());
        mStoragePreference.setOrder(mPreferenceOrder);
        mStoragePreference.setKey(PREFERENCE_KEY_BASE + mUser.id);
        mStoragePreference.setTitle(mUser.name);
        screen.addPreference(mStoragePreference);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference != null && mStoragePreference == preference) {
            final Bundle args = new Bundle();
            args.putInt(StorageProfileFragment.USER_ID_EXTRA, mUser.id);
            args.putString(VolumeInfo.EXTRA_VOLUME_ID, VolumeInfo.ID_PRIVATE_INTERNAL);

            new SubSettingLauncher(mContext)
                    .setDestination(StorageProfileFragment.class.getName())
                    .setArguments(args)
                    .setTitleText(mUser.name)
                    .setSourceMetricsCategory(SettingsEnums.DEVICEINFO_STORAGE)
                    .launch();
            return true;
        }

        return false;
    }

    @Override
    public void handleResult(SparseArray<StorageAsyncLoader.AppsStorageResult> stats) {
        Preconditions.checkNotNull(stats);

        int userId = mUser.id;
        StorageAsyncLoader.AppsStorageResult result = stats.get(userId);
        if (result != null) {
            setSize(result.externalStats.totalBytes
                            + result.otherAppsSize
                            + result.videoAppsSize
                            + result.musicAppsSize
                            + result.gamesSize,
                    mTotalSizeBytes);
        }
    }

    /**
     * Sets the size for the preference using a byte count.
     */
    public void setSize(long size, long totalSize) {
        if (mStoragePreference != null) {
            mStoragePreference.setStorageSize(size, totalSize);
        }
    }

    public void setTotalSize(long totalSize) {
        mTotalSizeBytes = totalSize;
    }

    @Override
    public void handleUserIcons(SparseArray<Drawable> fetchedIcons) {
        Drawable userIcon = fetchedIcons.get(mUser.id);
        if (userIcon != null) {
            mStoragePreference.setIcon(applyTint(mContext, userIcon));
        }
    }

    private static Drawable applyTint(Context context, Drawable icon) {
        icon = icon.mutate();
        icon.setTintList(Utils.getColorAttr(context, android.R.attr.colorControlNormal));
        return icon;
    }

}
