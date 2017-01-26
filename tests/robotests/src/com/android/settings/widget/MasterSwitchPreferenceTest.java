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

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.view.LayoutInflater;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class MasterSwitchPreferenceTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ShadowApplication.getInstance().getApplicationContext();
    }

    @Test
    public void createNewPreference_shouldSetLayout() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);

        assertThat(preference.getWidgetLayoutResource()).isEqualTo(
            R.layout.preference_widget_master_switch);
    }

    @Test
    public void setChecked_shouldUpdateButtonCheckedState() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = new PreferenceViewHolder(LayoutInflater.from(mContext)
            .inflate(R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.itemView.findViewById(R.id.switchWidget);
        preference.onBindViewHolder(holder);

        preference.setChecked(true);
        assertThat(toggle.isChecked()).isTrue();

        preference.setChecked(false);
        assertThat(toggle.isChecked()).isFalse();
    }

    @Test
    public void setSwitchEnabled_shouldUpdateButtonEnabledState() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = new PreferenceViewHolder(
            LayoutInflater.from(mContext).inflate(R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.itemView.findViewById(R.id.switchWidget);
        preference.onBindViewHolder(holder);

        preference.setSwitchEnabled(true);
        assertThat(toggle.isEnabled()).isTrue();

        preference.setSwitchEnabled(false);
        assertThat(toggle.isEnabled()).isFalse();
    }

    @Test
    public void toggleButtonOn_shouldNotifyChecked() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = new PreferenceViewHolder(
            LayoutInflater.from(mContext).inflate(R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.itemView.findViewById(R.id.switchWidget);
        final OnPreferenceChangeListener listener = mock(OnPreferenceChangeListener.class);
        preference.setOnPreferenceChangeListener(listener);
        preference.onBindViewHolder(holder);

        toggle.setChecked(true);
        verify(listener).onPreferenceChange(preference, true);
    }

    @Test
    public void toggleButtonOff_shouldNotifyUnchecked() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = new PreferenceViewHolder(
            LayoutInflater.from(mContext).inflate(R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.itemView.findViewById(R.id.switchWidget);
        final OnPreferenceChangeListener listener = mock(OnPreferenceChangeListener.class);
        preference.setChecked(true);
        preference.setOnPreferenceChangeListener(listener);
        preference.onBindViewHolder(holder);

        toggle.setChecked(false);
        verify(listener).onPreferenceChange(preference, false);
    }

    @Test
    public void setDisabledByAdmin_hasEnforcedAdmin_shouldDisableButton() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = new PreferenceViewHolder(
            LayoutInflater.from(mContext).inflate(R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.itemView.findViewById(R.id.switchWidget);
        toggle.setEnabled(true);
        preference.onBindViewHolder(holder);

        preference.setDisabledByAdmin(mock(EnforcedAdmin.class));
        assertThat(toggle.isEnabled()).isFalse();
    }

    @Test
    public void setDisabledByAdmin_noEnforcedAdmin_shouldEnaableButton() {
        final MasterSwitchPreference preference = new MasterSwitchPreference(mContext);
        final PreferenceViewHolder holder = new PreferenceViewHolder(
            LayoutInflater.from(mContext).inflate(R.layout.preference_widget_master_switch, null));
        final Switch toggle = (Switch) holder.itemView.findViewById(R.id.switchWidget);
        toggle.setEnabled(false);
        preference.onBindViewHolder(holder);

        preference.setDisabledByAdmin(null);
        assertThat(toggle.isEnabled()).isTrue();
    }
}
