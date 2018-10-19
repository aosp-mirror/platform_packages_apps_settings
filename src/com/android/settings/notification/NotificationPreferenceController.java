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
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import android.util.Log;

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
        if (mChannel != null) {
            return mChannel.getImportance() != IMPORTANCE_NONE;
        }
        if (mChannelGroup != null) {
            return !mChannelGroup.isBlocked();
        }
        return true;
    }

    // finds the preference recursively and removes it from its parent
    private void findAndRemovePreference(PreferenceGroup prefGroup, String key) {
        final int preferenceCount = prefGroup.getPreferenceCount();
        for (int i = preferenceCount - 1; i >= 0; i--) {
            final Preference preference = prefGroup.getPreference(i);
            final String curKey = preference.getKey();

            if (curKey != null && curKey.equals(key)) {
                mPreference = preference;
                prefGroup.removePreference(preference);
            }

            if (preference instanceof PreferenceGroup) {
                findAndRemovePreference((PreferenceGroup) preference, key);
            }
        }
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

    protected boolean isChannelConfigurable() {
        if (mChannel != null && mAppRow != null) {
            return !Objects.equals(mChannel.getId(), mAppRow.lockedChannelId);
        }
        return false;
    }

    protected boolean isChannelBlockable() {
        if (mChannel != null && mAppRow != null) {
            if (!mAppRow.systemApp) {
                return true;
            }

            return mChannel.isBlockableSystem()
                    || mChannel.getImportance() == IMPORTANCE_NONE;
        }
        return false;
    }

    protected boolean isChannelGroupBlockable() {
        if (mChannelGroup != null && mAppRow != null) {
            if (!mAppRow.systemApp) {
                return true;
            }

            return mChannelGroup.isBlocked();
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
