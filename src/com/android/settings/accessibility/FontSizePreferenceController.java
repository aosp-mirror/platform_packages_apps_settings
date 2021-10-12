/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityScreenSizeForSetupWizardActivity.VISION_FRAGMENT_NO;
import static com.android.settings.core.SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE;

import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;

import com.android.settings.accessibility.AccessibilityScreenSizeForSetupWizardActivity.FragmentType;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.transition.SettingsTransitionHelper.TransitionType;

/** PreferenceController for displaying font size page. */
public class FontSizePreferenceController extends BasePreferenceController {

    public FontSizePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!mPreferenceKey.equals(preference.getKey())) {
            return false;
        }

        final Intent intent = new Intent(mContext,
                AccessibilityScreenSizeForSetupWizardActivity.class);
        intent.putExtra(VISION_FRAGMENT_NO, FragmentType.FONT_SIZE);
        intent.putExtra(EXTRA_PAGE_TRANSITION_TYPE, TransitionType.TRANSITION_FADE);
        mContext.startActivity(intent);
        return true;
    }
}
