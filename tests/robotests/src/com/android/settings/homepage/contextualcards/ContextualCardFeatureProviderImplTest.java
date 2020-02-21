/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.homepage.contextualcards;

import static android.content.Context.MODE_PRIVATE;

import static com.android.settings.homepage.contextualcards.slices.ContextualNotificationChannelSlice.PREFS;
import static com.android.settings.homepage.contextualcards.slices.ContextualNotificationChannelSlice.PREF_KEY_INTERACTED_PACKAGES;
import static com.android.settings.slices.CustomSliceRegistry.CONTEXTUAL_NOTIFICATION_CHANNEL_SLICE_URI;
import static com.android.settings.slices.CustomSliceRegistry.FLASHLIGHT_SLICE_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.annotation.Nullable;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.ArraySet;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.intelligence.ContextualCardProto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardFeatureProviderImplTest {

    private Context mContext;
    private ContextualCardFeatureProviderImpl mImpl;
    private SharedPreferences mSharedPreferences;
    private CardDatabaseHelper mCardDatabaseHelper;
    private SQLiteDatabase mDatabase;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mImpl = new ContextualCardFeatureProviderImpl(mContext);
        mSharedPreferences = mContext.getSharedPreferences(PREFS, MODE_PRIVATE);
        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mCardDatabaseHelper = CardDatabaseHelper.getInstance(mContext);
        mDatabase = mCardDatabaseHelper.getWritableDatabase();
    }

    @After
    public void tearDown() {
        CardDatabaseHelper.getInstance(mContext).close();
        CardDatabaseHelper.sCardDatabaseHelper = null;
        removeInteractedPackageFromSharedPreference();
    }

    @Test
    public void getContextualCards_shouldSortByScore() {
        insertFakeCard(mDatabase, "card1", 1, "uri1", 1000L);
        insertFakeCard(mDatabase, "card2", 0, "uri2", 1000L);
        insertFakeCard(mDatabase, "card3", 10, "uri3", 1000L);
        // Should sort as 3,1,2
        try (Cursor cursor = mImpl.getContextualCards()) {
            assertThat(cursor.getCount()).isEqualTo(3);
            final List<ContextualCard> cards = new ArrayList<>();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                cards.add(new ContextualCard(cursor));
            }
            assertThat(cards.get(0).getName()).isEqualTo("card3");
            assertThat(cards.get(1).getName()).isEqualTo("card1");
            assertThat(cards.get(2).getName()).isEqualTo("card2");
        }
    }

    @Test
    public void resetDismissedTime_durationExpired_shouldResetToNull() {
        insertFakeCard(mDatabase, "card1", 1, "uri1", 100L);
        final long threshold = 1000L;

        final int rowsUpdated = mImpl.resetDismissedTime(threshold);

        assertThat(rowsUpdated).isEqualTo(1);
    }

    @Test
    public void resetDismissedTime_durationNotExpired_shouldNotUpdate() {
        insertFakeCard(mDatabase, "card1", 1, "uri1", 1111L);
        final long threshold = 1000L;

        final int rowsUpdated = mImpl.resetDismissedTime(threshold);

        assertThat(rowsUpdated).isEqualTo(0);
    }

    @Test
    public void logNotificationPackage_isContextualNotificationChannel_shouldLogPackage() {
        final String packageName = "com.android.test.app";
        final Slice slice = buildSlice(CONTEXTUAL_NOTIFICATION_CHANNEL_SLICE_URI, packageName);

        mImpl.logNotificationPackage(slice);

        final Set<String> interactedPackages = mSharedPreferences.getStringSet(
                PREF_KEY_INTERACTED_PACKAGES, new ArraySet<>());
        assertThat(interactedPackages.contains(packageName)).isTrue();
    }

    @Test
    public void logNotificationPackage_isNotContextualNotificationChannel_shouldNotLogPackage() {
        final String packageName = "com.android.test.app";
        final Slice slice = buildSlice(FLASHLIGHT_SLICE_URI, packageName);

        mImpl.logNotificationPackage(slice);

        final Set<String> interactedPackages = mSharedPreferences.getStringSet(
                PREF_KEY_INTERACTED_PACKAGES, new ArraySet<>());
        assertThat(interactedPackages.contains(packageName)).isFalse();
    }

    private static void insertFakeCard(
            SQLiteDatabase db, String name, double score, String uri, @Nullable Long time) {
        final ContentValues value = new ContentValues();
        value.put(CardDatabaseHelper.CardColumns.NAME, name);
        value.put(CardDatabaseHelper.CardColumns.SCORE, score);
        value.put(CardDatabaseHelper.CardColumns.SLICE_URI, uri);

        value.put(CardDatabaseHelper.CardColumns.TYPE, ContextualCard.CardType.SLICE);
        value.put(CardDatabaseHelper.CardColumns.CATEGORY,
                ContextualCardProto.ContextualCard.Category.DEFAULT.getNumber());
        value.put(CardDatabaseHelper.CardColumns.PACKAGE_NAME,
                RuntimeEnvironment.application.getPackageName());
        value.put(CardDatabaseHelper.CardColumns.APP_VERSION, 1);
        if (time == null) {
            value.putNull(CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP);
        } else {
            value.put(CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP, time);
        }

        db.insert(CardDatabaseHelper.CARD_TABLE, null, value);
    }

    private Slice buildSlice(Uri sliceUri, String packageName) {
        final Bundle args = new Bundle();
        args.putString(AppInfoBase.ARG_PACKAGE_NAME, packageName);
        final Intent intent = new Intent("action");
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);

        final PendingIntent pendingIntent = spy(
                PendingIntent.getActivity(mContext, 0 /* requestCode */, intent, 0 /* flags */));
        doReturn(intent).when(pendingIntent).getIntent();
        final IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.empty_icon);
        final SliceAction action = SliceAction.createDeeplink(pendingIntent, icon,
                ListBuilder.SMALL_IMAGE, "title");

        return new ListBuilder(mContext, sliceUri, ListBuilder.INFINITY)
                .addRow(new ListBuilder.RowBuilder()
                        .addEndItem(icon, ListBuilder.ICON_IMAGE)
                        .setTitle("title")
                        .setPrimaryAction(action))
                .build();
    }

    private void removeInteractedPackageFromSharedPreference() {
        if (mSharedPreferences.contains(PREF_KEY_INTERACTED_PACKAGES)) {
            mSharedPreferences.edit().remove(PREF_KEY_INTERACTED_PACKAGES).apply();
        }
    }
}
