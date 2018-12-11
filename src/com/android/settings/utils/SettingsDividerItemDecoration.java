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

package com.android.settings.utils;

import android.content.Context;

import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.setupdesign.DividerItemDecoration;

public class SettingsDividerItemDecoration extends DividerItemDecoration {

    public SettingsDividerItemDecoration(Context context) {
        super(context);
    }

    @Override
    protected boolean isDividerAllowedAbove(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof PreferenceViewHolder) {
            return ((PreferenceViewHolder) viewHolder).isDividerAllowedAbove();
        }
        return super.isDividerAllowedAbove(viewHolder);
    }

    @Override
    protected boolean isDividerAllowedBelow(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof PreferenceViewHolder) {
            return ((PreferenceViewHolder) viewHolder).isDividerAllowedBelow();
        }
        return super.isDividerAllowedBelow(viewHolder);
    }
}
