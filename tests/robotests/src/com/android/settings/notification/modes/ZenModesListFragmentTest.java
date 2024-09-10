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
import static com.android.settings.notification.modes.ZenModesListFragment.REQUEST_NEW_MODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Flags;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.settings.notification.modes.ZenModesListAddModePreferenceController.ModeType;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity.IntentForResult;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModesListFragmentTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final ModeType APP_PROVIDED_MODE_TYPE = new ModeType("Mode", new ColorDrawable(),
            "Details", new Intent().setComponent(new ComponentName("pkg", "configActivity")));

    private static final ModeType CUSTOM_MANUAL_TYPE = new ModeType("Custom", new ColorDrawable(),
            null, null); // null creationActivityIntent means custom_manual.

    private static final ImmutableList<ZenMode> EXISTING_MODES = ImmutableList.of(
            new TestModeBuilder().setId("A").build(),
            new TestModeBuilder().setId("B").build(),
            new TestModeBuilder().setId("C").build());

    @Rule
    public ActivityScenarioRule<EmptyFragmentActivity> mActivityScenario =
            new ActivityScenarioRule<>(EmptyFragmentActivity.class);

    private FragmentActivity mActivity;
    private ZenModesListFragment mFragment;
    @Mock private ZenModesBackend mBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ZenModesBackend.setInstance(mBackend);

        mFragment = new ZenModesListFragment();
        mActivityScenario.getScenario().onActivity(activity -> {
            activity.getSupportFragmentManager().beginTransaction()
                    .add(mFragment, "tag").commitNow();
            mActivity = activity;
        });
    }

    @Test
    public void onChosenModeTypeForAdd_appProvidedMode_startsCreationActivity() {
        when(mBackend.getModes()).thenReturn(EXISTING_MODES);

        mFragment.onChosenModeTypeForAdd(APP_PROVIDED_MODE_TYPE);

        IntentForResult intent = shadowOf(mActivity).getNextStartedActivityForResult();
        assertThat(intent).isNotNull();
        assertThat(intent.intent).isEqualTo(APP_PROVIDED_MODE_TYPE.creationActivityIntent());
    }

    @Test
    public void onChosenModeTypeForAdd_customManualMode_startsNameAndIconPicker() {
        mFragment.onChosenModeTypeForAdd(CUSTOM_MANUAL_TYPE);

        Intent nextIntent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(nextIntent).isNotNull();
        assertThat(nextIntent.getStringExtra(EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ZenModeNewCustomFragment.class.getName());
    }

    @Test
    public void onActivityResult_modeWasCreated_opensIt() {
        when(mBackend.getModes()).thenReturn(EXISTING_MODES);
        mFragment.onChosenModeTypeForAdd(APP_PROVIDED_MODE_TYPE);

        // App creates the new mode.
        ZenMode createdMode = new TestModeBuilder().setId("new_id").setPackage("pkg").build();
        when(mBackend.getModes()).thenReturn(new ImmutableList.Builder<ZenMode>()
                .addAll(EXISTING_MODES)
                .add(createdMode)
                .build());
        mFragment.onActivityResult(REQUEST_NEW_MODE, 0, new Intent());

        Intent openModePageIntent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(openModePageIntent.getStringExtra(EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ZenModeFragment.class.getName());
        Bundle fragmentArgs = openModePageIntent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(fragmentArgs).isNotNull();
        assertThat(fragmentArgs.getString(EXTRA_AUTOMATIC_ZEN_RULE_ID)).isEqualTo("new_id");
    }

    @Test
    public void onActivityResult_secondTime_doesNothing() {
        when(mBackend.getModes()).thenReturn(EXISTING_MODES);
        mFragment.onChosenModeTypeForAdd(APP_PROVIDED_MODE_TYPE);
        // App creates a new mode, we redirect to its page when coming back.
        ZenMode createdMode = new TestModeBuilder().setId("new_id").setPackage("pkg").build();
        when(mBackend.getModes()).thenReturn(new ImmutableList.Builder<ZenMode>()
                .addAll(EXISTING_MODES)
                .add(createdMode)
                .build());
        mFragment.onActivityResult(REQUEST_NEW_MODE, 0, new Intent());
        shadowOf(mActivity).clearNextStartedActivities();

        mFragment.onActivityResult(REQUEST_NEW_MODE, 0, new Intent());

        Intent nextIntent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(nextIntent).isNull();
    }

    @Test
    public void onActivityResult_modeWasNotCreated_doesNothing() {
        when(mBackend.getModes()).thenReturn(EXISTING_MODES);
        mFragment.onChosenModeTypeForAdd(APP_PROVIDED_MODE_TYPE);
        shadowOf(mActivity).clearNextStartedActivities();

        // Returning to settings without creating a new mode.
        mFragment.onActivityResult(REQUEST_NEW_MODE, 0, new Intent());

        Intent nextIntent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(nextIntent).isNull();
    }
}
