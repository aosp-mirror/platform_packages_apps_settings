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

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.Nullable;

import com.android.settingslib.utils.AsyncLoaderCompat;

import java.util.ArrayList;
import java.util.List;

public class CardContentLoader extends AsyncLoaderCompat<List<ContextualCard>> {
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

    @Nullable
    @Override
    public List<ContextualCard> loadInBackground() {
        List<ContextualCard> result;
        try (Cursor cursor = CardDatabaseHelper.getInstance(mContext).getAllContextualCards()) {
            if (cursor.getCount() == 0) {
                //TODO(b/113372471): Load Default static cards and return 3 static cards
                return new ArrayList<>();
            }
            result = buildContextualCardList(cursor);
        }
        return result;
    }

    private List<ContextualCard> buildContextualCardList(Cursor cursor) {
        final List<ContextualCard> result = new ArrayList<>();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            final ContextualCard card = new ContextualCard(cursor);
            if (card.isCustomCard()) {
                //TODO(b/114688391): Load and generate custom card,then add into list
            } else {
                result.add(card);
            }
        }
        return result;
    }
}
