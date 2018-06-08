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
 * limitations under the License.
 */

package com.android.settings.users;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(SettingsRobolectricTestRunner.class)
public class UserCapabilitiesTest {

    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
    }

    @Test
    public void disallowUserSwitchWhenRestrictionIsSet() {
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_USER_SWITCH)).thenReturn(true);

        UserCapabilities userCapabilities = UserCapabilities.create(mContext);
        userCapabilities.updateAddUserCapabilities(mContext);

        assertThat(userCapabilities.mDisallowSwitchUser).isTrue();
    }

    @Test
    public void allowUserSwitchWhenRestrictionIsNotSet() {
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_USER_SWITCH)).thenReturn(false);

        UserCapabilities userCapabilities = UserCapabilities.create(mContext);
        userCapabilities.updateAddUserCapabilities(mContext);

        assertThat(userCapabilities.mDisallowSwitchUser).isFalse();
    }
}
