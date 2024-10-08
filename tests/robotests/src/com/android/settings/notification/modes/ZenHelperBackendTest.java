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

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.app.Flags;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.notification.modes.ZenHelperBackend.Contact;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenHelperBackendTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ZenHelperBackend mBackend;
    private HashMap<Integer, FakeContactsProvider> mContactsProviders = new HashMap<>();

    private int mUserId;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
        mBackend = new ZenHelperBackend(mContext);

        mUserId = mContext.getUserId();
        addContactsProvider(mUserId);
    }

    private int addMainUserProfile() {
        UserInfo workProfile = new UserInfo(mUserId + 10, "Work Profile", 0);
        workProfile.userType = UserManager.USER_TYPE_PROFILE_MANAGED;
        UserManager userManager = mContext.getSystemService(UserManager.class);
        shadowOf(userManager).addProfile(mUserId, workProfile.id, workProfile);

        addContactsProvider(workProfile.id);

        return workProfile.id;
    }

    private void addContactsProvider(int userId) {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = String.format("%s@%s", userId, ContactsContract.AUTHORITY);
        mContactsProviders.put(userId, Robolectric.buildContentProvider(FakeContactsProvider.class)
                .create(providerInfo).get());
    }

    private void addContact(int userId, String name, boolean starred) {
        mContactsProviders.get(userId).addContact(name, starred);
    }

    @Test
    public void getAllContacts_singleProfile() {
        addContact(mUserId, "Huey", false);
        addContact(mUserId, "Dewey", true);
        addContact(mUserId, "Louie", false);

        ImmutableList<Contact> allContacts = mBackend.getAllContacts();

        assertThat(allContacts).containsExactly(
                new Contact(UserHandle.of(mUserId), 1, "Huey", null),
                new Contact(UserHandle.of(mUserId), 2, "Dewey", null),
                new Contact(UserHandle.of(mUserId), 3, "Louie", null));
    }

    @Test
    public void getAllContacts_multipleProfiles() {
        int profileId = addMainUserProfile();
        addContact(mUserId, "Huey", false);
        addContact(mUserId, "Dewey", true);
        addContact(mUserId, "Louie", false);
        addContact(profileId, "Fry", false);
        addContact(profileId, "Bender", true);

        ImmutableList<Contact> allContacts = mBackend.getAllContacts();

        assertThat(allContacts).containsExactly(
                new Contact(UserHandle.of(mUserId), 1, "Huey", null),
                new Contact(UserHandle.of(mUserId), 2, "Dewey", null),
                new Contact(UserHandle.of(mUserId), 3, "Louie", null),
                new Contact(UserHandle.of(profileId), 1, "Fry", null),
                new Contact(UserHandle.of(profileId), 2, "Bender", null));
    }

    @Test
    public void getStarredContacts_singleProfile() {
        addContact(mUserId, "Huey", false);
        addContact(mUserId, "Dewey", true);
        addContact(mUserId, "Louie", false);

        ImmutableList<Contact> allContacts = mBackend.getStarredContacts();

        assertThat(allContacts).containsExactly(
                new Contact(UserHandle.of(mUserId), 2, "Dewey", null));
    }

    @Test
    public void getStarredContacts_multipleProfiles() {
        int profileId = addMainUserProfile();
        addContact(mUserId, "Huey", false);
        addContact(mUserId, "Dewey", true);
        addContact(mUserId, "Louie", false);
        addContact(profileId, "Fry", false);
        addContact(profileId, "Bender", true);

        ImmutableList<Contact> allContacts = mBackend.getStarredContacts();

        assertThat(allContacts).containsExactly(
                new Contact(UserHandle.of(mUserId), 2, "Dewey", null),
                new Contact(UserHandle.of(profileId), 2, "Bender", null));
    }

    @Test
    public void getAllContactsCount_singleProfile() {
        addContact(mUserId, "Huey", false);
        addContact(mUserId, "Dewey", true);
        addContact(mUserId, "Louie", false);

        assertThat(mBackend.getAllContactsCount()).isEqualTo(3);
    }

    @Test
    public void getAllContactsCount_multipleProfiles() {
        int profileId = addMainUserProfile();
        addContact(mUserId, "Huey", false);
        addContact(mUserId, "Dewey", true);
        addContact(mUserId, "Louie", false);
        addContact(profileId, "Fry", false);
        addContact(profileId, "Bender", true);

        assertThat(mBackend.getAllContactsCount()).isEqualTo(5);
    }

    private static class FakeContactsProvider extends ContentProvider {

        private record ContactRow(int id, String name, boolean starred) {}

        private final ArrayList<ContactRow> mContacts = new ArrayList<>();

        FakeContactsProvider() {
        }

        @Override
        public boolean onCreate() {
            return true;
        }

        public int addContact(String name, boolean starred) {
            mContacts.add(new ContactRow(mContacts.size() + 1, name, starred));
            return mContacts.size();
        }

        @Nullable
        @Override
        public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                @Nullable String selection, @Nullable String[] selectionArgs,
                @Nullable String sortOrder) {
            Uri baseUri = ContentProvider.getUriWithoutUserId(uri);
            if (!ContactsContract.Contacts.CONTENT_URI.equals(baseUri)) {
                throw new IllegalArgumentException("Unsupported uri for fake: " + uri);
            }

            if (projection == null || !Iterables.elementsEqual(ImmutableList.copyOf(projection),
                    ImmutableList.of(ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI))) {
                throw new IllegalArgumentException(
                        "Unsupported projection for fake: " + Arrays.toString(projection));
            }

            if (selection != null && !selection.equals(ContactsContract.Data.STARRED + "=1")) {
                throw new IllegalArgumentException("Unsupported selection for fake: " + selection);
            }
            boolean selectingStarred = selection != null; // Checked as only valid selection above


            MatrixCursor cursor = new MatrixCursor(projection);
            for (ContactRow contactRow : mContacts) {
                if (!selectingStarred || contactRow.starred) {
                    cursor.addRow(ImmutableList.of(contactRow.id, contactRow.name, Uri.EMPTY));
                }
            }

            return cursor;
        }

        @Override
        @Nullable
        public String getType(@NonNull Uri uri) {
            return "";
        }

        @Nullable
        @Override
        public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(@NonNull Uri uri, @Nullable String selection,
                @Nullable String[] selectionArgs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(@NonNull Uri uri, @Nullable ContentValues values,
                @Nullable String selection, @Nullable String[] selectionArgs) {
            throw new UnsupportedOperationException();
        }
    }
}
