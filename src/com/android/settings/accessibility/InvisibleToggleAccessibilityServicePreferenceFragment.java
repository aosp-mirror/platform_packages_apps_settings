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
}
