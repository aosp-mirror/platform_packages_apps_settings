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

package com.android.settings.dashboard;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

public final class SuggestionCarouselAdapter
        extends RecyclerView.Adapter<SuggestionCarouselAdapter.SuggestionCarouselViewHolder> {

    @Override
    public SuggestionCarouselViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.suggestion_carousel_card_view, parent,
                        false /* attachToRoot */);
        return new SuggestionCarouselViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SuggestionCarouselViewHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public final class SuggestionCarouselViewHolder extends RecyclerView.ViewHolder {

        public ImageView mImageView;
        public TextView mTextView;

        public SuggestionCarouselViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.image);
            mTextView = (TextView) itemView.findViewById(R.id.title);
        }
    }
}
