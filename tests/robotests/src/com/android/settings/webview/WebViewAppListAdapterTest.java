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

package com.android.settings.webview;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WebViewAppListAdapterTest {
    private Context mContext = RuntimeEnvironment.application;

    private final static UserInfo FIRST_USER = new UserInfo(0, "FIRST_USER", 0);
    private final static UserInfo SECOND_USER = new UserInfo(0, "SECOND_USER", 0);

    private final static String DEFAULT_PACKAGE_NAME = "DEFAULT_PACKAGE_NAME";

    @Test
    public void testDisabledReasonNullIfPackagesOk() {
        UserPackageWrapper packageForFirstUser = mock(UserPackageWrapper.class);
        when(packageForFirstUser.isEnabledPackage()).thenReturn(true);
        when(packageForFirstUser.isInstalledPackage()).thenReturn(true);

        UserPackageWrapper packageForSecondUser = mock(UserPackageWrapper.class);
        when(packageForSecondUser.isEnabledPackage()).thenReturn(true);
        when(packageForSecondUser.isInstalledPackage()).thenReturn(true);

        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        when(wvusWrapper.getPackageInfosAllUsers(
                any(), eq(DEFAULT_PACKAGE_NAME))).thenReturn(
                        Arrays.asList(packageForFirstUser, packageForSecondUser));

        assertThat(WebViewAppListAdapter.getDisabledReason(
                wvusWrapper, mContext, DEFAULT_PACKAGE_NAME)).isNull();
    }

    @Test
    public void testDisabledReasonForSingleUserDisabledPackage() {
        UserPackageWrapper packageForFirstUser = mock(UserPackageWrapper.class);
        when(packageForFirstUser.isEnabledPackage()).thenReturn(false);
        when(packageForFirstUser.isInstalledPackage()).thenReturn(true);
        when(packageForFirstUser.getUserInfo()).thenReturn(FIRST_USER);

        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        when(wvusWrapper.getPackageInfosAllUsers(any(), eq(DEFAULT_PACKAGE_NAME)
                )).thenReturn(Arrays.asList(packageForFirstUser));

        assertThat(WebViewAppListAdapter.getDisabledReason(wvusWrapper, mContext,
                DEFAULT_PACKAGE_NAME)).isEqualTo("Disabled for user " + FIRST_USER.name + "\n");
    }

    @Test
    public void testDisabledReasonForSingleUserUninstalledPackage() {
        UserPackageWrapper packageForFirstUser = mock(UserPackageWrapper.class);
        when(packageForFirstUser.isEnabledPackage()).thenReturn(true);
        when(packageForFirstUser.isInstalledPackage()).thenReturn(false);
        when(packageForFirstUser.getUserInfo()).thenReturn(FIRST_USER);

        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        when(wvusWrapper.getPackageInfosAllUsers(any(), eq(DEFAULT_PACKAGE_NAME)
                )).thenReturn(Arrays.asList(packageForFirstUser));

        assertThat(WebViewAppListAdapter.getDisabledReason(wvusWrapper, mContext,
                DEFAULT_PACKAGE_NAME)).isEqualTo("Uninstalled for user " + FIRST_USER.name + "\n");
    }

    @Test
    public void testDisabledReasonSeveralUsers() {
        UserPackageWrapper packageForFirstUser = mock(UserPackageWrapper.class);
        when(packageForFirstUser.isEnabledPackage()).thenReturn(false);
        when(packageForFirstUser.isInstalledPackage()).thenReturn(true);
        when(packageForFirstUser.getUserInfo()).thenReturn(FIRST_USER);

        UserPackageWrapper packageForSecondUser = mock(UserPackageWrapper.class);
        when(packageForSecondUser.isEnabledPackage()).thenReturn(true);
        when(packageForSecondUser.isInstalledPackage()).thenReturn(false);
        when(packageForSecondUser.getUserInfo()).thenReturn(SECOND_USER);

        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        when(wvusWrapper.getPackageInfosAllUsers(any(), eq(DEFAULT_PACKAGE_NAME)
                )).thenReturn(Arrays.asList(packageForFirstUser, packageForSecondUser));

        final String EXPECTED_DISABLED_REASON = String.format(
                "Disabled for user %s\nUninstalled for user %s\n",
                FIRST_USER.name, SECOND_USER.name);
        assertThat(WebViewAppListAdapter.getDisabledReason(
                wvusWrapper, mContext,DEFAULT_PACKAGE_NAME)).isEqualTo(EXPECTED_DISABLED_REASON);
    }

    /**
     * Ensure we only proclaim a package as uninstalled for a certain user if it's both uninstalled
     * and disabled.
     */
    @Test
    public void testDisabledReasonUninstalledAndDisabled() {
        UserPackageWrapper packageForFirstUser = mock(UserPackageWrapper.class);
        when(packageForFirstUser.isEnabledPackage()).thenReturn(false);
        when(packageForFirstUser.isInstalledPackage()).thenReturn(false);
        when(packageForFirstUser.getUserInfo()).thenReturn(FIRST_USER);

        UserPackageWrapper packageForSecondUser = mock(UserPackageWrapper.class);
        when(packageForSecondUser.isEnabledPackage()).thenReturn(true);
        when(packageForSecondUser.isInstalledPackage()).thenReturn(true);
        when(packageForSecondUser.getUserInfo()).thenReturn(SECOND_USER);

        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        when(wvusWrapper.getPackageInfosAllUsers(any(), eq(DEFAULT_PACKAGE_NAME)
                )).thenReturn(Arrays.asList(packageForFirstUser, packageForSecondUser));

        final String EXPECTED_DISABLED_REASON = String.format(
                "Uninstalled for user %s\n", FIRST_USER.name);
        assertThat(WebViewAppListAdapter.getDisabledReason(wvusWrapper, mContext,
                DEFAULT_PACKAGE_NAME)).isEqualTo(EXPECTED_DISABLED_REASON);
    }
}
