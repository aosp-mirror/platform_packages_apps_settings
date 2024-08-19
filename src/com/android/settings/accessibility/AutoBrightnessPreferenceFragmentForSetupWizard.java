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

package com.android.settings.accessibility;

import static android.app.Activity.RESULT_CANCELED;

import android.app.settings.SettingsEnums;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.display.AutoBrightnessSettings;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.FooterPreference;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifPreferenceLayout;

/**
 * Fragment for adaptive brightness settings in the SetupWizard.
 */
public class AutoBrightnessPreferenceFragmentForSetupWizard extends AutoBrightnessSettings {

    private static final String FOOTER_PREFERENCE_KEY = "auto_brightness_footer";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateFooterContentDescription();

        if (view instanceof GlifPreferenceLayout) {
            final GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
            final String title = getContext().getString(
                    R.string.auto_brightness_title);
            final Drawable icon = getContext().getDrawable(R.drawable.ic_accessibility_visibility);
            icon.setTintList(Utils.getColorAttr(getContext(), android.R.attr.colorPrimary));
            AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(getContext(), layout, title,
                    /* description= */ null, icon);

            final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
            AccessibilitySetupWizardUtils.setPrimaryButton(getContext(), mixin, R.string.done,
                    () -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        }
    }

    @NonNull
    @Override
    public RecyclerView onCreateRecyclerView(@NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent, @Nullable Bundle savedInstanceState) {
        if (parent instanceof GlifPreferenceLayout) {
            final GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
        }
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SUW_ACCESSIBILITY_AUTO_BRIGHTNESS;
    }

    private void updateFooterContentDescription() {
        final PreferenceScreen screen = getPreferenceScreen();
        final FooterPreference footerPreference = screen.findPreference(FOOTER_PREFERENCE_KEY);
        if (footerPreference != null) {
            String title = getString(R.string.auto_brightness_content_description_title);
            final StringBuilder sb = new StringBuilder();
            sb.append(title).append("\n\n").append(footerPreference.getTitle());
            footerPreference.setContentDescription(sb);
        }
    }
}
