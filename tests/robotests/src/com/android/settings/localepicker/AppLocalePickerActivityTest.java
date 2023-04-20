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

package com.android.settings.localepicker;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.ApplicationPackageManager;
import android.app.LocaleConfig;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.LocaleList;
import android.os.Process;
import android.os.UserHandle;
import android.telephony.TelephonyManager;

import androidx.annotation.ArrayRes;

import com.android.internal.app.LocaleStore;
import com.android.settings.applications.AppInfoBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowTelephonyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
                AppLocalePickerActivityTest.ShadowApplicationPackageManager.class,
                AppLocalePickerActivityTest.ShadowResources.class,
                AppLocalePickerActivityTest.ShadowUserHandle.class,
                AppLocalePickerActivityTest.ShadowLocaleConfig.class,
        })
public class AppLocalePickerActivityTest {
    private static final String TEST_PACKAGE_NAME = "com.android.settings";
    private static final Uri TEST_PACKAGE_URI = Uri.parse("package:" + TEST_PACKAGE_NAME);

    @Mock
    LocaleStore.LocaleInfo mLocaleInfo;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private Context mContext;
    private ShadowPackageManager mPackageManager;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());
    }

    @After
    public void tearDown() {
        mPackageManager.removePackage(TEST_PACKAGE_NAME);
        ShadowResources.setDisAllowPackage(false);
        ShadowApplicationPackageManager.setNoLaunchEntry(false);
        ShadowUserHandle.setUserId(0);
        ShadowLocaleConfig.setStatus(LocaleConfig.STATUS_SUCCESS);
    }

    @Test
    public void launchAppLocalePickerActivity_hasPackageName_success() {
        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(true);
        controller.create();

        assertThat(controller.get().isFinishing()).isFalse();
    }

    @Test
    public void launchAppLocalePickerActivity_appNoLocaleConfig_failed() {
        ShadowLocaleConfig.setStatus(LocaleConfig.STATUS_NOT_SPECIFIED);

        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(true);
        controller.create();

        assertThat(controller.get().isFinishing()).isTrue();
    }

    @Test
    public void launchAppLocalePickerActivity_appSignPlatformKey_failed() {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.privateFlags |= ApplicationInfo.PRIVATE_FLAG_SIGNED_WITH_PLATFORM_KEY;
        applicationInfo.packageName = TEST_PACKAGE_NAME;

        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = TEST_PACKAGE_NAME;
        packageInfo.applicationInfo = applicationInfo;
        mPackageManager.installPackage(packageInfo);

        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(true);
        controller.create();

        assertThat(controller.get().isFinishing()).isTrue();
    }

    @Test
    public void launchAppLocalePickerActivity_appMatchDisallowedPackage_failed() {
        ShadowResources.setDisAllowPackage(true);

        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(true);
        controller.create();

        assertThat(controller.get().isFinishing()).isTrue();
    }

    @Test
    public void launchAppLocalePickerActivity_appNoLaunchEntry_failed() {
        ShadowApplicationPackageManager.setNoLaunchEntry(true);

        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(true);
        controller.create();

        assertThat(controller.get().isFinishing()).isTrue();
    }

    @Test
    public void launchAppLocalePickerActivity_modifyAppLocalesOfAnotherUser_failed() {
        ShadowUserHandle.setUserId(10);

        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(true);
        controller.create();

        assertThat(controller.get().isFinishing()).isTrue();
    }

    @Test
    public void launchAppLocalePickerActivity_intentWithoutPackageName_failed() {
        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(false);
        controller.create();

        assertThat(controller.get().isFinishing()).isTrue();
    }

    @Test
    public void onLocaleSelected_getLocaleNotNull_getLanguageTag() {
        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(true);
        Locale locale = new Locale("en", "US");
        when(mLocaleInfo.getLocale()).thenReturn(locale);
        when(mLocaleInfo.isSystemLocale()).thenReturn(false);

        controller.create();
        AppLocalePickerActivity mActivity = controller.get();
        mActivity.onLocaleSelected(mLocaleInfo);

        verify(mLocaleInfo, times(2)).getLocale();
        assertThat(mLocaleInfo.getLocale().toLanguageTag()).isEqualTo("en-US");
        assertThat(controller.get().isFinishing()).isTrue();
    }

    @Test
    public void onLocaleSelected_getLocaleNull_getEmptyLanguageTag() {
        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(true);
        when(mLocaleInfo.getLocale()).thenReturn(null);
        when(mLocaleInfo.isSystemLocale()).thenReturn(false);

        controller.create();
        AppLocalePickerActivity mActivity = controller.get();
        mActivity.onLocaleSelected(mLocaleInfo);

        verify(mLocaleInfo, times(1)).getLocale();
        assertThat(controller.get().isFinishing()).isTrue();
    }

    private ActivityController<TestAppLocalePickerActivity> initActivityController(
            boolean hasPackageName) {
        Intent data = new Intent();
        if (hasPackageName) {
            data.setData(TEST_PACKAGE_URI);
        }
        data.putExtra(AppInfoBase.ARG_PACKAGE_UID, Process.myUid());
        ActivityController<TestAppLocalePickerActivity> activityController =
                Robolectric.buildActivity(TestAppLocalePickerActivity.class, data);
        Activity activity = activityController.get();

        ShadowTelephonyManager shadowTelephonyManager = Shadows.shadowOf(
                activity.getSystemService(TelephonyManager.class));
        shadowTelephonyManager.setSimCountryIso("US");
        shadowTelephonyManager.setNetworkCountryIso("US");

        return activityController;
    }

    private static class TestAppLocalePickerActivity extends AppLocalePickerActivity {
        @Override
        public Context createContextAsUser(UserHandle user, int flags) {
            // return the current context as a work profile
            return this;
        }
    }

    @Implements(ApplicationPackageManager.class)
    public static class ShadowApplicationPackageManager extends
            org.robolectric.shadows.ShadowApplicationPackageManager {
        private static boolean sNoLaunchEntry = false;

        @Implementation
        protected Object getInstallSourceInfo(String packageName) {
            return new InstallSourceInfo("", null, null, "");
        }

        @Implementation
        protected List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
            if (sNoLaunchEntry) {
                return new ArrayList();
            } else {
                return super.queryIntentActivities(intent, flags);
            }
        }

        private static void setNoLaunchEntry(boolean noLaunchEntry) {
            sNoLaunchEntry = noLaunchEntry;
        }
    }

    @Implements(Resources.class)
    public static class ShadowResources extends
            org.robolectric.shadows.ShadowResources {
        private static boolean sDisAllowPackage = false;

        @Implementation
        public String[] getStringArray(@ArrayRes int id) {
            if (sDisAllowPackage) {
                return new String[]{TEST_PACKAGE_NAME};
            } else {
                return new String[0];
            }
        }

        private static void setDisAllowPackage(boolean disAllowPackage) {
            sDisAllowPackage = disAllowPackage;
        }
    }

    @Implements(UserHandle.class)
    public static class ShadowUserHandle {
        private static int sUserId = 0;
        private static void setUserId(int userId) {
            sUserId = userId;
        }

        @Implementation
        public static int getUserId(int userId) {
            return sUserId;
        }
    }

    @Implements(LocaleConfig.class)
    public static class ShadowLocaleConfig {
        private static int sStatus = 0;

        @Implementation
        public @Nullable LocaleList getSupportedLocales() {
            return LocaleList.forLanguageTags("en-US");
        }

        private static void setStatus(@LocaleConfig.Status int status) {
            sStatus = status;
        }

        @Implementation
        public @LocaleConfig.Status int getStatus() {
            return sStatus;
        }
    }
}
