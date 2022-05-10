/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.turnscreenon;


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.Manifest;
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
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TurnScreenOnSettingsTest {

    private static final int PRIMARY_USER_ID = 0;
    private static final int PROFILE_USER_ID = 10;

    private TurnScreenOnSettings mFragment;
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
        mFragment = new TurnScreenOnSettings(mPackageManager, mUserManager);
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
    public void testCollectTurnScreenOnApps_variousPackages_shouldReturnOnlyPackagesWithTurnScreenOnPermission() {
        PackageInfo primaryP1 = createPackage("Calculator", true);
        PackageInfo primaryP2 = createPackage("Clock", false);
        PackageInfo profileP1 = createPackage("Browser", false);
        PackageInfo profileP2 = createPackage("Files", true);
        mPrimaryUserPackages.add(primaryP1);
        mPrimaryUserPackages.add(primaryP2);
        mProfileUserPackages.add(profileP1);
        mProfileUserPackages.add(profileP2);

        List<Pair<ApplicationInfo, Integer>> apps = mFragment.collectTurnScreenOnApps(
                PRIMARY_USER_ID);

        assertThat(containsPackages(apps, primaryP1, profileP2)).isTrue();
        assertThat(containsPackages(apps, primaryP2, profileP1)).isFalse();
    }

    @Test
    public void collectTurnScreenOnApps_noTurnScreenOnPackages_shouldReturnEmptyList() {
        PackageInfo primaryP1 = createPackage("Calculator", false);
        PackageInfo profileP1 = createPackage("Browser", false);
        mPrimaryUserPackages.add(primaryP1);
        mProfileUserPackages.add(profileP1);

        List<Pair<ApplicationInfo, Integer>> apps = mFragment.collectTurnScreenOnApps(
                PRIMARY_USER_ID);

        assertThat(apps).isEmpty();
    }

    @Test
    public void sort_multiplePackages_appsShouldBeOrderedByAppName() {
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
        List<Pair<ApplicationInfo, Integer>> apps = mFragment.collectTurnScreenOnApps(
                PRIMARY_USER_ID);

        apps.sort(new TurnScreenOnSettings.AppComparator(null));

        assertThat(isOrdered(apps, primaryP1, profileP1, primaryP2, profileP2, primaryP3)).isTrue();
    }

    @Test
    public void hasTurnScreenOnPermission_ignoredPackages_shouldReturnFalse() {
        boolean res = false;

        for (String ignoredPackage : TurnScreenOnSettings.IGNORE_PACKAGE_LIST) {
            res |= TurnScreenOnSettings.hasTurnScreenOnPermission(mPackageManager, ignoredPackage);
        }

        assertThat(res).isFalse();
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

    private PackageInfo createPackage(String packageName, boolean hasTurnScreenOnPermission) {
        PackageInfo pi = new PackageInfo();
        when(mPackageManager.checkPermission(Manifest.permission.WAKE_LOCK,
                packageName)).thenReturn(
                hasTurnScreenOnPermission ? PackageManager.PERMISSION_GRANTED
                        : PackageManager.PERMISSION_DENIED);
        pi.packageName = packageName;
        pi.applicationInfo = new ApplicationInfo();
        pi.applicationInfo.name = packageName;
        return pi;
    }
}
