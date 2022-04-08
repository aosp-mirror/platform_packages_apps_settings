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
    private static final int USER_PROFILE_INSERTION_LOCATION = 6;
    private static final int SIZE_NOT_SET = -1;

    private @NonNull
    UserInfo mUser;
    private @Nullable
    StorageItemPreference mStoragePreference;
    private Drawable mUserIcon;
    private long mSize;
    private long mTotalSizeBytes;

    /**
     * Adds the appropriate controllers to a controller list for handling all secondary users on
     * a device.
     *
     * @param context     Context for initializing the preference controllers.
     * @param userManager UserManagerWrapper for figuring out which controllers to add.
     */
    public static List<AbstractPreferenceController> getSecondaryUserControllers(
            Context context, UserManager userManager) {

        List<AbstractPreferenceController> controllers = new ArrayList<>();
        UserInfo primaryUser = userManager.getPrimaryUser();
        boolean addedUser = false;
        List<UserInfo> infos = userManager.getUsers();
        for (int i = 0, size = infos.size(); i < size; i++) {
            UserInfo info = infos.get(i);
            if (info.isPrimary()) {
                continue;
            }

            if (info == null || Utils.isProfileOf(primaryUser, info)) {
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
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (mStoragePreference == null) {
            mStoragePreference = new StorageItemPreference(screen.getContext());

            PreferenceGroup group =
                    screen.findPreference(TARGET_PREFERENCE_GROUP_KEY);
            mStoragePreference.setTitle(mUser.name);
            mStoragePreference.setKey(PREFERENCE_KEY_BASE + mUser.id);
            if (mSize != SIZE_NOT_SET) {
                mStoragePreference.setStorageSize(mSize, mTotalSizeBytes);
            }

            group.setVisible(true);
            group.addPreference(mStoragePreference);
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
    public void setSize(long size) {
        mSize = size;
        if (mStoragePreference != null) {
            mStoragePreference.setStorageSize(mSize, mTotalSizeBytes);
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

    public void handleResult(SparseArray<StorageAsyncLoader.AppsStorageResult> stats) {
        int userId = getUser().id;
        StorageAsyncLoader.AppsStorageResult result = stats.get(userId);
        if (result != null) {
            setSize(result.externalStats.totalBytes);
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
