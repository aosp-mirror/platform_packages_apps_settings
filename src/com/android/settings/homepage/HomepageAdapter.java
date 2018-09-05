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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomepageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements
        HomepageCardUpdateListener {
    static final int SPAN_COUNT = 2;

    private static final String TAG = "HomepageAdapter";
    private static final int HALF_WIDTH = 1;
    private static final int FULL_WIDTH = 2;

    private final Context mContext;
    private final ControllerRendererPool mControllerRendererPool;

    private List<HomepageCard> mHomepageCards;

    public HomepageAdapter(Context context, HomepageManager manager) {
        mContext = context;
        mHomepageCards = new ArrayList<>();
        mControllerRendererPool = manager.getControllerRendererPool();
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return mHomepageCards.get(position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        return mHomepageCards.get(position).getCardType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int cardType) {
        final HomepageCardRenderer renderer = mControllerRendererPool.getRenderer(mContext,
                cardType);
        final int viewType = renderer.getViewType();
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);

        return renderer.createViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final int cardType = mHomepageCards.get(position).getCardType();
        final HomepageCardRenderer renderer = mControllerRendererPool.getRenderer(mContext,
                cardType);

        renderer.bindView(holder, mHomepageCards.get(position));
    }

    @Override
    public int getItemCount() {
        return mHomepageCards.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    final HomepageCard card = mHomepageCards.get(position);
                    //TODO(b/114009676): may use another field to make decision. still under review.
                    if (card.isHalfWidth()) {
                        return HALF_WIDTH;
                    }
                    return FULL_WIDTH;
                }
            });
        }
    }

    @Override
    public void onHomepageCardUpdated(int cardType, List<HomepageCard> homepageCards) {
        //TODO(b/112245748): Should implement a DiffCallback so we can use notifyItemChanged()
        // instead.
        if (homepageCards == null) {
            mHomepageCards.clear();
        } else {
            mHomepageCards = homepageCards;
        }
        notifyDataSetChanged();
    }
}
