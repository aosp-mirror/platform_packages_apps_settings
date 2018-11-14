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

package com.android.settings.homepage.contextualcards.conditional;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardRenderer;
import com.android.settings.homepage.contextualcards.ControllerRendererPool;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Card renderer for {@link ConditionalContextualCard}.
 */
public class ConditionContextualCardRenderer implements ContextualCardRenderer {
    @LayoutRes
    public static final int HALF_WIDTH_VIEW_TYPE = R.layout.homepage_condition_half_tile;
    @LayoutRes
    public static final int FULL_WIDTH_VIEW_TYPE = R.layout.homepage_condition_full_tile;

    private final Context mContext;
    private final ControllerRendererPool mControllerRendererPool;

    public ConditionContextualCardRenderer(Context context,
            ControllerRendererPool controllerRendererPool) {
        mContext = context;
        mControllerRendererPool = controllerRendererPool;
    }

    @Override
    public int getViewType(boolean isHalfWidth) {
        if (isHalfWidth) {
            return HALF_WIDTH_VIEW_TYPE;
        } else {
            return FULL_WIDTH_VIEW_TYPE;
        }
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View view) {
        return new ConditionalCardHolder(view);
    }

    @Override
    public void bindView(RecyclerView.ViewHolder holder, ContextualCard contextualCard) {
        final ConditionalCardHolder view = (ConditionalCardHolder) holder;
        final ConditionalContextualCard card = (ConditionalContextualCard) contextualCard;
        final MetricsFeatureProvider metricsFeatureProvider = FeatureFactory.getFactory(
                mContext).getMetricsFeatureProvider();

        metricsFeatureProvider.visible(mContext, MetricsProto.MetricsEvent.SETTINGS_HOMEPAGE,
                card.getMetricsConstant());
        initializePrimaryClick(view, card, metricsFeatureProvider);
        initializeView(view, card);
        initializeActionButton(view, card, metricsFeatureProvider);
    }

    private void initializePrimaryClick(ConditionalCardHolder view, ConditionalContextualCard card,
            MetricsFeatureProvider metricsFeatureProvider) {
        view.itemView.findViewById(R.id.content).setOnClickListener(
                v -> {
                    metricsFeatureProvider.action(mContext,
                            MetricsProto.MetricsEvent.ACTION_SETTINGS_CONDITION_CLICK,
                            card.getMetricsConstant());
                    mControllerRendererPool.getController(mContext,
                            card.getCardType()).onPrimaryClick(card);
                });
    }

    private void initializeView(ConditionalCardHolder view, ConditionalContextualCard card) {
        view.icon.setImageDrawable(card.getIconDrawable());
        view.title.setText(card.getTitleText());
        view.summary.setText(card.getSummaryText());
    }

    private void initializeActionButton(ConditionalCardHolder view, ConditionalContextualCard card,
            MetricsFeatureProvider metricsFeatureProvider) {
        final CharSequence action = card.getActionText();
        final boolean hasButtons = !TextUtils.isEmpty(action);

        final Button button = view.itemView.findViewById(R.id.first_action);
        if (hasButtons) {
            button.setVisibility(View.VISIBLE);
            button.setText(action);
            button.setOnClickListener(v -> {
                final Context viewContext = v.getContext();
                metricsFeatureProvider.action(
                        viewContext, MetricsProto.MetricsEvent.ACTION_SETTINGS_CONDITION_BUTTON,
                        card.getMetricsConstant());
                mControllerRendererPool.getController(mContext, card.getCardType())
                        .onDismissed(card);
            });
        } else {
            button.setVisibility(View.GONE);
        }
    }

    public static class ConditionalCardHolder extends RecyclerView.ViewHolder {

        public final ImageView icon;
        public final TextView title;
        public final TextView summary;

        public ConditionalCardHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(android.R.id.icon);
            title = itemView.findViewById(android.R.id.title);
            summary = itemView.findViewById(android.R.id.summary);
        }
    }
}
