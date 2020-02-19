/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.provider.Settings.Secure.NOTIFICATION_FEEDBACK_ENABLED;

import static com.android.settings.notification.AssistantFeedbackPreferenceController.OFF;
import static com.android.settings.notification.AssistantFeedbackPreferenceController.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AssistantFeedbackPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private AssistantFeedbackPreferenceController mController;
    private Preference mPreference;

    private static final String KEY = "asst_feedback_indicator";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new AssistantFeedbackPreferenceController(mContext, KEY);
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    public void testIsVisible() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_preferenceSetCheckedWhenSettingIsOn() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        final Context context = RuntimeEnvironment.application;
        Settings.Secure.putInt(context.getContentResolver(), NOTIFICATION_FEEDBACK_ENABLED, ON);

        mController = new AssistantFeedbackPreferenceController(context, KEY);
        mController.updateState(preference);

        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenSettingIsOff() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        final Context context = RuntimeEnvironment.application;
        Settings.Secure.putInt(context.getContentResolver(), NOTIFICATION_FEEDBACK_ENABLED, OFF);

        mController = new AssistantFeedbackPreferenceController(context, KEY);
        mController.updateState(preference);

        verify(preference).setChecked(false);
    }

    @Test
    public void isChecked_settingIsOff_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_FEEDBACK_ENABLED, OFF);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingIsOn_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_FEEDBACK_ENABLED, ON);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_setFalse_disablesSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_FEEDBACK_ENABLED, ON);

        mController.setChecked(false);
        int updatedValue = Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_FEEDBACK_ENABLED, -1);

        assertThat(updatedValue).isEqualTo(OFF);
    }

    @Test
    public void setChecked_setTrue_enablesSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_FEEDBACK_ENABLED, OFF);

        mController.setChecked(true);
        int updatedValue = Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_FEEDBACK_ENABLED, -1);

        assertThat(updatedValue).isEqualTo(ON);
    }
}
