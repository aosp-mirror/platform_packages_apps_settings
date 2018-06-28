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

import com.android.internal.util.Preconditions;
import com.android.settings.Utils;
import com.android.settingslib.utils.AsyncLoaderCompat;

/**
 * Fetches a user icon as a loader using a given icon loading lambda.
 */
public class UserIconLoader extends AsyncLoaderCompat<SparseArray<Drawable>> {
    private FetchUserIconTask mTask;

    /**
     * Task to load all user icons.
     */
    public interface FetchUserIconTask {
        SparseArray<Drawable> getUserIcons();
    }

    /**
     * Handle the output of this task.
     */
    public interface UserIconHandler {
        void handleUserIcons(SparseArray<Drawable> fetchedIcons);
    }

    public UserIconLoader(Context context, FetchUserIconTask task) {
        super(context);
        mTask = Preconditions.checkNotNull(task);
    }

    @Override
    public SparseArray<Drawable> loadInBackground() {
        return mTask.getUserIcons();
    }

    @Override
    protected void onDiscardResult(SparseArray<Drawable> result) {}

    /**
     * Loads the user icons using a given context. This returns a {@link SparseArray} which maps
     * user ids to their user icons.
     */
    public static SparseArray<Drawable> loadUserIconsWithContext(Context context) {
        SparseArray<Drawable> value = new SparseArray<>();
        UserManager um = context.getSystemService(UserManager.class);
        for (UserInfo userInfo : um.getUsers()) {
            value.put(userInfo.id, Utils.getUserIcon(context, um, userInfo));
        }
        return value;
    }
}
