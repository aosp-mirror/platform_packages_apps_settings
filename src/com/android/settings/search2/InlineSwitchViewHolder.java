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
 *
 */

package com.android.settings.search2;

import android.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.content.Context;

import com.android.internal.widget.PreferenceImageView;
import com.android.settings.R;

/**
 * ViewHolder for Settings represented as SwitchPreferences.
 */
public class InlineSwitchViewHolder extends SearchViewHolder {
    public final TextView titleView;
    public final TextView summaryView;
    public final PreferenceImageView iconView;
    public final Switch switchView;

    private final Context mContext;

    private final String TAG = "SwitchViewHolder";

    public InlineSwitchViewHolder(View view, Context context) {
        super(view);
        mContext = context;
        titleView = (TextView) view.findViewById(android.R.id.title);
        summaryView = (TextView) view.findViewById(android.R.id.summary);
        iconView = (PreferenceImageView) view.findViewById(android.R.id.icon);
        switchView = (Switch) view.findViewById(R.id.switchView);
    }

    @Override
    public void onBind(Fragment fragment, SearchResult result) {
        if (mContext == null) {
            return;
        }
        final InlineSwitchPayload payload = (InlineSwitchPayload) result.payload;
        switchView.setChecked(payload.getSwitchValue(mContext));

        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                payload.setSwitchValue(mContext, isChecked);
            }
        });

        titleView.setText(result.title);
        summaryView.setText(result.summary);
        iconView.setImageDrawable(result.icon);
    }
}
