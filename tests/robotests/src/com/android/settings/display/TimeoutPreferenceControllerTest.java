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

package com.android.settings.display;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDevicePolicyManager.class)
public class TimeoutPreferenceControllerTest {

    private static final int TIMEOUT = 30;
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String DEFAULT_TIMEOUT = "300000"; // 5 minutes

    private Context mContext;
    @Mock
    private TimeoutListPreference mPreference;
    @Mock
    private UserManager mUserManager;

    private TimeoutPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mPreference.getValue()).thenReturn(DEFAULT_TIMEOUT);
        mController = new TimeoutPreferenceController(mContext, KEY_SCREEN_TIMEOUT);
    }

    @After
    public void tearDown() {
          ShadowRestrictedLockUtilsInternal.reset();
    }

    @Test
    public void testOnPreferenceChange_SetTimeout_ReturnCorrectTimeout() {
        mController.onPreferenceChange(mPreference, Integer.toString(TIMEOUT));

        final int mode = Settings.System.getInt(mContext.getContentResolver(),
                SCREEN_OFF_TIMEOUT, 0);
        assertThat(mode).isEqualTo(TIMEOUT);
    }

    @Test
    public void testUpdateStateNoAdminTimeouts() {
        when(mUserManager.getProfiles(anyInt())).thenReturn(Collections.emptyList());
        mController.updateState(mPreference);
        verify(mPreference).removeUnusableTimeouts(0, null);
    }

    @Test
    public void testUpdateStateWithAdminTimeouts() {
        final int profileUserId = UserHandle.myUserId();
        final long timeout = 10000;
        when(mUserManager.getProfiles(profileUserId)).thenReturn(Collections.emptyList());
        ShadowDevicePolicyManager.getShadow().setMaximumTimeToLock(profileUserId, timeout);

        mController.updateState(mPreference);
        verify(mPreference).removeUnusableTimeouts(timeout, null);
    }

    @Test
    public void testUpdateStateWithAdminTimeoutsAndRestriction() {
        final int profileUserId = UserHandle.myUserId();
        final long timeout = 100;
        when(mUserManager.getProfiles(profileUserId)).thenReturn(Collections.emptyList());
        ShadowDevicePolicyManager.getShadow().setMaximumTimeToLock(profileUserId, timeout);

        int userId = UserHandle.myUserId();
        List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        // Add two enforcing users so that RestrictedLockUtils.checkIfRestrictionEnforced returns
        // non-null.
        enforcingUsers.add(new UserManager.EnforcingUser(userId,
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        enforcingUsers.add(new UserManager.EnforcingUser(userId,
                UserManager.RESTRICTION_SOURCE_PROFILE_OWNER));
        when(mUserManager.getUserRestrictionSources(
                UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT, UserHandle.of(userId)))
                .thenReturn(enforcingUsers);

        mController.updateState(mPreference);

        ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<EnforcedAdmin> adminCaptor = ArgumentCaptor.forClass(EnforcedAdmin.class);

        verify(mPreference, times(2))
                .removeUnusableTimeouts(longCaptor.capture(), adminCaptor.capture());
        assertThat(longCaptor.getValue()).isEqualTo(0);
        assertThat(adminCaptor.getValue()).isNotNull();
    }

    @Test
    @Config(shadows = ShadowRestrictedLockUtilsInternal.class)
    public void updateState_selectedTimeoutLargerThanAdminMax_shouldSetSummaryToUpdatedPrefValue() {
        final int profileUserId = UserHandle.myUserId();
        final long allowedTimeout = 480000L; // 8 minutes
        when(mUserManager.getProfiles(profileUserId)).thenReturn(Collections.emptyList());
        ShadowDevicePolicyManager.getShadow().setMaximumTimeToLock(profileUserId, allowedTimeout);
        ShadowRestrictedLockUtilsInternal.setMaximumTimeToLockIsSet(true);
        final CharSequence[] timeouts = {"15000", "30000", "60000", "120000", "300000", "600000"};
        final CharSequence[] summaries = {"15s", "30s", "1m", "2m", "5m", "10m"};
        // set current timeout to be 10 minutes, which is longer than the allowed 8 minutes
        Settings.System.putLong(mContext.getContentResolver(), SCREEN_OFF_TIMEOUT, 600000L);
        when(mPreference.getEntries()).thenReturn(summaries);
        when(mPreference.getEntryValues()).thenReturn(timeouts);
        when(mPreference.getValue()).thenReturn("300000");

        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getString(R.string.screen_timeout_summary, "5m"));
    }
}
