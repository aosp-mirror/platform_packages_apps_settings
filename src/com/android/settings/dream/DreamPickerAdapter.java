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


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settingslib.dream.DreamBackend.DreamInfo;

import java.util.List;

/**
 * RecyclerView adapter which displays list of available dreams for the user to select.
 */
class DreamPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<DreamInfo> mDreamInfoList;
    private final OnItemClickListener mItemClickListener;
    private final OnItemClickListener mOnDreamSelected = new OnItemClickListener() {
        @Override
        public void onItemClicked(DreamInfo dreamInfo) {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClicked(dreamInfo);
            }
            mDreamInfoList.forEach(dream -> {
                if (dream != null) {
                    dream.isActive = false;
                }
            });
            dreamInfo.isActive = true;
            notifyDataSetChanged();
        }
    };
    private final OnItemClickListener mOnCustomizeListener;

    interface OnItemClickListener {
        void onItemClicked(DreamInfo dreamInfo);
    }

    /**
     * View holder for each Dream service.
     */
    private static class DreamViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIconView;
        private final TextView mTitleView;
        private final TextView mSummaryView;
        private final ImageView mPreviewView;
        private final Button mCustomizeButton;

        DreamViewHolder(View view) {
            super(view);
            mPreviewView = view.findViewById(R.id.preview);
            mIconView = view.findViewById(R.id.icon);
            mTitleView = view.findViewById(R.id.title_text);
            mSummaryView = view.findViewById(R.id.summary_text);
            mCustomizeButton = view.findViewById(R.id.customize_button);
        }

        /**
         * Bind the dream service view at the given position. Add details on the
         * dream's icon, name and description.
         */
        public void bindView(DreamInfo dreamInfo, OnItemClickListener clickListener,
                OnItemClickListener customizeListener) {
            mIconView.setImageDrawable(dreamInfo.icon);
            mTitleView.setText(dreamInfo.caption);
            mPreviewView.setImageDrawable(dreamInfo.previewImage);
            mSummaryView.setText(dreamInfo.description);
            itemView.setActivated(dreamInfo.isActive);
            if (dreamInfo.isActive && dreamInfo.settingsComponentName != null) {
                mCustomizeButton.setVisibility(View.VISIBLE);
                mCustomizeButton.setOnClickListener(
                        v -> customizeListener.onItemClicked(dreamInfo));
            } else {
                mCustomizeButton.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> clickListener.onItemClicked(dreamInfo));
        }
    }

    DreamPickerAdapter(List<DreamInfo> dreamInfos, OnItemClickListener clickListener,
            OnItemClickListener onCustomizeListener) {
        mDreamInfoList = dreamInfos;
        mItemClickListener = clickListener;
        mOnCustomizeListener = onCustomizeListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.dream_preference_layout, viewGroup, false);
        return new DreamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ((DreamViewHolder) viewHolder).bindView(mDreamInfoList.get(i), mOnDreamSelected,
                mOnCustomizeListener);
    }

    @Override
    public int getItemCount() {
        return mDreamInfoList.size();
    }
}
