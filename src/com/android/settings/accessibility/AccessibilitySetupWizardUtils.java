/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.LinearLayout;

import com.android.settings.R;

import com.google.android.setupdesign.GlifPreferenceLayout;
import com.google.android.setupdesign.util.ThemeHelper;

/** Provides utility methods to accessibility settings for Setup Wizard only. */
class AccessibilitySetupWizardUtils {

    private AccessibilitySetupWizardUtils(){}

    /**
     * Update the {@link GlifPreferenceLayout} attributes if they have previously been initialized.
     * When the SetupWizard supports the extended partner configs, it means the material layout
     * would be applied. It should set a different padding/margin in views to align Settings style
     * for accessibility feature pages.
     *
     * @param layout The layout instance
     * @param title The text to be set as title
     * @param description The text to be set as description
     * @param icon The icon to be set
     */
    public static void updateGlifPreferenceLayout(Context context, GlifPreferenceLayout layout,
            CharSequence title, CharSequence description, Drawable icon) {
        layout.setHeaderText(title);
        layout.setDescriptionText(description);
        layout.setIcon(icon);
        layout.setDividerInsets(Integer.MAX_VALUE, 0);

        if (ThemeHelper.shouldApplyExtendedPartnerConfig(context)) {
            final LinearLayout headerLayout = layout.findManagedViewById(R.id.sud_layout_header);
            if (headerLayout != null) {
                headerLayout.setPadding(0, layout.getPaddingTop(), 0,
                        layout.getPaddingBottom());
            }
        }
    }
}
