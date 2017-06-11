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

package com.android.settings.applications;

import android.content.Context;

import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.notification.NotificationBackend;

import java.util.List;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class NotificationAppsTest {

    @Mock
    private PackageManagerWrapper mPackageManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private SummaryLoader mSummaryLoader;
    @Mock
    private NotificationBackend mBackend;

    private Context mContext;
    private NotificationApps.SummaryProvider mSummaryProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.USER_SERVICE, mUserManager);
        mContext = shadowApplication.getApplicationContext();
        mSummaryProvider = spy(new NotificationApps.SummaryProvider(mContext, mSummaryLoader));
        ReflectionHelpers.setField(mSummaryProvider, "mNotificationBackend", mBackend);
        ReflectionHelpers.setField(mSummaryProvider, "mPackageManager", mPackageManager);
    }

    @Test
    public void setListening_shouldSetSummary() {
        List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo(1, "user1", 0));
        when(mUserManager.getProfiles(anyInt())).thenReturn(userInfos);
        List<ApplicationInfo> appInfos = new ArrayList<>();
        ApplicationInfo info1 = new ApplicationInfo();
        info1.packageName = "package1";
        appInfos.add(info1);
        ApplicationInfo info2 = new ApplicationInfo();
        info2.packageName = "package2";
        appInfos.add(info2);
        when(mPackageManager.getInstalledApplicationsAsUser(anyInt(), anyInt()))
            .thenReturn(appInfos);

        // no notification off
        when(mBackend.getNotificationsBanned(anyString(), anyInt())).thenReturn(false);
        mSummaryProvider.setListening(true);
        ShadowApplication.runBackgroundTasks();
        verify(mSummaryLoader).setSummary(mSummaryProvider,
            mContext.getString(R.string.notification_summary_none));

        // some notification off
        when(mBackend.getNotificationsBanned(eq("package1"), anyInt())).thenReturn(true);
        mSummaryProvider.setListening(true);
        ShadowApplication.runBackgroundTasks();
        verify(mSummaryLoader).setSummary(mSummaryProvider,
            mContext.getResources().getQuantityString(R.plurals.notification_summary, 1, 1));

        when(mBackend.getNotificationsBanned(eq("package2"), anyInt())).thenReturn(true);
        mSummaryProvider.setListening(true);
        ShadowApplication.runBackgroundTasks();
        verify(mSummaryLoader).setSummary(mSummaryProvider,
            mContext.getResources().getQuantityString(R.plurals.notification_summary, 2, 2));
    }

}
