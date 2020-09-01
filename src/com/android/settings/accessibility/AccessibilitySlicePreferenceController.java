/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.List;
import java.util.Set;

/**
 * PreferenceController for accessibility services to be used by Slices.
 * Wraps the common logic which enables accessibility services and checks their availability.
 * <p>
 * Should not be used in a {@link com.android.settings.dashboard.DashboardFragment}.
 */
public class AccessibilitySlicePreferenceController extends TogglePreferenceController {

    private final ComponentName mComponentName;

    private static final String EMPTY_STRING = "";

    public AccessibilitySlicePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mComponentName = ComponentName.unflattenFromString(getPreferenceKey());

        if (mComponentName == null) {
            throw new IllegalArgumentException(
                    "Illegal Component Name from: " + preferenceKey);
        }
    }

    @Override
    public CharSequence getSummary() {
        final AccessibilityServiceInfo serviceInfo = getAccessibilityServiceInfo();

        return serviceInfo == null ? EMPTY_STRING : AccessibilitySettings.getServiceSummary(
                mContext, serviceInfo, isChecked());
    }

    @Override
    public boolean isChecked() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        final boolean accessibilityEnabled = Settings.Secure.getInt(contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, OFF) == ON;

        if (!accessibilityEnabled) {
            return false;
        }

        final Set<ComponentName> componentNames =
                AccessibilityUtils.getEnabledServicesFromSettings(mContext);

        return componentNames.contains(mComponentName);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (getAccessibilityServiceInfo() == null) {
            return false;
        }
        AccessibilityUtils.setAccessibilityServiceState(mContext, mComponentName, isChecked);
        return isChecked == isChecked(); // Verify that it was probably changed.
    }

    @Override
    public int getAvailabilityStatus() {
        // Return unsupported when the service is disabled or not installed.
        return getAccessibilityServiceInfo() == null ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    private AccessibilityServiceInfo getAccessibilityServiceInfo() {
        final AccessibilityManager accessibilityManager = mContext.getSystemService(
                AccessibilityManager.class);
        final List<AccessibilityServiceInfo> serviceList =
                accessibilityManager.getInstalledAccessibilityServiceList();

        for (AccessibilityServiceInfo serviceInfo : serviceList) {
            if (mComponentName.equals(serviceInfo.getComponentName())) {
                return serviceInfo;
            }
        }

        return null;
    }
}
