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

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.HelpUtils;

/**
 * Preference controller that controls the help link and customizes the preference title in {@link
 * AccessibilityFooterPreference}.
 */
public class AccessibilityFooterPreferenceController extends BasePreferenceController {

    private int mHelpResource;
    private String mLearnMoreText;
    private String mIntroductionTitle;

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
     * Setups a help item in the {@link AccessibilityFooterPreference} with specific content
     * description.
     */
    public void setupHelpLink(int helpResource, String learnMoreText) {
        mHelpResource = helpResource;
        mLearnMoreText = learnMoreText;
    }

    /**
     * Overrides this if showing a help item in the {@link AccessibilityFooterPreference}, by
     * returning the resource id.
     *
     * @return the resource id for the help url
     */
    protected int getHelpResource() {
        return mHelpResource;
    }

    /**
     * Overrides this if showing a help item in the {@link AccessibilityFooterPreference} with
     * specific learn more title.
     *
     * @return learn more title for the help url
     */
    protected String getLearnMoreText() {
        return mLearnMoreText;
    }

    /**
     * Sets the announcement the specific features introduction in the {@link
     * AccessibilityFooterPreference}.
     */
    public void setIntroductionTitle(String introductionTitle) {
        mIntroductionTitle = introductionTitle;
    }

    /**
     * Overrides this if announcement the specific features introduction in the {@link
     * AccessibilityFooterPreference}.
     *
     * @return the extended content description for specific features introduction
     */
    protected String getIntroductionTitle() {
        return mIntroductionTitle;
    }

    private void updateFooterPreferences(AccessibilityFooterPreference footerPreference) {
        final StringBuffer sb = new StringBuffer();
        sb.append(getIntroductionTitle()).append("\n\n").append(footerPreference.getTitle());
        footerPreference.setContentDescription(sb);

        final Intent helpIntent;
        if (getHelpResource() != 0) {
            // Returns may be null if content is wrong or empty.
            helpIntent = HelpUtils.getHelpIntent(mContext, mContext.getString(getHelpResource()),
                    mContext.getClass().getName());
        } else {
            helpIntent = null;
        }

        if (helpIntent != null) {
            footerPreference.setLearnMoreAction(view -> {
                view.startActivityForResult(helpIntent, 0);
            });
            footerPreference.setLearnMoreText(getLearnMoreText());
            footerPreference.setLinkEnabled(true);
        } else {
            footerPreference.setLinkEnabled(false);
        }

        // Grouping subcomponents to make more accessible.
        footerPreference.setSelectable(false);
    }
}
