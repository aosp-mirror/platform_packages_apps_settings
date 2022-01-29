/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.dream;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settingslib.Utils;

import java.util.List;

/**
 * RecyclerView adapter which displays list of items for the user to select.
 */
class DreamAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<IDreamItem> mItemList;

    /**
     * View holder for each {@link IDreamItem}.
     */
    private static class DreamViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIconView;
        private final TextView mTitleView;
        private final ImageView mPreviewView;
        private final Button mCustomizeButton;
        private final Context mContext;

        DreamViewHolder(View view, Context context) {
            super(view);
            mContext = context;
            mPreviewView = view.findViewById(R.id.preview);
            mIconView = view.findViewById(R.id.icon);
            mTitleView = view.findViewById(R.id.title_text);
            mCustomizeButton = view.findViewById(R.id.customize_button);
        }

        /**
         * Bind the view at the given position, populating the view with the provided data.
         */
        public void bindView(IDreamItem item) {
            mTitleView.setText(item.getTitle());
            mPreviewView.setImageDrawable(item.getPreviewImage());

            final Drawable icon = item.getIcon();
            if (icon instanceof VectorDrawable) {
                icon.setTint(Utils.getColorAttrDefaultColor(mContext,
                        com.android.internal.R.attr.colorAccentPrimaryVariant));
            }
            mIconView.setImageDrawable(icon);
            if (item.allowCustomization()) {
                mCustomizeButton.setVisibility(View.VISIBLE);
                mCustomizeButton.setOnClickListener(v -> item.onCustomizeClicked());
            } else {
                mCustomizeButton.setVisibility(View.GONE);
            }
            itemView.setOnClickListener(v -> item.onItemClicked());
            itemView.setActivated(item.isActive());
        }
    }

    DreamAdapter(List<IDreamItem> itemList) {
        mItemList = itemList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.dream_preference_layout, viewGroup, false);
        return new DreamViewHolder(view, viewGroup.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ((DreamViewHolder) viewHolder).bindView(mItemList.get(i));
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }
}
