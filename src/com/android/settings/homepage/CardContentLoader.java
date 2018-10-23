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

package com.android.settings.homepage;

import static android.app.slice.Slice.HINT_ERROR;

import static androidx.slice.widget.SliceLiveData.SUPPORTED_SPECS;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.slice.Slice;

import com.android.settings.homepage.deviceinfo.DataUsageSlice;
import com.android.settings.homepage.deviceinfo.DeviceInfoSlice;
import com.android.settingslib.utils.AsyncLoaderCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CardContentLoader extends AsyncLoaderCompat<List<ContextualCard>> {
    private static final String TAG = "CardContentLoader";
    static final int CARD_CONTENT_LOADER_ID = 1;

    private Context mContext;

    public interface CardContentLoaderListener {
        void onFinishCardLoading(List<ContextualCard> contextualCards);
    }

    CardContentLoader(Context context) {
        super(context);
        mContext = context.getApplicationContext();
    }

    @Override
    protected void onDiscardResult(List<ContextualCard> result) {

    }

    @NonNull
    @Override
    public List<ContextualCard> loadInBackground() {
        final List<ContextualCard> result = new ArrayList<>();
        try (Cursor cursor = getContextualCardsFromProvider()) {
            if (cursor.getCount() == 0) {
                result.addAll(createStaticCards());
            } else {
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
        return filter(result);
    }

    @VisibleForTesting
    Cursor getContextualCardsFromProvider() {
        return CardDatabaseHelper.getInstance(mContext).getContextualCards();
    }

    @VisibleForTesting
    List<ContextualCard> createStaticCards() {
        final long appVersionCode = getAppVersionCode();
        final String packageName = mContext.getPackageName();
        final double rankingScore = 0.0;
        final List<ContextualCard> result = new ArrayList() {{
            add(new ContextualCard.Builder()
                    .setSliceUri(DataUsageSlice.DATA_USAGE_CARD_URI)
                    .setName(DataUsageSlice.PATH_DATA_USAGE)
                    .setPackageName(packageName)
                    .setRankingScore(rankingScore)
                    .setAppVersion(appVersionCode)
                    .setCardType(ContextualCard.CardType.SLICE)
                    .setIsHalfWidth(false)
                    .build());
            //TODO(b/115971399): Will change following values of SliceUri and Name
            // after landing these slice cards.
//            add(new ContextualCard.Builder()
//                    .setSliceUri("content://com.android.settings.slices/battery_card")
//                    .setName(packageName + "/" + "battery_card")
//                    .setPackageName(packageName)
//                    .setRankingScore(rankingScore)
//                    .setAppVersion(appVersionCode)
//                    .setCardType(ContextualCard.CardType.SLICE)
//                    .setIsHalfWidth(true)
//                    .build());
            add(new ContextualCard.Builder()
                    .setSliceUri(DeviceInfoSlice.DEVICE_INFO_CARD_URI)
                    .setName(DeviceInfoSlice.PATH_DEVICE_INFO)
                    .setPackageName(packageName)
                    .setRankingScore(rankingScore)
                    .setAppVersion(appVersionCode)
                    .setCardType(ContextualCard.CardType.SLICE)
                    .setIsHalfWidth(false)
                    .build());
        }};
        return result;
    }

    @VisibleForTesting
    List<ContextualCard> filter(List<ContextualCard> candidates) {
        return candidates.stream().filter(card -> isCardEligibleToDisplay(card)).collect(
                Collectors.toList());
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

    private long getAppVersionCode() {
        try {
            return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(),
                    0 /* flags */).getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Invalid package name for context", e);
        }
        return -1L;
    }
}
