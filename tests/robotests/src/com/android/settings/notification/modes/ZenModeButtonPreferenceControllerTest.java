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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.ContentResolver;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.Duration;

@EnableFlags(Flags.FLAG_MODES_UI)
@RunWith(RobolectricTestRunner.class)
public final class ZenModeButtonPreferenceControllerTest {

    private ZenModeButtonPreferenceController mController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ContentResolver mContentResolver;

    @Mock
    private ZenModesBackend mBackend;

    @Mock
    private Fragment mParent;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mContentResolver = RuntimeEnvironment.application.getContentResolver();

        mController = new ZenModeButtonPreferenceController(
                mContext, "something", mParent, mBackend);
    }

    @Test
    public void isAvailable_notIfAppOptsOut() {
        ZenMode zenMode = new TestModeBuilder()
                .setManualInvocationAllowed(false)
                .build();
        mController.setZenMode(zenMode);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_ifModeActiveEvenIfAppOptsOut() {
        ZenMode zenMode = new TestModeBuilder()
                .setManualInvocationAllowed(false)
                .setActive(true)
                .build();
        mController.setZenMode(zenMode);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notIfModeDisabled() {
        ZenMode zenMode = new TestModeBuilder()
                .setManualInvocationAllowed(true)
                .setEnabled(false)
                .build();

        mController.setZenMode(zenMode);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_appOptedIn_modeEnabled() {
        ZenMode zenMode = new TestModeBuilder()
                .setManualInvocationAllowed(true)
                .setEnabled(true)
                .build();

        mController.setZenMode(zenMode);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_ruleActive() {
        Button button = new Button(mContext);
        LayoutPreference pref = mock(LayoutPreference.class);
        when(pref.findViewById(anyInt())).thenReturn(button);
        ZenMode zenMode = new TestModeBuilder()
                .setActive(true)
                .build();

        mController.updateZenMode(pref, zenMode);

        assertThat(button.getText().toString()).contains("off");
        assertThat(button.hasOnClickListeners()).isTrue();
    }

    @Test
    public void updateState_ruleNotActive() {
        Button button = new Button(mContext);
        LayoutPreference pref = mock(LayoutPreference.class);
        when(pref.findViewById(anyInt())).thenReturn(button);
        ZenMode zenMode = new TestModeBuilder()
                .setManualInvocationAllowed(true)
                .setActive(false)
                .build();

        mController.updateZenMode(pref, zenMode);

        assertThat(button.getText().toString()).contains("on");
        assertThat(button.hasOnClickListeners()).isTrue();
    }

    @Test
    public void updateStateThenClick_ruleActive() {
        Button button = new Button(mContext);
        LayoutPreference pref = mock(LayoutPreference.class);
        when(pref.findViewById(anyInt())).thenReturn(button);
        ZenMode zenMode = new TestModeBuilder()
                .setActive(true)
                .build();

        mController.updateZenMode(pref, zenMode);

        button.callOnClick();
        verify(mBackend).deactivateMode(zenMode);
    }

    @Test
    public void updateStateThenClick_ruleNotActive() {
        Button button = new Button(mContext);
        LayoutPreference pref = mock(LayoutPreference.class);
        when(pref.findViewById(anyInt())).thenReturn(button);
        ZenMode zenMode = new TestModeBuilder()
                .setManualInvocationAllowed(true)
                .setActive(false)
                .build();

        mController.updateZenMode(pref, zenMode);

        button.callOnClick();
        verify(mBackend).activateMode(zenMode, null);
    }

    @Test
    public void updateStateThenClick_withDuration() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ZEN_DURATION,
                45 /* minutes */);
        Button button = new Button(mContext);
        LayoutPreference pref = mock(LayoutPreference.class);
        when(pref.findViewById(anyInt())).thenReturn(button);
        ZenMode zenMode = TestModeBuilder.MANUAL_DND_INACTIVE;

        mController.updateZenMode(pref, zenMode);
        button.callOnClick();
        verify(mBackend).activateMode(zenMode, Duration.ofMinutes(45));
    }

    @Test
    public void updateStateThenClick_durationForever() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ZEN_DURATION,
                Settings.Secure.ZEN_DURATION_FOREVER);
        Button button = new Button(mContext);
        LayoutPreference pref = mock(LayoutPreference.class);
        when(pref.findViewById(anyInt())).thenReturn(button);
        ZenMode zenMode = TestModeBuilder.MANUAL_DND_INACTIVE;

        mController.updateZenMode(pref, zenMode);
        button.callOnClick();
        verify(mBackend).activateMode(zenMode, null);
    }
}