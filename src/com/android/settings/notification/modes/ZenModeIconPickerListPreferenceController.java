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

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.widget.LayoutPreference;

import com.google.common.collect.ImmutableList;

class ZenModeIconPickerListPreferenceController extends AbstractZenModePreferenceController {

    private final DashboardFragment mFragment;
    private final IconOptionsProvider mIconOptionsProvider;
    @Nullable private IconAdapter mAdapter;

    ZenModeIconPickerListPreferenceController(@NonNull Context context, @NonNull String key,
            @NonNull DashboardFragment fragment, @NonNull IconOptionsProvider iconOptionsProvider,
            @Nullable ZenModesBackend backend) {
        super(context, key, backend);
        mFragment = fragment;
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
    }

    @VisibleForTesting
    void onIconSelected(@DrawableRes int resId) {
        saveMode(mode -> {
            mode.getRule().setIconResId(resId);
            return mode;
        });
        mFragment.finish();
    }

    @Override
    void updateState(Preference preference, @NonNull ZenMode zenMode) {
        // Nothing to do, the current icon is shown in a different preference.
    }

    private class IconHolder extends RecyclerView.ViewHolder {

        private final ImageView mImageView;

        IconHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.icon_image_view);
        }

        void bindIcon(IconOptionsProvider.IconInfo icon) {
            mImageView.setImageDrawable(
                    IconUtil.makeIconCircle(itemView.getContext(), icon.resId()));
            itemView.setContentDescription(icon.description());
            itemView.setOnClickListener(v -> onIconSelected(icon.resId()));
        }
    }

    private class IconAdapter extends RecyclerView.Adapter<IconHolder> {

        private final ImmutableList<IconOptionsProvider.IconInfo> mIconResources;

        private IconAdapter(IconOptionsProvider iconOptionsProvider) {
            mIconResources = iconOptionsProvider.getIcons();
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
            holder.bindIcon(mIconResources.get(position));
        }

        @Override
        public int getItemCount() {
            return mIconResources.size();
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
