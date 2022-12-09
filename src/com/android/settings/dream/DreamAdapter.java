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

import android.annotation.LayoutRes;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settingslib.utils.ColorUtil;

import java.util.List;

/**
 * RecyclerView adapter which displays list of items for the user to select.
 */
public class DreamAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<IDreamItem> mItemList;
    private int mLastSelectedPos = -1;
    private boolean mEnabled = true;
    private SparseIntArray mLayouts = new SparseIntArray();

    /**
     * View holder for each {@link IDreamItem}.
     */
    private class DreamViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitleView;
        private final TextView mSummaryView;
        private final ImageView mPreviewView;
        private final ImageView mPreviewPlaceholderView;
        private final Button mCustomizeButton;
        private final Context mContext;
        private final float mDisabledAlphaValue;

        DreamViewHolder(View view, Context context) {
            super(view);
            mContext = context;
            mPreviewView = view.findViewById(R.id.preview);
            mPreviewPlaceholderView = view.findViewById(R.id.preview_placeholder);
            mTitleView = view.findViewById(R.id.title_text);
            mSummaryView = view.findViewById(R.id.summary_text);
            mCustomizeButton = view.findViewById(R.id.customize_button);
            mDisabledAlphaValue = ColorUtil.getDisabledAlpha(context);
        }

        /**
         * Bind the view at the given position, populating the view with the provided data.
         */
        public void bindView(IDreamItem item, int position) {
            mTitleView.setText(item.getTitle());

            final CharSequence summary = item.getSummary();
            if (TextUtils.isEmpty(summary)) {
                mSummaryView.setVisibility(View.GONE);
            } else {
                mSummaryView.setText(summary);
                mSummaryView.setVisibility(View.VISIBLE);
            }

            final Drawable icon = item.isActive()
                    ? mContext.getDrawable(R.drawable.ic_dream_check_circle)
                    : item.getIcon().mutate();
            if (icon instanceof VectorDrawable) {
                icon.setTintList(
                        mContext.getColorStateList(R.color.dream_card_icon_color_state_list));
            }
            final int iconSize = mContext.getResources().getDimensionPixelSize(
                    R.dimen.dream_item_icon_size);
            icon.setBounds(0, 0, iconSize, iconSize);
            mTitleView.setCompoundDrawablesRelative(icon, null, null, null);

            itemView.setOnClickListener(v -> {
                item.onItemClicked();
                if (mLastSelectedPos > -1 && mLastSelectedPos != position) {
                    notifyItemChanged(mLastSelectedPos);
                }
                notifyItemChanged(position);
            });

            if (item.isActive()) {
                mLastSelectedPos = position;
                itemView.setSelected(true);
                itemView.setClickable(false);
            } else {
                itemView.setSelected(false);
                itemView.setClickable(true);
            }

            if (item.viewType() != DreamItemViewTypes.NO_DREAM_ITEM) {
                final Drawable previewImage = item.getPreviewImage();
                if (previewImage != null) {
                    mPreviewView.setImageDrawable(previewImage);
                    mPreviewView.setClipToOutline(true);
                    mPreviewPlaceholderView.setVisibility(View.GONE);
                } else {
                    mPreviewView.setImageDrawable(null);
                    mPreviewPlaceholderView.setVisibility(View.VISIBLE);
                }

                mCustomizeButton.setOnClickListener(v -> item.onCustomizeClicked());
                mCustomizeButton.setVisibility(
                        item.allowCustomization() && mEnabled ? View.VISIBLE : View.GONE);
                // This must be called AFTER itemView.setSelected above, in order to keep the
                // customize button in an unselected state.
                mCustomizeButton.setSelected(false);
            }

            setEnabledStateOnViews(itemView, mEnabled);
        }

        /**
         * Makes sure the view (and any children) get the enabled state changed.
         */
        private void setEnabledStateOnViews(@NonNull View v, boolean enabled) {
            v.setEnabled(enabled);

            if (v instanceof ViewGroup) {
                final ViewGroup vg = (ViewGroup) v;
                for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                    setEnabledStateOnViews(vg.getChildAt(i), enabled);
                }
            } else {
                v.setAlpha(enabled ? 1 : mDisabledAlphaValue);
            }
        }
    }

    public DreamAdapter(SparseIntArray layouts, List<IDreamItem> itemList) {
        mItemList = itemList;
        mLayouts = layouts;
    }

    public DreamAdapter(@LayoutRes int layoutRes, List<IDreamItem> itemList) {
        mItemList = itemList;
        mLayouts.append(DreamItemViewTypes.DREAM_ITEM, layoutRes);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                      @DreamItemViewTypes.ViewType int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(mLayouts.get(viewType), viewGroup, false);
        return new DreamViewHolder(view, viewGroup.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ((DreamViewHolder) viewHolder).bindView(mItemList.get(i), i);
    }

    @Override
    public @DreamItemViewTypes.ViewType int getItemViewType(int position) {
        return mItemList.get(position).viewType();
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    /**
     * Sets the enabled state of all items.
     */
    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;
            notifyDataSetChanged();
        }
    }

    /**
     * Gets the enabled state of all items.
     */
    public boolean getEnabled() {
        return mEnabled;
    }
}
