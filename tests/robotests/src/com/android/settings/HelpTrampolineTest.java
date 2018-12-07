/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Intent;

import com.android.settings.testutils.shadow.ShadowHelpUtils;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowHelpUtils.class)
public class HelpTrampolineTest {

    @After
    public void tearDown() {
        ShadowHelpUtils.reset();
    }

    @Test
    public void launchHelp_noExtra_shouldDoNothing() {
        final Intent intent = new Intent().setClassName(
                RuntimeEnvironment.application.getPackageName(), HelpTrampoline.class.getName());

        Robolectric.buildActivity(HelpTrampoline.class, intent).create().get();

        assertThat(ShadowHelpUtils.isGetHelpIntentCalled()).isFalse();
    }

    @Test
    public void launchHelp_hasExtra_shouldLaunchHelp() {
        final Intent intent = new Intent().setClassName(
                RuntimeEnvironment.application.getPackageName(), HelpTrampoline.class.getName())
                .putExtra(Intent.EXTRA_TEXT, "help_url_upgrading");
        final ShadowActivity shadow = Shadows.
                shadowOf(Robolectric.buildActivity(HelpTrampoline.class, intent).create().get());
        final Intent launchedIntent = shadow.getNextStartedActivity();

        assertThat(ShadowHelpUtils.isGetHelpIntentCalled()).isTrue();
        assertThat(launchedIntent).isNotNull();
    }
}
