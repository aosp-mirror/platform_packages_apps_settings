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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link AccessibilityFooterPreferenceController}.
 */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityFooterPreferenceControllerTest {

    private static final String TEST_KEY = "test_pref_key";
    private static final String TEST_TITLE = "test_title";
    private static final String TEST_INTRODUCTION_TITLE = "test_introduction_title";
    private static final String TEST_CONTENT_DESCRIPTION = "test_content_description";
    private static final int TEST_HELP_ID = 12345;

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private PreferenceScreen mScreen;
    private AccessibilityFooterPreferenceController mController;
    private AccessibilityFooterPreference mPreference;
    private PreferenceViewHolder mPreferenceViewHolder;

    @Before
    public void setUp() {
        mController = new AccessibilityFooterPreferenceController(mContext, TEST_KEY);
        mPreference = new AccessibilityFooterPreference(mContext);
        mPreference.setKey(TEST_KEY);
        mPreference.setTitle(TEST_TITLE);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(
                com.android.settingslib.widget.preference.footer.R.layout.preference_footer, null);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(view);
        mPreference.onBindViewHolder(mPreferenceViewHolder);

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void setIntroductionTitle_setCorrectIntroductionTitle() {
        mController.setIntroductionTitle(TEST_INTRODUCTION_TITLE);

        assertThat(mController.getIntroductionTitle()).isEqualTo(TEST_INTRODUCTION_TITLE);
    }

    @Test
    public void onBindViewHolder_setIntroductionTitle_setCorrectIntroductionTitle() {
        mController.setIntroductionTitle(TEST_INTRODUCTION_TITLE);
        mController.displayPreference(mScreen);

        mPreference.onBindViewHolder(mPreferenceViewHolder);

        final TextView summaryView = (TextView) mPreferenceViewHolder
                .findViewById(android.R.id.title);
        assertThat(summaryView.getContentDescription().toString())
                .contains(TEST_INTRODUCTION_TITLE);
    }

    @Test
    public void setupHelpLink_setCorrectHelpLinkAndLearnMoreText() {
        mController.setupHelpLink(TEST_HELP_ID, TEST_CONTENT_DESCRIPTION);

        assertThat(mController.getHelpResource()).isEqualTo(TEST_HELP_ID);
        assertThat(mController.getLearnMoreText())
                .isEqualTo(TEST_CONTENT_DESCRIPTION);
    }

    @Test
    public void onBindViewHolder_setHelpResource_emptyString_notVisible() {
        mController.setupHelpLink(R.string.help_url_timeout, TEST_CONTENT_DESCRIPTION);
        mController.displayPreference(mScreen);

        mPreference.onBindViewHolder(mPreferenceViewHolder);

        final TextView learnMoreView = (TextView) mPreferenceViewHolder
                .findViewById(com.android.settingslib.widget.preference.footer.R.id.settingslib_learn_more);
        assertThat(learnMoreView.getContentDescription()).isNull();
        assertThat(learnMoreView.getVisibility()).isEqualTo(View.GONE);
        assertThat(mPreference.isLinkEnabled()).isFalse();
    }

    @Test
    public void onBindViewHolder_setHelpResource_expectSummaryViewIsNonFocusable() {
        mController.setupHelpLink(R.string.help_url_timeout, TEST_CONTENT_DESCRIPTION);
        mController.displayPreference(mScreen);

        mPreference.onBindViewHolder(mPreferenceViewHolder);

        final TextView summaryView = (TextView) mPreferenceViewHolder
                .findViewById(android.R.id.title);
        assertThat(summaryView.isFocusable()).isFalse();
    }
}
