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
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.display.AutoBrightnessSettings;
import com.android.settingslib.Utils;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifPreferenceLayout;

/**
 * Fragment for adaptive brightness settings in the SetupWizard.
 */
public class AutoBrightnessPreferenceFragmentForSetupWizard extends AutoBrightnessSettings {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
}
