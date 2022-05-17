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

import android.app.settings.SettingsEnums;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.LayoutPreference;

import com.google.android.setupdesign.GlifPreferenceLayout;
import com.google.android.setupdesign.util.LayoutStyler;


/**
 * A {@link androidx.preference.PreferenceFragmentCompat} that displays the settings page related
 * to the text and reading option in the SetupWizard.
 */
public class TextReadingPreferenceFragmentForSetupWizard extends TextReadingPreferenceFragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
        final String title = getContext().getString(
                R.string.accessibility_text_reading_options_title);
        final Drawable icon = getContext().getDrawable(R.drawable.ic_accessibility_visibility);
        icon.setTintList(Utils.getColorAttr(getContext(), android.R.attr.colorPrimary));
        AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(getContext(), layout, title,
                /* description= */ null, icon);

        updateResetButtonPadding();
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
            Bundle savedInstanceState) {
        final GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
        return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
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

    /**
     * Updates the padding of the reset button to meet for SetupWizard style.
     */
    private void updateResetButtonPadding() {
        final LayoutPreference resetPreference = (LayoutPreference) findPreference(RESET_KEY);
        final ViewGroup parentView =
                (ViewGroup) resetPreference.findViewById(R.id.reset_button).getParent();
        LayoutStyler.applyPartnerCustomizationLayoutPaddingStyle(parentView);
    }
}
