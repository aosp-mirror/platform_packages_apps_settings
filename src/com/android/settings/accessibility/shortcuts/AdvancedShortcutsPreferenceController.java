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

import com.android.settings.accessibility.ExpandablePreference;

import java.util.Set;

/**
 * A preference controller that controls an expandable preference that wraps
 * the advanced shortcut options.
 */
public class AdvancedShortcutsPreferenceController extends ShortcutOptionPreferenceController
        implements ExpandablePreference {

    private boolean mIsExpanded = false;

    public AdvancedShortcutsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected boolean isChecked() {
        return false;
    }

    @Override
    protected void enableShortcutForTargets(boolean enable) {
        // do nothing
    }

    @Override
    public int getAvailabilityStatus() {
        if (!isExpanded() && isShortcutAvailable()) {
            // "Advanced" is available when the user hasn't clicked on it
            return AVAILABLE_UNSEARCHABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void setExpanded(boolean expanded) {
        mIsExpanded = expanded;
    }

    @Override
    public boolean isExpanded() {
        return mIsExpanded;
    }

    @Override
    protected boolean isShortcutAvailable() {
        // Only Magnification has advanced shortcut options.
        Set<String> shortcutTargets = getShortcutTargets();
        return shortcutTargets.size() == 1
                && shortcutTargets.contains(MAGNIFICATION_CONTROLLER_NAME);
    }
}
