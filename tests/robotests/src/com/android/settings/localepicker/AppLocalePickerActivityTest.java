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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.ApplicationPackageManager;
import android.app.LocaleConfig;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.LocaleList;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.telephony.TelephonyManager;

import androidx.annotation.ArrayRes;

import com.android.internal.app.LocaleStore;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.AppLocaleUtil;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
                AppLocalePickerActivityTest.ShadowApplicationPackageManager.class,
                AppLocalePickerActivityTest.ShadowResources.class,
        })
public class AppLocalePickerActivityTest {
    private static final String TEST_PACKAGE_NAME = "com.android.settings";
    private static final Uri TEST_PACKAGE_URI = Uri.parse("package:" + TEST_PACKAGE_NAME);
    private static final String EN_CA = "en-CA";
    private static final String EN_US = "en-US";
    private static int sUid;

    private FakeFeatureFactory mFeatureFactory;
    private LocaleNotificationDataManager mDataManager;
    private AppLocalePickerActivity mActivity;

    @Mock
    LocaleStore.LocaleInfo mLocaleInfo;
    @Mock
    private LocaleConfig mLocaleConfig;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Rule
    public final CheckFlagsRule mCheckFlagsRule =
            DeviceFlagsValueProvider.createCheckFlagsRule();

    private Context mContext;
    private ShadowPackageManager mPackageManager;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mLocaleConfig = mock(LocaleConfig.class);
        when(mLocaleConfig.getStatus()).thenReturn(LocaleConfig.STATUS_SUCCESS);
        when(mLocaleConfig.getSupportedLocales()).thenReturn(LocaleList.forLanguageTags("en-US"));
        ReflectionHelpers.setStaticField(AppLocaleUtil.class, "sLocaleConfig", mLocaleConfig);
        sUid = Process.myUid();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() throws Exception {
        mPackageManager.removePackage(TEST_PACKAGE_NAME);
        ReflectionHelpers.setStaticField(AppLocaleUtil.class, "sLocaleConfig", null);
        ShadowResources.setDisAllowPackage(false);
        ShadowApplicationPackageManager.setNoLaunchEntry(false);
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
        when(mLocaleConfig.getStatus()).thenReturn(LocaleConfig.STATUS_NOT_SPECIFIED);

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
    public void launchAppLocalePickerActivity_intentWithoutPackageName_failed() {
        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(false);
        controller.create();

        assertThat(controller.get().isFinishing()).isTrue();
    }

    @Ignore("b/313604701")
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

    @Test
    public void onLocaleSelected_logLocaleSource() {
        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(true);
        LocaleList.setDefault(LocaleList.forLanguageTags("ja-JP,en-CA,en-US"));
        Locale locale = new Locale("en", "US");
        when(mLocaleInfo.getLocale()).thenReturn(locale);
        when(mLocaleInfo.isSystemLocale()).thenReturn(false);
        when(mLocaleInfo.isSuggested()).thenReturn(true);
        when(mLocaleInfo.isSuggestionOfType(LocaleStore.LocaleInfo.SUGGESTION_TYPE_SIM)).thenReturn(
                true);
        when(mLocaleInfo.isSuggestionOfType(
                LocaleStore.LocaleInfo.SUGGESTION_TYPE_SYSTEM_AVAILABLE_LANGUAGE)).thenReturn(
                true);
        when(mLocaleInfo.isSuggestionOfType(
                LocaleStore.LocaleInfo.SUGGESTION_TYPE_OTHER_APP_LANGUAGE)).thenReturn(
                true);
        when(mLocaleInfo.isSuggestionOfType(
                LocaleStore.LocaleInfo.SUGGESTION_TYPE_IME_LANGUAGE)).thenReturn(
                true);

        controller.create();
        AppLocalePickerActivity mActivity = controller.get();
        mActivity.onLocaleSelected(mLocaleInfo);

        int localeSource = 15; // SIM_LOCALE | SYSTEM_LOCALE |IME_LOCALE|APP_LOCALE
        verify(mFeatureFactory.metricsFeatureProvider).action(
                any(), eq(SettingsEnums.ACTION_CHANGE_APP_LANGUAGE_FROM_SUGGESTED),
                eq(localeSource));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void onLocaleSelected_evaluateNotification_simpleLocaleUpdate_localeCreatedWithUid()
            throws Exception {
        sUid = 100;
        initLocaleNotificationEnvironment();
        ActivityController<TestAppLocalePickerActivity> controller = initActivityController(true);
        controller.create();
        AppLocalePickerActivity mActivity = controller.get();
        LocaleNotificationDataManager dataManager =
                NotificationController.getInstance(mActivity).getDataManager();

        mActivity.onLocaleSelected(mLocaleInfo);

        // Notification is not triggered.
        // In the sharedpreference, en-US's uid list contains uid1 and the notificationCount
        // equals 0.
        NotificationInfo info = dataManager.getNotificationInfo(EN_US);
        assertThat(info.getUidCollection().contains(sUid)).isTrue();
        assertThat(info.getNotificationCount()).isEqualTo(0);
        assertThat(info.getDismissCount()).isEqualTo(0);
        assertThat(info.getLastNotificationTimeMs()).isEqualTo(0);

        mDataManager.clearLocaleNotificationMap();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void onLocaleSelected_evaluateNotification_twoLocaleUpdate_triggerNotification()
            throws Exception {
        // App with uid 101 changed its locale from System to en-US.
        sUid = 101;
        initLocaleNotificationEnvironment();
        // Initialize the proto to contain en-US locale. Its uid list includes 100.
        Set<Integer> uidSet = Set.of(100);
        initSharedPreference(EN_US, uidSet, 0, 0, 0, 0);

        mActivity.onLocaleSelected(mLocaleInfo);

        // Notification is triggered.
        // In the proto file, en-US's uid list contains 101, the notificationCount equals 1, and
        // LastNotificationTime > 0.
        NotificationInfo info = mDataManager.getNotificationInfo(EN_US);
        assertThat(info.getUidCollection()).contains(sUid);
        assertThat(info.getNotificationCount()).isEqualTo(1);
        assertThat(info.getDismissCount()).isEqualTo(0);
        assertThat(info.getLastNotificationTimeMs()).isNotEqualTo(0);

        mDataManager.clearLocaleNotificationMap();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void onLocaleSelected_evaluateNotification_oddLocaleUpdate_uidAddedWithoutNotification()
            throws Exception {
        // App with uid 102 changed its locale from System to en-US.
        sUid = 102;
        initLocaleNotificationEnvironment();
        // Initialize the proto to include en-US locale. Its uid list includes 100,101 and
        // the notification count equals 1.
        int notificationId = (int) SystemClock.uptimeMillis();
        Set<Integer> uidSet = Set.of(100, 101);
        initSharedPreference(EN_US, uidSet, 0, 1,
                Calendar.getInstance().getTimeInMillis(), notificationId);

        mActivity.onLocaleSelected(mLocaleInfo);

        // Notification is not triggered because count % 2 != 0.
        // In the proto file, en-US's uid list contains 102, the notificationCount equals 1, and
        // LastNotificationTime > 0.
        NotificationInfo info = mDataManager.getNotificationInfo(EN_US);
        assertThat(info.getUidCollection()).contains(sUid);
        assertThat(info.getNotificationCount()).isEqualTo(1);
        assertThat(info.getDismissCount()).isEqualTo(0);
        assertThat(info.getLastNotificationTimeMs()).isNotEqualTo(0);
        assertThat(info.getNotificationId()).isEqualTo(notificationId);

        mDataManager.clearLocaleNotificationMap();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void onLocaleSelected_evaluateNotification_frequentLocaleUpdate_uidAddedNoNotification()
            throws Exception {
        // App with uid 103 changed its locale from System to en-US.
        sUid = 103;
        initLocaleNotificationEnvironment();
        // Initialize the proto to include en-US locale. Its uid list includes 100,101,102 and
        // the notification count equals 1.
        int notificationId = (int) SystemClock.uptimeMillis();
        Set<Integer> uidSet = Set.of(100, 101, 102);
        initSharedPreference(EN_US, uidSet, 0, 1,
                Calendar.getInstance().getTimeInMillis(), notificationId);

        mActivity.onLocaleSelected(mLocaleInfo);

        // Notification is not triggered because the duration is less than the threshold.
        // In the proto file, en-US's uid list contains 103, the notificationCount equals 1, and
        // LastNotificationTime > 0.
        NotificationInfo info = mDataManager.getNotificationInfo(EN_US);
        assertThat(info.getUidCollection().contains(sUid)).isTrue();
        assertThat(info.getNotificationCount()).isEqualTo(1);
        assertThat(info.getDismissCount()).isEqualTo(0);
        assertThat(info.getLastNotificationTimeMs()).isNotEqualTo(0);
        assertThat(info.getNotificationId()).isEqualTo(notificationId);

        mDataManager.clearLocaleNotificationMap();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void onLocaleSelected_evaluateNotification_2ndOddLocaleUpdate_uidAddedNoNotification()
            throws Exception {
        // App with uid 104 changed its locale from System to en-US.
        sUid = 104;
        initLocaleNotificationEnvironment();

        // Initialize the proto to include en-US locale. Its uid list includes 100,101,102,103 and
        // the notification count equals 1.
        int notificationId = (int) SystemClock.uptimeMillis();
        Set<Integer> uidSet = Set.of(100, 101, 102, 103);
        initSharedPreference(EN_US, uidSet, 0, 1, Calendar.getInstance().getTimeInMillis(),
                notificationId);

        mActivity.onLocaleSelected(mLocaleInfo);

        // Notification is not triggered because uid count % 2 != 0
        // In the proto file, en-US's uid list contains uid4, the notificationCount equals 1, and
        // LastNotificationTime > 0.
        NotificationInfo info = mDataManager.getNotificationInfo(EN_US);
        assertThat(info.getUidCollection()).contains(sUid);
        assertThat(info.getNotificationCount()).isEqualTo(1);
        assertThat(info.getDismissCount()).isEqualTo(0);
        assertThat(info.getLastNotificationTimeMs()).isNotEqualTo(0);

        mDataManager.clearLocaleNotificationMap();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void testEvaluateLocaleNotification_evenLocaleUpdate_trigger2ndNotification()
            throws Exception {
        sUid = 105;
        initLocaleNotificationEnvironment();

        // Initialize the proto to include en-US locale. Its uid list includes 100,101,102,103,104
        // and the notification count equals 1.
        // Eight days later, App with uid 105 changed its locale from System to en-US
        int notificationId = (int) SystemClock.uptimeMillis();
        Set<Integer> uidSet = Set.of(100, 101, 102, 103, 104);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, -8); // Set the lastNotificationTime to eight days ago.
        long lastNotificationTime = now.getTimeInMillis();
        initSharedPreference(EN_US, uidSet, 0, 1, lastNotificationTime, notificationId);

        mActivity.onLocaleSelected(mLocaleInfo);

        // Notification is triggered.
        // In the proto file, en-US's uid list contains 105, the notificationCount equals 2, and
        // LastNotificationTime is updated.
        NotificationInfo info = mDataManager.getNotificationInfo(EN_US);
        assertThat(info.getUidCollection()).contains(sUid);
        assertThat(info.getNotificationCount()).isEqualTo(2);
        assertThat(info.getDismissCount()).isEqualTo(0);
        assertThat(info.getLastNotificationTimeMs()).isGreaterThan(lastNotificationTime);

        mDataManager.clearLocaleNotificationMap();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void testEvaluateLocaleNotification_localeUpdateReachThreshold_uidAddedNoNotification()
            throws Exception {
        // App with uid 106 changed its locale from System to en-US.
        sUid = 106;
        initLocaleNotificationEnvironment();
        // Initialize the proto to include en-US locale. Its uid list includes
        // 100,101,102,103,104,105 and the notification count equals 2.
        int notificationId = (int) SystemClock.uptimeMillis();
        Set<Integer> uidSet = Set.of(100, 101, 102, 103, 104, 105);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, -8);
        long lastNotificationTime = now.getTimeInMillis();
        initSharedPreference(EN_US, uidSet, 0, 2, lastNotificationTime, notificationId);

        mActivity.onLocaleSelected(mLocaleInfo);

        // Notification is not triggered because the notification count threshold, 2, is reached.
        // In the proto file, en-US's uid list contains 106, the notificationCount equals 2, and
        // LastNotificationTime > 0.
        NotificationInfo info = mDataManager.getNotificationInfo(EN_US);
        assertThat(info.getUidCollection()).contains(sUid);
        assertThat(info.getNotificationCount()).isEqualTo(2);
        assertThat(info.getDismissCount()).isEqualTo(0);
        assertThat(info.getLastNotificationTimeMs()).isEqualTo(lastNotificationTime);

        mDataManager.clearLocaleNotificationMap();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void testEvaluateLocaleNotification_appChangedLocales_newLocaleCreated()
            throws Exception {
        sUid = 100;
        initLocaleNotificationEnvironment();
        // App with uid 100 changed its locale from en-US to ja-JP.
        Locale locale = Locale.forLanguageTag("ja-JP");
        when(mLocaleInfo.getLocale()).thenReturn(locale);
        // Initialize the proto to include en-US locale. Its uid list includes
        // 100,101,102,103,104,105,106 and the notification count equals 2.
        int notificationId = (int) SystemClock.uptimeMillis();
        Set<Integer> uidSet = Set.of(100, 101, 102, 103, 104, 105, 106);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, -8);
        initSharedPreference(EN_US, uidSet, 0, 2, now.getTimeInMillis(),
                notificationId);

        mActivity.onLocaleSelected(mLocaleInfo);

        // Notification is not triggered
        // In the proto file, a map for ja-JP is created. Its uid list contains uid1.
        NotificationInfo info = mDataManager.getNotificationInfo("ja-JP");
        assertThat(info.getUidCollection()).contains(sUid);
        assertThat(info.getNotificationCount()).isEqualTo(0);
        assertThat(info.getDismissCount()).isEqualTo(0);
        assertThat(info.getLastNotificationTimeMs()).isEqualTo(0);

        mDataManager.clearLocaleNotificationMap();
    }

    private void initLocaleNotificationEnvironment() throws Exception {
        LocaleList.setDefault(LocaleList.forLanguageTags(EN_CA));

        Locale locale = Locale.forLanguageTag("en-US");
        when(mLocaleInfo.getLocale()).thenReturn(locale);
        when(mLocaleInfo.isSystemLocale()).thenReturn(false);
        when(mLocaleInfo.isAppCurrentLocale()).thenReturn(false);

        ActivityController<TestAppLocalePickerActivity> controller = initActivityController(true);
        controller.create();
        mActivity = controller.get();
        mDataManager = NotificationController.getInstance(mActivity).getDataManager();
    }

    private void initSharedPreference(String locale, Set<Integer> uidSet, int dismissCount,
            int notificationCount, long lastNotificationTime, int notificationId)
            throws Exception {
        NotificationInfo info = new NotificationInfo(uidSet, notificationCount, dismissCount,
                lastNotificationTime, notificationId);
        mDataManager.putNotificationInfo(locale, info);
    }

    private ActivityController<TestAppLocalePickerActivity> initActivityController(
            boolean hasPackageName) {
        Intent data = new Intent();
        if (hasPackageName) {
            data.setData(TEST_PACKAGE_URI);
        }
        data.putExtra(AppInfoBase.ARG_PACKAGE_UID, sUid);
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

        @Implementation
        protected ApplicationInfo getApplicationInfo(String packageName, int flags)
                throws NameNotFoundException {
            if (packageName.equals(TEST_PACKAGE_NAME)) {
                ApplicationInfo applicationInfo = new ApplicationInfo();
                applicationInfo.packageName = TEST_PACKAGE_NAME;
                applicationInfo.uid = sUid;
                return applicationInfo;
            } else {
                return super.getApplicationInfo(packageName, flags);
            }
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
}
