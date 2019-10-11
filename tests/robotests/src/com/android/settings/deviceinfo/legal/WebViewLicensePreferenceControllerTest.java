/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.deviceinfo.legal;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WebViewLicensePreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private WebViewLicensePreferenceController mController;
    private Preference mPreference;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPreference = new Preference(mContext);
        mPreference.setIntent(new Intent());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mController = new WebViewLicensePreferenceController(mContext, "pref_key");
        mController.displayPreference(mScreen);
    }

    @Test
    public void getIntent_shouldUseRightIntent() {
        final Intent intent = mController.getIntent();
        assertThat(intent.getAction()).isEqualTo("android.settings.WEBVIEW_LICENSE");
    }

    @Test
    public void getAvailabilityStatus_systemApp_shouldReturnTrue() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(getTestResolveInfo(true /* isSystemApp */));
        when(mPackageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notSystemApp_shouldReturnFalseAndNoCrash() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(getTestResolveInfo(false /* isSystemApp */));
        when(mPackageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);

        when(mPackageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(null);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    /**
     * Returns a ResolveInfo object for testing
     *
     * @param isSystemApp If true, the application is a system app.
     */
    private ResolveInfo getTestResolveInfo(boolean isSystemApp) {
        final ResolveInfo testResolveInfo = new ResolveInfo();
        final ApplicationInfo testAppInfo = new ApplicationInfo();
        if (isSystemApp) {
            testAppInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
        }
        final ActivityInfo testActivityInfo = new ActivityInfo();
        testActivityInfo.name = "TestActivityName";
        testActivityInfo.packageName = "TestPackageName";
        testActivityInfo.applicationInfo = testAppInfo;
        testResolveInfo.activityInfo = testActivityInfo;
        return testResolveInfo;
    }
}
