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

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;
import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Flags;
import android.content.Intent;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.internal.R;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeNewCustomFragmentTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule
    public ActivityScenarioRule<EmptyFragmentActivity> mActivityScenario =
            new ActivityScenarioRule<>(EmptyFragmentActivity.class);

    private Activity mActivity;
    private ZenModeNewCustomFragment mFragment;
    @Mock
    private ZenModesBackend mBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new ZenModeNewCustomFragment();
        mFragment.setBackend(mBackend); // before onAttach()

        mActivityScenario.getScenario().onActivity(activity -> {
            mActivity = activity;
            activity.getSupportFragmentManager().beginTransaction()
                    .add(mFragment, "tag").commitNow();
        });
    }

    @Test
    public void saveMode_addsCustomManualMode() {
        mFragment.setModeName("The first name");
        mFragment.setModeIcon(R.drawable.ic_zen_mode_type_theater);
        mFragment.setModeName("Actually no, this name");

        mFragment.saveMode();

        verify(mBackend).addCustomManualMode("Actually no, this name",
                R.drawable.ic_zen_mode_type_theater);
    }

    @Test
    public void saveMode_withoutEdits_addsModeWithDefaultValues() {
        mFragment.saveMode();

        verify(mBackend).addCustomManualMode("Custom mode", 0);
    }

    @Test
    public void saveMode_redirectsToModeView() {
        when(mBackend.addCustomManualMode(any(), anyInt())).then(
                (Answer<ZenMode>) invocationOnMock -> new TestModeBuilder()
                        .setId("Id of a mode named " + invocationOnMock.getArgument(0))
                        .setName(invocationOnMock.getArgument(0))
                        .setIconResId(invocationOnMock.getArgument(1))
                        .build());

        mFragment.setModeName("something");
        mFragment.setModeIcon(R.drawable.ic_zen_mode_type_immersive);
        mFragment.saveMode();

        Intent nextIntent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(nextIntent.getStringExtra(EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ZenModeFragment.class.getName());
        Bundle fragmentArgs = nextIntent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(fragmentArgs).isNotNull();
        assertThat(fragmentArgs.getString(EXTRA_AUTOMATIC_ZEN_RULE_ID)).isEqualTo(
                "Id of a mode named something");
    }

    @Test
    public void onCreate_whenRecreating_preservesEdits() {
        FragmentScenario<ZenModeNewCustomFragment> scenario =
                FragmentScenario.launch(ZenModeNewCustomFragment.class, /* bundle= */ null, 0,
                        Lifecycle.State.INITIALIZED);
        scenario.onFragment(first -> {
            first.setBackend(mBackend);
            mFragment = first;
        });
        scenario.moveToState(Lifecycle.State.RESUMED);

        // Perform some edits in the first fragment.
        mFragment.setModeName("Don't forget me!");
        mFragment.setModeIcon(R.drawable.ic_zen_mode_type_immersive);

        // Destroy the first fragment and creates a new one (which should restore state).
        scenario.recreate().onFragment(second -> {
            assertThat(second).isNotSameInstanceAs(mFragment);
            second.setBackend(mBackend);
            mFragment = second;
        });

        mFragment.saveMode();
        verify(mBackend).addCustomManualMode("Don't forget me!",
                R.drawable.ic_zen_mode_type_immersive);
        scenario.close();
    }
}
