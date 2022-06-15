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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import androidx.preference.ListPreference;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class ThemePreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private ListPreference mPreference;
    @Mock
    private IOverlayManager mOverlayManager;

    private ThemePreferenceController mController;

    @Before
    public void setUp() throws NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        when(mPackageManager.getApplicationInfo(any(), anyInt())).thenReturn(mApplicationInfo);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getString(R.string.default_theme))
                .thenReturn(RuntimeEnvironment.application.getString(R.string.default_theme));

        when(mContext.getSystemService(Context.OVERLAY_SERVICE)).thenReturn(mOverlayManager);
        mController = spy(new ThemePreferenceController(mContext, mOverlayManager));
    }

    @Test
    public void testAvailable_false() throws Exception {
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(
                new PackageInfo());
        when(mOverlayManager.getOverlayInfosForTarget(any(), anyInt()))
                .thenReturn(Arrays.asList(new OverlayInfo("", "", "", "", "", 0, 0, 0, false)));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testAvailable_true() throws Exception {
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(
                new PackageInfo());
        when(mOverlayManager.getOverlayInfosForTarget(any(), anyInt()))
                .thenReturn(Arrays.asList(
                        new OverlayInfo("", "", "", OverlayInfo.CATEGORY_THEME, "", 0, 0, 0, true),
                        new OverlayInfo("", "", "", OverlayInfo.CATEGORY_THEME, "", 0, 0, 0,
                                true)));
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_themeSet_shouldSetPreferenceValue() throws NameNotFoundException {
        final String pkg1 = "pkg1.theme1";
        final String pkg2 = "pkg2.theme2";
        final String themeLabel1 = "Theme1";
        final String themeLabel2 = "Theme2";
        final String[] themes = {pkg1, pkg2};
        doReturn("pkg1.theme1").when(mController).getCurrentTheme();
        doReturn(themes).when(mController).getAvailableThemes(false /* currentThemeOnly */);
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
        doReturn(themes).when(mController).getAvailableThemes(false /* currentThemeOnly */);
        when(mPackageManager.getApplicationInfo(anyString(), anyInt()).loadLabel(mPackageManager))
                .thenReturn(themeLabel1)
                .thenReturn(themeLabel2);

        mController.updateState(mPreference);

        verify(mPreference)
                .setSummary(RuntimeEnvironment.application.getString(R.string.default_theme));
        verify(mPreference).setValue(null);
    }

    @Test
    public void getCurrentTheme_withEnabledState() throws Exception {
        OverlayInfo info1 = new OverlayInfo("com.android.Theme1", "android", "",
                OverlayInfo.CATEGORY_THEME, "", OverlayInfo.STATE_ENABLED, 0, 0, true);
        OverlayInfo info2 = new OverlayInfo("com.android.Theme2", "android", "",
                OverlayInfo.CATEGORY_THEME, "", 0, 0, 0, true);
        when(mOverlayManager.getOverlayInfosForTarget(any(), anyInt())).thenReturn(
                Arrays.asList(info1, info2));
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(
                new PackageInfo());

        assertThat(mController.getCurrentTheme()).isEqualTo(info1.packageName);
    }

    @Test
    public void testGetCurrentTheme_withoutEnabledState() throws Exception {
        OverlayInfo info1 = new OverlayInfo("com.android.Theme1", "android", "",
                OverlayInfo.CATEGORY_THEME, "", OverlayInfo.STATE_DISABLED, 0, 0, true);
        OverlayInfo info2 = new OverlayInfo("com.android.Theme2", "android", "",
                OverlayInfo.CATEGORY_THEME, "", 0, 0, 0, true);
        when(mOverlayManager.getOverlayInfosForTarget(any(), anyInt())).thenReturn(
                Arrays.asList(info1, info2));
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(
                new PackageInfo());

        assertThat(mController.getCurrentTheme()).isNull();
    }
}
