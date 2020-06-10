/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development.storage;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

/**
 * View holder for {@link LeaseInfoListView}.
 */
class LeaseInfoViewHolder {
    View rootView;
    ImageView appIcon;
    TextView leasePackageName;
    TextView leaseDescription;
    TextView leaseExpiry;

    static LeaseInfoViewHolder createOrRecycle(LayoutInflater inflater, View convertView) {
        if (convertView != null) {
            return (LeaseInfoViewHolder) convertView.getTag();
        }
        convertView = inflater.inflate(R.layout.lease_list_item_view, null);

        final LeaseInfoViewHolder holder = new LeaseInfoViewHolder();
        holder.rootView = convertView;
        holder.appIcon = convertView.findViewById(R.id.app_icon);
        holder.leasePackageName = convertView.findViewById(R.id.lease_package);
        holder.leaseDescription = convertView.findViewById(R.id.lease_desc);
        holder.leaseExpiry = convertView.findViewById(R.id.lease_expiry);
        convertView.setTag(holder);
        return holder;
    }
}
