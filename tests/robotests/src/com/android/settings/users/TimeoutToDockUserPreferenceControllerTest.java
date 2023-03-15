/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.users;

import static android.provider.Settings.Secure.TIMEOUT_TO_DOCK_USER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatteryBackupHelperTest.ShadowUserHandle;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSecureSettings.class, ShadowUserHandle.class})
public class TimeoutToDockUserPreferenceControllerTest {
    private Context mContext;
    private Resources mResources;
    private TimeoutToDockUserPreferenceController mController;

    private static final String FAKE_PREFERENCE_KEY = "timeout_to_dock_user_preference";

    private String[] mEntries;
    private String[] mValues;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        doReturn(mResources).when(mContext).getResources();

        mEntries = mResources.getStringArray(
                R.array.switch_to_dock_user_when_docked_timeout_entries);
        mValues = mResources.getStringArray(
                R.array.switch_to_dock_user_when_docked_timeout_values);

        mController = new TimeoutToDockUserPreferenceController(mContext, FAKE_PREFERENCE_KEY);

        // Feature enabled.
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_enableTimeoutToDockUserWhenDocked)).thenReturn(
                true);

        // Multi-user feature enabled.
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.USER_SWITCHER_ENABLED,
                1);

        // Set to user 1;
        ShadowUserHandle.setUid(1);
    }

    @After
    public void tearDown() {
        ShadowUserHandle.reset();
    }

    @Test
    public void getAvailabilityStatus_featureFlagDisabled_returnUnsupportedOnDevice() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_enableTimeoutToDockUserWhenDocked)).thenReturn(
                false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_multiUserDisabled_returnConditionallyUnavailable() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.USER_SWITCHER_ENABLED,
                0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_isCurrentlyUserZero_returnDisabledForUser() {
        ShadowUserHandle.setUid(UserHandle.USER_SYSTEM);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_featureAndMultiUserEnabledAndNonUserZero_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_settingNotSet() {
        Settings.Secure.putStringForUser(mContext.getContentResolver(), TIMEOUT_TO_DOCK_USER,
                null, UserHandle.myUserId());

        assertThat(mController.getSummary().toString()).isEqualTo(
                mEntries[TimeoutToDockUserSettings.DEFAULT_TIMEOUT_SETTING_VALUE_INDEX]);
    }

    @Test
    public void getSummary_setToNever() {
        Settings.Secure.putStringForUser(mContext.getContentResolver(), TIMEOUT_TO_DOCK_USER,
                mValues[0], UserHandle.myUserId());

        assertThat(mController.getSummary().toString()).isEqualTo(mEntries[0]);
    }

    @Test
    public void getSummary_setToOneMinute() {
        Settings.Secure.putStringForUser(mContext.getContentResolver(), TIMEOUT_TO_DOCK_USER,
                mValues[1], UserHandle.myUserId());

        assertThat(mController.getSummary().toString()).isEqualTo(mEntries[1]);
    }
}
