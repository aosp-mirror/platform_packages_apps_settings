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

package com.android.settings.display;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.support.v7.preference.ListPreference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.display.ThemePreferenceController.OverlayManager;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ThemePreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private ListPreference mPreference;

    private ThemePreferenceController mController;

    @Before
    public void setUp() throws NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        when(mPackageManager.getApplicationInfo(any(), anyInt())).thenReturn(mApplicationInfo);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getString(R.string.default_theme))
                .thenReturn(RuntimeEnvironment.application.getString(R.string.default_theme));

        mController = spy(new ThemePreferenceController(mContext, mock(OverlayManager.class)));
    }

    @Test
    public void updateState_themeSet_shouldSetPreferenceValue() throws NameNotFoundException {
        final String pkg1 = "pkg1.theme1";
        final String pkg2 = "pkg2.theme2";
        final String themeLabel1 = "Theme1";
        final String themeLabel2 = "Theme2";
        final String[] themes = {pkg1, pkg2};
        doReturn("pkg1.theme1").when(mController).getCurrentTheme();
        doReturn(themes).when(mController).getAvailableThemes();
        when(mPackageManager.getApplicationInfo(anyString(), anyInt()).loadLabel(mPackageManager))
                .thenReturn(themeLabel1)
                .thenReturn(themeLabel2);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(themeLabel1);
        verify(mPreference).setValue(pkg1);
    }

    @Test
    public void updateState_themeNull_shouldSetDefaultSummary() throws NameNotFoundException {
        final String pkg1 = "pkg1.theme1";
        final String pkg2 = "pkg2.theme2";
        final String themeLabel1 = "Theme1";
        final String themeLabel2 = "Theme2";
        final String[] themes = {pkg1, pkg2};
        doReturn(null).when(mController).getCurrentTheme();
        doReturn(themes).when(mController).getAvailableThemes();
        when(mPackageManager.getApplicationInfo(anyString(), anyInt()).loadLabel(mPackageManager))
                .thenReturn(themeLabel1)
                .thenReturn(themeLabel2);

        mController.updateState(mPreference);

        verify(mPreference)
                .setSummary(RuntimeEnvironment.application.getString(R.string.default_theme));
        verify(mPreference).setValue(null);
    }
}
