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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import com.android.settings.TestConfig;
import com.android.settings.TimeoutListPreference;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {ShadowDevicePolicyManagerWrapper.class})
public class TimeoutPreferenceControllerTest {
    private static final int TIMEOUT = 30;
    private Context mContext;
    @Mock
    private TimeoutListPreference mPreference;
    @Mock
    private UserManager mUserManager;

    private TimeoutPreferenceController mController;
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = new TimeoutPreferenceController(mContext, KEY_SCREEN_TIMEOUT);
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
        ShadowDevicePolicyManagerWrapper.setMaximumTimeToLock(profileUserId, timeout);

        mController.updateState(mPreference);
        verify(mPreference).removeUnusableTimeouts(timeout, null);
    }
}
