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

package com.android.settings.accounts;


import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AutoSyncWorkDataPreferenceControllerTest {

    private static int MANAGED_PROFILE_ID = 10;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private Fragment mFragment;
    @Mock
    private Context mContext;

    private AutoSyncWorkDataPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mController = new AutoSyncWorkDataPreferenceController(mContext, mFragment);
    }

    @Test
    public void checkIsAvailable_managedProfile_shouldNotDisplay() {
        when(mUserManager.isManagedProfile()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void checkIsAvailable_linkedUser_shouldNotDisplay() {
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void checkIsAvailable_singleUserProfile_shouldNotDisplay() {
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(false);

        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(UserHandle.USER_SYSTEM, "user 1", 0 /* flags */));
        when(mUserManager.getProfiles(eq(UserHandle.USER_SYSTEM))).thenReturn(infos);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void checkIsAvailable_null_workProfileUserHandle_shouldNotDisplay() {
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(false);

        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(UserHandle.USER_SYSTEM, "user 1", 0 /* flags */));
        infos.add(new UserInfo(999, "xspace", 800010));
        when(mUserManager.getProfiles(eq(UserHandle.USER_SYSTEM))).thenReturn(infos);
        mController = new AutoSyncWorkDataPreferenceController(mContext, mFragment);

        assertThat(mController.mUserHandle).isEqualTo(null);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void multipleProfile_shouldInitWithWorkProfileUserHandle() {
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(false);

        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(UserHandle.USER_SYSTEM, "user 1", 0 /* flags */));
        infos.add(new UserInfo(
                MANAGED_PROFILE_ID, "work profile", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.getProfiles(eq(UserHandle.USER_SYSTEM))).thenReturn(infos);

        mController = new AutoSyncWorkDataPreferenceController(mContext, mFragment);

        assertThat(mController.mUserHandle.getIdentifier()).isEqualTo(MANAGED_PROFILE_ID);
        assertThat(mController.isAvailable()).isTrue();
    }
}
