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

package com.android.settings.security.screenlock;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.display.TimeoutListPreference;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDevicePolicyManager.class)
public class LockAfterTimeoutPreferenceControllerTest {

    private static final int TEST_USER_ID = 0;

    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private TrustAgentManager mTrustAgentManager;
    @Mock
    private TimeoutListPreference mPreference;

    private Context mContext;
    private LockAfterTimeoutPreferenceController mController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.securityFeatureProvider.getTrustAgentManager())
                .thenReturn(mTrustAgentManager);

        mController = new LockAfterTimeoutPreferenceController(
                mContext, TEST_USER_ID, mLockPatternUtils);
    }

    @Test
    public void isAvailable_lockSetToPattern_shouldReturnTrue() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(TEST_USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_lockSetToPin_shouldReturnTrue() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(TEST_USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_lockSetToPassword_shouldReturnTrue() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(TEST_USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_lockSetToNone_shouldReturnFalse() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testUpdateStateWithAdminTimeouts() {
        final int userId = UserHandle.myUserId();
        final long adminTimeout = 10000;
        final int displayTimeout = 5000;

        final UserManager um = mock(UserManager.class);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(um);
        when(um.getProfiles(userId)).thenReturn(Collections.emptyList());

        // Fake list of timeout values.
        when(mPreference.getEntries()).thenReturn(new CharSequence[] {"10"});
        when(mPreference.getEntryValues()).thenReturn(new CharSequence[] {"10000"});

        Settings.System.putInt(mContext.getContentResolver(), SCREEN_OFF_TIMEOUT, displayTimeout);
        ShadowDevicePolicyManager.getShadow().setMaximumTimeToLock(userId, adminTimeout);

        mController.updateState(mPreference);

        verify(mPreference).removeUnusableTimeouts(adminTimeout - displayTimeout, null);
    }
}
