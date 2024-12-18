/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.service.notification.ZenPolicy.STATE_ALLOW;
import static android.service.notification.ZenPolicy.STATE_UNSET;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;

import androidx.preference.TwoStatePreference;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public final class ZenModeOtherPreferenceControllerTest {

    private Context mContext;
    @Mock
    private ZenModesBackend mBackend;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testUpdateState_alarms() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowAlarms(true).build())
                .build();

        ZenModeOtherPreferenceController controller =
                new ZenModeOtherPreferenceController(mContext, "modes_category_alarm", mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_alarms() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowAlarms(false).build())
                .build();

        ZenModeOtherPreferenceController controller =
                new ZenModeOtherPreferenceController(mContext, "modes_category_alarm", mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, true);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityCategoryAlarms())
                .isEqualTo(STATE_ALLOW);
        assertThat(captor.getValue().getPolicy().getPriorityCategoryEvents())
                .isEqualTo(STATE_UNSET);
    }

    @Test
    public void testUpdateState_media() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowMedia(true).build())
                .build();

        ZenModeOtherPreferenceController controller =
                new ZenModeOtherPreferenceController(mContext, "modes_category_media", mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_media() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowMedia(false).build())
                .build();

        ZenModeOtherPreferenceController controller =
                new ZenModeOtherPreferenceController(mContext, "modes_category_media", mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, true);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityCategoryMedia())
                .isEqualTo(STATE_ALLOW);
        assertThat(captor.getValue().getPolicy().getPriorityCategoryEvents())
                .isEqualTo(STATE_UNSET);
    }

    @Test
    public void testUpdateState_system() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowSystem(true).build())
                .build();

        ZenModeOtherPreferenceController controller =
                new ZenModeOtherPreferenceController(mContext, "modes_category_system", mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_system() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowSystem(false).build())
                .build();

        ZenModeOtherPreferenceController controller =
                new ZenModeOtherPreferenceController(mContext, "modes_category_system", mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, true);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityCategorySystem())
                .isEqualTo(STATE_ALLOW);
        assertThat(captor.getValue().getPolicy().getPriorityCategoryEvents())
                .isEqualTo(STATE_UNSET);
    }

    @Test
    public void testUpdateState_reminders() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowReminders(true).build())
                .build();

        ZenModeOtherPreferenceController controller =
                new ZenModeOtherPreferenceController(mContext, "modes_category_reminders",
                        mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_reminders() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowReminders(false).build())
                .build();

        ZenModeOtherPreferenceController controller =
                new ZenModeOtherPreferenceController(mContext, "modes_category_reminders",
                        mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, true);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityCategoryReminders())
                .isEqualTo(STATE_ALLOW);
        assertThat(captor.getValue().getPolicy().getPriorityCategoryEvents())
                .isEqualTo(STATE_UNSET);
    }

    @Test
    public void testUpdateState_events() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowEvents(true).build())
                .build();

        ZenModeOtherPreferenceController controller =
                new ZenModeOtherPreferenceController(mContext, "modes_category_events", mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_events() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowEvents(false).build())
                .build();

        ZenModeOtherPreferenceController controller =
                new ZenModeOtherPreferenceController(mContext, "modes_category_events", mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, true);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityCategoryEvents())
                .isEqualTo(STATE_ALLOW);
        assertThat(captor.getValue().getPolicy().getPriorityCategoryAlarms())
                .isEqualTo(STATE_UNSET);
    }
}