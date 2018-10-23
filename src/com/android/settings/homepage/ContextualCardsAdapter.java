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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextualCardsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ContextualCardUpdateListener {
    static final int SPAN_COUNT = 2;

    private static final String TAG = "ContextualCardsAdapter";
    private static final int HALF_WIDTH = 1;
    private static final int FULL_WIDTH = 2;

    private final Context mContext;
    private final ControllerRendererPool mControllerRendererPool;
    private final List<ContextualCard> mContextualCards;
    private final LifecycleOwner mLifecycleOwner;

    public ContextualCardsAdapter(Context context, LifecycleOwner lifecycleOwner,
            ContextualCardManager manager) {
        mContext = context;
        mContextualCards = new ArrayList<>();
        mControllerRendererPool = manager.getControllerRendererPool();
        mLifecycleOwner = lifecycleOwner;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return mContextualCards.get(position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        return mContextualCards.get(position).getCardType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int cardType) {
        final ContextualCardRenderer renderer = mControllerRendererPool.getRenderer(mContext,
                mLifecycleOwner, cardType);
        final int viewType = renderer.getViewType();
        final View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);

        return renderer.createViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final int cardType = mContextualCards.get(position).getCardType();
        final ContextualCardRenderer renderer = mControllerRendererPool.getRenderer(mContext,
                mLifecycleOwner, cardType);

        renderer.bindView(holder, mContextualCards.get(position));
    }

    @Override
    public int getItemCount() {
        return mContextualCards.size();
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
                    final ContextualCard card = mContextualCards.get(position);
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
    public void onContextualCardUpdated(Map<Integer, List<ContextualCard>> cards) {
        final List<ContextualCard> contextualCards = cards.get(ContextualCard.CardType.DEFAULT);
        if (contextualCards == null) {
            mContextualCards.clear();
            notifyDataSetChanged();
        } else {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new ContextualCardsDiffCallback(mContextualCards, contextualCards));
            mContextualCards.clear();
            mContextualCards.addAll(contextualCards);
            diffResult.dispatchUpdatesTo(this);
        }
    }
}
