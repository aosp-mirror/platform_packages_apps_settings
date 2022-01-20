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
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dream.DreamPickerAdapter.OnItemClickListener;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;

/**
 * Controller for the dream picker where the user can select a screensaver.
 */
public class DreamPickerController extends BasePreferenceController {
    public static final String KEY = "dream_picker";

    private final DreamBackend mBackend;
    private final List<DreamBackend.DreamInfo> mDreamInfos;
    private Button mPreviewButton;
    @Nullable
    private DreamBackend.DreamInfo mActiveDream;

    private final OnItemClickListener mItemClickListener =
            new OnItemClickListener() {
                @Override
                public void onItemClicked(DreamBackend.DreamInfo dreamInfo) {
                    mActiveDream = dreamInfo;
                    mBackend.setActiveDream(
                            mActiveDream == null ? null : mActiveDream.componentName);
                    updatePreviewButtonState();
                }
            };

    private final OnItemClickListener mCustomizeListener = new OnItemClickListener() {
        @Override
        public void onItemClicked(DreamBackend.DreamInfo dreamInfo) {
            mBackend.launchSettings(mContext, dreamInfo);
        }
    };

    public DreamPickerController(Context context, String preferenceKey) {
        this(context, preferenceKey, DreamBackend.getInstance(context));
    }

    public DreamPickerController(Context context, String preferenceKey, DreamBackend backend) {
        super(context, preferenceKey);
        mBackend = backend;
        mDreamInfos = mBackend.getDreamInfos();
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public int getAvailabilityStatus() {
        return mDreamInfos.size() > 0 ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        mActiveDream = getActiveDreamInfo();

        final DreamPickerAdapter adapter =
                new DreamPickerAdapter(mDreamInfos, mItemClickListener, mCustomizeListener);

        final RecyclerView recyclerView =
                ((LayoutPreference) preference).findViewById(R.id.dream_list);
        recyclerView.setLayoutManager(new AutoFitGridLayoutManager(mContext));
        recyclerView.setAdapter(adapter);

        mPreviewButton = ((LayoutPreference) preference).findViewById(R.id.preview_button);
        mPreviewButton.setOnClickListener(v -> mBackend.preview(mActiveDream));
        updatePreviewButtonState();
    }

    private void updatePreviewButtonState() {
        final boolean hasDream = mActiveDream != null;
        mPreviewButton.setClickable(hasDream);
        mPreviewButton.setEnabled(hasDream);
    }

    @Nullable
    private DreamBackend.DreamInfo getActiveDreamInfo() {
        return mDreamInfos
                .stream()
                .filter(d -> d.isActive)
                .findFirst()
                .orElse(null);
    }

    /** Grid layout manager that calculates the number of columns for the screen size. */
    private static final class AutoFitGridLayoutManager extends GridLayoutManager {
        private final float mColumnWidth;

        AutoFitGridLayoutManager(Context context) {
            super(context, /* spanCount= */ 1);
            this.mColumnWidth = context
                    .getResources()
                    .getDimensionPixelSize(R.dimen.dream_item_min_column_width);
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
