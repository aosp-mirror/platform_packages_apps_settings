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

package com.android.settings.accessibility;

import static android.app.Activity.RESULT_CANCELED;

import static com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums.DIALOG_RESET_SETTINGS;

import android.app.settings.SettingsEnums;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.Utils;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifPreferenceLayout;

/**
 * A {@link androidx.preference.PreferenceFragmentCompat} that displays the settings page related
 * to the text and reading option in the SetupWizard.
 */
public class TextReadingPreferenceFragmentForSetupWizard extends TextReadingPreferenceFragment {

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);

        adjustPreviewPaddingsForSetupWizard();
    }

    @VisibleForTesting
    void adjustPreviewPaddingsForSetupWizard() {
        TextReadingPreviewPreference textReadingPreviewPreference = findPreference(PREVIEW_KEY);
        textReadingPreviewPreference.setLayoutMinHorizontalPadding(
                getContext().getResources().getDimensionPixelSize(
                        R.dimen.text_reading_preview_layout_padding_horizontal_min_suw));
        textReadingPreviewPreference.setBackgroundMinHorizontalPadding(
                getContext().getResources().getDimensionPixelSize(
                        R.dimen.text_reading_preview_background_padding_horizontal_min_suw));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view instanceof GlifPreferenceLayout) {
            final GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
            final String title = getContext().getString(
                    R.string.accessibility_text_reading_options_title);
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
            AccessibilitySetupWizardUtils.setSecondaryButton(getContext(), mixin,
                    R.string.accessibility_text_reading_reset_button_title,
                    () -> showDialog(DIALOG_RESET_SETTINGS)
            );
        }
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
            Bundle savedInstanceState) {
        if (parent instanceof GlifPreferenceLayout) {
            final GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
        }
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SUW_ACCESSIBILITY_TEXT_READING_OPTIONS;
    }

    @Override
    public int getHelpResource() {
        // Hides help center in action bar and footer bar in SuW
        return 0;
    }
}
