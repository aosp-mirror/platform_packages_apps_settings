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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_NONE;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Comparator;
import java.util.List;
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
    @Nullable
    protected Drawable mConversationDrawable;
    @Nullable
    protected ShortcutInfo mConversationInfo;
    protected List<String> mPreferenceFilter;

    boolean overrideCanBlock;
    boolean overrideCanConfigure;
    boolean overrideCanBlockValue;
    boolean overrideCanConfigureValue;

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
            if (mPreferenceFilter != null && !isIncludedInFilter()) {
                return false;
            }
            return mChannel.getImportance() != IMPORTANCE_NONE;
        }
        return true;
    }

    protected void onResume(NotificationBackend.AppRow appRow,
            @Nullable NotificationChannel channel, @Nullable NotificationChannelGroup group,
            Drawable conversationDrawable,
            ShortcutInfo conversationInfo,
            RestrictedLockUtils.EnforcedAdmin admin,
            List<String> preferenceFilter) {
        mAppRow = appRow;
        mChannel = channel;
        mChannelGroup = group;
        mAdmin = admin;
        mConversationDrawable = conversationDrawable;
        mConversationInfo = conversationInfo;
        mPreferenceFilter = preferenceFilter;
    }

    abstract boolean isIncludedInFilter();

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
        if (overrideCanBlock) {
            return overrideCanBlockValue;
        }
        if (overrideCanConfigure) {
            return overrideCanConfigureValue;
        }
        if (channel != null && mAppRow != null) {
            boolean locked = mAppRow.lockedImportance;
            if (locked) {
                return channel.isBlockable() || channel.getImportance() == IMPORTANCE_NONE;
            }

            return channel.isBlockable() || !mAppRow.systemApp
                    || channel.getImportance() == IMPORTANCE_NONE;
        }
        return false;
    }

    protected boolean isAppBlockable() {
        if (overrideCanBlock) {
            return overrideCanBlockValue;
        }
        if (overrideCanConfigure) {
            return overrideCanConfigureValue;
        }
        if (mAppRow != null) {
            boolean systemBlockable = !mAppRow.systemApp || (mAppRow.systemApp && mAppRow.banned);
            return systemBlockable && !mAppRow.lockedImportance;
        }
        return true;
    }

    protected boolean isChannelConfigurable(NotificationChannel channel) {
        if (overrideCanConfigure) {
            return overrideCanConfigureValue;
        }
        if (channel != null && mAppRow != null) {
            boolean locked = mAppRow.lockedImportance;
            return !locked || channel.isBlockable();
        }
        return false;
    }

    protected boolean isChannelGroupBlockable() {
        return isChannelGroupBlockable(mChannelGroup);
    }

    protected boolean isChannelGroupBlockable(NotificationChannelGroup group) {
        if (overrideCanBlock) {
            return overrideCanBlockValue;
        }
        if (overrideCanConfigure) {
            return overrideCanConfigureValue;
        }
        if (group != null && mAppRow != null) {
            if (!mAppRow.systemApp && !mAppRow.lockedImportance) {
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

    protected final void setOverrideCanBlock(boolean canBlock) {
        overrideCanBlock = true;
        overrideCanBlockValue = canBlock;
    }

    protected final void setOverrideCanConfigure(boolean canConfigure) {
        overrideCanConfigure = true;
        overrideCanConfigureValue = canConfigure;
    }

    public static final Comparator<NotificationChannelGroup> CHANNEL_GROUP_COMPARATOR =
            new Comparator<NotificationChannelGroup>() {
        @Override
        public int compare(NotificationChannelGroup left, NotificationChannelGroup right) {
            // Non-grouped channels (in placeholder group with a null id) come last
            if (left.getId() == null && right.getId() != null) {
                return 1;
            } else if (right.getId() == null && left.getId() != null) {
                return -1;
            }
            return left.getId().compareTo(right.getId());
        }
    };

    public static final Comparator<NotificationChannel> CHANNEL_COMPARATOR = (left, right) -> {
        if (left.isDeleted() != right.isDeleted()) {
            return Boolean.compare(left.isDeleted(), right.isDeleted());
        } else if (left.getId().equals(NotificationChannel.DEFAULT_CHANNEL_ID)) {
            // Uncategorized/miscellaneous legacy channel goes last
            return 1;
        } else if (right.getId().equals(NotificationChannel.DEFAULT_CHANNEL_ID)) {
            return -1;
        }

        return left.getId().compareTo(right.getId());
    };
}
