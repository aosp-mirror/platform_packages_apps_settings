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

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.DreamInfo;
import com.android.settingslib.widget.LayoutPreference;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the dream picker where the user can select a screensaver.
 */
public class DreamPickerController extends BasePreferenceController {
    public static final String PREF_KEY = "dream_picker";

    private final DreamBackend mBackend;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final List<DreamInfo> mDreamInfos;
    @Nullable
    private DreamInfo mActiveDream;
    private DreamAdapter mAdapter;

    private final HashSet<Callback> mCallbacks = new HashSet<>();

    public DreamPickerController(Context context) {
        this(context, DreamBackend.getInstance(context));
    }

    public DreamPickerController(Context context, DreamBackend backend) {
        super(context, PREF_KEY);
        mBackend = backend;
        mDreamInfos = mBackend.getDreamInfos();
        mActiveDream = getActiveDreamInfo(mDreamInfos);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return mDreamInfos.size() > 0 ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mAdapter = new DreamAdapter(R.layout.dream_preference_layout,
                mDreamInfos.stream()
                        .map(DreamItem::new)
                        .collect(Collectors.toList()));

        mAdapter.setEnabled(mBackend.isEnabled());

        final LayoutPreference pref = screen.findPreference(getPreferenceKey());
        if (pref == null) {
            return;
        }
        final RecyclerView recyclerView = pref.findViewById(R.id.dream_list);
        recyclerView.setLayoutManager(new AutoFitGridLayoutManager(mContext));
        recyclerView.addItemDecoration(
                new GridSpacingItemDecoration(mContext, R.dimen.dream_preference_card_padding));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mAdapter != null) {
            mAdapter.setEnabled(preference.isEnabled());
        }
    }

    @Nullable
    public DreamInfo getActiveDreamInfo() {
        return mActiveDream;
    }

    @Nullable
    private static DreamInfo getActiveDreamInfo(List<DreamInfo> dreamInfos) {
        return dreamInfos
                .stream()
                .filter(d -> d.isActive)
                .findFirst()
                .orElse(null);
    }

    void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    interface Callback {
        // Triggered when the selected dream changes.
        void onActiveDreamChanged();
    }

    private class DreamItem implements IDreamItem {
        DreamInfo mDreamInfo;

        DreamItem(DreamInfo dreamInfo) {
            mDreamInfo = dreamInfo;
        }

        @Override
        public CharSequence getTitle() {
            return mDreamInfo.caption;
        }

        @Override
        public CharSequence getSummary() {
            return mDreamInfo.description;
        }

        @Override
        public Drawable getIcon() {
            return mDreamInfo.icon;
        }

        @Override
        public void onItemClicked() {
            mActiveDream = mDreamInfo;
            mBackend.setActiveDream(mDreamInfo.componentName);
            mCallbacks.forEach(Callback::onActiveDreamChanged);
            mMetricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_DREAM_SELECT_TYPE, SettingsEnums.DREAM,
                    mDreamInfo.componentName.flattenToString(), 1);
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
            if (!mAdapter.getEnabled() || mActiveDream == null) {
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
