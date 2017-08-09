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
package com.android.settings.applications;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PackageUtilTest {
    private static final String ALL_USERS_APP_NAME = "com.google.allusers.app";
    private static final String ONE_USER_APP_NAME = "com.google.oneuser.app";
    private static final int USER1_ID = 1;
    private static final int USER2_ID = 11;

    @Mock
    private PackageManager mMockPackageManager;
    @Mock
    private UserManager mMockUserManager;

    private InstalledAppDetails.PackageUtil mPackageUtil;
    private List<UserInfo> mUserInfos;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);

        mUserInfos = new ArrayList<>();
        mUserInfos.add(new UserInfo(USER1_ID, "lei", 0));
        mUserInfos.add(new UserInfo(USER2_ID, "yue", 0));
        when(mMockUserManager.getUsers(true)).thenReturn(mUserInfos);

        ApplicationInfo usersApp = new ApplicationInfo();
        usersApp.flags = ApplicationInfo.FLAG_INSTALLED;

        when(mMockPackageManager.getApplicationInfoAsUser(
                ALL_USERS_APP_NAME, PackageManager.GET_META_DATA, USER1_ID))
                .thenReturn(usersApp);
        when(mMockPackageManager.getApplicationInfoAsUser(
                ALL_USERS_APP_NAME, PackageManager.GET_META_DATA, USER2_ID))
                .thenReturn(usersApp);

        when(mMockPackageManager.getApplicationInfoAsUser(
                ONE_USER_APP_NAME, PackageManager.GET_META_DATA, USER1_ID))
                .thenReturn(usersApp);

        when(mMockPackageManager.getApplicationInfoAsUser(
                ONE_USER_APP_NAME, PackageManager.GET_META_DATA, USER2_ID))
                .thenThrow(new PackageManager.NameNotFoundException());

        mPackageUtil = new InstalledAppDetails.PackageUtil();
    }

    @Test
    public void testCountPackageInUsers_twoUsersInstalled_returnTwo() {
        assertEquals(2, mPackageUtil.countPackageInUsers(
                mMockPackageManager, mMockUserManager, ALL_USERS_APP_NAME));
    }

    @Test
    public void testCountPackageInUsers_oneUsersInstalled_returnOne() {
        assertEquals(1, mPackageUtil.countPackageInUsers(
                mMockPackageManager, mMockUserManager, ONE_USER_APP_NAME));
    }
}
