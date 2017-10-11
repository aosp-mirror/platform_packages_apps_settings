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

package com.android.settings.applications.defaultapps;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.PackageManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DefaultBrowserPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PackageManagerWrapper mPackageManager;

    private DefaultBrowserPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mController = new DefaultBrowserPreferenceController(mContext);
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);
    }

    @Test
    public void isAvailable_noBrowser_shouldReturnFalse() {
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(null);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_hasBrowser_shouldReturnTrue() {
        final List<ResolveInfo> candidates = new ArrayList<>();
        candidates.add(new ResolveInfo());
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(candidates);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getSoleAppLabel_hasNoApp_shouldNotReturnLabel() {
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(null);
        final Preference pref = mock(Preference.class);

        mController.updateState(pref);
        verify(pref).setSummary(R.string.app_list_preference_none);
    }

    @Test
    public void getDefaultApp_shouldGetDefaultBrowserPackage() {
        mController.getDefaultAppInfo();

        verify(mPackageManager).getDefaultBrowserPackageNameAsUser(anyInt());
    }

    @Test
    public void isBrowserDefault_onlyApp_shouldReturnTrue() {
        final String testPkg = "pkg";
        when(mPackageManager.getDefaultBrowserPackageNameAsUser(anyInt()))
                .thenReturn(null);
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(Arrays.asList(new ResolveInfo()));

        assertThat(mController.isBrowserDefault(testPkg, 0)).isTrue();
    }
}
