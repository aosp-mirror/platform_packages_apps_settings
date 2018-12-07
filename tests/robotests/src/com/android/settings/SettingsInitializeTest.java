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

import android.content.pm.ShortcutManager;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SettingsInitializeTest {

    private Context mContext;
    private SettingsInitialize mSettingsInitialize;
    private ShortcutManager mShortcutManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mSettingsInitialize = new SettingsInitialize();
        mShortcutManager = (ShortcutManager) mContext.getSystemService(Context.SHORTCUT_SERVICE);
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

        mShortcutManager.addDynamicShortcuts(Collections.singletonList(info));
        mShortcutManager.requestPinShortcut(info, null);

        mSettingsInitialize.refreshExistingShortcuts(mContext);

        final List<ShortcutInfo> updatedShortcuts = mShortcutManager.getPinnedShortcuts();
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

    @Test
    public void refreshExistingShortcuts_shouldNotUpdateImmutableShortcut() {
        final String id = "test_shortcut_id";
        final ShortcutInfo info = new ShortcutInfo.Builder(mContext, id)
            .setShortLabel("test123")
            .setIntent(new Intent(Intent.ACTION_DEFAULT))
            .build();
        info.addFlags(ShortcutInfo.FLAG_IMMUTABLE);

        mShortcutManager.addDynamicShortcuts(Collections.singletonList(info));
        mShortcutManager.requestPinShortcut(info, null);

        mSettingsInitialize.refreshExistingShortcuts(mContext);

        final List<ShortcutInfo> updatedShortcuts = mShortcutManager.getPinnedShortcuts();
        assertThat(updatedShortcuts).hasSize(1);
        assertThat(updatedShortcuts.get(0)).isSameAs(info);
    }
}
