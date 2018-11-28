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

package com.android.settings.homepage.contextualcards;

import static android.app.slice.Slice.HINT_ERROR;

import static androidx.slice.widget.SliceLiveData.SUPPORTED_SPECS;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.slice.Slice;

import com.android.settings.homepage.contextualcards.slices.ConnectedDeviceSlice;
import com.android.settings.wifi.WifiSlice;
import com.android.settingslib.utils.AsyncLoaderCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContextualCardLoader extends AsyncLoaderCompat<List<ContextualCard>> {

    @VisibleForTesting
    static final int DEFAULT_CARD_COUNT = 4;
    static final int CARD_CONTENT_LOADER_ID = 1;

    private static final String TAG = "ContextualCardLoader";

    private final ContentObserver mObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            if (isStarted()) {
                forceLoad();
            }
        }
    };

    private Context mContext;

    ContextualCardLoader(Context context) {
        super(context);
        mContext = context.getApplicationContext();
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        mContext.getContentResolver().registerContentObserver(CardContentProvider.URI,
                false /*notifyForDescendants*/, mObserver);
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    protected void onDiscardResult(List<ContextualCard> result) {

    }

    @NonNull
    @Override
    public List<ContextualCard> loadInBackground() {
        final List<ContextualCard> result = new ArrayList<>();
        try (Cursor cursor = getContextualCardsFromProvider()) {
            if (cursor.getCount() > 0) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    final ContextualCard card = new ContextualCard(cursor);
                    if (card.isCustomCard()) {
                        //TODO(b/114688391): Load and generate custom card,then add into list
                    } else {
                        result.add(card);
                    }
                }
            }
        }
        return getFinalDisplayableCards(result);
    }

    @VisibleForTesting
    List<ContextualCard> getFinalDisplayableCards(List<ContextualCard> candidates) {
        List<ContextualCard> eligibleCards = filterEligibleCards(candidates);
        eligibleCards = eligibleCards.stream().limit(DEFAULT_CARD_COUNT).collect(
                Collectors.toList());

        if (eligibleCards.size() <= 2 || getNumberOfLargeCard(eligibleCards) == 0) {
            return eligibleCards;
        }

        if (eligibleCards.size() == DEFAULT_CARD_COUNT) {
            eligibleCards.remove(eligibleCards.size() - 1);
        }

        if (getNumberOfLargeCard(eligibleCards) == 1) {
            return eligibleCards;
        }

        eligibleCards.remove(eligibleCards.size() - 1);

        return eligibleCards;
    }

    @VisibleForTesting
    Cursor getContextualCardsFromProvider() {
        return CardDatabaseHelper.getInstance(mContext).getContextualCards();
    }

    @VisibleForTesting
    List<ContextualCard> filterEligibleCards(List<ContextualCard> candidates) {
        return candidates.stream().filter(card -> isCardEligibleToDisplay(card))
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    boolean isCardEligibleToDisplay(ContextualCard card) {
        if (card.isCustomCard()) {
            return true;
        }

        final Uri uri = card.getSliceUri();

        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            return false;
        }

        //check if the uri has a provider associated with.
        final ContentProviderClient provider =
                mContext.getContentResolver().acquireContentProviderClient(uri);
        if (provider == null) {
            return false;
        }
        //release contentProviderClient to prevent from memory leak.
        provider.release();

        final Slice slice = Slice.bindSlice(mContext, uri, SUPPORTED_SPECS);
        if (slice == null || slice.hasHint(HINT_ERROR)) {
            Log.w(TAG, "Failed to bind slice, not eligible for display " + uri);
            return false;
        }

        return true;
    }

    private int getNumberOfLargeCard(List<ContextualCard> cards) {
        return (int) cards.stream()
                .filter(card -> card.getSliceUri().equals(WifiSlice.WIFI_URI)
                        || card.getSliceUri().equals(ConnectedDeviceSlice.CONNECTED_DEVICE_URI))
                .count();
    }

    public interface CardContentLoaderListener {
        void onFinishCardLoading(List<ContextualCard> contextualCards);
    }
}