/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility.shortcuts;

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityButtonFragment;
import com.android.settings.accessibility.FloatingMenuSizePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.utils.AnnotationSpan;

/**
 * A base controller for the preference controller of software shortcuts.
 */
public abstract class SoftwareShortcutOptionPreferenceController
        extends ShortcutOptionPreferenceController {

    public SoftwareShortcutOptionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @ShortcutConstants.UserShortcutType
    @Override
    protected int getShortcutType() {
        return ShortcutConstants.UserShortcutType.SOFTWARE;
    }

    private boolean isMagnificationInTargets() {
        return getShortcutTargets().contains(MAGNIFICATION_CONTROLLER_NAME);
    }

    protected CharSequence getCustomizeAccessibilityButtonLink() {
        final View.OnClickListener linkListener = v -> new SubSettingLauncher(mContext)
                .setDestination(AccessibilityButtonFragment.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
        final AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(
                AnnotationSpan.LinkInfo.DEFAULT_ANNOTATION, linkListener);
        return AnnotationSpan.linkify(
                mContext.getText(
                        R.string.accessibility_shortcut_edit_dialog_summary_software_floating),
                linkInfo);
    }

    @Override
    protected void enableShortcutForTargets(boolean enable) {
        super.enableShortcutForTargets(enable);

        if (enable) {
            // Update the A11y FAB size to large when the Magnification shortcut is enabled
            // and the user hasn't changed the floating button size
            if (isMagnificationInTargets()
                    && Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE,
                    FloatingMenuSizePreferenceController.Size.UNKNOWN)
                    == FloatingMenuSizePreferenceController.Size.UNKNOWN) {
                Settings.Secure.putInt(mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE,
                        FloatingMenuSizePreferenceController.Size.LARGE);
            }
        }
    }
}
