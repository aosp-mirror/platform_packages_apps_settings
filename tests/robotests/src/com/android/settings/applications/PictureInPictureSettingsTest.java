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
 * limitations under the License
 */

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.UserInfo;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PictureInPictureSettingsTest {

    private static final int PRIMARY_USER_ID = 0;
    private static final int PROFILE_USER_ID = 10;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private FakeFeatureFactory mFeatureFactory;
    private PictureInPictureSettings mFragment;
    @Mock
    private PackageManagerWrapper mPackageManager;
    @Mock
    private UserManagerWrapper mUserManager;
    private ArrayList<PackageInfo> mPrimaryUserPackages;
    private ArrayList<PackageInfo> mProfileUserPackages;
    private ArrayList<UserInfo> mUsers;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mFragment = new PictureInPictureSettings(mPackageManager, mUserManager);
        mPrimaryUserPackages = new ArrayList<>();
        mProfileUserPackages = new ArrayList<>();
        mUsers = new ArrayList<>();
        when(mPackageManager.getInstalledPackagesAsUser(anyInt(), eq(PRIMARY_USER_ID)))
                .thenReturn(mPrimaryUserPackages);
        when(mPackageManager.getInstalledPackagesAsUser(anyInt(), eq(PROFILE_USER_ID)))
                .thenReturn(mProfileUserPackages);
        when(mUserManager.getProfiles(anyInt())).thenReturn(mUsers);
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

        ArrayList<Pair<ApplicationInfo, Integer>> apps = mFragment.collectPipApps(PRIMARY_USER_ID);
        assertThat(containsPackages(apps, primaryP1, profileP2));
        assertThat(!containsPackages(apps, primaryP2, profileP1));
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

        ArrayList<Pair<ApplicationInfo, Integer>> apps = mFragment.collectPipApps(PRIMARY_USER_ID);
        Collections.sort(apps, new PictureInPictureSettings.AppComparator(null));
        assertThat(isOrdered(apps, primaryP1, profileP1, primaryP2, profileP2));
    }

    private boolean containsPackages(ArrayList<Pair<ApplicationInfo, Integer>> apps,
            PackageInfo... packages) {
        for (int i = 0; i < packages.length; i++) {
            boolean found = false;
            for (int j = 0; j < apps.size(); j++) {
                if (apps.get(j).first == packages[i].applicationInfo) {
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

    private boolean isOrdered(ArrayList<Pair<ApplicationInfo, Integer>> apps,
            PackageInfo... packages) {
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
