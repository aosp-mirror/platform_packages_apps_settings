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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link AccessibilityFooterPreference} */
@RunWith(RobolectricTestRunner.class)
public final class AccessibilityFooterPreferenceTest {

    private static final String DEFAULT_SUMMARY = "default summary";
    private static final String DEFAULT_DESCRIPTION = "default description";

    private Context mContext = ApplicationProvider.getApplicationContext();
    private AccessibilityFooterPreference mAccessibilityFooterPreference;
    private PreferenceViewHolder mPreferenceViewHolder;

    @Before
    public void setUp() {
        final Context context = RuntimeEnvironment.application;
        mAccessibilityFooterPreference = new AccessibilityFooterPreference(context);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view =
                inflater.inflate(R.layout.preference_footer, null);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @Test
    public void onBindViewHolder_initTextConfig_parseTextAndFocusable() {
        mAccessibilityFooterPreference.setSummary(DEFAULT_SUMMARY);

        mAccessibilityFooterPreference.onBindViewHolder(mPreferenceViewHolder);

        final TextView summaryView = (TextView) mPreferenceViewHolder.findViewById(
                android.R.id.title);
        assertThat(summaryView.getText().toString()).isEqualTo(DEFAULT_SUMMARY);
        assertThat(summaryView.isFocusable()).isEqualTo(true);
    }

    @Test
    public void onBindViewHolder_initTextConfigAndAccessibleIcon_groupContentForAccessible() {
        mAccessibilityFooterPreference.setSummary(DEFAULT_SUMMARY);
        mAccessibilityFooterPreference.setIconContentDescription(DEFAULT_DESCRIPTION);

        mAccessibilityFooterPreference.onBindViewHolder(mPreferenceViewHolder);

        final TextView summaryView = (TextView) mPreferenceViewHolder.findViewById(
                android.R.id.title);
        assertThat(summaryView.getText().toString()).isEqualTo(DEFAULT_SUMMARY);
        assertThat(summaryView.isFocusable()).isEqualTo(false);
        final LinearLayout infoFrame = (LinearLayout) mPreferenceViewHolder.findViewById(
                R.id.icon_frame);
        assertThat(infoFrame.getContentDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(infoFrame.isFocusable()).isEqualTo(false);
    }

    @Test
    public void appendHelpLink_timeoutHelpUri_updateSummary() {
        mAccessibilityFooterPreference.setSummary(DEFAULT_SUMMARY);

        mAccessibilityFooterPreference.appendHelpLink(R.string.help_url_timeout);

        final String title = mAccessibilityFooterPreference.getTitle().toString();
        assertThat(title.contains(mContext.getString(R.string.footer_learn_more))).isTrue();
    }
}
