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

package com.android.settings.privatespace;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/**
 * Tests for HidePrivateSpaceSensitiveNotificationsController.
 * Run as {@code atest SettingsUnitTests:HidePrivateSpaceSensitiveNotificationsControllerTest}
 */
@RunWith(AndroidJUnit4.class)
public class HidePrivateSpaceSensitiveNotificationsControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private HidePrivateSpaceSensitiveNotificationsController
            mHidePrivateSpaceSensitiveNotificationsController;
    @Mock
    private ContentResolver mContentResolver;
    private int mOriginalDeviceSensitiveNotifValue;
    private int mOriginalDeviceNotifValue;
    private int mOriginalPsSensitiveNotifValue;
    private int mPrivateProfileId;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mContentResolver = mContext.getContentResolver();
        assumeTrue(PrivateSpaceMaintainer.getInstance(mContext).doesPrivateSpaceExist());

        mSetFlagsRule.enableFlags(
                android.multiuser.Flags.FLAG_ENABLE_PS_SENSITIVE_NOTIFICATIONS_TOGGLE);
        mSetFlagsRule.enableFlags(android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        mPrivateProfileId = PrivateSpaceMaintainer.getInstance(
                mContext).getPrivateProfileHandle().getIdentifier();

        mOriginalDeviceSensitiveNotifValue = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 1);
        mOriginalDeviceNotifValue = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1);
        mOriginalPsSensitiveNotifValue = Settings.Secure.getIntForUser(mContentResolver,
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0, mPrivateProfileId);

        final String preferenceKey = "private_space_sensitive_notifications";
        mHidePrivateSpaceSensitiveNotificationsController =
                new HidePrivateSpaceSensitiveNotificationsController(mContext, preferenceKey);
    }

    @After
    public void tearDown() {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                mOriginalDeviceSensitiveNotifValue
        );
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, mOriginalDeviceNotifValue);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                mOriginalPsSensitiveNotifValue, mPrivateProfileId);
    }

    /**
     * Tests that the controller is unavailable if lockscreen sensitive notifications are disabled
     * on the device.
     */
    @Test
    public void getAvailabilityStatus_lockScreenPrivateNotificationsOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0);
        assertThat(mHidePrivateSpaceSensitiveNotificationsController.getAvailabilityStatus())
                .isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    /**
     * Tests that the controller is unavailable if lockscreen notifications are disabled on the
     * device.
     */
    @Test
    public void getAvailabilityStatus_lockScreenNotificationsOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0);
        assertThat(mHidePrivateSpaceSensitiveNotificationsController.getAvailabilityStatus())
                .isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    /**
     * Tests that the controller is available if lockscreen notifications and lockscreen private
     * notifications are enabled on the device.
     */
    @Test
    public void getAvailabilityStatus_returnAvailable() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 1);
        assertThat(mHidePrivateSpaceSensitiveNotificationsController.getAvailabilityStatus())
                .isEqualTo(AVAILABLE);
    }


    /**
     * Tests that toggle is not available if the flag for this feature and MVP flag are disabled.
     */
    @Test
    public void getAvailabilityStatus_flagDisabled() {
        mSetFlagsRule.disableFlags(
                android.multiuser.Flags.FLAG_ENABLE_PS_SENSITIVE_NOTIFICATIONS_TOGGLE);
        mSetFlagsRule.disableFlags(android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 1);
        assertThat(mHidePrivateSpaceSensitiveNotificationsController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void testSetChecked() {
        assertThat(mHidePrivateSpaceSensitiveNotificationsController.setChecked(true)).isTrue();
        assertThat(mHidePrivateSpaceSensitiveNotificationsController.isChecked()).isEqualTo(true);
        assertThat(mHidePrivateSpaceSensitiveNotificationsController.setChecked(false)).isTrue();
        assertThat(mHidePrivateSpaceSensitiveNotificationsController.isChecked()).isEqualTo(false);
    }
}
