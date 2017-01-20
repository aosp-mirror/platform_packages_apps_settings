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
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.applications.UserManagerWrapper;
import com.android.settings.core.PreferenceController;

import java.util.List;

/**
 * SecondaryUserController controls the preferences on the Storage screen which had to do with
 * secondary users.
 */
public class SecondaryUserController extends PreferenceController {
    // PreferenceGroupKey to try to add our preference onto.
    private static final String TARGET_PREFERENCE_GROUP_KEY = "pref_secondary_users";
    private static final String PREFERENCE_KEY_BASE = "pref_user_";

    private UserInfo mUser;
    private StorageItemPreferenceAlternate mPreference;

    /**
     * Adds the appropriate controllers to a controller list for handling all secondary users on
     * a device.
     * @param context Context for initializing the preference controllers.
     * @param controllers List of preference controllers for a Settings fragment.
     */
    public static void addAllSecondaryUserControllers(Context context,
            UserManagerWrapper userManager, List<PreferenceController> controllers) {
        UserInfo primaryUser = userManager.getPrimaryUser();
        boolean addedUser = false;
        List<UserInfo> infos = userManager.getUsers();
        for (int i = 0, size = infos.size(); i < size; i++) {
            UserInfo info = infos.get(i);
            if (Utils.isProfileOf(primaryUser, info)) {
                continue;
            }

            controllers.add(new SecondaryUserController(context, info));
            addedUser = true;
        }

        if (!addedUser) {
            controllers.add(new NoSecondaryUserController(context));
        }
    }

    /**
     * Constructor for a given secondary user.
     * @param context Context to initialize the underlying {@link PreferenceController}.
     * @param info {@link UserInfo} for the secondary user which this controllers covers.
     */
    @VisibleForTesting
    SecondaryUserController(Context context, UserInfo info) {
        super(context);
        mUser = info;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (mPreference == null) {
            mPreference = new StorageItemPreferenceAlternate(mContext);

            PreferenceGroup group =
                    (PreferenceGroup) screen.findPreference(TARGET_PREFERENCE_GROUP_KEY);
            mPreference.setTitle(mUser.name);
            mPreference.setKey(PREFERENCE_KEY_BASE + mUser.id);
            group.setVisible(true);
            group.addPreference(mPreference);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return (mPreference != null) ? mPreference.getKey() : null;
    }

    /**
     * Sets the size for the preference.
     * @param size Size in bytes.
     */
    public void setSize(long size) {
        if (mPreference != null) {
            mPreference.setStorageSize(size);
        }
    }

    private static class NoSecondaryUserController extends PreferenceController {
        public NoSecondaryUserController(Context context) {
            super(context);
        }

        @Override
        public void displayPreference(PreferenceScreen screen) {
            PreferenceGroup group =
                    (PreferenceGroup) screen.findPreference(TARGET_PREFERENCE_GROUP_KEY);
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
