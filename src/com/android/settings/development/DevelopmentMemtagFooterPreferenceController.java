/*
 * Copyright (C) 2022 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.security;

import android.content.Context;
import android.text.TextUtils;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

/** Footer for face settings showing the help text and help link. */
public class DevelopmentMemtagFooterPreferenceController extends BasePreferenceController {

    public DevelopmentMemtagFooterPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        // Set up learn more link.
        FooterPreference prefFooter = screen.findPreference(getPreferenceKey());
        String helpUrl = mContext.getString(R.string.help_url_development_memtag);
        if (prefFooter != null && !TextUtils.isEmpty(helpUrl)) {
            prefFooter.setLearnMoreAction(
                    v ->
                            mContext.startActivity(
                                    HelpUtils.getHelpIntent(
                                            mContext, helpUrl, /* backupContext= */ "")));
            prefFooter.setLearnMoreText(mContext.getString(R.string.development_memtag_learn_more));
        }
    }
}
