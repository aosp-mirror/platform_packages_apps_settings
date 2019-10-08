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

package com.android.settings.homepage.contextualcards.legacysuggestion;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardController;
import com.android.settings.homepage.contextualcards.ContextualCardRenderer;
import com.android.settings.homepage.contextualcards.ControllerRendererPool;

public class LegacySuggestionContextualCardRenderer implements ContextualCardRenderer {

    @LayoutRes
    public static final int VIEW_TYPE = R.layout.legacy_suggestion_tile;

    private final Context mContext;
    private final ControllerRendererPool mControllerRendererPool;

    public LegacySuggestionContextualCardRenderer(Context context,
            ControllerRendererPool controllerRendererPool) {
        mContext = context;
        mControllerRendererPool = controllerRendererPool;
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View view, @LayoutRes int viewType) {
        return new LegacySuggestionViewHolder(view);
    }

    @Override
    public void bindView(RecyclerView.ViewHolder holder, ContextualCard card) {
        final LegacySuggestionViewHolder vh = (LegacySuggestionViewHolder) holder;
        final ContextualCardController controller = mControllerRendererPool
                .getController(mContext, card.getCardType());
        vh.icon.setImageDrawable(card.getIconDrawable());
        vh.title.setText(card.getTitleText());
        vh.summary.setText(card.getSummaryText());
        vh.itemView.setOnClickListener(v -> controller.onPrimaryClick(card));
        vh.closeButton.setOnClickListener(v -> controller.onDismissed(card));
    }

    private static class LegacySuggestionViewHolder extends RecyclerView.ViewHolder {

        public final ImageView icon;
        public final TextView title;
        public final TextView summary;
        public final View closeButton;

        public LegacySuggestionViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(android.R.id.icon);
            title = itemView.findViewById(android.R.id.title);
            summary = itemView.findViewById(android.R.id.summary);
            closeButton = itemView.findViewById(R.id.close_button);
        }
    }
}

