/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class MasterSwitchPreferenceTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void createNewPreference_shouldSetLayout() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);

        assertThat(preference.getWidgetLayoutResource())
                .isEqualTo(R.layout.preference_widget_master_switch);
    }

    @Test
    public void setChecked_shouldUpdateButtonCheckedState() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                  R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.findViewById(R.id.switchWidget);
        preference.onBindViewHolder(holder);

        preference.setChecked(true);
        assertThat(toggle.isChecked()).isTrue();

        preference.setChecked(false);
        assertThat(toggle.isChecked()).isFalse();
    }

    @Test
    public void setSwitchEnabled_shouldUpdateButtonEnabledState() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                  R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.findViewById(R.id.switchWidget);
        preference.onBindViewHolder(holder);

        preference.setSwitchEnabled(true);
        assertThat(toggle.isEnabled()).isTrue();

        preference.setSwitchEnabled(false);
        assertThat(toggle.isEnabled()).isFalse();
    }

    @Test
    public void setSwitchEnabled_shouldUpdateButtonEnabledState_beforeViewBound() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                        R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.findViewById(R.id.switchWidget);

        preference.setSwitchEnabled(false);
        preference.onBindViewHolder(holder);
        assertThat(toggle.isEnabled()).isFalse();
    }

    @Test
    public void clickWidgetView_shouldToggleButton() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.preference_two_target, null));
        final LinearLayout widgetView = holder.itemView.findViewById(android.R.id.widget_frame);
        assertThat(widgetView).isNotNull();

        inflater.inflate(R.layout.preference_widget_master_switch, widgetView, true);
        final Switch toggle = (Switch) holder.findViewById(R.id.switchWidget);
        preference.onBindViewHolder(holder);

        widgetView.performClick();
        assertThat(toggle.isChecked()).isTrue();

        widgetView.performClick();
        assertThat(toggle.isChecked()).isFalse();
    }

    @Test
    public void clickWidgetView_shouldNotToggleButtonIfDisabled() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.preference_two_target, null));
        final LinearLayout widgetView = holder.itemView.findViewById(android.R.id.widget_frame);
        assertThat(widgetView).isNotNull();

        inflater.inflate(R.layout.preference_widget_master_switch, widgetView, true);
        final Switch toggle = (Switch) holder.findViewById(R.id.switchWidget);
        preference.onBindViewHolder(holder);
        toggle.setEnabled(false);

        widgetView.performClick();
        assertThat(toggle.isChecked()).isFalse();
    }

    @Test
    public void clickWidgetView_shouldNotifyPreferenceChanged() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(R.layout.preference_two_target, null));
        final View widgetView = holder.findViewById(android.R.id.widget_frame);
        final OnPreferenceChangeListener listener = mock(OnPreferenceChangeListener.class);
        preference.setOnPreferenceChangeListener(listener);
        preference.onBindViewHolder(holder);

        preference.setChecked(false);
        widgetView.performClick();
        verify(listener).onPreferenceChange(preference, true);

        preference.setChecked(true);
        widgetView.performClick();
        verify(listener).onPreferenceChange(preference, false);
    }

    @Test
    public void setDisabledByAdmin_hasEnforcedAdmin_shouldDisableButton() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext)
                  .inflate(R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.findViewById(R.id.switchWidget);
        toggle.setEnabled(true);
        preference.onBindViewHolder(holder);

        preference.setDisabledByAdmin(mock(EnforcedAdmin.class));
        assertThat(toggle.isEnabled()).isFalse();
    }

    @Test
    public void setDisabledByAdmin_noEnforcedAdmin_shouldEnableButton() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext)
                  .inflate(R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.findViewById(R.id.switchWidget);
        toggle.setEnabled(false);
        preference.onBindViewHolder(holder);

        preference.setDisabledByAdmin(null);
        assertThat(toggle.isEnabled()).isTrue();
    }

    @Test
    public void onBindViewHolder_toggleButtonShouldHaveContentDescription() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
            LayoutInflater.from(mContext)
                .inflate(R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.findViewById(R.id.switchWidget);
        final String label = "TestButton";
        preference.setTitle(label);

        preference.onBindViewHolder(holder);

        assertThat(toggle.getContentDescription()).isEqualTo(label);
    }
}
