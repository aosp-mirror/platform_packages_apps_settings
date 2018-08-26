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

import androidx.annotation.Nullable;

import com.android.settingslib.utils.AsyncLoaderCompat;

import java.util.List;

//TODO(b/112521307): Implement this to make it work with the card database.
public class CardContentLoader {

    private static final String TAG = "CardContentLoader";

    private CardContentLoaderListener mListener;

    public interface CardContentLoaderListener {
        void onFinishCardLoading(List<HomepageCard> homepageCards);
    }

    public CardContentLoader() {
    }

    void setListener(CardContentLoaderListener listener) {
        mListener = listener;
    }

    private static class CardLoader extends AsyncLoaderCompat<List<HomepageCard>> {

        public CardLoader(Context context) {
            super(context);
        }

        @Override
        protected void onDiscardResult(List<HomepageCard> result) {

        }

        @Nullable
        @Override
        public List<HomepageCard> loadInBackground() {
            return null;
        }
    }
}
