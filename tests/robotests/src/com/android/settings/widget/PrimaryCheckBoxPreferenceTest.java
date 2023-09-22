/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PrimaryCheckBoxPreferenceTest {

    private Context mContext;
    private PrimaryCheckBoxPreference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new PrimaryCheckBoxPreference(mContext);
    }

    @Test
    public void createNewPreference_shouldSetLayout() {
        assertThat(mPreference.getWidgetLayoutResource())
                .isEqualTo(R.layout.preference_widget_primary_checkbox);
    }

    @Test
    public void setChecked_shouldUpdateCheckBoxCheckedState() {
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                  R.layout.preference_widget_primary_checkbox, null));
        final CheckBox checkBox = (CheckBox) holder.findViewById(R.id.checkboxWidget);
        mPreference.onBindViewHolder(holder);

        mPreference.setChecked(true);
        assertThat(checkBox.isChecked()).isTrue();

        mPreference.setChecked(false);
        assertThat(checkBox.isChecked()).isFalse();
    }

    @Test
    public void setEnabled_shouldUpdateCheckBoxEnabledState() {
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                  R.layout.preference_widget_primary_checkbox, null));
        final CheckBox checkBox = (CheckBox) holder.findViewById(R.id.checkboxWidget);
        mPreference.onBindViewHolder(holder);

        mPreference.setEnabled(true);
        assertThat(checkBox.isEnabled()).isTrue();

        mPreference.setEnabled(false);
        assertThat(checkBox.isEnabled()).isFalse();
    }

    @Test
    public void setCheckboxEnabled_shouldOnlyUpdateCheckBoxEnabledState() {
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                        R.layout.preference_widget_primary_checkbox, null));
        final CheckBox checkBox = (CheckBox) holder.findViewById(R.id.checkboxWidget);
        mPreference.onBindViewHolder(holder);

        mPreference.setCheckBoxEnabled(false);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(checkBox.isEnabled()).isFalse();

        mPreference.setCheckBoxEnabled(true);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(checkBox.isEnabled()).isTrue();
    }

    @Test
    public void onBindViewHolder_shouldSetCheckboxEnabledState() {
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                        R.layout.preference_widget_primary_checkbox, null));
        final CheckBox checkBox = (CheckBox) holder.findViewById(R.id.checkboxWidget);

        mPreference.setCheckBoxEnabled(false);
        mPreference.onBindViewHolder(holder);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(checkBox.isEnabled()).isFalse();

        mPreference.setCheckBoxEnabled(true);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(checkBox.isEnabled()).isTrue();
    }

    @Test
    public void clickWidgetView_shouldToggleCheckBox() {
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(
                        com.android.settingslib.widget.preference.twotarget.R.layout.preference_two_target, null));
        final LinearLayout widgetView = holder.itemView.findViewById(android.R.id.widget_frame);
        assertThat(widgetView).isNotNull();

        inflater.inflate(R.layout.preference_widget_primary_checkbox, widgetView, true);
        final CheckBox checkBox = (CheckBox) holder.findViewById(R.id.checkboxWidget);
        mPreference.onBindViewHolder(holder);

        widgetView.performClick();
        assertThat(checkBox.isChecked()).isTrue();

        widgetView.performClick();
        assertThat(checkBox.isChecked()).isFalse();
    }

    @Test
    public void clickWidgetView_shouldNotToggleCheckBoxIfDisabled() {
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(
                        com.android.settingslib.widget.preference.twotarget.R.layout.preference_two_target, null));
        final LinearLayout widgetView = holder.itemView.findViewById(android.R.id.widget_frame);
        assertThat(widgetView).isNotNull();

        inflater.inflate(R.layout.preference_widget_primary_checkbox, widgetView, true);
        final CheckBox checkBox = (CheckBox) holder.findViewById(R.id.checkboxWidget);
        mPreference.onBindViewHolder(holder);
        mPreference.setEnabled(false);

        widgetView.performClick();
        assertThat(checkBox.isChecked()).isFalse();
    }

    @Test
    public void clickWidgetView_shouldNotifyPreferenceChanged() {
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                        com.android.settingslib.widget.preference.twotarget.R.layout.preference_two_target, null));
        final View widgetView = holder.findViewById(android.R.id.widget_frame);
        final OnPreferenceChangeListener listener = mock(OnPreferenceChangeListener.class);
        mPreference.setOnPreferenceChangeListener(listener);
        mPreference.onBindViewHolder(holder);

        mPreference.setChecked(false);
        widgetView.performClick();
        verify(listener).onPreferenceChange(mPreference, true);

        mPreference.setChecked(true);
        widgetView.performClick();
        verify(listener).onPreferenceChange(mPreference, false);
    }

    @Test
    public void onBindViewHolder_checkBoxShouldHaveContentDescription() {
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(R.layout.preference_widget_primary_checkbox,
                        null));
        final CheckBox checkBox = (CheckBox) holder.findViewById(R.id.checkboxWidget);
        final String label = "TestButton";
        mPreference.setTitle(label);

        mPreference.onBindViewHolder(holder);

        assertThat(checkBox.getContentDescription()).isEqualTo(label);
    }
}
