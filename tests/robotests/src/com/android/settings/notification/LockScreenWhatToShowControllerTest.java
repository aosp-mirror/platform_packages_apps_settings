/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.server.notification.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_NOTIFICATION_LOCK_SCREEN_SETTINGS)
public class LockScreenWhatToShowControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "lockscreen_notification_what_to_show";

    private LockScreenWhatToShowController mController;
    private Context mContext;
    @Nullable
    private Preference mPreference;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        MockitoAnnotations.initMocks(this);
        mController = new LockScreenWhatToShowController(
                RuntimeEnvironment.application,
                PREFERENCE_KEY);
        mPreference = new Preference(mContext);
        mPreference.setKey(PREFERENCE_KEY);
    }

    @Test
    public void updateState_preferenceVisibleWhenSettingIsOn() {
        // Before: the show LOCK_SCREEN_SHOW_NOTIFICATIONS setting is on
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS,
                LockScreenNotificationsGlobalPreferenceController.ON
        );

        // When: update state
        mController.updateState(mPreference);

        // Then: the preference is visible
        assertNotNull(mPreference);
        assertTrue(mPreference.isVisible());
    }

    @Test
    public void updateState_preferenceInvisibleWhenSettingIsOff() {
        // Before: the show LOCK_SCREEN_SHOW_NOTIFICATIONS setting is off
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS,
                LockScreenNotificationsGlobalPreferenceController.OFF
        );

        // When: update state
        mController.updateState(mPreference);

        // Then: the preference is not visible
        assertNotNull(mPreference);
        assertFalse(mPreference.isVisible());
    }
}
