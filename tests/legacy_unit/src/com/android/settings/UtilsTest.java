/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.test.AndroidTestCase;

import androidx.test.filters.SmallTest;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;

public class UtilsTest extends AndroidTestCase {
    private static final int TEST_PRIMARY_USER_ID = 10;
    private static final int TEST_MANAGED_PROFILE_ID = 11;

    @Mock private UserManager mUserManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // // this is necessary for mockito to work
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().toString());

        MockitoAnnotations.initMocks(this);
        when(mUserManager.getUserHandle()).thenReturn(TEST_PRIMARY_USER_ID);
        UserInfo primaryUser = new UserInfo(TEST_PRIMARY_USER_ID, null,
                UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_PRIMARY);
        when(mUserManager.getUserInfo(TEST_PRIMARY_USER_ID)).thenReturn(primaryUser);
        UserInfo managedProfile = new UserInfo(TEST_MANAGED_PROFILE_ID, null,
                UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_MANAGED_PROFILE);
        when(mUserManager.getUserInfo(eq(TEST_MANAGED_PROFILE_ID))).thenReturn(managedProfile);
    }

    @SmallTest
    public void testGetManagedProfile() {
        UserHandle[] userHandles = new UserHandle[] {
            new UserHandle(TEST_PRIMARY_USER_ID),
            new UserHandle(TEST_MANAGED_PROFILE_ID)
        };
        when(mUserManager.getUserProfiles())
                .thenReturn(new ArrayList<UserHandle>(Arrays.asList(userHandles)));
        assertEquals(TEST_MANAGED_PROFILE_ID,
                Utils.getManagedProfile(mUserManager).getIdentifier());
    }

    @SmallTest
    public void testGetManagedProfile_notPresent() {
        UserHandle[] userHandles = new UserHandle[] {
            new UserHandle(TEST_PRIMARY_USER_ID)
        };
        when(mUserManager.getUserProfiles())
                .thenReturn(new ArrayList<UserHandle>(Arrays.asList(userHandles)));
        assertNull(Utils.getManagedProfile(mUserManager));
    }
}
