/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.shortcut;

import static com.android.settings.shortcut.Shortcuts.SHORTCUT_ID_PREFIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.Settings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ShortcutsUpdaterTest {

    private Context mContext;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private ShortcutManager mShortcutManager;
    @Captor
    private ArgumentCaptor<List<ShortcutInfo>> mListCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));
    }

    @Test
    public void updatePinnedShortcuts_updatesAllShortcuts() {
        final List<ShortcutInfo> pinnedShortcuts = Arrays.asList(
                makeShortcut("d1"),
                makeShortcut("d2"),
                makeShortcut(Settings.ManageApplicationsActivity.class),
                makeShortcut("d3"),
                makeShortcut(Settings.SoundSettingsActivity.class));
        when(mShortcutManager.getPinnedShortcuts()).thenReturn(pinnedShortcuts);

        ShortcutsUpdater.updatePinnedShortcuts(mContext);

        verify(mShortcutManager, times(1)).updateShortcuts(mListCaptor.capture());

        final List<ShortcutInfo> updates = mListCaptor.getValue();

        assertThat(updates).hasSize(2);
        assertThat(pinnedShortcuts.get(2).getId()).isEqualTo(updates.get(0).getId());
        assertThat(pinnedShortcuts.get(4).getId()).isEqualTo(updates.get(1).getId());
        assertThat(updates.get(0).getShortLabel().toString()).isEqualTo("App info");
        assertThat(updates.get(1).getShortLabel().toString()).isEqualTo("Sound & vibration");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void updatePinnedShortcuts_withModesFlag_replacesDndByModes() {
        List<ShortcutInfo> shortcuts = List.of(
                makeShortcut(Settings.ZenModeSettingsActivity.class));
        when(mShortcutManager.getPinnedShortcuts()).thenReturn(shortcuts);

        ShortcutsUpdater.updatePinnedShortcuts(mContext);

        verify(mShortcutManager, times(1)).updateShortcuts(mListCaptor.capture());
        final List<ShortcutInfo> updates = mListCaptor.getValue();
        assertThat(updates).hasSize(1);

        // Id hasn't changed, but intent and label has.
        ComponentName zenCn = new ComponentName(mContext, Settings.ZenModeSettingsActivity.class);
        ComponentName modesCn = new ComponentName(mContext, Settings.ModesSettingsActivity.class);
        assertThat(updates.get(0).getId()).isEqualTo(
                SHORTCUT_ID_PREFIX + zenCn.flattenToShortString());
        assertThat(updates.get(0).getIntent().getComponent()).isEqualTo(modesCn);
        assertThat(updates.get(0).getShortLabel().toString()).isEqualTo("Modes");
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void updatePinnedShortcuts_withoutModesFlag_leavesDndAlone() {
        List<ShortcutInfo> shortcuts = List.of(
                makeShortcut(Settings.ZenModeSettingsActivity.class));
        when(mShortcutManager.getPinnedShortcuts()).thenReturn(shortcuts);

        ShortcutsUpdater.updatePinnedShortcuts(mContext);

        verify(mShortcutManager, times(1)).updateShortcuts(mListCaptor.capture());
        final List<ShortcutInfo> updates = mListCaptor.getValue();
        assertThat(updates).hasSize(1);

        // Nothing has changed.
        ComponentName zenCn = new ComponentName(mContext, Settings.ZenModeSettingsActivity.class);
        assertThat(updates.get(0).getId()).isEqualTo(
                SHORTCUT_ID_PREFIX + zenCn.flattenToShortString());
        assertThat(updates.get(0).getIntent().getComponent()).isEqualTo(zenCn);
        assertThat(updates.get(0).getShortLabel().toString()).isEqualTo("Do Not Disturb");

    }

    private ShortcutInfo makeShortcut(Class<?> className) {
        ComponentName cn = new ComponentName(mContext, className);
        return makeShortcut(SHORTCUT_ID_PREFIX + cn.flattenToShortString());
    }

    private ShortcutInfo makeShortcut(String id) {
        return new ShortcutInfo.Builder(mContext, id).build();
    }
}
