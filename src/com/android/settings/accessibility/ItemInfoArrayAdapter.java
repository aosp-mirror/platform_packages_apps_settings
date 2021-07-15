/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

import java.util.List;

/**
 * An {@link ArrayAdapter} to fill the information of {@link ItemInfo} in the item view. The item
 * view must have textview to set the title.
 *
 * @param <T> the type of elements in the array, inherited from {@link ItemInfo}.
 */
public class ItemInfoArrayAdapter<T extends ItemInfoArrayAdapter.ItemInfo> extends ArrayAdapter<T> {

    public ItemInfoArrayAdapter(@NonNull Context context, @NonNull List<T> items) {
        super(context, R.layout.dialog_single_radio_choice_list_item, R.id.title, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final View root = super.getView(position, convertView, parent);

        final ItemInfo item = getItem(position);
        final TextView title = root.findViewById(R.id.title);
        title.setText(item.mTitle);
        final TextView summary = root.findViewById(R.id.summary);
        if (!TextUtils.isEmpty(item.mSummary)) {
            summary.setVisibility(View.VISIBLE);
            summary.setText(item.mSummary);
        } else {
            summary.setVisibility(View.GONE);
        }
        final ImageView image = root.findViewById(R.id.image);
        image.setImageResource(item.mDrawableId);
        if (getContext().getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_LTR) {
            image.setScaleType(ImageView.ScaleType.FIT_START);
        } else {
            image.setScaleType(ImageView.ScaleType.FIT_END);
        }
        return root;
    }

    /**
     * Presents a data structure shown in the item view.
     */
    public static class ItemInfo {
        @NonNull
        public final CharSequence mTitle;
        @Nullable
        public final CharSequence mSummary;
        @DrawableRes
        public final int mDrawableId;

        public ItemInfo(@NonNull CharSequence title, @Nullable CharSequence summary,
                @DrawableRes int drawableId) {
            mTitle = title;
            mSummary = summary;
            mDrawableId = drawableId;
        }
    }
}
