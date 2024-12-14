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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Flags;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.annotation.Nullable;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;

import com.android.internal.R;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeEditNameIconFragmentTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final ZenMode MODE = new TestModeBuilder().setId("id").setName("Mode").build();

    private Activity mActivity;
    private ZenModeEditNameIconFragment mFragment;
    private FragmentScenario<ZenModeEditNameIconFragment> mScenario;

    @Mock
    private ZenModesBackend mBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Note: Each test should call startFragment() to set mScenario, mFragment and mActivity.
    }

    @After
    public void tearDown() {
        if (mScenario != null) {
            mScenario.close();
        }
    }

    @Test
    public void onCreate_loadsMode() {
        when(mBackend.getMode(MODE.getId())).thenReturn(MODE);

        startFragment(MODE.getId());

        assertThat(mFragment.getZenMode()).isEqualTo(MODE);
        assertThat(mActivity.isFinishing()).isFalse();
    }

    @Test
    public void onCreate_noModeId_exits() {
        when(mBackend.getMode(any())).thenReturn(MODE);

        startFragment(null);

        assertThat(mActivity.isFinishing()).isTrue();
        verifyNoMoreInteractions(mBackend);
    }
    @Test
    public void onCreate_missingMode_exits() {
        when(mBackend.getMode(any())).thenReturn(null);

        startFragment(MODE.getId());

        assertThat(mActivity.isFinishing()).isTrue();
        verify(mBackend).getMode(MODE.getId());
    }

    @Test
    public void saveMode_appliesChangesAndFinishes() {
        when(mBackend.getMode(MODE.getId())).thenReturn(MODE);
        startFragment(MODE.getId());

        mFragment.setModeName("A new name");
        mFragment.setModeIcon(R.drawable.ic_zen_mode_type_theater);
        mFragment.setModeName("A newer name");

        mFragment.saveMode();

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        ZenMode savedMode = captor.getValue();
        assertThat(savedMode.getName()).isEqualTo("A newer name");
        assertThat(savedMode.getRule().getIconResId()).isEqualTo(
                R.drawable.ic_zen_mode_type_theater);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void saveMode_appliesOnyNameAndIconChanges() {
        when(mBackend.getMode(MODE.getId())).thenReturn(MODE);
        startFragment(MODE.getId());
        mFragment.setModeName("A new name");
        mFragment.setModeIcon(R.drawable.ic_zen_mode_type_theater);
        // Before the user saves, something else about the mode was modified by someone else.
        ZenMode newerMode = new TestModeBuilder(MODE).setTriggerDescription("Whenever").build();
        when(mBackend.getMode(MODE.getId())).thenReturn(newerMode);

        mFragment.saveMode();

        // Verify that saving only wrote the mode name, and didn't accidentally stomp over
        // unrelated fields of the mode.
        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        ZenMode savedMode = captor.getValue();
        assertThat(savedMode.getName()).isEqualTo("A new name");
        assertThat(savedMode.getTriggerDescription()).isEqualTo("Whenever");
    }

    @Test
    public void saveMode_forModeThatDisappeared_ignoresSave() {
        when(mBackend.getMode(MODE.getId())).thenReturn(MODE);
        startFragment(MODE.getId());
        mFragment.setModeName("A new name");
        mFragment.setModeIcon(R.drawable.ic_zen_mode_type_theater);
        // Before the user saves, the mode was removed by someone else.
        when(mBackend.getMode(MODE.getId())).thenReturn(null);

        mFragment.saveMode();

        verify(mBackend, never()).updateMode(any());
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void setModeFields_withoutSaveMode_doesNotSaveChanges() {
        when(mBackend.getMode(MODE.getId())).thenReturn(MODE);
        startFragment(MODE.getId());

        mFragment.setModeName("Not a good idea");
        mFragment.setModeIcon(R.drawable.emergency_icon);
        mActivity.finish();

        verify(mBackend, never()).updateMode(any());
    }

    @Test
    public void onCreate_whenRecreating_preservesEdits() {
        when(mBackend.getMode(MODE.getId())).thenReturn(MODE);
        startFragment(MODE.getId());

        mFragment.setModeName("A better name");
        mScenario.recreate().onFragment(newFragment -> {
            assertThat(newFragment).isNotSameInstanceAs(mFragment);
            newFragment.setBackend(mBackend);
            mActivity = newFragment.getActivity();
            mFragment = newFragment;
        });
        mFragment.saveMode();

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        ZenMode savedMode = captor.getValue();
        assertThat(savedMode.getName()).isEqualTo("A better name");
        assertThat(mActivity.isFinishing()).isTrue();
    }

    private void startFragment(@Nullable String modeId) {
        Bundle fragmentArgs = null;
        if (modeId != null) {
            fragmentArgs = new Bundle();
            fragmentArgs.putString(EXTRA_AUTOMATIC_ZEN_RULE_ID, modeId);
        }

        mScenario = FragmentScenario.launch(ZenModeEditNameIconFragment.class, fragmentArgs, 0,
                Lifecycle.State.INITIALIZED);

        mScenario.onFragment(fragment -> {
            fragment.setBackend(mBackend); // Before onCreate().
            mFragment = fragment;
        });

        mScenario.moveToState(Lifecycle.State.RESUMED).onFragment(fragment -> {
            mActivity = fragment.requireActivity();
        });
    }
}
