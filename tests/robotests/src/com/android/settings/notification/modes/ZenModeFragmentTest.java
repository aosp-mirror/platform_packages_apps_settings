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

import static android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ZenModeFragmentTest {
    private static final String MODE_ID = "modeId";

    @Mock
    private ZenModesBackend mBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // set up static instance so that the fragment will get a mock version of the backend
        ZenModesBackend.setInstance(mBackend);
    }

    // Sets up the scenario's fragment by passing in arguments setting the provided mode ID.
    // After running this method, users can then use scenario.onFragment(fragment -> {...}) on the
    // returned scenario to test fragment behavior.
    private FragmentScenario<ZenModeFragment> setUpScenarioForModeId(String modeId) {
        Bundle args = new Bundle();
        args.putString(EXTRA_AUTOMATIC_ZEN_RULE_ID, modeId);
        return FragmentScenario.launch(
                ZenModeFragment.class, /* bundle= */ args, 0, Lifecycle.State.INITIALIZED);
    }

    @Test
    public void disabledMode_redirectsToInterstitial() {
        // Mode is disabled, and not by the user
        ZenMode mode = new TestModeBuilder().setId(MODE_ID).setEnabled(false, false)
                .build();

        when(mBackend.getMode(MODE_ID)).thenReturn(mode);

        // actually set up fragment for testing
        FragmentScenario scenario = setUpScenarioForModeId(MODE_ID);
        scenario.moveToState(Lifecycle.State.STARTED);

        scenario.onFragment(fragment -> {
            // since the mode is disabled & not by the user, we should go to the next activity
            Intent nextIntent = shadowOf(fragment.getActivity()).getNextStartedActivity();
            assertThat(nextIntent).isNotNull();
            assertThat(nextIntent.getComponent().getClassName()).isEqualTo(
                    SetupInterstitialActivity.class.getCanonicalName());
            assertThat(nextIntent.getStringExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID)).isEqualTo(MODE_ID);
        });
        scenario.close();
    }

    @Test
    public void disabledMode_byUser_noRedirect() {
        // Mode is disabled by the user
        ZenMode mode = new TestModeBuilder().setId(MODE_ID).setEnabled(false, true)
                .build();

        when(mBackend.getMode(MODE_ID)).thenReturn(mode);
        FragmentScenario scenario = setUpScenarioForModeId(MODE_ID);
        scenario.moveToState(Lifecycle.State.STARTED);

        scenario.onFragment(fragment -> {
            // there shouldn't be a next started activity, because we don't redirect
            Intent nextIntent = shadowOf(fragment.getActivity()).getNextStartedActivity();
            assertThat(nextIntent).isNull();
        });
        scenario.close();
    }

    @Test
    public void enabledMode_noRedirect() {
        // enabled rule
        ZenMode mode = new TestModeBuilder().setId(MODE_ID).setEnabled(true)
                .build();

        when(mBackend.getMode(MODE_ID)).thenReturn(mode);
        FragmentScenario scenario = setUpScenarioForModeId(MODE_ID);
        scenario.moveToState(Lifecycle.State.STARTED);

        scenario.onFragment(fragment -> {
            // there shouldn't be a next started activity, because we don't redirect
            Intent nextIntent = shadowOf(fragment.getActivity()).getNextStartedActivity();
            assertThat(nextIntent).isNull();
        });
        scenario.close();
    }
}
