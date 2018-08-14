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
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardAdapter.DashboardItemHolder;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import com.android.settingslib.suggestions.SuggestionControllerMixin;
import com.android.settingslib.utils.IconCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SuggestionAdapter extends RecyclerView.Adapter<DashboardItemHolder> implements
    LifecycleObserver, OnSaveInstanceState {
    public static final String TAG = "SuggestionAdapter";

    private static final String STATE_SUGGESTIONS_SHOWN_LOGGED = "suggestions_shown_logged";
    private static final String STATE_SUGGESTION_LIST = "suggestion_list";

    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final IconCache mCache;
    private final ArrayList<String> mSuggestionsShownLogged;
    private final SuggestionFeatureProvider mSuggestionFeatureProvider;
    private final SuggestionControllerMixin mSuggestionControllerMixin;
    private final Callback mCallback;
    private final CardConfig mConfig;

    private List<Suggestion> mSuggestions;

    public interface Callback {
        /**
         * Called when the close button of the suggestion card is clicked.
         */
        void onSuggestionClosed(Suggestion suggestion);
    }

    public SuggestionAdapter(Context context, SuggestionControllerMixin suggestionControllerMixin,
        Bundle savedInstanceState, Callback callback, Lifecycle lifecycle) {
        mContext = context;
        mSuggestionControllerMixin = suggestionControllerMixin;
        mCache = new IconCache(context);
        final FeatureFactory factory = FeatureFactory.getFactory(context);
        mMetricsFeatureProvider = factory.getMetricsFeatureProvider();
        mSuggestionFeatureProvider = factory.getSuggestionFeatureProvider(context);
        mCallback = callback;
        if (savedInstanceState != null) {
            mSuggestions = savedInstanceState.getParcelableArrayList(STATE_SUGGESTION_LIST);
            mSuggestionsShownLogged = savedInstanceState.getStringArrayList(
                STATE_SUGGESTIONS_SHOWN_LOGGED);
        } else {
            mSuggestionsShownLogged = new ArrayList<>();
        }

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mConfig = CardConfig.get(context);

        setHasStableIds(true);
    }

    @Override
    public DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DashboardItemHolder(LayoutInflater.from(parent.getContext()).inflate(
                viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(DashboardItemHolder holder, int position) {
        final Suggestion suggestion = mSuggestions.get(position);
        final String id = suggestion.getId();
        final int suggestionCount = mSuggestions.size();
        if (!mSuggestionsShownLogged.contains(id)) {
            mMetricsFeatureProvider.action(
                    mContext, MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION, id);
            mSuggestionsShownLogged.add(id);
        }
        final Icon icon = suggestion.getIcon();
        final Drawable drawable = mCache.getIcon(icon);
        if (drawable != null && (suggestion.getFlags() & Suggestion.FLAG_ICON_TINTABLE) != 0) {
            drawable.setTint(Utils.getColorAccent(mContext));
        }
        holder.icon.setImageDrawable(drawable);
        holder.title.setText(suggestion.getTitle());
        holder.title.setTypeface(Typeface.create(
            mContext.getString(com.android.internal.R.string.config_headlineFontFamily),
            Typeface.NORMAL));

        if (suggestionCount == 1) {
            final CharSequence summary = suggestion.getSummary();
            if (!TextUtils.isEmpty(summary)) {
                holder.summary.setText(summary);
                holder.summary.setVisibility(View.VISIBLE);
            } else {
                holder.summary.setVisibility(View.GONE);
            }
        } else {
            mConfig.setCardLayout(holder, position);
        }

        final View closeButton = holder.itemView.findViewById(R.id.close_button);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                mSuggestionFeatureProvider.dismissSuggestion(
                    mContext, mSuggestionControllerMixin, suggestion);
                if (mCallback != null) {
                    mCallback.onSuggestionClosed(suggestion);
                }
            });
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
        }
        if (getItemCount() == 1) {
            return R.layout.suggestion_tile;
        }
        return R.layout.suggestion_tile_two_cards;
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
        final int position = mSuggestions.indexOf(suggestion);
        mSuggestions.remove(suggestion);
        notifyItemRemoved(position);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mSuggestions != null) {
            outState.putParcelableArrayList(STATE_SUGGESTION_LIST,
                new ArrayList<>(mSuggestions));
        }
        outState.putStringArrayList(STATE_SUGGESTIONS_SHOWN_LOGGED, mSuggestionsShownLogged);
    }

    public void setSuggestions(List<Suggestion> suggestions) {
        mSuggestions = suggestions;
    }

    public List<Suggestion> getSuggestions() {
        return mSuggestions;
    }

    @VisibleForTesting
    static class CardConfig {
        // Card start/end margin
        private final int mMarginInner;
        private final int mMarginOuter;
        private final WindowManager mWindowManager;

        private static CardConfig sConfig;

        private CardConfig(Context context) {
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            final Resources res = context.getResources();
            mMarginInner =
                res.getDimensionPixelOffset(R.dimen.suggestion_card_inner_margin);
            mMarginOuter =
                res.getDimensionPixelOffset(R.dimen.suggestion_card_outer_margin);
        }

        public static CardConfig get(Context context) {
            if (sConfig == null) {
                sConfig = new CardConfig(context);
            }
            return sConfig;
        }

        @VisibleForTesting
        void setCardLayout(DashboardItemHolder holder, int position) {
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                getWidthForTwoCrads(), LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginStart(position == 0 ? mMarginOuter : mMarginInner);
            params.setMarginEnd(position != 0 ? mMarginOuter : 0);
            holder.itemView.setLayoutParams(params);
        }

        private int getWidthForTwoCrads() {
            return (getScreenWidth() - mMarginInner - mMarginOuter * 2) / 2;
        }

        @VisibleForTesting
        int getScreenWidth() {
            final DisplayMetrics metrics = new DisplayMetrics();
            mWindowManager.getDefaultDisplay().getMetrics(metrics);
            return metrics.widthPixels;
        }
    }

}
