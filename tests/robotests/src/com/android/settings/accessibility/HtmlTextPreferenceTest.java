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
import android.text.Editable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.xml.sax.XMLReader;

/** Tests for {@link HtmlTextPreference} */
@RunWith(RobolectricTestRunner.class)
public final class HtmlTextPreferenceTest {

    private HtmlTextPreference mHtmlTextPreference;
    private PreferenceViewHolder mPreferenceViewHolder;
    private String mHandledTag;
    private final Html.TagHandler mTagHandler = new Html.TagHandler() {
        @Override
        public void handleTag(boolean opening, String tag, Editable editable, XMLReader xmlReader) {
            mHandledTag = tag;
        }
    };

    @Before
    public void setUp() {
        final Context context = RuntimeEnvironment.application;
        mHtmlTextPreference = new HtmlTextPreference(context);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view =
                inflater.inflate(R.layout.preference_static_text, null);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @Test
    public void testTagHandler() {
        final String testStr = "<testTag>Real description</testTag>";

        mHtmlTextPreference.setSummary(testStr);
        mHtmlTextPreference.setTagHandler(mTagHandler);
        mHtmlTextPreference.onBindViewHolder(mPreferenceViewHolder);

        assertThat(mHandledTag).isEqualTo("testTag");
    }
}
