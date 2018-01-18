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
import com.android.settings.dashboard.DashboardAdapter.DashboardItemHolder;
import com.android.settings.dashboard.DashboardAdapter.IconCache;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.suggestions.SuggestionControllerMixin;

import java.util.List;
import java.util.Objects;

public class SuggestionAdapter extends RecyclerView.Adapter<DashboardItemHolder> {
    public static final String TAG = "SuggestionAdapter";

    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final List<Suggestion> mSuggestions;
    private final IconCache mCache;
    private final List<String> mSuggestionsShownLogged;
    private final SuggestionControllerMixin mSuggestionControllerMixin;

    public SuggestionAdapter(Context context, SuggestionControllerMixin suggestionControllerMixin,
            List<Suggestion> suggestions, List<String> suggestionsShownLogged) {
        mContext = context;
        mSuggestionControllerMixin = suggestionControllerMixin;
        mSuggestions = suggestions;
        mSuggestionsShownLogged = suggestionsShownLogged;
        mCache = new IconCache(context);
        final FeatureFactory factory = FeatureFactory.getFactory(context);
        mMetricsFeatureProvider = factory.getMetricsFeatureProvider();

        setHasStableIds(true);
    }

    @Override
    public DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DashboardItemHolder(LayoutInflater.from(parent.getContext()).inflate(
                viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(DashboardItemHolder holder, int position) {
        bindSuggestion(holder, position);
    }

    private void bindSuggestion(DashboardItemHolder holder, int position) {
        final Suggestion suggestion = mSuggestions.get(position);
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
            mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_SETTINGS_SUGGESTION, id);
            try {
                suggestion.getPendingIntent().send();
                mSuggestionControllerMixin.launchSuggestion(suggestion);
            } catch (PendingIntent.CanceledException e) {
                Log.w(TAG, "Failed to start suggestion " + suggestion.getTitle());
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return Objects.hash(mSuggestions.get(position).getId());
    }

    @Override
    public int getItemViewType(int position) {
        final Suggestion suggestion = getSuggestion(position);
        if ((suggestion.getFlags() & Suggestion.FLAG_HAS_BUTTON) != 0) {
            return R.layout.suggestion_tile_with_button;
        } else {
            return R.layout.suggestion_tile;
        }
    }

    @Override
    public int getItemCount() {
        return mSuggestions.size();
    }

    public Suggestion getSuggestion(int position) {
        final long itemId = getItemId(position);
        if (mSuggestions == null) {
            return null;
        }
        for (Suggestion suggestion : mSuggestions) {
            if (Objects.hash(suggestion.getId()) == itemId) {
                return suggestion;
            }
        }
        return null;
    }

    public void removeSuggestion(Suggestion suggestion) {
        mSuggestions.remove(suggestion);
        notifyDataSetChanged();
    }
}
