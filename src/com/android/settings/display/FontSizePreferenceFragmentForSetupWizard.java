/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.display;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.viewpager.widget.ViewPager;

import com.android.settings.R;

public class FontSizePreferenceFragmentForSetupWizard
        extends ToggleFontSizePreferenceFragment {

    @Override
    protected int getActivityLayoutResId() {
        return R.layout.suw_font_size_fragment;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SUW_ACCESSIBILITY_FONT_SIZE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        if (getResources().getBoolean(R.bool.config_supported_large_screen)) {
            final ViewPager viewPager = root.findViewById(R.id.preview_pager);
            final View view = (View) viewPager.getAdapter().instantiateItem(viewPager,
                    viewPager.getCurrentItem());
            final LinearLayout layout = view.findViewById(R.id.font_size_preview_text_group);
            final int paddingStart = getResources().getDimensionPixelSize(
                    R.dimen.font_size_preview_padding_start);
            layout.setPaddingRelative(paddingStart, layout.getPaddingTop(),
                    layout.getPaddingEnd(), layout.getPaddingBottom());
        }
        return root;
    }

    @Override
    public void onStop() {
        // Log the final choice in value if it's different from the previous value.
        if (mCurrentIndex != mInitialIndex) {
            mMetricsFeatureProvider.action(getContext(), SettingsEnums.SUW_ACCESSIBILITY_FONT_SIZE,
                    mCurrentIndex);
        }

        super.onStop();
    }
}
