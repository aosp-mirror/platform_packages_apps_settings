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

import android.app.PendingIntent;
import android.content.Context;
import android.service.settings.suggestions.Suggestion;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
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
    @Deprecated // in favor of mSuggestionsV2
    private final List<Tile> mSuggestions;
    private final List<Suggestion> mSuggestionsV2;
    private final IconCache mCache;
    private final List<String> mSuggestionsShownLogged;
    private final SuggestionControllerMixin mSuggestionControllerMixin;

    public SuggestionAdapter(Context context, SuggestionControllerMixin suggestionControllerMixin,
            List<Tile> suggestions, List<Suggestion> suggestionsV2,
            List<String> suggestionsShownLogged) {
        mContext = context;
        mSuggestionControllerMixin = suggestionControllerMixin;
        mSuggestions = suggestions;
        mSuggestionsV2 = suggestionsV2;
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
        if (mSuggestions != null) {
            bindSuggestionTile(holder, position);
        } else {
            bindSuggestion(holder, position);
        }
    }

    private void bindSuggestion(DashboardItemHolder holder, int position) {
        final Suggestion suggestion = mSuggestionsV2.get(position);
        final String id = suggestion.getId();
        if (!mSuggestionsShownLogged.contains(id)) {
            mMetricsFeatureProvider.action(
                    mContext, MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION, id);
            mSuggestionsShownLogged.add(id);
        }

        holder.icon.setImageDrawable(mCache.getIcon(suggestion.getIcon()));
        holder.title.setText(suggestion.getTitle());
        final CharSequence summary = suggestion.getSummary();
        if (!TextUtils.isEmpty(summary)) {
            holder.summary.setText(summary);
            holder.summary.setVisibility(View.VISIBLE);
        } else {
            holder.summary.setVisibility(View.GONE);
        }
        final View divider = holder.itemView.findViewById(R.id.divider);
        if (divider != null) {
            divider.setVisibility(position < mSuggestionsV2.size() - 1 ? View.VISIBLE : View.GONE);
        }
        View clickHandler = holder.itemView;
        // If a view with @android:id/primary is defined, use that as the click handler
        // instead.
        final View primaryAction = holder.itemView.findViewById(android.R.id.primary);
        if (primaryAction != null) {
            clickHandler = primaryAction;
        }
        clickHandler.setOnClickListener(v -> {
            mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_SETTINGS_SUGGESTION, id);
            try {
                suggestion.getPendingIntent().send();
                mSuggestionControllerMixin.launchSuggestion(suggestion);
            } catch (PendingIntent.CanceledException e) {
                Log.w(TAG, "Failed to start suggestion " + suggestion.getTitle());
            }
        });
    }

    /**
     * @deprecated in favor {@link #bindSuggestion(DashboardItemHolder, int)}.
     */
    @Deprecated
    private void bindSuggestionTile(DashboardItemHolder holder, int position) {
        final Tile suggestion = (Tile) mSuggestions.get(position);
        final String suggestionId = mSuggestionFeatureProvider.getSuggestionIdentifier(
                mContext, suggestion);
        // This is for cases when a suggestion is dismissed and the next one comes to view
        if (!mSuggestionsShownLogged.contains(suggestionId)) {
            mMetricsFeatureProvider.action(
                    mContext, MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION, suggestionId,
                    mSuggestionFeatureProvider.getLoggingTaggedData(mContext));
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
                    mSuggestionFeatureProvider.getLoggingTaggedData(mContext));
            ((SettingsActivity) mContext).startSuggestion(suggestion.intent);
        });
    }

    @Override
    public long getItemId(int position) {
        if (mSuggestions != null) {
            return Objects.hash(mSuggestions.get(position).title);
        } else {
            return Objects.hash(mSuggestionsV2.get(position).getId());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mSuggestions != null) {
            Tile suggestion = getSuggestion(position);

            return suggestion.remoteViews != null
                    ? R.layout.suggestion_tile_remote_container
                    : R.layout.suggestion_tile;
        } else {
            final Suggestion suggestion = getSuggestionsV2(position);
            if ((suggestion.getFlags() & Suggestion.FLAG_HAS_BUTTON) != 0) {
                return R.layout.suggestion_tile_with_button;
            } else {
                return R.layout.suggestion_tile;
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mSuggestions != null) {
            return mSuggestions.size();
        } else {
            return mSuggestionsV2.size();
        }
    }

    public Tile getSuggestion(int position) {
        final long itemId = getItemId(position);
        if (mSuggestions == null) {
            return null;
        }
        for (Tile tile : mSuggestions) {
            if (Objects.hash(tile.title) == itemId) {
                return tile;
            }
        }
        return null;
    }

    public Suggestion getSuggestionsV2(int position) {
        final long itemId = getItemId(position);
        if (mSuggestionsV2 == null) {
            return null;
        }
        for (Suggestion suggestion : mSuggestionsV2) {
            if (Objects.hash(suggestion.getId()) == itemId) {
                return suggestion;
            }
        }
        return null;
    }

    public void removeSuggestion(Tile suggestion) {
        mSuggestions.remove(suggestion);
        notifyDataSetChanged();
    }

    public void removeSuggestion(Suggestion suggestion) {
        mSuggestionsV2.remove(suggestion);
        notifyDataSetChanged();
    }
}
