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

import android.content.Context;
import android.os.UserHandle;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.accessibility.AccessibilityUtils;

public class AccessibilityShortcutPreferenceController extends BasePreferenceController {
    public AccessibilityShortcutPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AccessibilityManager
                .getInstance(mContext).getInstalledAccessibilityServiceList().isEmpty()
                ? DISABLED_DEPENDENT_SETTING : AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        if (AccessibilityManager.getInstance(mContext)
                .getInstalledAccessibilityServiceList().isEmpty()) {
            return mContext.getString(R.string.accessibility_no_services_installed);
        } else {
            final boolean shortcutEnabled =
                    AccessibilityUtils.isShortcutEnabled(mContext, UserHandle.myUserId());
            return shortcutEnabled
                    ? AccessibilityShortcutPreferenceFragment.getServiceName(mContext)
                    : mContext.getString(R.string.accessibility_feature_state_off);
        }
    }
}
