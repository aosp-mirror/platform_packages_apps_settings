/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.widget.LayoutPreference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.HashMap;
import java.util.Map;

class ZenModeIconPickerListPreferenceController extends AbstractZenModePreferenceController {

    private final IconOptionsProvider mIconOptionsProvider;
    private final IconPickerListener mListener;
    @Nullable private IconAdapter mAdapter;
    private @DrawableRes int mCurrentIconResId;

    ZenModeIconPickerListPreferenceController(@NonNull Context context, @NonNull String key,
            @NonNull IconPickerListener listener) {
        this(context, key, listener, new IconOptionsProviderImpl(context));
    }

    @VisibleForTesting
    ZenModeIconPickerListPreferenceController(@NonNull Context context, @NonNull String key,
            @NonNull IconPickerListener listener,
            @NonNull IconOptionsProvider iconOptionsProvider) {
        super(context, key);
        mListener = listener;
        mIconOptionsProvider = iconOptionsProvider;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        LayoutPreference pref = screen.findPreference(getPreferenceKey());
        if (pref == null) {
            return;
        }

        if (mAdapter == null) {
            mAdapter = new IconAdapter(mIconOptionsProvider);
        }
        RecyclerView recyclerView = pref.findViewById(R.id.icon_list);
        recyclerView.setLayoutManager(new AutoFitGridLayoutManager(mContext));
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(true);
        if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator animator) {
            animator.setSupportsChangeAnimations(true);
        }
    }

    @Override
    void updateState(Preference preference, @NonNull ZenMode zenMode) {
        @DrawableRes int iconResId = zenMode.getIconKey().resId();
        updateIconSelection(iconResId);
    }

    private void updateIconSelection(@DrawableRes int iconResId) {
        if (iconResId != mCurrentIconResId) {
            int oldIconResId = mCurrentIconResId;
            mCurrentIconResId = iconResId;
            if (mAdapter != null) {
                mAdapter.notifyIconChanged(oldIconResId);
                mAdapter.notifyIconChanged(mCurrentIconResId);
            }
        }
    }

    private void onIconSelected(@DrawableRes int iconResId) {
        updateIconSelection(iconResId);
        mListener.onIconSelected(iconResId);
    }

    interface IconPickerListener {
        void onIconSelected(@DrawableRes int iconResId);
    }

    private class IconHolder extends RecyclerView.ViewHolder {

        private final ImageView mImageView;

        IconHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.icon_image_view);
        }

        void bindIcon(IconOptionsProvider.IconInfo icon, Drawable iconDrawable) {
            mImageView.setImageDrawable(iconDrawable);
            itemView.setContentDescription(icon.description());
            itemView.setOnClickListener(v -> {
                itemView.setSelected(true); // Immediately, to avoid flicker until we rebind.
                onIconSelected(icon.resId());
            });
            itemView.setSelected(icon.resId() == mCurrentIconResId);
        }
    }

    private class IconAdapter extends RecyclerView.Adapter<IconHolder> {

        private final ImmutableList<IconOptionsProvider.IconInfo> mIconResources;
        private final Map<IconOptionsProvider.IconInfo, Drawable> mIconCache;

        private IconAdapter(IconOptionsProvider iconOptionsProvider) {
            mIconResources = iconOptionsProvider.getIcons();
            mIconCache = new HashMap<>();
        }

        @NonNull
        @Override
        public IconHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.modes_icon_list_item, parent, false);
            return new IconHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull IconHolder holder, int position) {
            IconOptionsProvider.IconInfo iconInfo = mIconResources.get(position);
            Drawable iconDrawable = mIconCache.computeIfAbsent(iconInfo,
                    info -> IconUtil.makeIconPickerItem(mContext, info.resId()));
            holder.bindIcon(iconInfo, iconDrawable);
        }

        @Override
        public int getItemCount() {
            return mIconResources.size();
        }

        private void notifyIconChanged(@DrawableRes int iconResId) {
            int position = Iterables.indexOf(mIconResources,
                    iconInfo -> iconInfo.resId() == iconResId);
            if (position != -1) {
                notifyItemChanged(position);
            }
        }
    }

    private static class AutoFitGridLayoutManager extends GridLayoutManager {
        private final float mColumnWidth;

        AutoFitGridLayoutManager(Context context) {
            super(context, /* spanCount= */ 1);
            this.mColumnWidth = context
                    .getResources()
                    .getDimensionPixelSize(R.dimen.zen_mode_icon_list_item_size);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            final int totalSpace = getWidth() - getPaddingRight() - getPaddingLeft();
            final int spanCount = Math.max(1, (int) (totalSpace / mColumnWidth));
            setSpanCount(spanCount);
            super.onLayoutChildren(recycler, state);
        }
    }
}
