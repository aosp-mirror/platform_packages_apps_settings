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
import android.content.Intent;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.HelpUtils;

/**
 * Base class for accessibility preference footer.
 */
public abstract class AccessibilityFooterPreferenceController extends BasePreferenceController {

    public AccessibilityFooterPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        final AccessibilityFooterPreference footerPreference =
                screen.findPreference(getPreferenceKey());
        updateFooterPreferences(footerPreference);
    }

    /**
     * Override this if showing a help item in the footer bar, by returning the resource id.
     *
     * @return the resource id for the help url
     */
    protected int getHelpResource() {
        return 0;
    }

    /** Returns the accessibility feature name. */
    protected abstract String getLabelName();

    private void updateFooterPreferences(AccessibilityFooterPreference footerPreference) {
        final StringBuffer sb = new StringBuffer();
        sb.append(mContext.getString(
                R.string.accessibility_introduction_title, getLabelName()))
                .append("\n\n")
                .append(footerPreference.getTitle());
        footerPreference.setContentDescription(sb);

        if (getHelpResource() != 0) {
            footerPreference.setLearnMoreAction(view -> {
                final Intent helpIntent = HelpUtils.getHelpIntent(
                        mContext, mContext.getString(getHelpResource()),
                        mContext.getClass().getName());
                view.startActivityForResult(helpIntent, 0);
            });

            final String learnMoreContentDescription = mContext.getString(
                    R.string.footer_learn_more_content_description, getLabelName());
            footerPreference.setLearnMoreContentDescription(learnMoreContentDescription);
        }
    }
}
