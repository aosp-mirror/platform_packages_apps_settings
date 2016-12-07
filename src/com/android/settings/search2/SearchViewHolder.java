/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.search2;

import android.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * The ViewHolder for the Search RecyclerView.
 * There are multiple search result types in the same Recycler view with different UI requirements.
 * Some examples include Intent results, Inline results, and Help articles.
 */
public abstract class SearchViewHolder extends RecyclerView.ViewHolder {

    public SearchViewHolder(View view) {
        super(view);
    }

    public abstract void onBind(Fragment fragment, SearchResult result);
}