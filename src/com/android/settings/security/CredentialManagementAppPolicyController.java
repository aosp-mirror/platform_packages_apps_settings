/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.security;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;

import android.content.Context;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

/**
 * Controller that shows the credential management app's authentication policy.
 */
public class CredentialManagementAppPolicyController extends BasePreferenceController {

    public CredentialManagementAppPolicyController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        PreferenceGroup group = screen.findPreference(getPreferenceKey());
        group.addPreference(new CredentialManagementAppPolicyPreference(group.getContext()));
    }
}
