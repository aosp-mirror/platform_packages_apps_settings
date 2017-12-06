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

package com.android.settings.notification;

import static com.android.settings.notification.ConfigureNotificationSettings.KEY_LOCKSCREEN;
import static com.android.settings.notification.ConfigureNotificationSettings
        .KEY_LOCKSCREEN_WORK_PROFILE;
import static com.android.settings.notification.ConfigureNotificationSettings
        .KEY_LOCKSCREEN_WORK_PROFILE_HEADER;
import static com.android.settings.notification.ConfigureNotificationSettings.KEY_SWIPE_DOWN;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ConfigureNotificationSettingsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    @Config(shadows = {
            ShadowUtils.class,
            ShadowLockPatternUtils.class
    })
    public void getNonIndexableKeys_shouldContainLockScreenPrefs() {
        final List<String> keys = ConfigureNotificationSettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        assertThat(keys).containsAllOf(
                KEY_SWIPE_DOWN, KEY_LOCKSCREEN, KEY_LOCKSCREEN_WORK_PROFILE,
                KEY_LOCKSCREEN_WORK_PROFILE_HEADER);
    }
}
