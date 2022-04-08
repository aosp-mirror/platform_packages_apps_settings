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

import static com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Bundle;
import android.view.View;

import com.android.settings.R;

import com.google.common.collect.ImmutableSet;

/**
 * Fragment that only allowed hardware {@link UserShortcutType} for shortcut to open.
 *
 * <p>The child {@link ToggleAccessibilityServicePreferenceFragment} shows the actual UI for
 * providing basic accessibility service setup.
 *
 * <p>For accessibility services that target SDK <= Q.
 */
public class VolumeShortcutToggleAccessibilityServicePreferenceFragment extends
        ToggleAccessibilityServicePreferenceFragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final CharSequence hardwareTitle = getPrefContext().getText(
                R.string.accessibility_shortcut_edit_dialog_title_hardware);
        mShortcutPreference.setSummary(hardwareTitle);
        mShortcutPreference.setSettingsEditable(false);

        setAllowedPreferredShortcutType(UserShortcutType.HARDWARE);
    }

    @Override
    int getUserShortcutTypes() {
        int shortcutTypes = super.getUserShortcutTypes();
        final boolean isServiceOn =
                getArguments().getBoolean(AccessibilitySettings.EXTRA_CHECKED);
        final AccessibilityServiceInfo info = getAccessibilityServiceInfo();
        final boolean hasRequestAccessibilityButtonFlag =
                (info.flags & AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON) != 0;
        if (hasRequestAccessibilityButtonFlag && isServiceOn) {
            shortcutTypes |= UserShortcutType.SOFTWARE;
        } else {
            shortcutTypes &= (~UserShortcutType.SOFTWARE);
        }

        return shortcutTypes;
    }

    private void setAllowedPreferredShortcutType(int type) {
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(
                mComponentName.flattenToString(), type);

        SharedPreferenceUtils.setUserShortcutType(getPrefContext(),
                ImmutableSet.of(shortcut.flattenToString()));
    }
}
