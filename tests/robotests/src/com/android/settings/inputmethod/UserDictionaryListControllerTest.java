/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.UserDictionary;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.TreeSet;

@RunWith(RobolectricTestRunner.class)
public class UserDictionaryListControllerTest {

    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private FakeProvider mContentProvider;
    private UserDictionaryListPreferenceController mController;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContentProvider = new FakeProvider();
        ShadowContentResolver.registerProviderInternal(UserDictionary.AUTHORITY, mContentProvider);
        mContext = RuntimeEnvironment.application;
        mController = spy(new UserDictionaryListPreferenceController(mContext, "controller_key"));
        mPreference = new Preference(mContext);
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mContext);
    }

    @Test
    public void userDictionaryList_byDefault_shouldBeShown() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getUserDictionaryLocalesSet_noLocale_shouldReturnEmptySet() {
        mContentProvider.hasDictionary = false;

        assertThat(UserDictionaryListPreferenceController.getUserDictionaryLocalesSet(
                mContext)).isEmpty();
    }

    @Test
    public void displayPreference_isOrderingAsAdd_shouldBeFalse() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.isOrderingAsAdded()).isFalse();
    }

    @Test
    public void createUserDictSettings_emptyLocaleSetWithNewScreen_shouldAddOnePreference() {
        final TreeSet<String> locales = new TreeSet<>();

        doReturn(locales).when(mController).getUserDictLocalesSet(mContext);

        mController.setLocale(null);
        mController.displayPreference(mPreferenceScreen);
        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void createUserDictSettings_emptyLocaleSetWithOldScreen_shouldNotAddNewPreference() {
        final TreeSet<String> locales = new TreeSet<>();

        doReturn(locales).when(mController).getUserDictLocalesSet(mContext);
        mPreferenceScreen.addPreference(mPreference);

        mController.setLocale(null);
        mController.displayPreference(mPreferenceScreen);
        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void createUserDictSettings_threeLocales_shouldAddFourPreference() {
        //There will be 4 preferences : 3 locales + 1 "All languages" entry
        final TreeSet<String> locales = new TreeSet<>();
        locales.add("en");
        locales.add("es");
        locales.add("fr");

        doReturn(locales).when(mController).getUserDictLocalesSet(mContext);

        mController.setLocale("en");
        mController.displayPreference(mPreferenceScreen);
        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(4);
    }

    public static class FakeProvider extends ContentProvider {

        private boolean hasDictionary = true;

        @Override
        public boolean onCreate() {
            return false;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                String sortOrder) {
            if (hasDictionary) {
                final MatrixCursor cursor = new MatrixCursor(
                        new String[]{UserDictionary.Words.LOCALE});
                cursor.addRow(new Object[]{"en"});
                cursor.addRow(new Object[]{"es"});
                return cursor;
            } else {
                return null;
            }
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            return null;
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return 0;
        }
    }
}
