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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowShortcutManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowShortcutManager.class})
public class SettingsInitializeTest {

    private Context mContext;
    private SettingsInitialize mSettingsInitialize;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mSettingsInitialize = new SettingsInitialize();
    }

    @Test
    public void refreshExistingShortcuts_shouldUpdateLaunchIntentFlagsForExistingShortcut() {
        final String id = "test_shortcut_id";
        final Intent intent = new Intent(Intent.ACTION_DEFAULT);
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        final ShortcutInfo info = new ShortcutInfo.Builder(mContext, id)
                .setShortLabel("test123")
                .setIntent(intent)
                .build();
        final List<ShortcutInfo> shortcuts = new ArrayList<>();
        shortcuts.add(info);
        ShadowShortcutManager.get().setPinnedShortcuts(shortcuts);

        mSettingsInitialize.refreshExistingShortcuts(mContext);

        final List<ShortcutInfo> updatedShortcuts =
                ShadowShortcutManager.get().getLastUpdatedShortcuts();
        assertThat(updatedShortcuts).hasSize(1);
        final ShortcutInfo updateInfo = updatedShortcuts.get(0);
        assertThat(updateInfo.getId()).isEqualTo(id);
        final int flags = updateInfo.getIntent().getFlags();
        // The original flag should be removed
        assertThat(flags & Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED).isEqualTo(0);
        // The new flags should be set
        assertThat(flags & Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK);
        assertThat(flags & Intent.FLAG_ACTIVITY_CLEAR_TOP).isEqualTo(
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
