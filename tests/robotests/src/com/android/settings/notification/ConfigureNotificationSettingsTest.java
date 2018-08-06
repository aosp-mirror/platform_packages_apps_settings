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
import static com.android.settings.notification.ConfigureNotificationSettings
        .SUMMARY_PROVIDER_FACTORY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.notification.ConfigureNotificationSettings.SummaryProvider;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class ConfigureNotificationSettingsTest {

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = spy(Robolectric.buildActivity(Activity.class).get());
    }

    @Test
    @Config(shadows = {
            ShadowUtils.class,
            ShadowLockPatternUtils.class
    })
    public void getNonIndexableKeys_shouldContainLockScreenPrefs() {
        final List<String> keys = ConfigureNotificationSettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(RuntimeEnvironment.application);

        assertThat(keys).containsAllOf(
                KEY_SWIPE_DOWN, KEY_LOCKSCREEN, KEY_LOCKSCREEN_WORK_PROFILE,
                KEY_LOCKSCREEN_WORK_PROFILE_HEADER);
    }

    @Test
    public void getSummary_noneBlocked() {
        SummaryLoader loader = mock(SummaryLoader.class);
        NotificationBackend backend = mock(NotificationBackend.class);
        when(backend.getBlockedAppCount()).thenReturn(0);
        SummaryProvider provider =
                (SummaryProvider) SUMMARY_PROVIDER_FACTORY.createSummaryProvider(mActivity, loader);
        provider.setBackend(backend);

        provider.setListening(true);

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(loader).setSummary(any(), captor.capture());

        assertThat(captor.getValue().toString()).contains("On");
    }

    @Test
    public void getSummary_someBlocked() {
        SummaryLoader loader = mock(SummaryLoader.class);
        NotificationBackend backend = mock(NotificationBackend.class);
        when(backend.getBlockedAppCount()).thenReturn(5);
        SummaryProvider provider =
                (SummaryProvider) SUMMARY_PROVIDER_FACTORY.createSummaryProvider(mActivity, loader);
        provider.setBackend(backend);

        provider.setListening(true);

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(loader).setSummary(any(), captor.capture());

        assertThat(captor.getValue().toString()).contains("Off");
        assertThat(captor.getValue().toString()).contains("5");
    }
}
