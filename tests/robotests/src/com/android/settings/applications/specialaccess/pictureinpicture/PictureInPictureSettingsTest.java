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

package com.android.settings.applications.specialaccess.pictureinpicture;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.util.Pair;

import com.android.settings.testutils.FakeFeatureFactory;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PictureInPictureSettingsTest {

    private static final int PRIMARY_USER_ID = 0;
    private static final int PROFILE_USER_ID = 10;

    private PictureInPictureSettings mFragment;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private UserManager mUserManager;
    private ArrayList<PackageInfo> mPrimaryUserPackages;
    private ArrayList<PackageInfo> mProfileUserPackages;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mFragment = new PictureInPictureSettings(mPackageManager, mUserManager);
        mPrimaryUserPackages = new ArrayList<>();
        mProfileUserPackages = new ArrayList<>();
        when(mPackageManager.getInstalledPackagesAsUser(anyInt(), eq(PRIMARY_USER_ID)))
                .thenReturn(mPrimaryUserPackages);
        when(mPackageManager.getInstalledPackagesAsUser(anyInt(), eq(PROFILE_USER_ID)))
                .thenReturn(mProfileUserPackages);

        UserInfo primaryUserInfo = new UserInfo();
        primaryUserInfo.id = PRIMARY_USER_ID;
        UserInfo profileUserInfo = new UserInfo();
        profileUserInfo.id = PROFILE_USER_ID;

        when(mUserManager.getProfiles(PRIMARY_USER_ID))
            .thenReturn(ImmutableList.of(primaryUserInfo, profileUserInfo));
    }

    @Test
    public void testCollectPipApps() {
        PackageInfo primaryP1 = createPackage("Calculator", true);
        PackageInfo primaryP2 = createPackage("Clock", false);
        PackageInfo profileP1 = createPackage("Calculator", false);
        PackageInfo profileP2 = createPackage("Clock", true);

        mPrimaryUserPackages.add(primaryP1);
        mPrimaryUserPackages.add(primaryP2);
        mProfileUserPackages.add(profileP1);
        mProfileUserPackages.add(profileP2);

        List<Pair<ApplicationInfo, Integer>> apps = mFragment.collectPipApps(PRIMARY_USER_ID);
        assertThat(containsPackages(apps, primaryP1, profileP2)).isTrue();
        assertThat(containsPackages(apps, primaryP2, profileP1)).isFalse();
    }

    @Test
    public void testAppSort() {
        PackageInfo primaryP1 = createPackage("Android", true);
        PackageInfo primaryP2 = createPackage("Boop", true);
        PackageInfo primaryP3 = createPackage("Deck", true);
        PackageInfo profileP1 = createPackage("Android", true);
        PackageInfo profileP2 = createPackage("Cool", true);
        PackageInfo profileP3 = createPackage("Fast", false);

        mPrimaryUserPackages.add(primaryP1);
        mPrimaryUserPackages.add(primaryP2);
        mPrimaryUserPackages.add(primaryP3);
        mProfileUserPackages.add(profileP1);
        mProfileUserPackages.add(profileP2);
        mProfileUserPackages.add(profileP3);

        List<Pair<ApplicationInfo, Integer>> apps = mFragment.collectPipApps(PRIMARY_USER_ID);
        apps.sort(new PictureInPictureSettings.AppComparator(null));
        assertThat(isOrdered(apps, primaryP1, profileP1, primaryP2, profileP2, primaryP3)).isTrue();
    }

    private boolean containsPackages(List<Pair<ApplicationInfo, Integer>> apps,
            PackageInfo... packages) {
        for (PackageInfo aPackage : packages) {
            boolean found = false;
            for (Pair<ApplicationInfo, Integer> app : apps) {
                if (app.first == aPackage.applicationInfo) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean isOrdered(List<Pair<ApplicationInfo, Integer>> apps, PackageInfo... packages) {
        if (apps.size() != packages.length) {
            return false;
        }

        for (int i = 0; i < packages.length; i++) {
            if (packages[i].applicationInfo != apps.get(i).first) {
                return false;
            }
        }
        return true;
    }

    private PackageInfo createPackage(String appTitle, boolean supportsPip) {
        PackageInfo pi = new PackageInfo();
        ActivityInfo ai = new ActivityInfo();
        if (supportsPip) {
            ai.flags |= ActivityInfo.FLAG_SUPPORTS_PICTURE_IN_PICTURE;
        }
        pi.activities = new ActivityInfo[1];
        pi.activities[0] = ai;
        pi.applicationInfo = new ApplicationInfo();
        pi.applicationInfo.name = appTitle;
        return pi;
    }
}
