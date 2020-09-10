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

package com.android.settings.notification.zen;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.notification.zen.ZenCustomRadioButtonPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ZenCustomRadioButtonPreferenceTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void createNewPreference_shouldSetLayout() {
        final ZenCustomRadioButtonPreference preference
                = new ZenCustomRadioButtonPreference(mContext);

        assertThat(preference.getLayoutResource()).isEqualTo(R.layout.preference_two_target_radio);
        assertThat(preference.getWidgetLayoutResource())
                .isEqualTo(R.layout.preference_widget_gear);
    }

    @Test
    public void setChecked_shouldUpdateButtonCheckedState() {
        final ZenCustomRadioButtonPreference preference =
                new ZenCustomRadioButtonPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                  R.layout.preference_two_target_radio, null));
        final RadioButton toggle = (RadioButton) holder.findViewById(android.R.id.checkbox);
        preference.onBindViewHolder(holder);

        preference.setChecked(true);
        assertThat(toggle.isChecked()).isTrue();

        preference.setChecked(false);
        assertThat(toggle.isChecked()).isFalse();
    }

    @Test
    public void clickRadioButton_shouldNotifyRadioButtonClicked() {
        final ZenCustomRadioButtonPreference preference
                = new ZenCustomRadioButtonPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(R.layout.preference_two_target_radio, null));
        final View toggle = holder.findViewById(R.id.checkbox_frame);

        ZenCustomRadioButtonPreference.OnRadioButtonClickListener l = mock(
                ZenCustomRadioButtonPreference.OnRadioButtonClickListener.class);
        preference.setOnRadioButtonClickListener(l);
        preference.onBindViewHolder(holder);

        toggle.performClick();
        verify(l).onRadioButtonClick(preference);
    }

    @Test
    public void clickWidgetView_shouldNotifyWidgetClicked() {
        final ZenCustomRadioButtonPreference preference =
                new ZenCustomRadioButtonPreference(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(R.layout.preference_two_target_radio, null));
        final View widgetView = holder.findViewById(android.R.id.widget_frame);

        ZenCustomRadioButtonPreference.OnGearClickListener l = mock(
                ZenCustomRadioButtonPreference.OnGearClickListener.class);
        preference.setOnGearClickListener(l);
        preference.onBindViewHolder(holder);

        widgetView.performClick();
        verify(l).onGearClick(preference);
    }
}
