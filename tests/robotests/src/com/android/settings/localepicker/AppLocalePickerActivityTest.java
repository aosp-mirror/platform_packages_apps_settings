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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.ApplicationPackageManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.InstallSourceInfo;
import android.net.Uri;
import android.os.Process;
import android.os.UserHandle;
import android.telephony.TelephonyManager;

import com.android.internal.app.LocaleStore;
import com.android.settings.applications.AppInfoBase;

import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowTelephonyManager;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
                AppLocalePickerActivityTest.ShadowApplicationPackageManager.class,
        })
public class AppLocalePickerActivityTest {
    private static final String TEST_PACKAGE_NAME = "com.android.settings";
    private static final Uri TEST_PACKAGE_URI = Uri.parse("package:" + TEST_PACKAGE_NAME);

    @Mock
    LocaleStore.LocaleInfo mLocaleInfo;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Test
    public void launchAppLocalePickerActivity_hasPackageName_success() {
        ActivityController<TestAppLocalePickerActivity> controller =
                initActivityController(true);

        controller.create();

        assertThat(controller.get().isFinishing()).isFalse();
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
        data.putExtra(AppInfoBase.ARG_PACKAGE_UID, UserHandle.getUserId(Process.myUid()));
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

        @Implementation
        protected Object getInstallSourceInfo(String packageName) {
            return new InstallSourceInfo("", null, null, "");
        }
    }
}
