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

package com.android.settings.notification;

import static android.app.NotificationManager.IMPORTANCE_NONE;

import android.annotation.Nullable;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Objects;

/**
 * Parent class for preferences appearing on notification setting pages at the app,
 * notification channel group, or notification channel level.
 */
public abstract class NotificationPreferenceController extends AbstractPreferenceController {
    private static final String TAG = "ChannelPrefContr";
    @Nullable
    protected NotificationChannel mChannel;
    @Nullable
    protected NotificationChannelGroup mChannelGroup;
    protected RestrictedLockUtils.EnforcedAdmin mAdmin;
    protected NotificationBackend.AppRow mAppRow;
    protected final NotificationManager mNm;
    protected final NotificationBackend mBackend;
    protected final Context mContext;
    protected final UserManager mUm;
    protected final PackageManager mPm;
    protected Preference mPreference;

    public NotificationPreferenceController(Context context, NotificationBackend backend) {
        super(context);
        mContext = context;
        mNm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mBackend = backend;
        mUm = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mPm = mContext.getPackageManager();
    }

    /**
     * Returns true if field's parent object is not blocked.
     */
    @Override
    public boolean isAvailable() {
        if (mAppRow == null) {
            return false;
        }
        if (mAppRow.banned) {
            return false;
        }
        if (mChannelGroup != null) {
            if (mChannelGroup.isBlocked()) {
                return false;
            }
        }
        if (mChannel != null) {
            return mChannel.getImportance() != IMPORTANCE_NONE;
        }
        return true;
    }

    protected void onResume(NotificationBackend.AppRow appRow,
            @Nullable NotificationChannel channel, @Nullable NotificationChannelGroup group,
            RestrictedLockUtils.EnforcedAdmin admin) {
        mAppRow = appRow;
        mChannel = channel;
        mChannelGroup = group;
        mAdmin = admin;
    }

    protected boolean checkCanBeVisible(int minImportanceVisible) {
        if (mChannel == null) {
            Log.w(TAG, "No channel");
            return false;
        }

        int importance = mChannel.getImportance();
        if (importance == NotificationManager.IMPORTANCE_UNSPECIFIED) {
            return true;
        }
        return importance >= minImportanceVisible;
    }

    protected void saveChannel() {
        if (mChannel != null && mAppRow != null) {
            mBackend.updateChannel(mAppRow.pkg, mAppRow.uid, mChannel);
        }
    }

    protected boolean isChannelBlockable() {
        return isChannelBlockable(mChannel);
    }

    protected boolean isChannelBlockable(NotificationChannel channel) {
        if (channel != null && mAppRow != null) {
            if (channel.isImportanceLockedByCriticalDeviceFunction()
                    || channel.isImportanceLockedByOEM()) {
                return channel.getImportance() == IMPORTANCE_NONE;
            }

            return channel.isBlockableSystem() || !mAppRow.systemApp
                    || channel.getImportance() == IMPORTANCE_NONE;
        }
        return false;
    }

    protected boolean isChannelConfigurable(NotificationChannel channel) {
        if (channel != null && mAppRow != null) {
            return !channel.isImportanceLockedByOEM();
        }
        return false;
    }

    protected boolean isChannelGroupBlockable() {
        return isChannelGroupBlockable(mChannelGroup);
    }

    protected boolean isChannelGroupBlockable(NotificationChannelGroup group) {
        if (group != null && mAppRow != null) {
            if (!mAppRow.systemApp) {
                return true;
            }

            return group.isBlocked();
        }
        return false;
    }

    protected boolean hasValidGroup() {
        return mChannelGroup != null;
    }

    protected final boolean isDefaultChannel() {
        if (mChannel == null) {
            return false;
        }
        return Objects.equals(NotificationChannel.DEFAULT_CHANNEL_ID, mChannel.getId());
    }
}
