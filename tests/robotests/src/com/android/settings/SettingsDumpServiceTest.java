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
package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.PrintWriter;
import java.io.StringWriter;

@RunWith(RobolectricTestRunner.class)
public class SettingsDumpServiceTest {

    private static final String PACKAGE_BROWSER = "com.android.test.browser";
    private static final String PACKAGE_NULL = "android";
    private static final int ANOMALY_VERSION = 2;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ResolveInfo mResolveInfo;
    private TestService mTestService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mPackageManager.resolveActivity(TestService.BROWSER_INTENT,
                PackageManager.MATCH_DEFAULT_ONLY)).thenReturn(mResolveInfo);
        mTestService = spy(new TestService());
        mTestService.setPackageManager(mPackageManager);
    }

    @Test
    public void testDumpDefaultBrowser_DefaultBrowser_ReturnBrowserName() {
        mResolveInfo.activityInfo = new ActivityInfo();
        mResolveInfo.activityInfo.packageName = PACKAGE_BROWSER;

        assertThat(mTestService.dumpDefaultBrowser()).isEqualTo(PACKAGE_BROWSER);
    }

    @Test
    public void testDumpDefaultBrowser_NoDefault_ReturnNull() {
        mResolveInfo.activityInfo = new ActivityInfo();
        mResolveInfo.activityInfo.packageName = PACKAGE_NULL;

        assertThat(mTestService.dumpDefaultBrowser()).isEqualTo(null);
    }

    @Test
    public void testDump_printServiceAsKey() {
        mResolveInfo.activityInfo = new ActivityInfo();
        mResolveInfo.activityInfo.packageName = PACKAGE_BROWSER;
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        mTestService.dump(null, printWriter, null);

        assertThat(stringWriter.toString())
                .contains("{\"" + SettingsDumpService.KEY_SERVICE + "\":");
    }

    /**
     * Test service used to pass in the mock {@link PackageManager}
     */
    private class TestService extends SettingsDumpService {
        private PackageManager mPm;

        public void setPackageManager(PackageManager pm) {
            mPm = pm;
        }

        @Override
        public PackageManager getPackageManager() {
            return mPm;
        }
    }
}
