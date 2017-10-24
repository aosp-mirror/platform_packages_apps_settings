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
import android.support.v7.widget.helper.ItemTouchHelper;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.suggestions.SuggestionParser;

public class SuggestionDismissController extends ItemTouchHelper.SimpleCallback {

    public interface Callback {

        /**
         * Returns suggestion tile data from the callback
         */
        Tile getSuggestionForPosition(int position);

        /**
         * Called when a suggestion is dismissed.
         */
        void onSuggestionDismissed(Tile suggestion);
    }

    private final Context mContext;
    private final SuggestionFeatureProvider mSuggestionFeatureProvider;
    private final SuggestionParser mSuggestionParser;
    private final Callback mCallback;

    public SuggestionDismissController(Context context, RecyclerView recyclerView,
            SuggestionParser parser, Callback callback) {
        super(0, ItemTouchHelper.START | ItemTouchHelper.END);
        mContext = context;
        mSuggestionParser = parser;
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
                || layoutId == R.layout.suggestion_tile_remote_container) {
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
        final Tile suggestion = mCallback.getSuggestionForPosition(viewHolder.getAdapterPosition());
        mSuggestionFeatureProvider.dismissSuggestion(mContext, mSuggestionParser, suggestion);
        mCallback.onSuggestionDismissed(suggestion);
    }
}
