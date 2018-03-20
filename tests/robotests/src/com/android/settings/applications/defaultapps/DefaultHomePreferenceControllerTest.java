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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.wrapper.PackageManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;
import java.util.Collections;

@RunWith(SettingsRobolectricTestRunner.class)
public class DefaultHomePreferenceControllerTest {

    @Mock
    private UserManager mUserManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PackageManagerWrapper mPackageManager;

    private Context mContext;
    private DefaultHomePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mController = spy(new DefaultHomePreferenceController(mContext));
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);
    }

    @Test
    public void testDefaultHome_byDefault_shouldBeShown() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testDefaultHome_ifDisabled_shouldNotBeShown() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getDefaultApp_shouldGetDefaultBrowserPackage() {
        assertThat(mController.getDefaultAppInfo()).isNotNull();

        verify(mPackageManager).getHomeActivities(anyList());
    }

    @Test
    public void getDefaultApp_noDefaultHome_shouldReturnNull() {
        when(mPackageManager.getHomeActivities(anyList())).thenReturn(null);

        assertThat(mController.getDefaultAppInfo()).isNull();
    }

    @Test
    public void updateState_noDefaultApp_shouldAskPackageManagerForOnlyApp() {
        when(mPackageManager.getHomeActivities(anyList())).thenReturn(null);
        mController.updateState(mock(Preference.class));

        verify(mPackageManager, atLeastOnce()).getHomeActivities(anyList());
    }

    @Test
    public void testIsHomeDefault_noDefaultSet_shouldReturnTrue() {
        when(mPackageManager.getHomeActivities(anyList())).thenReturn(null);
        assertThat(DefaultHomePreferenceController.isHomeDefault("test.pkg", mPackageManager))
                .isTrue();
    }

    @Test
    public void testIsHomeDefault_defaultSetToPkg_shouldReturnTrue() {
        final String pkgName = "test.pkg";
        final ComponentName defaultHome = new ComponentName(pkgName, "class");

        when(mPackageManager.getHomeActivities(anyList())).thenReturn(defaultHome);

        assertThat(DefaultHomePreferenceController.isHomeDefault(pkgName, mPackageManager))
                .isTrue();
    }

    @Test
    public void testIsHomeDefault_defaultSetToOtherPkg_shouldReturnFalse() {
        final String pkgName = "test.pkg";
        final ComponentName defaultHome = new ComponentName("not" + pkgName, "class");

        when(mPackageManager.getHomeActivities(anyList())).thenReturn(defaultHome);

        assertThat(DefaultHomePreferenceController.isHomeDefault(pkgName, mPackageManager))
                .isFalse();
    }

    @Test
    public void testGetSettingIntent_homeHasNoSetting_shouldNotReturnSettingIntent() {
        when(mPackageManager.getHomeActivities(anyList()))
            .thenReturn(new ComponentName("test.pkg", "class"));
        assertThat(mController.getSettingIntent(mController.getDefaultAppInfo())).isNull();
    }

    @Test
    public void testGetSettingIntent_homeHasOneSetting_shouldReturnSettingIntent() {
        when(mPackageManager.getHomeActivities(anyList()))
            .thenReturn(new ComponentName("test.pkg", "class"));
        when(mPackageManager.queryIntentActivities(any(), eq(0)))
            .thenReturn(Collections.singletonList(mock(ResolveInfo.class)));

        Intent intent = mController.getSettingIntent(mController.getDefaultAppInfo());
        assertThat(intent).isNotNull();
        assertThat(intent.getPackage()).isEqualTo("test.pkg");
    }

    @Test
    public void testGetSettingIntent_homeHasMultipleSettings_shouldNotReturnSettingIntent() {
        when(mPackageManager.getHomeActivities(anyList()))
            .thenReturn(new ComponentName("test.pkg", "class"));
        when(mPackageManager.queryIntentActivities(any(), eq(0)))
            .thenReturn(Arrays.asList(mock(ResolveInfo.class), mock(ResolveInfo.class)));
        assertThat(mController.getSettingIntent(mController.getDefaultAppInfo())).isNull();
    }

    @Test
    public void testGetSettingIntent_noDefauldHome_shouldReturnNull() {
        when(mPackageManager.getHomeActivities(anyList())).thenReturn(null);
        assertThat(mController.getSettingIntent(mController.getDefaultAppInfo())).isNull();
    }

}
