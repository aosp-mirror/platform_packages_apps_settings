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

import android.content.Context;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.deviceinfo.StorageItemPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * SecondaryUserController controls the preferences on the Storage screen which had to do with
 * secondary users.
 */
public class SecondaryUserController extends AbstractPreferenceController implements
        PreferenceControllerMixin, StorageAsyncLoader.ResultHandler,
        UserIconLoader.UserIconHandler {
    // PreferenceGroupKey to try to add our preference onto.
    private static final String TARGET_PREFERENCE_GROUP_KEY = "pref_secondary_users";
    private static final String PREFERENCE_KEY_BASE = "pref_user_";
    private static final int SIZE_NOT_SET = -1;

    private @NonNull
    UserInfo mUser;
    private @Nullable
    StorageItemPreference mStoragePreference;
    private PreferenceGroup mPreferenceGroup;
    private Drawable mUserIcon;
    private long mSize;
    private long mTotalSizeBytes;
    private boolean mIsVisible;
    private StorageCacheHelper mStorageCacheHelper;

    /**
     * Adds the appropriate controllers to a controller list for handling all secondary users on
     * a device.
     *
     * @param context     Context for initializing the preference controllers.
     * @param userManager UserManagerWrapper for figuring out which controllers to add.
     * @param isWorkProfileOnly only shows secondary users of work profile.
     *                          (e.g., it should be true in work profile tab)
     */
    public static List<AbstractPreferenceController> getSecondaryUserControllers(
            Context context, UserManager userManager, boolean isWorkProfileOnly) {

        List<AbstractPreferenceController> controllers = new ArrayList<>();
        UserInfo primaryUser = userManager.getPrimaryUser();
        boolean addedUser = false;
        List<UserInfo> infos = userManager.getUsers();
        for (int i = 0, size = infos.size(); i < size; i++) {
            UserInfo info = infos.get(i);
            if (info.isPrimary()) {
                continue;
            }

            if (Utils.isProfileOf(primaryUser, info)) {
                continue;
            }

            if (isWorkProfileOnly && !info.isManagedProfile()) {
                continue;
            }

            controllers.add(new SecondaryUserController(context, info));
            addedUser = true;
        }

        if (!addedUser) {
            controllers.add(new NoSecondaryUserController(context));
        }
        return controllers;
    }

    /**
     * Constructor for a given secondary user.
     *
     * @param context Context to initialize the underlying {@link AbstractPreferenceController}.
     * @param info    {@link UserInfo} for the secondary user which this controllers covers.
     */
    @VisibleForTesting
    SecondaryUserController(Context context, @NonNull UserInfo info) {
        super(context);
        mUser = info;
        mSize = SIZE_NOT_SET;
        mStorageCacheHelper = new StorageCacheHelper(context, info.id);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (mStoragePreference == null) {
            mStoragePreference = new StorageItemPreference(screen.getContext());

            mPreferenceGroup = screen.findPreference(TARGET_PREFERENCE_GROUP_KEY);
            mStoragePreference.setTitle(mUser.name);
            mStoragePreference.setKey(PREFERENCE_KEY_BASE + mUser.id);
            setSize(mStorageCacheHelper.retrieveUsedSize(), false /* animate */);

            mPreferenceGroup.setVisible(mIsVisible);
            mPreferenceGroup.addPreference(mStoragePreference);
            maybeSetIcon();
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mStoragePreference != null ? mStoragePreference.getKey() : null;
    }

    /**
     * Returns the user for which this is the secondary user controller.
     */
    @NonNull
    public UserInfo getUser() {
        return mUser;
    }

    /**
     * Sets the size for the preference.
     *
     * @param size Size in bytes.
     */
    public void setSize(long size, boolean animate) {
        mSize = size;
        if (mStoragePreference != null) {
            mStoragePreference.setStorageSize(mSize, mTotalSizeBytes, animate);
        }
    }

    /**
     * Sets the total size for the preference for the progress bar.
     *
     * @param totalSizeBytes Total size in bytes.
     */
    public void setTotalSize(long totalSizeBytes) {
        mTotalSizeBytes = totalSizeBytes;
    }

    /**
     * Sets visibility of the PreferenceGroup of secondary user.
     *
     * @param visible Visibility of the PreferenceGroup.
     */
    public void setPreferenceGroupVisible(boolean visible) {
        mIsVisible = visible;
        if (mPreferenceGroup != null) {
            mPreferenceGroup.setVisible(mIsVisible);
        }
    }

    @Override
    public void handleResult(SparseArray<StorageAsyncLoader.StorageResult> stats) {
        if (stats == null) {
            setSize(mStorageCacheHelper.retrieveUsedSize(), false /* animate */);
            return;
        }
        final StorageAsyncLoader.StorageResult result = stats.get(getUser().id);
        if (result != null) {
            setSize(result.externalStats.totalBytes, true /* animate */);
            // TODO(b/171758224): Update the source of size info
            mStorageCacheHelper.cacheUsedSize(result.externalStats.totalBytes);
        }
    }

    @Override
    public void handleUserIcons(SparseArray<Drawable> fetchedIcons) {
        mUserIcon = fetchedIcons.get(mUser.id);
        maybeSetIcon();
    }

    private void maybeSetIcon() {
        if (mUserIcon != null && mStoragePreference != null) {
            mStoragePreference.setIcon(mUserIcon);
        }
    }

    @VisibleForTesting
    static class NoSecondaryUserController extends AbstractPreferenceController implements
            PreferenceControllerMixin {
        public NoSecondaryUserController(Context context) {
            super(context);
        }

        @Override
        public void displayPreference(PreferenceScreen screen) {
            final PreferenceGroup group = screen.findPreference(TARGET_PREFERENCE_GROUP_KEY);
            if (group == null) {
                return;
            }
            screen.removePreference(group);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }
    }
}
