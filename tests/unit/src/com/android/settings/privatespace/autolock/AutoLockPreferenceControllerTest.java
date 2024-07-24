/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace.autolock;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.privatespace.PrivateSpaceMaintainer.PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Flags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AutoLockPreferenceControllerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final String KEY = "private_space_auto_lock";
    private Context mContext;
    private AutoLockPreferenceController mAutoLockPreferenceController;
    private ContentResolver mContentResolver;
    @Settings.Secure.PrivateSpaceAutoLockOption private int mOriginalAutoLockValue;

    /** Required setup before a test. */
    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mContentResolver = mContext.getContentResolver();
        mAutoLockPreferenceController = new AutoLockPreferenceController(mContext, KEY);
        mOriginalAutoLockValue =
                Settings.Secure.getInt(
                        mContentResolver,
                        Settings.Secure.PRIVATE_SPACE_AUTO_LOCK,
                        PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL);
    }

    @After
    public void tearDown() {
        Settings.Secure.putInt(
                mContentResolver, Settings.Secure.PRIVATE_SPACE_AUTO_LOCK, mOriginalAutoLockValue);
    }

    /**
     * Tests that the controller is available when both allow private profile and auto lock support
     * flags are enabled.
     */
    @Test
    public void getAvailabilityStatus_withAutoLockFlagEnabled_returnsAvailable() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);

        assertThat(mAutoLockPreferenceController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    /** Tests that the controller is not available when auto lock flag is off. */
    @Test
    public void getAvailabilityStatus_withAutoLockFlagDisabled_returnsNull() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        mSetFlagsRule.disableFlags(android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);

        assertThat(mAutoLockPreferenceController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    /**
     * Tests that auto lock preference displays the correct summary for option - every time device
     * locks.
     */
    @Test
    public void getSummary_whenOptionEveryTimeDeviceLocks_returnsEveryTimeDeviceLocks() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);

        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.PRIVATE_SPACE_AUTO_LOCK,
                Settings.Secure.PRIVATE_SPACE_AUTO_LOCK_ON_DEVICE_LOCK);
        assertThat(mAutoLockPreferenceController.getSummary().toString())
                .isEqualTo("Every time device locks");
    }

    /**
     * Tests that auto lock preference displays the correct summary for option - lock after 5
     * minutes of inactivity.
     */
    @Test
    public void getSummary_whenOptionAfter5MinutesOfInactivity_returnsAfter5MinutesOfInactivity() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);

        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.PRIVATE_SPACE_AUTO_LOCK,
                Settings.Secure.PRIVATE_SPACE_AUTO_LOCK_AFTER_INACTIVITY);
        assertThat(mAutoLockPreferenceController.getSummary().toString())
                .isEqualTo("After 5 minutes of inactivity");
    }

    /** Tests that auto lock preference displays the correct summary for option - Never. */
    @Test
    public void getSummary_whenOptionNever_returnsNever() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);

        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.PRIVATE_SPACE_AUTO_LOCK,
                Settings.Secure.PRIVATE_SPACE_AUTO_LOCK_NEVER);
        assertThat(mAutoLockPreferenceController.getSummary().toString()).isEqualTo("Never");
    }
}
