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

import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public final class ZenModeDisplayLinkPreferenceControllerTest {

    private ZenModeDisplayLinkPreferenceController mController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    @Mock private ZenModesBackend mBackend;
    @Mock private ZenHelperBackend mHelperBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        mController = new ZenModeDisplayLinkPreferenceController(
                mContext, "something", mBackend, mHelperBackend);
    }

    @Test
    public void updateState_dnd_enabled() {
        Preference preference = mock(Preference.class);
        ZenMode dnd = TestModeBuilder.MANUAL_DND_ACTIVE;

        mController.updateState(preference, dnd);

        verify(preference).setEnabled(true);
    }

    @Test
    public void updateState_specialDnd_disabled() {
        Preference preference = mock(Preference.class);
        ZenMode specialDnd = TestModeBuilder.manualDnd(INTERRUPTION_FILTER_NONE, true);

        mController.updateState(preference, specialDnd);

        verify(preference).setEnabled(false);
    }

    @Test
    public void testUpdateState_disabled() {
        Preference preference = mock(Preference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setEnabled(false)
                .build();

        mController.updateState(preference, zenMode);

        verify(preference).setEnabled(false);
    }

    @Test
    public void testHasSummary() {
        Preference pref = mock(Preference.class);
        mController.updateZenMode(pref, TestModeBuilder.EXAMPLE);
        verify(pref).setSummary(any());
    }
}