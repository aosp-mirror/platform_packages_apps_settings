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
 *
 */

package com.android.settings.homepage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowThreadUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowThreadUtils.class)
public class CardContentProviderTest {

    private Context mContext;
    private CardContentProvider mProvider;
    private ContentResolver mResolver;
    private Uri mUri;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mProvider = spy(Robolectric.setupContentProvider(CardContentProvider.class));
        mResolver = mContext.getContentResolver();
        mUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(CardContentProvider.CARD_AUTHORITY)
                .path(CardDatabaseHelper.CARD_TABLE)
                .build();
    }

    @After
    public void cleanUp() {
        ShadowThreadUtils.reset();
        CardDatabaseHelper.getInstance(mContext).close();
        CardDatabaseHelper.sCardDatabaseHelper = null;
    }

    @Test
    public void cardData_insert() {
        final int cnt_before_instert = getRowCount();
        mResolver.insert(mUri, insertOneRow());
        final int cnt_after_instert = getRowCount();

        assertThat(cnt_after_instert - cnt_before_instert).isEqualTo(1);
    }

    @Test
    public void cardData_query() {
        mResolver.insert(mUri, insertOneRow());
        final int count = getRowCount();

        assertThat(count).isGreaterThan(0);
    }

    @Test
    public void cardData_delete() {
        mResolver.insert(mUri, insertOneRow());
        final int del_count = mResolver.delete(mUri, null, null);

        assertThat(del_count).isGreaterThan(0);
    }

    @Test
    public void cardData_update() {
        mResolver.insert(mUri, insertOneRow());

        final double updatingScore = 0.87;
        final ContentValues values = new ContentValues();
        values.put(CardDatabaseHelper.CardColumns.SCORE, updatingScore);
        final String strWhere = CardDatabaseHelper.CardColumns.NAME + "=?";
        final String[] selectionArgs = {"auto_rotate"};
        final int update_count = mResolver.update(mUri, values, strWhere, selectionArgs);

        assertThat(update_count).isGreaterThan(0);

        final String[] columns = {CardDatabaseHelper.CardColumns.SCORE};
        final Cursor cr = mResolver.query(mUri, columns, strWhere, selectionArgs, null);
        cr.moveToFirst();
        final double qryScore = cr.getDouble(0);

        cr.close();
        assertThat(qryScore).isEqualTo(updatingScore);
    }

    @Test
    public void insert_isMainThread_shouldEnableStrictMode() {
        ShadowThreadUtils.setIsMainThread(true);
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);

        mProvider.insert(mUri, insertOneRow());

        verify(mProvider).enableStrictMode();
    }

    @Test
    public void query_isMainThread_shouldEnableStrictMode() {
        ShadowThreadUtils.setIsMainThread(true);
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);

        mProvider.query(mUri, null, null, null);

        verify(mProvider).enableStrictMode();
    }

    @Test
    public void delete_isMainThread_shouldEnableStrictMode() {
        ShadowThreadUtils.setIsMainThread(true);
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);

        mProvider.delete(mUri, null, null);

        verify(mProvider).enableStrictMode();
    }

    @Test
    public void update_isMainThread_shouldEnableStrictMode() {
        ShadowThreadUtils.setIsMainThread(true);
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);
        final ContentValues values = new ContentValues();
        values.put(CardDatabaseHelper.CardColumns.SCORE, "0.01");

        mProvider.update(mUri, values, null, null);

        verify(mProvider).enableStrictMode();
    }

    @Test
    public void insert_notMainThread_shouldNotEnableStrictMode() {
        ShadowThreadUtils.setIsMainThread(false);
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);

        mProvider.insert(mUri, insertOneRow());

        verify(mProvider, never()).enableStrictMode();
    }

    @Test
    public void query_notMainThread_shouldNotEnableStrictMode() {
        ShadowThreadUtils.setIsMainThread(false);
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);

        mProvider.query(mUri, null, null, null);

        verify(mProvider, never()).enableStrictMode();
    }

    @Test
    public void delete_notMainThread_shouldNotEnableStrictMode() {
        ShadowThreadUtils.setIsMainThread(false);
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);

        mProvider.delete(mUri, null, null);

        verify(mProvider, never()).enableStrictMode();
    }

    @Test
    public void update_notMainThread_shouldNotEnableStrictMode() {
        ShadowThreadUtils.setIsMainThread(false);
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);
        final ContentValues values = new ContentValues();
        values.put(CardDatabaseHelper.CardColumns.SCORE, "0.01");

        mProvider.update(mUri, values, null, null);

        verify(mProvider, never()).enableStrictMode();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getType_shouldCrash() {
        mProvider.getType(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalid_Uri_shouldCrash() {
        final Uri invalid_Uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(CardContentProvider.CARD_AUTHORITY)
                .path("Invalid_table")
                .build();

        mProvider.getTableFromMatch(invalid_Uri);
    }

    private ContentValues insertOneRow() {
        final ContentValues values = new ContentValues();
        values.put(CardDatabaseHelper.CardColumns.NAME, "auto_rotate");
        values.put(CardDatabaseHelper.CardColumns.TYPE, 0);
        values.put(CardDatabaseHelper.CardColumns.SCORE, 0.9);
        values.put(CardDatabaseHelper.CardColumns.SLICE_URI,
                "content://com.android.settings.slices/action/auto_rotate");
        values.put(CardDatabaseHelper.CardColumns.CATEGORY, 2);
        values.put(CardDatabaseHelper.CardColumns.PACKAGE_NAME, "com.android.settings");
        values.put(CardDatabaseHelper.CardColumns.APP_VERSION, "1.0.0");

        return values;
    }

    private int getRowCount() {
        final Cursor cr = mResolver.query(mUri, null, null, null);
        final int count = cr.getCount();
        cr.close();
        return count;
    }
}