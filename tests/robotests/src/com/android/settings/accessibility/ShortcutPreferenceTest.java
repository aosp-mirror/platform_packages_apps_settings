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

    private static final String TOGGLE_CLICKED = "toggle_clicked";
    private static final String SETTINGS_CLICKED = "settings_clicked";

    private ShortcutPreference mShortcutPreference;
    private PreferenceViewHolder mPreferenceViewHolder;
    private String mResult;

    private ShortcutPreference.OnClickCallback mListener =
            new ShortcutPreference.OnClickCallback() {
                @Override
                public void onToggleClicked(ShortcutPreference preference) {
                    mResult = TOGGLE_CLICKED;
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
    public void clickToggle_toggleClicked() {
        mShortcutPreference.onBindViewHolder(mPreferenceViewHolder);
        mShortcutPreference.setOnClickCallback(mListener);

        mPreferenceViewHolder.itemView.performClick();

        assertThat(mResult).isEqualTo(TOGGLE_CLICKED);
        assertThat(mShortcutPreference.isChecked()).isTrue();
    }

    @Test
    public void clickSettings_settingsClicked() {
        mShortcutPreference.onBindViewHolder(mPreferenceViewHolder);
        mShortcutPreference.setOnClickCallback(mListener);

        final View settings = mPreferenceViewHolder.itemView.findViewById(R.id.main_frame);
        settings.performClick();

        assertThat(mResult).isEqualTo(SETTINGS_CLICKED);
    }

    @Test
    public void setCheckedTrue_getToggleIsTrue() {
        mShortcutPreference.setChecked(true);

        assertThat(mShortcutPreference.isChecked()).isEqualTo(true);
    }
}
