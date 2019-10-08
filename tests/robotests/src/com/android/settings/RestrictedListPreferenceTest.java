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
 * limitations under the License
 */
package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;

import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedPreferenceHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowKeyguardManager;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowKeyguardManager.class})
public class RestrictedListPreferenceTest {
    private static final int PROFILE_USER_ID = 11;
    // From UnlaunchableAppActivity
    private static final int UNLAUNCHABLE_REASON_QUIET_MODE = 1;
    private static final String EXTRA_UNLAUNCHABLE_REASON = "unlaunchable_reason";

    private Activity mActivity;
    private ShadowUserManager mShadowUserManager;
    private ShadowKeyguardManager mShadowKeyguardManager;
    private RestrictedListPreference mPreference;
    private RestrictedPreferenceHelper mMockHelper;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(Activity.class);
        mShadowKeyguardManager =
                Shadows.shadowOf(application.getSystemService(KeyguardManager.class));
        mMockHelper = mock(RestrictedPreferenceHelper.class);
        mShadowUserManager = ShadowUserManager.getShadow();

        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        mPreference = new RestrictedListPreference(mActivity, attributeSet);
        mPreference.setProfileUserId(PROFILE_USER_ID);
        ReflectionHelpers.setField(mPreference, "mHelper", mMockHelper);
    }

    @Test
    public void performClick_profileLocked() {
        mPreference.setRequiresActiveUnlockedProfile(true);
        mShadowUserManager.setQuietModeEnabled(false);
        mShadowKeyguardManager.setIsDeviceLocked(PROFILE_USER_ID, true);
        // Device has to be marked as secure so the real KeyguardManager will create a non-null
        // intent.
        mShadowKeyguardManager.setIsDeviceSecure(PROFILE_USER_ID, true);
        mPreference.performClick();
        // Make sure that the performClick method on the helper is never reached.
        verify(mMockHelper, never()).performClick();
        // Assert that a CONFIRM_DEVICE_CREDENTIAL intent has been started.
        Intent started = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertThat(started.getExtras().getInt(Intent.EXTRA_USER_ID)).isEqualTo(PROFILE_USER_ID);
        assertThat(started.getAction())
                .isEqualTo(KeyguardManager.ACTION_CONFIRM_DEVICE_CREDENTIAL_WITH_USER);
    }

    @Test
    public void performClick_profileDisabled() {
        mPreference.setRequiresActiveUnlockedProfile(true);
        mShadowUserManager.setQuietModeEnabled(true);
        mShadowKeyguardManager.setIsDeviceLocked(PROFILE_USER_ID, false);
        mPreference.performClick();
        // Make sure that the performClick method on the helper is never reached.
        verify(mMockHelper, never()).performClick();
        // Assert that a new intent for enabling the work profile is started.
        Intent started = Shadows.shadowOf(mActivity).getNextStartedActivity();
        Bundle extras = started.getExtras();
        int reason = extras.getInt(EXTRA_UNLAUNCHABLE_REASON);
        assertThat(reason).isEqualTo(UNLAUNCHABLE_REASON_QUIET_MODE);
    }

    @Test
    public void performClick_profileAvailable() {
        // Verify that the helper's perfomClick method is called if the profile is
        // available and unlocked.
        mPreference.setRequiresActiveUnlockedProfile(true);
        mShadowUserManager.setQuietModeEnabled(false);
        mShadowKeyguardManager.setIsDeviceLocked(PROFILE_USER_ID, false);
        when(mMockHelper.performClick()).thenReturn(true);
        mPreference.performClick();
        verify(mMockHelper).performClick();
    }

    @Test
    public void performClick_profileLockedAndUnlockedProfileNotRequired() {
        // Verify that even if the profile is disabled, if the Preference class does not
        // require it than the regular flow takes place.
        mPreference.setRequiresActiveUnlockedProfile(false);
        mShadowUserManager.setQuietModeEnabled(true);
        when(mMockHelper.performClick()).thenReturn(true);
        mPreference.performClick();
        verify(mMockHelper).performClick();
    }
}
