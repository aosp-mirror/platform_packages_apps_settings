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

package com.android.settings.shortcut;

import static com.android.settings.shortcut.Shortcuts.SHORTCUT_PROBE;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;

import com.android.settings.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ShortcutsTest {

    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.getApplication();
    }

    @Test
    public void shortcutsUpdateTask() {
        final Intent intent = new Intent(SHORTCUT_PROBE)
                .setClass(mContext, Settings.ManageApplicationsActivity.class);
        final ResolveInfo ri = mContext.getPackageManager().resolveActivity(intent, 0);
        assertThat(ri).isNotNull();

        ShortcutInfo shortcut = Shortcuts.createShortcutInfo(mContext, ri);

        assertThat(shortcut.getLabel()).isNotNull();
        assertThat(shortcut.getLabel().toString()).isEqualTo("App info");

        assertThat(shortcut.getIntent()).isNotNull();
        assertThat(shortcut.getIntent().getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(shortcut.getIntent().getCategories()).contains("com.android.settings.SHORTCUT");
        assertThat(shortcut.getIntent().getComponent()).isEqualTo(
                new ComponentName(mContext, Settings.ManageApplicationsActivity.class));
        assertThat(shortcut.getIcon()).isNotNull();
    }
}
