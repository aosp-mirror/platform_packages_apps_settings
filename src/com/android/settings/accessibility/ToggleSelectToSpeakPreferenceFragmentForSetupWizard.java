/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifPreferenceLayout;

public class ToggleSelectToSpeakPreferenceFragmentForSetupWizard
        extends InvisibleToggleAccessibilityServicePreferenceFragment {

    private boolean mToggleSwitchWasInitiallyChecked;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view instanceof GlifPreferenceLayout) {
            final GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
            final String title = getArguments().getString(AccessibilitySettings.EXTRA_TITLE);
            final String description = getContext().getString(R.string.select_to_speak_summary);
            final Drawable icon = getContext().getDrawable(R.drawable.ic_accessibility_visibility);
            AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(getContext(), layout, title,
                    description, icon);

            final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
            AccessibilitySetupWizardUtils.setPrimaryButton(getContext(), mixin, R.string.done,
                    () -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        }

        mToggleSwitchWasInitiallyChecked = mToggleServiceSwitchPreference.isChecked();
        if (mTopIntroPreference != null) {
            mTopIntroPreference.setVisible(false);
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
        return SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SELECT_TO_SPEAK;
    }

    @Override
    public void onStop() {
        // Log the final choice in value if it's different from the previous value.
        if (mToggleServiceSwitchPreference.isChecked() != mToggleSwitchWasInitiallyChecked) {
            mMetricsFeatureProvider.action(getContext(),
                    SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SELECT_TO_SPEAK,
                    mToggleServiceSwitchPreference.isChecked());
        }

        super.onStop();
    }
}
