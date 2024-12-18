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

import static com.android.settings.notification.modes.CharSequenceTruth.assertThat;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Flags;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle.State;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
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
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeFragmentBaseTest {

    private static final Uri SETTINGS_URI = Settings.Global.getUriFor(
            Settings.Global.ZEN_MODE_CONFIG_ETAG);

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock ZenModesBackend mBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void fragment_noArguments_finishes() {
        when(mBackend.getMode(any())).thenReturn(TestModeBuilder.EXAMPLE);

        FragmentScenario<TestableFragment> scenario = createScenario(null);

        scenario.moveToState(State.RESUMED).onFragment(fragment -> {
            assertThat(fragment.requireActivity().isFinishing()).isTrue();
        });

        scenario.close();
    }

    @Test
    public void fragment_modeDoesNotExist_finishes() {
        when(mBackend.getMode(any())).thenReturn(null);

        FragmentScenario<TestableFragment> scenario = createScenario("mode_id");

        scenario.moveToState(State.RESUMED).onFragment(fragment -> {
            assertThat(fragment.requireActivity().isFinishing()).isTrue();
        });

        scenario.close();
    }

    @Test
    public void fragment_validMode_updatesControllersOnce() {
        ZenMode mode = new TestModeBuilder().setId("mode_id").build();
        when(mBackend.getMode("mode_id")).thenReturn(mode);

        FragmentScenario<TestableFragment> scenario = createScenario("mode_id");

        scenario.moveToState(State.CREATED).onFragment(fragment -> {
            assertThat(fragment.mShowsId.getZenMode()).isEqualTo(mode);
            assertThat(fragment.mShowsId.isAvailable()).isTrue();
            assertThat(fragment.mAvailableIfEnabled.getZenMode()).isEqualTo(mode);
            assertThat(fragment.mAvailableIfEnabled.isAvailable()).isTrue();

            verify(fragment.mShowsId, never()).updateState(any(), any());
            verify(fragment.mAvailableIfEnabled, never()).updateState(any(), any());
        });

        scenario.moveToState(State.RESUMED).onFragment(fragment -> {
            Preference preferenceOne = fragment.requirePreference("pref_id");
            assertThat(preferenceOne.getSummary()).isEqualTo("Id is mode_id");

            verify(fragment.mShowsId).updateState(any(), eq(mode));
            verify(fragment.mAvailableIfEnabled).updateState(any(), eq(mode));
        });

        scenario.close();
    }

    @Test
    public void fragment_onStartToOnStop_hasRegisteredContentObserver() {
        when(mBackend.getMode(any())).thenReturn(TestModeBuilder.EXAMPLE);
        FragmentScenario<TestableFragment> scenario = createScenario("id");

        scenario.moveToState(State.CREATED).onFragment(fragment ->
                assertThat(getSettingsContentObservers(fragment)).isEmpty());

        scenario.moveToState(State.STARTED).onFragment(fragment ->
                assertThat(getSettingsContentObservers(fragment)).hasSize(1));

        scenario.moveToState(State.RESUMED).onFragment(fragment ->
                assertThat(getSettingsContentObservers(fragment)).hasSize(1));

        scenario.moveToState(State.STARTED).onFragment(fragment ->
                assertThat(getSettingsContentObservers(fragment)).hasSize(1));

        scenario.moveToState(State.CREATED).onFragment(fragment ->
                assertThat(getSettingsContentObservers(fragment)).isEmpty());

        scenario.close();
    }

    @Test
    public void fragment_onModeUpdatedWithDifferences_updatesControllers() {
        ZenMode originalMode = new TestModeBuilder().setId("id").setName("Original").build();
        when(mBackend.getMode("id")).thenReturn(originalMode);

        FragmentScenario<TestableFragment> scenario = createScenario("id");
        scenario.moveToState(State.RESUMED).onFragment(fragment -> {
            Preference preference = fragment.requirePreference("pref_name");
            assertThat(preference.getSummary()).isEqualTo("Original");
            verify(fragment.mShowsName, times(1)).updateState(any(), eq(originalMode));

            // Now, we get a message saying something changed.
            ZenMode updatedMode = new TestModeBuilder().setId("id").setName("Updated").build();
            when(mBackend.getMode("id")).thenReturn(updatedMode);
            getSettingsContentObservers(fragment).stream().findFirst().get()
                    .dispatchChange(false, SETTINGS_URI);
            ShadowLooper.idleMainLooper();

            // The screen was updated, and only updated once.
            assertThat(preference.getSummary()).isEqualTo("Updated");
            verify(fragment.mShowsName, times(1)).updateState(any(), eq(updatedMode));
        });

        scenario.close();
    }

    @Test
    public void fragment_onModeUpdatedWithoutDifferences_setsModeInControllersButNothingElse() {
        ZenMode originalMode = new TestModeBuilder().setId("id").setName("Original").build();
        when(mBackend.getMode("id")).thenReturn(originalMode);

        FragmentScenario<TestableFragment> scenario = createScenario("id");
        scenario.moveToState(State.RESUMED).onFragment(fragment -> {
            Preference preference = fragment.requirePreference("pref_name");
            assertThat(preference.getSummary()).isEqualTo("Original");
            verify(fragment.mShowsName, times(1)).updateState(any(), eq(originalMode));

            // Now, we get a message saying something changed, but it was for a different mode.
            ZenMode notUpdatedMode = new TestModeBuilder(originalMode).build();
            when(mBackend.getMode("id")).thenReturn(notUpdatedMode);
            getSettingsContentObservers(fragment).stream().findFirst().get()
                    .dispatchChange(false, SETTINGS_URI);
            ShadowLooper.idleMainLooper();

            // The mode instance was updated, but updateState() was not called.
            assertThat(preference.getSummary()).isEqualTo("Original");
            assertThat(fragment.mShowsName.getZenMode()).isSameInstanceAs(notUpdatedMode);
            verify(fragment.mShowsName, never()).updateState(any(), same(notUpdatedMode));
        });

        scenario.close();
    }

    @Test
    public void fragment_onFragmentRestart_reloadsMode() {
        ZenMode originalMode = new TestModeBuilder().setId("id").setName("Original").build();
        when(mBackend.getMode("id")).thenReturn(originalMode);

        FragmentScenario<TestableFragment> scenario = createScenario("id");
        scenario.moveToState(State.RESUMED).onFragment(fragment -> {
            Preference preference = fragment.requirePreference("pref_name");
            assertThat(preference.getSummary()).isEqualTo("Original");
            verify(fragment.mShowsName, times(1)).updateState(any(), eq(originalMode));
        });

        ZenMode updatedMode = new TestModeBuilder().setId("id").setName("Updated").build();
        when(mBackend.getMode("id")).thenReturn(updatedMode);

        scenario.moveToState(State.CREATED).moveToState(State.RESUMED).onFragment(fragment -> {
            Preference preference = fragment.requirePreference("pref_name");
            assertThat(preference.getSummary()).isEqualTo("Updated");
            assertThat(fragment.mShowsName.getZenMode()).isSameInstanceAs(updatedMode);
        });

        scenario.close();
    }

    @Test
    public void fragment_onModeDeleted_finishes() {
        ZenMode originalMode = new TestModeBuilder().setId("mode_id").build();
        when(mBackend.getMode("mode_id")).thenReturn(originalMode);

        FragmentScenario<TestableFragment> scenario = createScenario("mode_id");
        scenario.moveToState(State.RESUMED).onFragment(fragment -> {
            assertThat(fragment.requireActivity().isFinishing()).isFalse();

            // Now it's no longer there...
            when(mBackend.getMode(any())).thenReturn(null);
            getSettingsContentObservers(fragment).stream().findFirst().get()
                    .dispatchChange(false, SETTINGS_URI);
            ShadowLooper.idleMainLooper();

            assertThat(fragment.requireActivity().isFinishing()).isTrue();
        });

        scenario.close();
    }

    private FragmentScenario<TestableFragment> createScenario(@Nullable String modeId) {
        Bundle fragmentArgs = null;
        if (modeId != null) {
            fragmentArgs = new Bundle();
            fragmentArgs.putString(EXTRA_AUTOMATIC_ZEN_RULE_ID, modeId);
        }

        FragmentScenario<TestableFragment> scenario = FragmentScenario.launch(
                TestableFragment.class, fragmentArgs, 0, State.INITIALIZED);

        scenario.onFragment(fragment -> {
            fragment.setBackend(mBackend); // Before onCreate().
        });

        return scenario;
    }

    public static class TestableFragment extends ZenModeFragmentBase {

        private ShowsIdPreferenceController mShowsId;
        private ShowsNamePreferenceController mShowsName;
        private AvailableIfEnabledPreferenceController mAvailableIfEnabled;

        @Override
        protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            mShowsId = spy(new ShowsIdPreferenceController(context, "pref_id"));
            mShowsName = spy(new ShowsNamePreferenceController(context, "pref_name"));
            mAvailableIfEnabled = spy(
                    new AvailableIfEnabledPreferenceController(context, "pref_enabled"));
            return ImmutableList.of(mShowsId, mShowsName, mAvailableIfEnabled);
        }

        @NonNull
        Preference requirePreference(String key) {
            Preference preference = getPreferenceScreen().findPreference(key);
            checkNotNull(preference, "Didn't find preference with key " + key);
            return preference;
        }

        ShadowContentResolver getShadowContentResolver() {
            return shadowOf(requireActivity().getContentResolver());
        }

        @Override
        protected int getPreferenceScreenResId() {
            return R.xml.modes_fake_settings;
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }
    }

    private static class ShowsIdPreferenceController extends AbstractZenModePreferenceController {

        ShowsIdPreferenceController(@NonNull Context context, @NonNull String key) {
            super(context, key);
        }

        @Override
        void updateState(Preference preference, @NonNull ZenMode zenMode) {
            preference.setSummary("Id is " + zenMode.getId());
        }
    }

    private static class ShowsNamePreferenceController extends AbstractZenModePreferenceController {

        ShowsNamePreferenceController(@NonNull Context context, @NonNull String key) {
            super(context, key);
        }

        @Override
        void updateState(Preference preference, @NonNull ZenMode zenMode) {
            preference.setSummary(zenMode.getName());
        }
    }

    private static class AvailableIfEnabledPreferenceController extends
            AbstractZenModePreferenceController {

        AvailableIfEnabledPreferenceController(@NonNull Context context, @NonNull String key) {
            super(context, key);
        }

        @Override
        public boolean isAvailable(@NonNull ZenMode zenMode) {
            return zenMode.isEnabled();
        }

        @Override
        void updateState(Preference preference, @NonNull ZenMode zenMode) {
            preference.setSummary("Enabled is " + zenMode.isEnabled());
        }
    }

    private ImmutableList<ContentObserver> getSettingsContentObservers(Fragment fragment) {
        return ImmutableList.copyOf(
                shadowOf(fragment.requireActivity().getContentResolver())
                        .getContentObservers(SETTINGS_URI));
    }
}
