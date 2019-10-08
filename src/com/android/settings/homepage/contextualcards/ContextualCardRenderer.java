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

import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

/**
 * UI renderer for {@link ContextualCard}.
 */
public interface ContextualCardRenderer {

    /**
     * When {@link ContextualCardsAdapter} calls {@link ContextualCardsAdapter#onCreateViewHolder},
     * this method will be called to retrieve the corresponding
     * {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}.
     */
    RecyclerView.ViewHolder createViewHolder(View view, @LayoutRes int viewType);

    /**
     * When {@link ContextualCardsAdapter} calls {@link ContextualCardsAdapter#onBindViewHolder},
     * this method will be called to bind data to the
     * {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}.
     */
    void bindView(RecyclerView.ViewHolder holder, ContextualCard card);
}