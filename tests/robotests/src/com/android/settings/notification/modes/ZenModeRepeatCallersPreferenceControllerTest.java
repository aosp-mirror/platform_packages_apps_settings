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

import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_STARRED;
import static android.service.notification.ZenPolicy.STATE_DISALLOW;
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
public final class ZenModeRepeatCallersPreferenceControllerTest {

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
    public void testUpdateState_allCalls() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowCalls(PEOPLE_TYPE_ANYONE)
                        .build())
                .build();

        ZenModeRepeatCallersPreferenceController controller =
                new ZenModeRepeatCallersPreferenceController(mContext, "repeat", mBackend, 1);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
        verify(preference).setEnabled(false);
    }

    @Test
    public void testUpdateState_someCalls() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowCalls(PEOPLE_TYPE_STARRED)
                        .allowRepeatCallers(true)
                        .build())
                .build();

        ZenModeRepeatCallersPreferenceController controller =
                new ZenModeRepeatCallersPreferenceController(mContext, "repeat", mBackend, 1);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
        verify(preference).setEnabled(true);
    }

    @Test
    public void testOnPreferenceChange() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowRepeatCallers(true).build())
                .build();

        ZenModeRepeatCallersPreferenceController controller =
                new ZenModeRepeatCallersPreferenceController(mContext, "repeat", mBackend, 1);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getPriorityCategoryRepeatCallers())
                .isEqualTo(STATE_DISALLOW);
        assertThat(captor.getValue().getPolicy().getPriorityCategoryEvents())
                .isEqualTo(STATE_UNSET);
        assertThat(captor.getValue().getPolicy().getPriorityCallSenders())
                .isEqualTo(STATE_UNSET);
    }
}
