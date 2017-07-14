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
 *
 */

package com.android.settings.search;

import android.view.View;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;

public class SavedQueryViewHolder extends SearchViewHolder {

    public final TextView titleView;

    public SavedQueryViewHolder(View view) {
        super(view);
        titleView = view.findViewById(android.R.id.title);
    }

    @Override
    public int getClickActionMetricName() {
        return MetricsProto.MetricsEvent.ACTION_CLICK_SETTINGS_SEARCH_SAVED_QUERY;
    }

    @Override
    public void onBind(SearchFragment fragment, SearchResult result) {
        itemView.setOnClickListener(v -> fragment.onSavedQueryClicked(result.title));
        titleView.setText(result.title);
    }
}