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

package com.android.settings.accessibility;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.gestures.OneHandedEnablePreferenceController;
import com.android.settings.gestures.OneHandedSettingsUtils;

/**
 * OneHandedPreferenceController is the same as {@link OneHandedEnablePreferenceController} excepts
 * that the summary shown on the preference item would include the short description of One-handed
 * mode, so that the UI representation is consistent with other items on Accessibility Settings
 */
public final class OneHandedPreferenceController extends OneHandedEnablePreferenceController {

    public OneHandedPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(
                R.string.preference_summary_default_combination,
                mContext.getText(OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)
                        ? R.string.gesture_setting_on : R.string.gesture_setting_off),
                mContext.getText(R.string.one_handed_mode_intro_text));
    }
}
