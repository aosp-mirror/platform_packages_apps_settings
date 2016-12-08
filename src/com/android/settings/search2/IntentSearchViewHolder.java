/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.search2;

import android.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

/**
 * ViewHolder for intent based search results.
 * The DatabaseResultLoader is the primary use case for this ViewHolder.
 */
public class IntentSearchViewHolder extends SearchViewHolder {

    public final TextView titleView;
    public final TextView summaryView;
    public final ImageView iconView;

    public IntentSearchViewHolder(View view) {
        super(view);
        titleView = (TextView) view.findViewById(R.id.title);
        summaryView = (TextView) view.findViewById(R.id.summary);
        iconView = (ImageView) view.findViewById(R.id.icon);
    }

    @Override
    public void onBind(Fragment fragment, SearchResult result) {
        titleView.setText(result.title);
        summaryView.setText(result.summary);
        iconView.setImageDrawable(result.icon);
        if (result.icon == null) {
            iconView.setBackgroundResource(R.drawable.empty_icon);
        }
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.startActivity(((IntentPayload) result.payload).intent);
            }
        });
    }
}
