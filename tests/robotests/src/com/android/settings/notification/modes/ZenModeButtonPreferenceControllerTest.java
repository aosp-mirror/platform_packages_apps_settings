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

import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;
import android.net.Uri;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;
import android.widget.Button;

import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@EnableFlags(Flags.FLAG_MODES_UI)
@RunWith(RobolectricTestRunner.class)
public final class ZenModeButtonPreferenceControllerTest {

    private ZenModeButtonPreferenceController mController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();


    private Context mContext;
    @Mock
    private ZenModesBackend mBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        mController = new ZenModeButtonPreferenceController(
                mContext, "something", mBackend);
    }

    @Test
    public void isAvailable_notIfAppOptsOut() {
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                .setType(AutomaticZenRule.TYPE_DRIVING)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                .setManualInvocationAllowed(false)
                .setEnabled(true)
                .build(), false);
        mController.setZenMode(zenMode);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_notIfModeDisabled() {
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                        .setManualInvocationAllowed(true)
                        .setEnabled(false)
                        .build(), false);
        mController.setZenMode(zenMode);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_appOptedIn_modeEnabled() {
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                        .setManualInvocationAllowed(true)
                        .setEnabled(true)
                        .build(), false);
        mController.setZenMode(zenMode);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_ruleActive() {
        Button button = new Button(mContext);
        LayoutPreference pref = mock(LayoutPreference.class);
        when(pref.findViewById(anyInt())).thenReturn(button);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                        .setManualInvocationAllowed(true)
                        .setEnabled(true)
                        .build(), true);
        mController.updateZenMode(pref, zenMode);
        assertThat(button.getText().toString()).contains("off");
        assertThat(button.hasOnClickListeners()).isTrue();
    }

    @Test
    public void updateState_ruleNotActive() {
        Button button = new Button(mContext);
        LayoutPreference pref = mock(LayoutPreference.class);
        when(pref.findViewById(anyInt())).thenReturn(button);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                        .setManualInvocationAllowed(true)
                        .setEnabled(true)
                        .build(), false);
        mController.updateZenMode(pref, zenMode);
        assertThat(button.getText().toString()).contains("on");
        assertThat(button.hasOnClickListeners()).isTrue();
    }

    @Test
    public void updateStateThenClick_ruleActive() {
        Button button = new Button(mContext);
        LayoutPreference pref = mock(LayoutPreference.class);
        when(pref.findViewById(anyInt())).thenReturn(button);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                        .setManualInvocationAllowed(true)
                        .setEnabled(true)
                        .build(), true);
        mController.updateZenMode(pref, zenMode);

        button.callOnClick();
        verify(mBackend).deactivateMode(zenMode);
    }

    @Test
    public void updateStateThenClick_ruleNotActive() {
        Button button = new Button(mContext);
        LayoutPreference pref = mock(LayoutPreference.class);
        when(pref.findViewById(anyInt())).thenReturn(button);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                        .setManualInvocationAllowed(true)
                        .setEnabled(true)
                        .build(), false);
        mController.updateZenMode(pref, zenMode);

        button.callOnClick();
        verify(mBackend).activateMode(zenMode, null);
    }
}