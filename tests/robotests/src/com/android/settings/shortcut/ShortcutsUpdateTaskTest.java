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

import static com.android.settings.shortcut.CreateShortcutPreferenceController.SHORTCUT_ID_PREFIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;

import com.android.settings.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ShortcutsUpdateTaskTest {

    private Context mContext;
    private ShadowPackageManager mPackageManager;

    @Mock
    private ShortcutManager mShortcutManager;
    @Captor
    private ArgumentCaptor<List<ShortcutInfo>> mListCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPackageManager = Shadow.extract(mContext.getPackageManager());
    }

    @Test
    public void shortcutsUpdateTask() {
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));
        final Intent shortcut1 = new Intent(CreateShortcutPreferenceController.SHORTCUT_PROBE)
                .setComponent(new ComponentName(
                        mContext, Settings.ManageApplicationsActivity.class));
        final ResolveInfo ri1 = mock(ResolveInfo.class);
        ri1.nonLocalizedLabel = "label1";

        final Intent shortcut2 = new Intent(CreateShortcutPreferenceController.SHORTCUT_PROBE)
                .setComponent(new ComponentName(
                        mContext, Settings.SoundSettingsActivity.class));
        final ResolveInfo ri2 = mock(ResolveInfo.class);
        ri2.nonLocalizedLabel = "label2";

        mPackageManager.addResolveInfoForIntent(shortcut1, ri1);
        mPackageManager.addResolveInfoForIntent(shortcut2, ri2);

        final List<ShortcutInfo> pinnedShortcuts = Arrays.asList(
                makeShortcut("d1"),
                makeShortcut("d2"),
                makeShortcut(Settings.ManageApplicationsActivity.class),
                makeShortcut("d3"),
                makeShortcut(Settings.SoundSettingsActivity.class));
        when(mShortcutManager.getPinnedShortcuts()).thenReturn(pinnedShortcuts);

        new ShortcutsUpdateTask(mContext).doInBackground();

        verify(mShortcutManager, times(1)).updateShortcuts(mListCaptor.capture());

        final List<ShortcutInfo> updates = mListCaptor.getValue();

        assertThat(updates).hasSize(2);
        assertThat(pinnedShortcuts.get(2).getId()).isEqualTo(updates.get(0).getId());
        assertThat(pinnedShortcuts.get(4).getId()).isEqualTo(updates.get(1).getId());
    }

    private ShortcutInfo makeShortcut(Class<?> className) {
        ComponentName cn = new ComponentName(mContext, className);
        return makeShortcut(SHORTCUT_ID_PREFIX + cn.flattenToShortString());
    }

    private ShortcutInfo makeShortcut(String id) {
        return new ShortcutInfo.Builder(mContext, id).build();
    }
}
