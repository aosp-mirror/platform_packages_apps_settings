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

package com.android.settings.dashboard.suggestions;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.service.settings.suggestions.Suggestion;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowSecureSettings.class)
public class SuggestionFeatureProviderImplTest {

    @Mock
    private Context mContext;
    @Mock
    private Suggestion mSuggestion;
    @Mock
    private ActivityManager mActivityManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private FingerprintManager mFingerprintManager;

    private ActivityInfo mActivityInfo;
    private SuggestionFeatureProviderImpl mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mActivityInfo = new ActivityInfo();
        mActivityInfo.packageName = "pkg";
        mActivityInfo.name = "class";
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        // Explicit casting to object due to MockitoCast bug
        when((Object) mContext.getSystemService(FingerprintManager.class))
                .thenReturn(mFingerprintManager);
        when(mSuggestion.getId()).thenReturn("test_id");
        when(mContext.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(mActivityManager);
        when(mActivityManager.isLowRamDevice()).thenReturn(false);

        mProvider = new SuggestionFeatureProviderImpl(mContext);
    }

    @Test
    public void getSuggestionServiceComponentName_shouldReturnAndroidPackage() {
        assertThat(mProvider.getSuggestionServiceComponent().getPackageName())
                .isEqualTo("com.android.settings.intelligence");
    }

    @Test
    public void isSuggestionEnabled_isLowMemoryDevice_shouldReturnFalse() {
        when(mActivityManager.isLowRamDevice()).thenReturn(true);

        assertThat(mProvider.isSuggestionEnabled(mContext)).isFalse();
    }

    @Test
    public void isSuggestionEnabled_isNotLowMemoryDevice_shouldReturnTrue() {
        when(mActivityManager.isLowRamDevice()).thenReturn(false);
        assertThat(mProvider.isSuggestionEnabled(mContext)).isTrue();
    }
}
