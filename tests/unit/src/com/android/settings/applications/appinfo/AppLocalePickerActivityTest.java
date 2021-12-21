/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.SettingsActivity;
import com.android.settings.applications.AppInfoBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppLocalePickerActivityTest {
    private TestAppLocalePickerActivity mActivity;

    @Before
    @UiThreadTest
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mActivity = new TestAppLocalePickerActivity();
    }

    @After
    public void cleanUp() {
        mActivity = null;
    }

    @Test
    public void onCreate_getEntryIntent_returnNull() {
        TestAppLocalePickerActivity.setCallingPackage(null);
        Intent intent = new Intent();

        assertThat(mActivity.getEntryIntent(intent)).isEqualTo(null);
    }

    @Test
    public void onCreate_getEntryIntent_returnIntentWithPackageName() {
        String callingPackageName = "com.example.android";
        TestAppLocalePickerActivity.setCallingPackage(callingPackageName);
        Intent intent = new Intent();

        Intent entryIntent = mActivity.getEntryIntent(intent);

        Bundle outputBundle =
                entryIntent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        String packageName = outputBundle.getString(AppInfoBase.ARG_PACKAGE_NAME);
        assertThat(packageName).isEqualTo(callingPackageName);
    }

    private static class TestAppLocalePickerActivity extends AppLocalePickerActivity {
        private static String sCallingPackage;
        @Override
        public String getCallingPackage() {
            return sCallingPackage;
        }

        public static void setCallingPackage(String packageName) {
            sCallingPackage = packageName;
        }
    }
}
