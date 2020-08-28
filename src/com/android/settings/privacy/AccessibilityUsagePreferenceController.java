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

package com.android.settings.privacy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.List;


public class AccessibilityUsagePreferenceController extends BasePreferenceController {

    private final AccessibilityManager mAccessibilityManager;
    @NonNull
    private List<AccessibilityServiceInfo> mEnabledServiceInfos;

    public AccessibilityUsagePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mAccessibilityManager = mContext.getSystemService(AccessibilityManager.class);
        mEnabledServiceInfos = mAccessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mEnabledServiceInfos = mAccessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        if (mEnabledServiceInfos.isEmpty()) {
            preference.setVisible(false);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mEnabledServiceInfos.isEmpty() ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getResources().getQuantityString(R.plurals.accessibility_usage_summary,
                mEnabledServiceInfos.size(), mEnabledServiceInfos.size());
    }
}
