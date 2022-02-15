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

import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.LayoutPreference;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for the component allowing a user to select overlays to show on top of dreams.
 */
public class DreamComplicationPickerController extends BasePreferenceController {
    private static final String KEY = "dream_complication_picker";

    private final DreamBackend mBackend;
    private final Set<Integer> mSupportedComplications;

    private class ComplicationItem implements IDreamItem {
        private final int mComplicationType;
        private boolean mEnabled;

        ComplicationItem(@DreamBackend.ComplicationType int complicationType) {
            mComplicationType = complicationType;
            mEnabled = mBackend.isComplicationEnabled(mComplicationType);
        }

        @Override
        public CharSequence getTitle() {
            return mBackend.getComplicationTitle(mComplicationType);
        }

        @Override
        public Drawable getIcon() {
            // TODO(b/215703483): add icon for each complication
            return null;
        }

        @Override
        public void onItemClicked() {
            mEnabled = !mEnabled;
            mBackend.setComplicationEnabled(mComplicationType, mEnabled);
        }

        @Override
        public Drawable getPreviewImage() {
            // TODO(b/215703483): add preview image for each complication
            return null;
        }

        @Override
        public boolean isActive() {
            return mEnabled;
        }
    }

    public DreamComplicationPickerController(Context context) {
        super(context, KEY);
        mBackend = DreamBackend.getInstance(context);
        mSupportedComplications = mBackend.getSupportedComplications();
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public int getAvailabilityStatus() {
        return mSupportedComplications.size() > 0 ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        DreamAdapter adapter = new DreamAdapter(mSupportedComplications.stream()
                .map(ComplicationItem::new)
                .collect(Collectors.toList()));

        LayoutPreference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            final RecyclerView recyclerView = pref.findViewById(R.id.dream_list);
            recyclerView.setLayoutManager(new AutoFitGridLayoutManager(mContext));
            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(adapter);
        }
    }
}
