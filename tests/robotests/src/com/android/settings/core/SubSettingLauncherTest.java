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

package com.android.settings.core;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;

import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SubSettingLauncherTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test(expected = IllegalStateException.class)
    public void cannotReuseLauncher() {
        final SubSettingLauncher launcher = spy(new SubSettingLauncher(mContext));
        doNothing().when(launcher).launch(any(Intent.class));
        launcher.launch();
        launcher.launch();
    }

    @Test
    public void launch_shouldIncludeAllParams() {
        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        final SubSettingLauncher launcher = spy(new SubSettingLauncher(mContext));
        launcher.setTitle("123")
                .setDestination(SubSettingLauncherTest.class.getName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .launch();
        doNothing().when(launcher).launch(any(Intent.class));
        verify(launcher).launch(intentArgumentCaptor.capture());
        final Intent intent = intentArgumentCaptor.getValue();

        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE))
                .isEqualTo("123");
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(SubSettingLauncherTest.class.getName());
        assertThat(intent.getFlags()).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
