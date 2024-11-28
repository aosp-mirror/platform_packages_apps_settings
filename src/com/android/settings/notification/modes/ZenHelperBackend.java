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
import android.app.ZenBypassingApp;
import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.service.notification.ConversationChannelWrapper;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
    private final UserManager mUserManager;

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
        mUserManager = context.getSystemService(UserManager.class);
    }

    /**
     * Returns a mapping between a user's packages that have at least one channel that will
     * bypass DND, and a Boolean indicating whether all of the package's channels bypass.
     */
    Map<String, Boolean> getPackagesBypassingDnd(int userId) {
        Map<String, Boolean> bypassingAppsMap = new HashMap<>();
        try {
            List<ZenBypassingApp> bypassingApps = mInm.getPackagesBypassingDnd(userId).getList();
            for (ZenBypassingApp zba : bypassingApps) {
                bypassingAppsMap.put(zba.getPkg(), zba.doAllChannelsBypass());
            }
        } catch (Exception e) {
            Log.w(TAG, "Error calling NoMan", e);
        }
        return bypassingAppsMap;
    }

    /** Returns all conversation channels for profiles of the current user. */
    ImmutableList<ConversationChannelWrapper> getAllConversations() {
        return getConversations(false);
    }

    /** Returns all important (priority) conversation channels for profiles of the current user. */
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
                    if (conversation.getShortcutInfo() != null
                            && conversation.getNotificationChannel() != null
                            && !conversation.getNotificationChannel().isDemoted()) {
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

    record Contact(UserHandle user, long contactId, @Nullable String displayName,
                   @Nullable Uri photoUri) { }

    /** Returns all contacts for profiles of the current user. */
    ImmutableList<Contact> getAllContacts() {
        return getContactsForUserProfiles(this::queryAllContactsData);
    }

    /** Returns all starred contacts for profiles of the current user. */
    ImmutableList<Contact> getStarredContacts() {
        return getContactsForUserProfiles(this::queryStarredContactsData);
    }

    private ImmutableList<Contact> getContactsForUserProfiles(
            Function<UserHandle, Cursor> userQuery) {
        ImmutableList.Builder<Contact> contacts = new ImmutableList.Builder<>();
        for (UserHandle user : mUserManager.getAllProfiles()) {
            try (Cursor cursor = userQuery.apply(user)) {
                loadContactsFromCursor(user, cursor, contacts);
            }
        }
        return contacts.build();
    }

    private void loadContactsFromCursor(UserHandle user, Cursor cursor,
            ImmutableList.Builder<Contact> contactsListBuilder) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(0);
                String name = Strings.emptyToNull(cursor.getString(1));
                String photoUriStr = cursor.getString(2);
                Uri photoUri = !Strings.isNullOrEmpty(photoUriStr) ? Uri.parse(photoUriStr) : null;
                contactsListBuilder.add(new Contact(user, id, name,
                        ContentProvider.maybeAddUserId(photoUri, user.getIdentifier())));
            } while (cursor.moveToNext());
        }
    }

    int getAllContactsCount() {
        int count = 0;
        for (UserHandle user : mUserManager.getEnabledProfiles()) {
            try (Cursor cursor = queryAllContactsData(user)) {
                count += (cursor != null ? cursor.getCount() : 0);
            }
        }
        return count;
    }

    private static final String[] CONTACTS_PROJECTION = new String[] {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
    };

    private Cursor queryStarredContactsData(UserHandle user) {
        return mContext.getContentResolver().query(
                ContentProvider.maybeAddUserId(Contacts.CONTENT_URI, user.getIdentifier()),
                CONTACTS_PROJECTION,
                /* selection= */ ContactsContract.Data.STARRED + "=1", /* selectionArgs= */ null,
                /* sortOrder= */ ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
    }

    private Cursor queryAllContactsData(UserHandle user) {
        return mContext.getContentResolver().query(
                ContentProvider.maybeAddUserId(Contacts.CONTENT_URI, user.getIdentifier()),
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
