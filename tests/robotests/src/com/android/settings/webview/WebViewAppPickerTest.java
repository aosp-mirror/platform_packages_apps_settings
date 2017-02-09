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

import static android.provider.Settings.ACTION_WEBVIEW_SETTINGS;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.os.UserManager;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settings.applications.PackageManagerWrapper;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WebViewAppPickerTest {
    private Context mContext = RuntimeEnvironment.application;

    private final static UserInfo FIRST_USER = new UserInfo(0, "FIRST_USER", 0);
    private final static UserInfo SECOND_USER = new UserInfo(0, "SECOND_USER", 0);

    private final static String DEFAULT_PACKAGE_NAME = "DEFAULT_PACKAGE_NAME";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManagerWrapper mPackageManager;

    private WebViewAppPicker mPicker;
    private WebViewUpdateServiceWrapper mWvusWrapper;

    private static ApplicationInfo createApplicationInfo(String packageName) {
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = packageName;
        return ai;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mPicker = new WebViewAppPicker();
        mPicker = spy(mPicker);
        doNothing().when(mPicker).updateCandidates();
        doNothing().when(mPicker).updateCheckedState(any());
        doReturn(mActivity).when(mPicker).getActivity();

        mWvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        mPicker.setWebViewUpdateServiceWrapper(mWvusWrapper);
    }

    @Test
    public void testClickingItemChangesProvider() {
        testSuccessfulClickChangesProvider();
    }

    @Test
    public void testFailingClick() {
        testFailingClickUpdatesSetting();
    }

    @Test
    public void testClickingItemInActivityModeChangesProviderAndFinishes() {
        useWebViewSettingIntent();
        testSuccessfulClickChangesProvider();
        verify(mActivity, times(1)).finish();
    }

    @Test
    public void testFailingClickInActivityMode() {
        useWebViewSettingIntent();
        testFailingClick();
    }

    private void useWebViewSettingIntent() {
        Intent intent = new Intent(ACTION_WEBVIEW_SETTINGS);
        when(mActivity.getIntent()).thenReturn(intent);
    }

    private void testSuccessfulClickChangesProvider() {
        when(mWvusWrapper.getValidWebViewApplicationInfos(any())).thenReturn(
                Arrays.asList(createApplicationInfo(DEFAULT_PACKAGE_NAME)));
        when(mWvusWrapper.setWebViewProvider(eq(DEFAULT_PACKAGE_NAME))).thenReturn(true);

        RadioButtonPreference defaultPackagePref = mock(RadioButtonPreference.class);
        when(defaultPackagePref.getKey()).thenReturn(DEFAULT_PACKAGE_NAME);
        mPicker.onRadioButtonClicked(defaultPackagePref);

        verify(mWvusWrapper, times(1)).setWebViewProvider(eq(DEFAULT_PACKAGE_NAME));
        verify(mPicker, times(1)).updateCheckedState(DEFAULT_PACKAGE_NAME);
        verify(mWvusWrapper, never()).showInvalidChoiceToast(any());
    }

    private void testFailingClickUpdatesSetting() {
        when(mWvusWrapper.getValidWebViewApplicationInfos(any())).thenReturn(
                Arrays.asList(createApplicationInfo(DEFAULT_PACKAGE_NAME)));
        when(mWvusWrapper.setWebViewProvider(eq(DEFAULT_PACKAGE_NAME))).thenReturn(false);

        RadioButtonPreference defaultPackagePref = mock(RadioButtonPreference.class);
        when(defaultPackagePref.getKey()).thenReturn(DEFAULT_PACKAGE_NAME);
        mPicker.onRadioButtonClicked(defaultPackagePref);

        verify(mWvusWrapper, times(1)).setWebViewProvider(eq(DEFAULT_PACKAGE_NAME));
        // Ensure we update the list of packages when we click a non-valid package - the list must
        // have changed, otherwise this click wouldn't fail.
        verify(mPicker, times(1)).updateCandidates();
        verify(mWvusWrapper, times(1)).showInvalidChoiceToast(any());
    }

    @Test
    public void testFinishIfNotAdmin() {
        doReturn(false).when(mUserManager).isAdminUser();
        mPicker.onAttach((Context) mActivity);
        verify(mActivity, times(1)).finish();
    }

    @Test
    public void testNotFinishedIfAdmin() {
        doReturn(true).when(mUserManager).isAdminUser();
        mPicker.onAttach((Context) mActivity);
        verify(mActivity, never()).finish();
    }

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

        assertThat(mPicker.getDisabledReason(wvusWrapper, mContext, DEFAULT_PACKAGE_NAME)).isNull();
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

        final String EXPECTED_DISABLED_REASON = String.format(
                "(disabled for user %s)", FIRST_USER.name);
        assertThat(mPicker.getDisabledReason(wvusWrapper, mContext,
                DEFAULT_PACKAGE_NAME)).isEqualTo(EXPECTED_DISABLED_REASON);
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

        final String EXPECTED_DISABLED_REASON = String.format(
                "(uninstalled for user %s)", FIRST_USER.name);
        assertThat(mPicker.getDisabledReason(wvusWrapper, mContext,
                DEFAULT_PACKAGE_NAME)).isEqualTo(EXPECTED_DISABLED_REASON);
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
                "(disabled for user %s)", FIRST_USER.name);
        assertThat(mPicker.getDisabledReason(
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
                "(uninstalled for user %s)", FIRST_USER.name);
        assertThat(mPicker.getDisabledReason(wvusWrapper, mContext,
                DEFAULT_PACKAGE_NAME)).isEqualTo(EXPECTED_DISABLED_REASON);
    }
}
