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

import android.content.Context;

import com.android.settings.R;

/**
 * Preference controller for accessibility control timeout footer.
 */
public class AccessibilityControlTimeoutFooterPreferenceController extends
        AccessibilityFooterPreferenceController {

    public AccessibilityControlTimeoutFooterPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    protected String getLearnMoreText() {
        return mContext.getString(
            R.string.accessibility_control_timeout_footer_learn_more_content_description);
    }

    @Override
    protected String getIntroductionTitle() {
        return mContext.getString(R.string.accessibility_control_timeout_about_title);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_timeout;
    }
}
