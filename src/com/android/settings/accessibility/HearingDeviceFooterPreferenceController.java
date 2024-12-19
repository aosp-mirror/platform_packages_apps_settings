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
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

/** Preference controller for footer in hearing device page. */
public class HearingDeviceFooterPreferenceController extends
        AccessibilityFooterPreferenceController {
    public HearingDeviceFooterPreferenceController(Context context,
            String key) {
        super(context, key);
    }

    @Override
    protected String getIntroductionTitle() {
        return mContext.getString(R.string.accessibility_hearing_device_about_title);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);

        final AccessibilityFooterPreference footerPreference =
                screen.findPreference(getPreferenceKey());
        // We use html tag inside footer string, so it is better to load from html to have better
        // html tag support.
        final CharSequence title = Html.fromHtml(
                mContext.getString(R.string.accessibility_hearing_device_footer_summary),
                Html.FROM_HTML_MODE_COMPACT, /* imageGetter= */ null, /* tagHandler= */ null);
        footerPreference.setTitle(title);

        // Need to update contentDescription string to announce "than" rather than ">"
        final String summaryTts = mContext.getString(
                R.string.accessibility_hearing_device_footer_summary_tts);
        final String contentDescription = getIntroductionTitle() + "\n\n" + summaryTts;
        footerPreference.setContentDescription(contentDescription);
    }
}
