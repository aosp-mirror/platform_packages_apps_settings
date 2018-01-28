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
import android.service.settings.suggestions.Suggestion;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.suggestions.SuggestionControllerMixin;

/**
 * Deprecated as a close button is provided to dismiss the suggestion.
 */
@Deprecated
public class SuggestionDismissController extends ItemTouchHelper.SimpleCallback {

    public interface Callback {
        /**
         * Returns suggestion tile data from the callback
         */
        Suggestion getSuggestionAt(int position);

        /**
         * Called when a suggestion is dismissed.
         */
        void onSuggestionDismissed(Suggestion suggestion);
    }

    private final Context mContext;
    private final SuggestionFeatureProvider mSuggestionFeatureProvider;
    private final SuggestionControllerMixin mSuggestionMixin;
    private final Callback mCallback;

    public SuggestionDismissController(Context context, RecyclerView recyclerView,
            SuggestionControllerMixin suggestionMixin, Callback callback) {
        super(0, ItemTouchHelper.START | ItemTouchHelper.END);
        mSuggestionMixin = suggestionMixin;
        mContext = context;
        mSuggestionFeatureProvider = FeatureFactory.getFactory(context)
                .getSuggestionFeatureProvider(context);
        mCallback = callback;
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
            RecyclerView.ViewHolder target) {
        return true;
    }

    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final int layoutId = viewHolder.getItemViewType();
        if (layoutId == R.layout.suggestion_tile
                || layoutId == R.layout.suggestion_tile_with_button) {
            // Only return swipe direction for suggestion tiles. All other types are not swipeable.
            return super.getSwipeDirs(recyclerView, viewHolder);
        }
        return 0;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        if (mCallback == null) {
            return;
        }
        final int position = viewHolder.getAdapterPosition();
        final Suggestion suggestionV2 = mCallback.getSuggestionAt(position);
        mSuggestionFeatureProvider.dismissSuggestion(mContext, mSuggestionMixin, suggestionV2);
        mCallback.onSuggestionDismissed(suggestionV2);
    }
}
