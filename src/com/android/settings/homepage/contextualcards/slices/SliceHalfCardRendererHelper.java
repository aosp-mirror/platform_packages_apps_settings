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

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.EventInfo;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.logging.ContextualCardLogUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

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
        return new HalfCardViewHolder(view);
    }

    void bindView(RecyclerView.ViewHolder holder, ContextualCard card, Slice slice) {
        final HalfCardViewHolder view = (HalfCardViewHolder) holder;
        final SliceMetadata sliceMetadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = sliceMetadata.getPrimaryAction();
        view.icon.setImageDrawable(primaryAction.getIcon().loadDrawable(mContext));
        view.title.setText(primaryAction.getTitle());
        view.content.setOnClickListener(v -> {
            try {
                primaryAction.getAction().send();
            } catch (PendingIntent.CanceledException e) {
                Log.w(TAG, "Failed to start intent " + primaryAction.getTitle());
            }
            final String log = ContextualCardLogUtils.buildCardClickLog(card, 0 /* row */,
                    EventInfo.ACTION_TYPE_CONTENT, view.getAdapterPosition());

            final MetricsFeatureProvider metricsFeatureProvider =
                    FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();

            metricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_CONTEXTUAL_CARD_CLICK, log);
        });
    }

    static class HalfCardViewHolder extends RecyclerView.ViewHolder {
        public final LinearLayout content;
        public final ImageView icon;
        public final TextView title;

        public HalfCardViewHolder(View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.content);
            icon = itemView.findViewById(android.R.id.icon);
            title = itemView.findViewById(android.R.id.title);
        }
    }
}
