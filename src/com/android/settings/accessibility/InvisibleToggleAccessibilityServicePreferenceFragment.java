/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.DialogInterface;
import android.view.View;

import com.android.settings.R;
import com.android.settingslib.accessibility.AccessibilityUtils;

/**
 * Fragment that does not have toggle bar to turn on service to use.
 *
 * <p>The child {@link ToggleAccessibilityServicePreferenceFragment} shows the actual UI for
 * providing basic accessibility service setup.
 *
 * <p>For accessibility services that target SDK > Q, and
 * {@link AccessibilityServiceInfo#FLAG_REQUEST_ACCESSIBILITY_BUTTON} is set.
 */
public class InvisibleToggleAccessibilityServicePreferenceFragment extends
        ToggleAccessibilityServicePreferenceFragment implements ShortcutPreference.OnClickCallback {

    @Override
    protected void onInstallSwitchPreferenceToggleSwitch() {
        super.onInstallSwitchPreferenceToggleSwitch();
        mToggleServiceSwitchPreference.setVisible(false);
    }

    /**
     * {@inheritDoc}
     *
     * Enables accessibility service only when user had allowed permission. Disables
     * accessibility service when shortcutPreference is unchecked.
     */
    @Override
    public void onToggleClicked(ShortcutPreference preference) {
        super.onToggleClicked(preference);
        boolean enabled = getArguments().getBoolean(AccessibilitySettings.EXTRA_CHECKED)
                && preference.isChecked();

        AccessibilityUtils.setAccessibilityServiceState(getContext(), mComponentName, enabled);
    }

    /**
     * {@inheritDoc}
     *
     * Enables accessibility service when user clicks permission allow button.
     */
    @Override
    void onDialogButtonFromShortcutToggleClicked(View view) {
        super.onDialogButtonFromShortcutToggleClicked(view);
        if (!android.view.accessibility.Flags.cleanupAccessibilityWarningDialog()) {
            if (view.getId() == R.id.permission_enable_allow_button) {
                AccessibilityUtils.setAccessibilityServiceState(getContext(), mComponentName,
                        true);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Enables accessibility service when user clicks permission allow button.
     */
    @Override
    void onAllowButtonFromShortcutToggleClicked() {
        super.onAllowButtonFromShortcutToggleClicked();
        if (android.view.accessibility.Flags.cleanupAccessibilityWarningDialog()) {
            AccessibilityUtils.setAccessibilityServiceState(getContext(), mComponentName, true);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Enables accessibility service when shortcutPreference is checked.
     */
    @Override
    protected void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        super.callOnAlertDialogCheckboxClicked(dialog, which);

        final boolean enabled = mShortcutPreference.isChecked();
        AccessibilityUtils.setAccessibilityServiceState(getContext(), mComponentName, enabled);
    }
}
