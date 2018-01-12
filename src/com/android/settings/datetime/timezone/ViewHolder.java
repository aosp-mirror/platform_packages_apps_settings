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
package com.android.settings.datetime.timezone;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;

/**
 * View holder for a time zone list item.
 */
class ViewHolder extends RecyclerView.ViewHolder {

    final TextView mNameView;
    final TextView mDstView;
    final TextView mDetailsView;
    final TextView mTimeView;

    public ViewHolder(View itemView) {
        super(itemView);
        mNameView = itemView.findViewById(R.id.tz_item_name);
        mDstView = itemView.findViewById(R.id.tz_item_dst);
        mDetailsView = itemView.findViewById(R.id.tz_item_details);
        mTimeView = itemView.findViewById(R.id.tz_item_time);
    }
}
