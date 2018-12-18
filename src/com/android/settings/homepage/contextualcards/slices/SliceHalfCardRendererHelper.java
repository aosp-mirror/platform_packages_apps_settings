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
 * limitations under the License.
 */

package com.android.settings.homepage.contextualcards.slices;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;

import com.android.settings.homepage.contextualcards.ContextualCard;

/**
 * Card renderer helper for {@link ContextualCard} built as slice half card.
 */
class SliceHalfCardRendererHelper {
    private static final String TAG = "SliceHCRendererHelper";

    private final Context mContext;

    SliceHalfCardRendererHelper(Context context) {
        mContext = context;
    }

    RecyclerView.ViewHolder createViewHolder(View view) {
        return null;
    }

    void bindView(RecyclerView.ViewHolder holder, ContextualCard card, Slice slice) {

    }
}
