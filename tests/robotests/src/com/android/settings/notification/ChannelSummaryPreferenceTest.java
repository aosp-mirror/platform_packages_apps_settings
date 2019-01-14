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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

@RunWith(RobolectricTestRunner.class)
public class ChannelSummaryPreferenceTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void createNewPreference_shouldSetLayout() {
        final ChannelSummaryPreference preference = new ChannelSummaryPreference(mContext);
        assertThat(preference.getLayoutResource()).isEqualTo(
                R.layout.preference_checkable_two_target);
        assertThat(preference.getWidgetLayoutResource()).isEqualTo(
                R.layout.zen_rule_widget);
    }

    @Test
    public void setChecked_shouldUpdateButtonCheckedState() {
        final ChannelSummaryPreference preference = new ChannelSummaryPreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.preference_checkable_two_target, null));
        final LinearLayout widgetView = holder.itemView.findViewById(R.id.checkbox_container);
        inflater.inflate(R.layout.preference_widget_checkbox, widgetView, true);
        final CheckBox toggle = (CheckBox) holder.findViewById(com.android.internal.R.id.checkbox);
        preference.onBindViewHolder(holder);

        preference.setChecked(true);
        assertThat(toggle.isChecked()).isTrue();

        preference.setChecked(false);
        assertThat(toggle.isChecked()).isFalse();
    }

    @Test
    public void setCheckboxEnabled_shouldUpdateButtonEnabledState() {
        final ChannelSummaryPreference preference = new ChannelSummaryPreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.preference_checkable_two_target, null));
        final LinearLayout widgetView = holder.itemView.findViewById(R.id.checkbox_container);
        inflater.inflate(R.layout.preference_widget_checkbox, widgetView, true);
        final CheckBox toggle = (CheckBox) holder.findViewById(com.android.internal.R.id.checkbox);
        preference.onBindViewHolder(holder);

        preference.setCheckBoxEnabled(true);
        assertThat(toggle.isEnabled()).isTrue();

        preference.setCheckBoxEnabled(false);
        assertThat(toggle.isEnabled()).isFalse();
    }

    @Test
    public void setCheckBoxEnabled_shouldUpdateButtonEnabledState_beforeViewBound() {
        final ChannelSummaryPreference preference = new ChannelSummaryPreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.preference_checkable_two_target, null));
        final LinearLayout widgetView = holder.itemView.findViewById(R.id.checkbox_container);
        inflater.inflate(R.layout.preference_widget_checkbox, widgetView, true);
        final CheckBox toggle = (CheckBox) holder.findViewById(com.android.internal.R.id.checkbox);

        preference.setCheckBoxEnabled(false);
        preference.onBindViewHolder(holder);
        assertThat(toggle.isEnabled()).isFalse();
    }

    @Test
    public void clickWidgetView_shouldToggleButton() {
        final ChannelSummaryPreference preference = new ChannelSummaryPreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.preference_checkable_two_target, null));
        final LinearLayout widgetView = holder.itemView.findViewById(R.id.checkbox_container);
        assertThat(widgetView).isNotNull();

        inflater.inflate(R.layout.preference_widget_checkbox, widgetView, true);
        final CheckBox toggle = (CheckBox) holder.findViewById(com.android.internal.R.id.checkbox);
        preference.onBindViewHolder(holder);

        widgetView.performClick();
        assertThat(toggle.isChecked()).isTrue();

        widgetView.performClick();
        assertThat(toggle.isChecked()).isFalse();
    }

    @Test
    public void clickWidgetView_shouldNotToggleButtonIfDisabled() {
        final ChannelSummaryPreference preference = new ChannelSummaryPreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.preference_checkable_two_target, null));
        final LinearLayout widgetView = holder.itemView.findViewById(R.id.checkbox_container);
        assertThat(widgetView).isNotNull();

        inflater.inflate(R.layout.preference_widget_checkbox, widgetView, true);
        final CheckBox toggle = (CheckBox) holder.findViewById(com.android.internal.R.id.checkbox);
        preference.onBindViewHolder(holder);
        toggle.setEnabled(false);

        widgetView.performClick();
        assertThat(toggle.isChecked()).isFalse();
    }

    @Test
    public void clickWidgetView_shouldNotifyPreferenceChanged() {
        final ChannelSummaryPreference preference = new ChannelSummaryPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                        R.layout.preference_checkable_two_target, null));
        final View widgetView = holder.findViewById(R.id.checkbox_container);
        final Preference.OnPreferenceChangeListener
                listener = mock(Preference.OnPreferenceChangeListener.class);
        preference.setOnPreferenceChangeListener(listener);
        preference.onBindViewHolder(holder);

        preference.setChecked(false);
        widgetView.performClick();
        verify(listener).onPreferenceChange(preference, true);

        preference.setChecked(true);
        widgetView.performClick();
        verify(listener).onPreferenceChange(preference, false);
    }
}
