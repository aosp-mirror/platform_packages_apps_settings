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

package com.android.settings.notification.app;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.content.Intent;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.SettingsActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AppNotificationSettingsTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static Intent sAppNotifSettingsIntent = new Intent()
            .setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
    private static Intent sAppNotifPromotionSettingsIntent = new Intent()
            .setAction(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS);

    @Mock
    private PreferenceScreen mPreferenceScreen;

    private AppNotificationSettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSettings = new AppNotificationSettings();

        // Non-null context is needed to call onCreateAdapter
        Context context = ApplicationProvider.getApplicationContext();
        when(mPreferenceScreen.getContext()).thenReturn(context);
    }

    @Test
    @EnableFlags(Flags.FLAG_UI_RICH_ONGOING)
    public void testOnCreateAdapter_flagOn_wrongIntent() {
        mSettings.mIntent = sAppNotifSettingsIntent;

        // notification settings intent goes to this page, but should not trigger a highlight.
        // we check specifically that getArguments() is null because we haven't set up any other
        // arguments, so the only ones would be ones created during onCreateAdapter.
        mSettings.onCreateAdapter(mPreferenceScreen);
        assertThat(mSettings.getArguments()).isNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_UI_RICH_ONGOING)
    public void testOnCreateAdapter_flagOn_correctIntent() {
        mSettings.mIntent = sAppNotifPromotionSettingsIntent;

        // for promotion settings intent, we expect a highlighted setting
        mSettings.onCreateAdapter(mPreferenceScreen);
        assertThat(mSettings.getArguments().getString(
                SettingsActivity.EXTRA_FRAGMENT_ARG_KEY)).isEqualTo(
                PromotedNotificationsPreferenceController.KEY_PROMOTED_SWITCH);
    }

    @Test
    @DisableFlags(Flags.FLAG_UI_RICH_ONGOING)
    public void testOnCreateAdapter_flagOff() {
        mSettings.mIntent = sAppNotifPromotionSettingsIntent;

        // regardless of intent, if the flag is off we should not have a highlight
        mSettings.onCreateAdapter(mPreferenceScreen);
        assertThat(mSettings.getArguments()).isNull();
    }
}
