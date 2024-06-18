/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.annotation.Nullable;
import android.app.INotificationManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.database.Cursor;
import android.os.ServiceManager;
import android.provider.ContactsContract;
import android.service.notification.ConversationChannelWrapper;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used for Settings-system_server interactions that are not <em>directly</em> related to
 * Mode management, but still used in the UI of its Settings pages (such as listing priority
 * conversations, contacts, etc).
 */
class ZenHelperBackend {

    private static final String TAG = "ZenHelperBackend";

    @Nullable // Until first usage
    private static ZenHelperBackend sInstance;

    private final Context mContext;
    private final INotificationManager mInm;

    static ZenHelperBackend getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ZenHelperBackend(context.getApplicationContext());
        }
        return sInstance;
    }

    ZenHelperBackend(Context context) {
        mContext = context;
        mInm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
    }

    /**
     * Returns all of a user's packages that have at least one channel that will bypass DND
     */
    List<String> getPackagesBypassingDnd(int userId,
            boolean includeConversationChannels) {
        try {
            return mInm.getPackagesBypassingDnd(userId, includeConversationChannels);
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    ParceledListSlice<ConversationChannelWrapper> getConversations(boolean onlyImportant) {
        try {
            return mInm.getConversations(onlyImportant);
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return ParceledListSlice.emptyList();
        }
    }

    List<String> getStarredContacts() {
        try (Cursor cursor = queryStarredContactsData()) {
            return getStarredContacts(cursor);
        }
    }

    @VisibleForTesting
    List<String> getStarredContacts(Cursor cursor) {
        List<String> starredContacts = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String contact = cursor.getString(0);
                starredContacts.add(contact != null ? contact :
                        mContext.getString(R.string.zen_mode_starred_contacts_empty_name));

            } while (cursor.moveToNext());
        }
        return starredContacts;
    }

    private Cursor queryStarredContactsData() {
        return mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                ContactsContract.Data.STARRED + "=1", null,
                ContactsContract.Data.TIMES_CONTACTED);
    }

    Cursor queryAllContactsData() {
        return mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                null, null, null);
    }
}
