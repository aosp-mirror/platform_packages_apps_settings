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
import android.widget.TextView;

import com.android.settings.R;

/**
 * View holder for {@link BlobInfoListView}.
 */
class BlobInfoViewHolder {
    View rootView;
    TextView blobLabel;
    TextView blobId;
    TextView blobExpiry;
    TextView blobSize;

    static BlobInfoViewHolder createOrRecycle(LayoutInflater inflater, View convertView) {
        if (convertView != null) {
            return (BlobInfoViewHolder) convertView.getTag();
        }
        convertView = inflater.inflate(R.layout.blob_list_item_view, null);

        final BlobInfoViewHolder holder = new BlobInfoViewHolder();
        holder.rootView = convertView;
        holder.blobLabel = convertView.findViewById(R.id.blob_label);
        holder.blobId = convertView.findViewById(R.id.blob_id);
        holder.blobExpiry = convertView.findViewById(R.id.blob_expiry);
        holder.blobSize = convertView.findViewById(R.id.blob_size);
        convertView.setTag(holder);
        return holder;
    }
}
