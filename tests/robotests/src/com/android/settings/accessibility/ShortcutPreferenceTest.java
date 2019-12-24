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

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link ShortcutPreference} */
@RunWith(RobolectricTestRunner.class)
public class ShortcutPreferenceTest {

    private static final String CHECKBOX_CLICKED = "checkbox_clicked";
    private static final String SETTINGS_CLICKED = "settings_clicked";

    private ShortcutPreference mShortcutPreference;
    private PreferenceViewHolder mPreferenceViewHolder;
    private String mResult;

    private ShortcutPreference.OnClickListener mListener =
            new ShortcutPreference.OnClickListener() {
                @Override
                public void onCheckboxClicked(ShortcutPreference preference) {
                    mResult = CHECKBOX_CLICKED;
                }

                @Override
                public void onSettingsClicked(ShortcutPreference preference) {
                    mResult = SETTINGS_CLICKED;
                }
            };

    @Before
    public void setUp() {
        final Context context = RuntimeEnvironment.application;
        mShortcutPreference = new ShortcutPreference(context, null);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view =
                inflater.inflate(R.layout.accessibility_shortcut_secondary_action, null);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(view);
    }
    
    @Test
    public void testClickLinearLayout_checkboxClicked() {
        mShortcutPreference.onBindViewHolder(mPreferenceViewHolder);
        mShortcutPreference.setOnClickListener(mListener);

        LinearLayout mainFrame = mPreferenceViewHolder.itemView.findViewById(R.id.main_frame);
        mainFrame.performClick();

        assertThat(mResult).isEqualTo(CHECKBOX_CLICKED);
        assertThat(mShortcutPreference.getChecked()).isTrue();
    }

    @Test
    public void testClickSettings_settingsClicked() {
        mShortcutPreference.onBindViewHolder(mPreferenceViewHolder);
        mShortcutPreference.setOnClickListener(mListener);

        View settings = mPreferenceViewHolder.itemView.findViewById(android.R.id.widget_frame);
        settings.performClick();

        assertThat(mResult).isEqualTo(SETTINGS_CLICKED);
    }

    @Test
    public void testSetCheckedTrue_getCheckedIsTrue() {
        mShortcutPreference.setChecked(true);

        assertThat(mShortcutPreference.getChecked()).isEqualTo(true);
    }
}
