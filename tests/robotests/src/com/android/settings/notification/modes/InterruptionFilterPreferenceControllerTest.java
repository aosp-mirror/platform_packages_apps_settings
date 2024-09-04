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

import static android.app.NotificationManager.INTERRUPTION_FILTER_ALL;
import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;
import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;
import static android.service.notification.ZenPolicy.STATE_DISALLOW;

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
public final class InterruptionFilterPreferenceControllerTest {

    private InterruptionFilterPreferenceController mController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    @Mock private ZenModesBackend mBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mController = new InterruptionFilterPreferenceController(mContext, "something",  mBackend);
    }

    @Test
    public void updateState_dnd_enabled() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode dnd = TestModeBuilder.MANUAL_DND_ACTIVE;

        mController.updateState(preference, dnd);

        verify(preference).setEnabled(true);
    }

    @Test
    public void updateState_specialDnd_disabled() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode specialDnd = TestModeBuilder.manualDnd(INTERRUPTION_FILTER_NONE, true);

        mController.updateState(preference, specialDnd);

        verify(preference).setEnabled(false);
    }

    @Test
    public void testUpdateState_disabled() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setEnabled(false)
                .build();

        mController.updateZenMode(preference, zenMode);

        verify(preference).setEnabled(false);
    }

    @Test
    public void testUpdateState_all() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setInterruptionFilter(INTERRUPTION_FILTER_ALL)
                .build();
        mController.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_fromAll() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setInterruptionFilter(INTERRUPTION_FILTER_ALL)
                .build();

        mController.updateZenMode(preference, zenMode);

        mController.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityCategoryAlarms())
                .isEqualTo(STATE_DISALLOW);
        assertThat(captor.getValue().getRule().getInterruptionFilter())
                .isEqualTo(INTERRUPTION_FILTER_PRIORITY);
    }

    @Test
    public void testUpdateState_priority() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().allowAlarms(true).build())
                .build();
        mController.updateZenMode(preference, zenMode);

        verify(preference).setChecked(false);
    }

    @Test
    public void testOnPreferenceChange_fromPriority() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().allowAlarms(false).build())
                .build();

        mController.updateZenMode(preference, zenMode);

        mController.onPreferenceChange(preference, true);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityCategoryAlarms())
                .isEqualTo(STATE_DISALLOW);
        assertThat(captor.getValue().getRule().getInterruptionFilter())
                .isEqualTo(INTERRUPTION_FILTER_ALL);
    }
}