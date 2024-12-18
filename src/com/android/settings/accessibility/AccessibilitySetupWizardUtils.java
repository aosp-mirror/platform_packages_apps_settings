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
import android.text.TextUtils;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.template.Mixin;
import com.google.android.setupcompat.util.ForceTwoPaneHelper;
import com.google.android.setupdesign.GlifPreferenceLayout;
import com.google.android.setupdesign.R;
import com.google.android.setupdesign.util.ThemeHelper;

/** Provides utility methods to accessibility settings for Setup Wizard only. */
public class AccessibilitySetupWizardUtils {

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
            @Nullable CharSequence title, @Nullable CharSequence description,
            @Nullable Drawable icon) {
        if (!TextUtils.isEmpty(title)) {
            layout.setHeaderText(title);
        }

        if (!TextUtils.isEmpty(description)) {
            layout.setDescriptionText(description);
        }

        if (icon != null) {
            layout.setIcon(icon);
        }
        layout.setDividerInsets(Integer.MAX_VALUE, 0);

        if (ThemeHelper.shouldApplyMaterialYouStyle(context)) {
            // For b/323771329#comment26, if the layout is forced-two-pan, we should not adjust the
            // headerLayout horizontal padding to 0, which will make an unexpected large padding on
            // two-pane layout.
            // TODO: This is just a short-term quick workaround for force-two-pane devices padding
            //  issue. The long-term goal here is to remove the header padding adjustment since it
            //  should be handled in setup design lib. (b/331878747)
            if (ForceTwoPaneHelper.shouldForceTwoPane(context)) {
                return;
            }

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
