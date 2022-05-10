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

import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowDevicePolicyManager.class,
        SettingsShadowResources.class})
public class UserCapabilitiesTest {

    private Context mContext;
    private ShadowUserManager mUserManager;
    private ShadowDevicePolicyManager mDpm;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mUserManager = ShadowUserManager.getShadow();
        mDpm = ShadowDevicePolicyManager.getShadow();
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void disallowUserSwitch_restrictionIsSet_true() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, true);

        UserCapabilities userCapabilities = UserCapabilities.create(mContext);
        userCapabilities.updateAddUserCapabilities(mContext);

        assertThat(userCapabilities.mDisallowSwitchUser).isTrue();
    }

    @Test
    public void disallowUserSwitch_restrictionIsNotSet_false() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);

        UserCapabilities userCapabilities = UserCapabilities.create(mContext);
        userCapabilities.updateAddUserCapabilities(mContext);

        assertThat(userCapabilities.mDisallowSwitchUser).isFalse();
    }

    @Test
    public void userSwitchEnabled_off() {
        mUserManager.setUserSwitcherEnabled(false);

        final UserCapabilities userCapabilities = UserCapabilities.create(mContext);
        userCapabilities.updateAddUserCapabilities(mContext);

        assertThat(userCapabilities.mUserSwitcherEnabled).isFalse();
    }

    @Test
    public void userSwitchEnabled_on() {
        mUserManager.setUserSwitcherEnabled(true);

        final UserCapabilities userCapabilities = UserCapabilities.create(mContext);
        userCapabilities.updateAddUserCapabilities(mContext);

        assertThat(userCapabilities.mUserSwitcherEnabled).isTrue();
    }

    @Test
    @Ignore
    public void restrictedProfile_enabled() {
        mUserManager.setUserTypeEnabled(UserManager.USER_TYPE_FULL_RESTRICTED, true);
        mDpm.setDeviceOwner(null);
        SettingsShadowResources.overrideResource(R.bool.config_offer_restricted_profiles, true);
        final UserCapabilities userCapabilities = UserCapabilities.create(mContext);
        assertThat(userCapabilities.mCanAddRestrictedProfile).isTrue();
    }

    @Test
    public void restrictedProfile_configNotSet() {
        mUserManager.setUserTypeEnabled(UserManager.USER_TYPE_FULL_RESTRICTED, true);
        mDpm.setDeviceOwner(null);
        SettingsShadowResources.overrideResource(R.bool.config_offer_restricted_profiles, false);
        final UserCapabilities userCapabilities = UserCapabilities.create(mContext);
        assertThat(userCapabilities.mCanAddRestrictedProfile).isFalse();
    }

    @Test
    public void restrictedProfile_deviceIsManaged() {
        mUserManager.setUserTypeEnabled(UserManager.USER_TYPE_FULL_RESTRICTED, true);
        mDpm.setDeviceOwner(new ComponentName("test", "test"));
        SettingsShadowResources.overrideResource(R.bool.config_offer_restricted_profiles, true);
        final UserCapabilities userCapabilities = UserCapabilities.create(mContext);
        assertThat(userCapabilities.mCanAddRestrictedProfile).isFalse();
    }

    @Test
    public void restrictedProfile_typeNotEnabled() {
        mUserManager.setUserTypeEnabled(UserManager.USER_TYPE_FULL_RESTRICTED, false);
        mDpm.setDeviceOwner(null);
        SettingsShadowResources.overrideResource(R.bool.config_offer_restricted_profiles, true);
        final UserCapabilities userCapabilities = UserCapabilities.create(mContext);
        assertThat(userCapabilities.mCanAddRestrictedProfile).isFalse();
    }
}
