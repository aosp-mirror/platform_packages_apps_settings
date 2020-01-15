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
import android.os.Bundle;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.accessibility.AccessibilityUtils;

/**
 * For accessibility services that target SDK > Q, and
 * {@link AccessibilityServiceInfo#FLAG_REQUEST_ACCESSIBILITY_BUTTON}
 * is set.
 */
public class InvisibleToggleAccessibilityServicePreferenceFragment extends
        ToggleAccessibilityServicePreferenceFragment implements ShortcutPreference.OnClickListener{

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SwitchBar mSwitchBar = activity.getSwitchBar();
        mSwitchBar.hide();
    }

    /**
     * {@inheritDoc}
     *
     * Enables accessibility service only when user had allowed permission.
     */
    @Override
    public void onCheckboxClicked(ShortcutPreference preference) {
        super.onCheckboxClicked(preference);
        AccessibilityUtils.setAccessibilityServiceState(getContext(), mComponentName,
                getArguments().getBoolean(AccessibilitySettings.EXTRA_CHECKED));
    }

    /**
     * {@inheritDoc}
     *
     * Enables accessibility service when user clicks permission allow button.
     */
    @Override
    void onDialogButtonFromShortcutClicked(View view) {
        super.onDialogButtonFromShortcutClicked(view);
        if (view.getId() == R.id.permission_enable_allow_button) {
            AccessibilityUtils.setAccessibilityServiceState(getContext(), mComponentName,
                    true);
        }
    }
}
