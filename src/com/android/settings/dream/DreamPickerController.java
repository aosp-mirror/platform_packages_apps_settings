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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the dream picker where the user can select a screensaver.
 */
public class DreamPickerController extends BasePreferenceController {
    public static final String KEY = "dream_picker";

    private final DreamBackend mBackend;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final List<DreamBackend.DreamInfo> mDreamInfos;
    private final Drawable mActiveDrawable;
    private Button mPreviewButton;
    @Nullable
    private DreamBackend.DreamInfo mActiveDream;
    private DreamAdapter mAdapter;

    public DreamPickerController(Context context, String preferenceKey) {
        this(context, preferenceKey, DreamBackend.getInstance(context));
    }

    public DreamPickerController(Context context, String preferenceKey, DreamBackend backend) {
        super(context, preferenceKey);
        mBackend = backend;
        mDreamInfos = mBackend.getDreamInfos();
        mActiveDrawable = context.getDrawable(R.drawable.ic_dream_check_circle);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
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

        mAdapter = new DreamAdapter(mDreamInfos.stream()
                .map(DreamItem::new)
                .collect(Collectors.toList()));

        final RecyclerView recyclerView =
                ((LayoutPreference) preference).findViewById(R.id.dream_list);
        recyclerView.setLayoutManager(new AutoFitGridLayoutManager(mContext));
        recyclerView.setAdapter(mAdapter);

        mPreviewButton = ((LayoutPreference) preference).findViewById(R.id.preview_button);
        mPreviewButton.setVisibility(View.VISIBLE);
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

    private class DreamItem implements IDreamItem {
        DreamBackend.DreamInfo mDreamInfo;

        DreamItem(DreamBackend.DreamInfo dreamInfo) {
            mDreamInfo = dreamInfo;
        }

        @Override
        public CharSequence getTitle() {
            return mDreamInfo.caption;
        }

        @Override
        public Drawable getIcon() {
            return isActive() ? mActiveDrawable : mDreamInfo.icon;
        }

        @Override
        public void onItemClicked() {
            mActiveDream = mDreamInfo;
            mBackend.setActiveDream(mDreamInfo.componentName);
            mAdapter.notifyDataSetChanged();
            updatePreviewButtonState();
            mMetricsFeatureProvider.action(
                    mContext,
                    SettingsEnums.ACTION_DREAM_SELECT_TYPE,
                    mDreamInfo.componentName.flattenToString());
        }

        @Override
        public void onCustomizeClicked() {
            mBackend.launchSettings(mContext, mDreamInfo);
        }

        @Override
        public Drawable getPreviewImage() {
            return mDreamInfo.previewImage;
        }

        @Override
        public boolean isActive() {
            if (mActiveDream == null) {
                return false;
            }
            return mDreamInfo.componentName.equals(mActiveDream.componentName);
        }

        @Override
        public boolean allowCustomization() {
            return isActive() && mDreamInfo.settingsComponentName != null;
        }
    }
}
