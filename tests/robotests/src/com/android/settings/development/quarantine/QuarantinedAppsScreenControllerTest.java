/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development.quarantine;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.SuspendDialogInfo;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class QuarantinedAppsScreenControllerTest {
    private static final String PREF_KEY = "quarantined_apps_screen";
    private static final String TEST_PACKAGE = "com.example.test.pkg";
    private static final int TEST_APP_ID = 1234;
    private static final int TEST_USER_ID = 10;

    private Context mContext;
    private QuarantinedAppsScreenController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new QuarantinedAppsScreenController(mContext, PREF_KEY);
    }

    @Test
    public void testOnPreferenceChange() {
        final Context userContext = mock(Context.class);
        doReturn(userContext).when(mContext).createContextAsUser(
                eq(UserHandle.of(TEST_USER_ID)), anyInt());
        final PackageManager packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(userContext).getPackageManager();

        final AppEntry entry = createAppEntry(TEST_PACKAGE, TEST_APP_ID, TEST_USER_ID);
        final QuarantinedAppPreference preference = new QuarantinedAppPreference(mContext, entry);

        mController.onPreferenceChange(preference, true);
        verify(packageManager).setPackagesSuspended(aryEq(new String[] {TEST_PACKAGE}), eq(true),
                isNull(), isNull(), any(SuspendDialogInfo.class),
                eq(PackageManager.FLAG_SUSPEND_QUARANTINED));

        mController.onPreferenceChange(preference, false);
        verify(packageManager).setPackagesSuspended(aryEq(new String[] {TEST_PACKAGE}), eq(false),
                isNull(), isNull(), isNull(),
                eq(PackageManager.FLAG_SUSPEND_QUARANTINED));
    }

    private AppEntry createAppEntry(String packageName, int appId, int userId) {
        final AppEntry entry = mock(AppEntry.class);
        entry.info = createApplicationInfo(packageName, appId, userId);
        entry.extraInfo = false;
        return entry;
    }

    private ApplicationInfo createApplicationInfo(String packageName, int appId, int userId) {
        final ApplicationInfo info = new ApplicationInfo();
        info.packageName = packageName;
        info.uid = UserHandle.getUid(userId, appId);
        return info;
    }
}
