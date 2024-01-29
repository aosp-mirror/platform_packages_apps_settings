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

import androidx.annotation.StringRes;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.template.Mixin;
import com.google.android.setupdesign.GlifPreferenceLayout;
import com.google.android.setupdesign.R;
import com.google.android.setupdesign.util.ThemeHelper;

/** Provides utility methods to accessibility settings for Setup Wizard only. */
class AccessibilitySetupWizardUtils {

    private AccessibilitySetupWizardUtils(){}

    /**
     * Updates the {@link GlifPreferenceLayout} attributes if they have previously been initialized.
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

        if (ThemeHelper.shouldApplyMaterialYouStyle(context)) {
            final LinearLayout headerLayout = layout.findManagedViewById(R.id.sud_layout_header);
            if (headerLayout != null) {
                headerLayout.setPadding(0, layout.getPaddingTop(), 0,
                        layout.getPaddingBottom());
            }
        }
    }

    /**
     * Sets primary button for footer of the {@link GlifPreferenceLayout}.
     *
     * <p> This will be the initial by given material theme style.
     *
     * @param context A {@link Context}
     * @param mixin A {@link Mixin} for managing buttons.
     * @param text The {@code text} by resource.
     * @param runnable The {@link Runnable} to run.
     */
    public static void setPrimaryButton(Context context, FooterBarMixin mixin, @StringRes int text,
            Runnable runnable) {
        mixin.setPrimaryButton(
                new FooterButton.Builder(context)
                        .setText(text)
                        .setListener(l -> runnable.run())
                        .setButtonType(FooterButton.ButtonType.DONE)
                        .setTheme(R.style.SudGlifButton_Primary)
                        .build());
    }

    /**
     * Sets secondary button for the footer of the {@link GlifPreferenceLayout}.
     *
     * <p> This will be the initial by given material theme style.
     *
     * @param context A {@link Context}
     * @param mixin A {@link Mixin} for managing buttons.
     * @param text The {@code text} by resource.
     * @param runnable The {@link Runnable} to run.
     */
    public static void setSecondaryButton(Context context, FooterBarMixin mixin,
            @StringRes int text, Runnable runnable) {
        mixin.setSecondaryButton(
                new FooterButton.Builder(context)
                        .setText(text)
                        .setListener(l -> runnable.run())
                        .setButtonType(FooterButton.ButtonType.CLEAR)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build());
    }
}
