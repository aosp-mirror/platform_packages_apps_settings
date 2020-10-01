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

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;

public class DeletedChannelsPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin {

    private static final String  KEY_DELETED = "deleted";

    public DeletedChannelsPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DELETED;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        // only visible on app screen
        if (mChannel != null || hasValidGroup()) {
            return false;
        }

        return mBackend.getDeletedChannelCount(mAppRow.pkg, mAppRow.uid) > 0;
    }

    public void updateState(Preference preference) {
        if (mAppRow != null) {
            int deletedChannelCount = mBackend.getDeletedChannelCount(mAppRow.pkg, mAppRow.uid);
            preference.setTitle(mContext.getResources().getQuantityString(
                    R.plurals.deleted_channels, deletedChannelCount, deletedChannelCount));
        }
        preference.setSelectable(false);
    }
}
