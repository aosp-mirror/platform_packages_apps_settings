/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.dashboard.suggestions;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.DashboardAdapter.DashboardItemHolder;
import com.android.settings.dashboard.DashboardAdapter.IconCache;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.Tile;

import java.util.List;
import java.util.Objects;

public class SuggestionAdapter extends RecyclerView.Adapter<DashboardItemHolder> {
    public static final String TAG = "SuggestionAdapter";

    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final SuggestionFeatureProvider mSuggestionFeatureProvider;
    private List<Tile> mSuggestions;
    private final IconCache mCache;
    private final List<String> mSuggestionsShownLogged;

    public SuggestionAdapter(Context context, List<Tile> suggestions,
            List<String> suggestionsShownLogged) {
        mContext = context;
        mSuggestions = suggestions;
        mSuggestionsShownLogged = suggestionsShownLogged;
        mCache = new IconCache(context);
        final FeatureFactory factory = FeatureFactory.getFactory(context);
        mMetricsFeatureProvider = factory.getMetricsFeatureProvider();
        mSuggestionFeatureProvider = factory.getSuggestionFeatureProvider(context);

        setHasStableIds(true);
    }

    @Override
    public DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DashboardItemHolder(LayoutInflater.from(parent.getContext()).inflate(
                viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(DashboardItemHolder holder, int position) {
        final Tile suggestion = (Tile) mSuggestions.get(position);
        final String suggestionId = mSuggestionFeatureProvider.getSuggestionIdentifier(
                mContext, suggestion);
        // This is for cases when a suggestion is dismissed and the next one comes to view
        if (!mSuggestionsShownLogged.contains(suggestionId)) {
            mMetricsFeatureProvider.action(
                    mContext, MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION, suggestionId,
                    getSuggestionTaggedData());
            mSuggestionsShownLogged.add(suggestionId);
        }
        if (suggestion.remoteViews != null) {
            final ViewGroup itemView = (ViewGroup) holder.itemView;
            itemView.removeAllViews();
            itemView.addView(suggestion.remoteViews.apply(itemView.getContext(), itemView));
        } else {
            holder.icon.setImageDrawable(mCache.getIcon(suggestion.icon));
            holder.title.setText(suggestion.title);
            if (!TextUtils.isEmpty(suggestion.summary)) {
                holder.summary.setText(suggestion.summary);
                holder.summary.setVisibility(View.VISIBLE);
            } else {
                holder.summary.setVisibility(View.GONE);
            }
        }
        final View divider = holder.itemView.findViewById(R.id.divider);
        if (divider != null) {
            divider.setVisibility(position < mSuggestions.size() - 1 ? View.VISIBLE : View.GONE);
        }
        View clickHandler = holder.itemView;
        // If a view with @android:id/primary is defined, use that as the click handler
        // instead.
        final View primaryAction = holder.itemView.findViewById(android.R.id.primary);
        if (primaryAction != null) {
            clickHandler = primaryAction;
        }

        clickHandler.setOnClickListener(v -> {
            mMetricsFeatureProvider.action(mContext,
                    MetricsEvent.ACTION_SETTINGS_SUGGESTION, suggestionId,
                    getSuggestionTaggedData());
            ((SettingsActivity) mContext).startSuggestion(suggestion.intent);
        });
    }

    @Override
    public long getItemId(int position) {
        return Objects.hash(mSuggestions.get(position).title);
    }

    @Override
    public int getItemViewType(int position) {
        Tile suggestion = getSuggestion(position);
        return suggestion.remoteViews != null
                ? R.layout.suggestion_tile_remote_container
                : R.layout.suggestion_tile;
    }

    @Override
    public int getItemCount() {
        return mSuggestions.size();
    }

    public Tile getSuggestion(int position) {
        final long itemId = getItemId(position);
        for (Tile tile : mSuggestions) {
            if (Objects.hash(tile.title) == itemId) {
                return tile;
            }
        }
        return null;
    }

    public void removeSuggestion(Tile suggestion) {
        mSuggestions.remove(suggestion);
        notifyDataSetChanged();
    }

    private Pair<Integer, Object>[] getSuggestionTaggedData() {
        return SuggestionLogHelper.getSuggestionTaggedData(
                mSuggestionFeatureProvider.isSmartSuggestionEnabled(mContext));
    }

}
