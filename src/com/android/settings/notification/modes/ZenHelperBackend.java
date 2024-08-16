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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ServiceManager;
import android.provider.ContactsContract;
import android.service.notification.ConversationChannelWrapper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.InputStream;
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

    ImmutableList<ConversationChannelWrapper> getAllConversations() {
        return getConversations(false);
    }

    ImmutableList<ConversationChannelWrapper> getImportantConversations() {
        return getConversations(true);
    }

    @SuppressWarnings("unchecked")
    private ImmutableList<ConversationChannelWrapper> getConversations(boolean onlyImportant) {
        try {
            ImmutableList.Builder<ConversationChannelWrapper> list = new ImmutableList.Builder<>();
            ParceledListSlice<ConversationChannelWrapper> parceledList = mInm.getConversations(
                    onlyImportant);
            if (parceledList != null) {
                for (ConversationChannelWrapper conversation : parceledList.getList()) {
                    if (!conversation.getNotificationChannel().isDemoted()) {
                        list.add(conversation);
                    }
                }
            }
            return list.build();
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
            return ImmutableList.of();
        }
    }

    record Contact(long id, @Nullable String displayName, @Nullable Uri photoUri) { }

    ImmutableList<Contact> getAllContacts() {
        try (Cursor cursor = queryAllContactsData()) {
            return getContactsFromCursor(cursor);
        }
    }

    ImmutableList<Contact> getStarredContacts() {
        try (Cursor cursor = queryStarredContactsData()) {
            return getContactsFromCursor(cursor);
        }
    }

    private ImmutableList<Contact> getContactsFromCursor(Cursor cursor) {
        ImmutableList.Builder<Contact> list = new ImmutableList.Builder<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                String name = Strings.emptyToNull(cursor.getString(1));
                String photoUriStr = cursor.getString(2);
                Uri photoUri = !Strings.isNullOrEmpty(photoUriStr) ? Uri.parse(photoUriStr) : null;
                list.add(new Contact(id, name, photoUri));
            } while (cursor.moveToNext());
        }
        return list.build();
    }

    int getAllContactsCount() {
        try (Cursor cursor = queryAllContactsData()) {
            return cursor != null ? cursor.getCount() : 0;
        }
    }

    private static final String[] CONTACTS_PROJECTION = new String[] {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
    };

    private Cursor queryStarredContactsData() {
        return mContext.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                CONTACTS_PROJECTION,
                /* selection= */ ContactsContract.Data.STARRED + "=1", /* selectionArgs= */ null,
                /* sortOrder= */ ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
    }

    private Cursor queryAllContactsData() {
        return mContext.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                CONTACTS_PROJECTION,
                /* selection= */ null, /* selectionArgs= */ null,
                /* sortOrder= */ ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
    }

    @NonNull
    Drawable getContactPhoto(Contact contact) {
        if (contact.photoUri != null) {
            try (InputStream is = mContext.getContentResolver().openInputStream(contact.photoUri)) {
                if (is != null) {
                    RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(
                            mContext.getResources(), is);
                    if (rbd != null && rbd.getBitmap() != null) {
                        rbd.setCircular(true);
                        return rbd;
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "Couldn't load photo for " + contact, e);
            }
        }

        // Fall back to a monogram if no picture.
        return IconUtil.makeContactMonogram(mContext, contact.displayName);
    }
}
