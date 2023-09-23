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
import android.text.method.MovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link AccessibilityFooterPreference} */
@RunWith(RobolectricTestRunner.class)
public final class AccessibilityFooterPreferenceTest {

    private AccessibilityFooterPreference mAccessibilityFooterPreference;
    private PreferenceViewHolder mPreferenceViewHolder;

    @Before
    public void setUp() {
        final Context context = RuntimeEnvironment.application;
        mAccessibilityFooterPreference = new AccessibilityFooterPreference(context);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view =
                inflater.inflate(com.android.settingslib.widget.preference.footer.R.layout.preference_footer, null);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @Test
    public void onBindViewHolder_LinkDisabledByDefault_notReturnLinkMovement() {
        mAccessibilityFooterPreference.onBindViewHolder(mPreferenceViewHolder);

        final TextView summaryView = (TextView) mPreferenceViewHolder.findViewById(
                android.R.id.title);
        assertThat(summaryView.getMovementMethod()).isNull();
    }

    @Test
    public void onBindViewHolder_setLinkEnabled_returnLinkMovement() {
        mAccessibilityFooterPreference.setLinkEnabled(true);

        mAccessibilityFooterPreference.onBindViewHolder(mPreferenceViewHolder);

        final TextView summaryView = (TextView) mPreferenceViewHolder.findViewById(
                android.R.id.title);
        assertThat(summaryView.getMovementMethod()).isInstanceOf(MovementMethod.class);
    }

    @Test
    public void onBindViewHolder_setLinkEnabled_expectSummaryViewIsNonFocusable() {
        mAccessibilityFooterPreference.setLinkEnabled(true);

        mAccessibilityFooterPreference.onBindViewHolder(mPreferenceViewHolder);

        final TextView summaryView = (TextView) mPreferenceViewHolder.findViewById(
                android.R.id.title);
        assertThat(summaryView.isFocusable()).isFalse();
    }
}
