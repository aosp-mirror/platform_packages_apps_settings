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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardRenderer;
import com.android.settings.homepage.contextualcards.ControllerRendererPool;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class ConditionFooterContextualCardRenderer implements ContextualCardRenderer {
    public static final int VIEW_TYPE = R.layout.conditional_card_footer;
    private static final String TAG = "ConditionFooterRenderer";

    private final Context mContext;
    private final ControllerRendererPool mControllerRendererPool;

    public ConditionFooterContextualCardRenderer(Context context,
            ControllerRendererPool controllerRendererPool) {
        mContext = context;
        mControllerRendererPool = controllerRendererPool;
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View view, @LayoutRes int viewType) {
        return new ConditionFooterCardHolder(view);
    }

    @Override
    public void bindView(RecyclerView.ViewHolder holder, ContextualCard card) {
        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        holder.itemView.setOnClickListener(v -> {
            metricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_SETTINGS_CONDITION_EXPAND,
                    SettingsEnums.SETTINGS_HOMEPAGE,
                    null /* key */,
                    0 /* false */);
            final ConditionContextualCardController controller =
                    mControllerRendererPool.getController(mContext,
                            ContextualCard.CardType.CONDITIONAL_FOOTER);
            controller.setIsExpanded(false);
            controller.onConditionsChanged();
        });
    }

    public static class ConditionFooterCardHolder extends RecyclerView.ViewHolder {
        public ConditionFooterCardHolder(View itemView) {
            super(itemView);
        }
    }
}
