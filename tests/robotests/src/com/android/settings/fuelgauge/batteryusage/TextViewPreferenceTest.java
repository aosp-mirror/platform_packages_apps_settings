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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class TextViewPreferenceTest {

    private Context mContext;
    private TextViewPreference mTextViewPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mTextViewPreference = new TextViewPreference(mContext, /* attrs= */ null);
    }

    @Test
    public void constructor_returnExpectedResult() {
        assertThat(mTextViewPreference.getLayoutResource())
                .isEqualTo(R.layout.preference_text_view);
    }

    @Test
    public void setText_returnExpectedResult() {
        final String text = "TEST_TEXT";
        mTextViewPreference.setText(text);

        assertThat(mTextViewPreference.mText.toString()).isEqualTo(text);
    }
}
