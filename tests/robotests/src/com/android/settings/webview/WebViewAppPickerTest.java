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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.UserInfo;
import android.graphics.drawable.ColorDrawable;
import android.webkit.UserPackage;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class WebViewAppPickerTest {

    private final static String PACKAGE_NAME = "com.example.test";
    private final static String PACKAGE_VERSION = "1.0.0";

    @Mock
    private FragmentActivity mActivity;

    private Context mContext;
    private UserInfo mFirstUser;
    private UserInfo mSecondUser;
    private ShadowPackageManager mPackageManager;
    private WebViewAppPicker mPicker;
    private WebViewUpdateServiceWrapper mWvusWrapper;
    private ShadowUserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mUserManager = Shadow.extract(mContext.getSystemService(Context.USER_SERVICE));
        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());

        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.name = PACKAGE_NAME;
        applicationInfo.uid = 0;
        applicationInfo.flags = 0;
        applicationInfo.packageName = PACKAGE_NAME;

        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = PACKAGE_NAME;
        packageInfo.applicationInfo = applicationInfo;
        packageInfo.versionName = PACKAGE_VERSION;
        mPackageManager.addPackage(packageInfo);
        mPackageManager.setUnbadgedApplicationIcon(PACKAGE_NAME, new ColorDrawable());

        mFirstUser = new UserInfo(0, "FIRST_USER", 0);
        mSecondUser = new UserInfo(0, "SECOND_USER", 0);
        mPicker = new WebViewAppPicker();
        mPicker = spy(mPicker);
        doNothing().when(mPicker).updateCandidates();
        doNothing().when(mPicker).updateCheckedState(any());
        doReturn(mActivity).when(mPicker).getActivity();

        ReflectionHelpers.setField(mPicker, "mMetricsFeatureProvider",
                mock(MetricsFeatureProvider.class));
        mWvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        mPicker.setWebViewUpdateServiceWrapper(mWvusWrapper);
    }

    @After
    public void tearDown() {
        mPackageManager.removePackage(PACKAGE_NAME);
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

    @Test
    public void testDisabledPackageShownAsDisabled() {
        DefaultAppInfo webviewAppInfo = mPicker.createDefaultAppInfo(mContext,
                mContext.getPackageManager(),
                createApplicationInfo(PACKAGE_NAME), "disabled");

        SelectorWithWidgetPreference preference = mock(SelectorWithWidgetPreference.class);
        mPicker.bindPreference(preference, PACKAGE_NAME, webviewAppInfo, null);
        mPicker.bindPreferenceExtra(preference, PACKAGE_NAME, webviewAppInfo, null, null);
        verify(preference, times(1)).setEnabled(eq(false));
        verify(preference, never()).setEnabled(eq(true));
    }

    @Test
    public void testEnabledPackageShownAsEnabled() {
        String disabledReason = "";
        DefaultAppInfo webviewAppInfo = mPicker.createDefaultAppInfo(mContext,
                mContext.getPackageManager(),
                createApplicationInfo(PACKAGE_NAME), disabledReason);

        SelectorWithWidgetPreference preference = mock(SelectorWithWidgetPreference.class);
        mPicker.bindPreference(preference, PACKAGE_NAME, webviewAppInfo, null);
        mPicker.bindPreferenceExtra(preference, PACKAGE_NAME, webviewAppInfo, null, null);
        verify(preference, times(1)).setEnabled(eq(true));
        verify(preference, never()).setEnabled(eq(false));
    }

    @Test
    public void testDisabledPackageShowsDisabledReasonSummary() {
        String disabledReason = "disabled";
        DefaultAppInfo webviewAppInfo = mPicker.createDefaultAppInfo(mContext,
                mContext.getPackageManager(),
                createApplicationInfo(PACKAGE_NAME), disabledReason);

        SelectorWithWidgetPreference preference = mock(SelectorWithWidgetPreference.class);
        mPicker.bindPreference(preference, PACKAGE_NAME, webviewAppInfo, null);
        mPicker.bindPreferenceExtra(preference, PACKAGE_NAME, webviewAppInfo, null, null);
        verify(preference, times(1)).setSummary(eq(disabledReason));
        // Ensure we haven't called setSummary several times.
        verify(preference, times(1)).setSummary(any());
    }

    @Test
    public void testEnabledPackageShowsEmptySummary() {
        DefaultAppInfo webviewAppInfo = mPicker.createDefaultAppInfo(mContext,
                mContext.getPackageManager(),
                createApplicationInfo(PACKAGE_NAME), null);

        SelectorWithWidgetPreference preference = mock(SelectorWithWidgetPreference.class);
        mPicker.bindPreference(preference, PACKAGE_NAME, webviewAppInfo, null);
        mPicker.bindPreferenceExtra(preference, PACKAGE_NAME, webviewAppInfo, null, null);
        verify(preference, never()).setSummary(any());
    }

    @Test
    public void testFinishIfNotAdmin() {
        mUserManager.setIsAdminUser(false);
        mPicker.onAttach(mContext);
        verify(mActivity, times(1)).finish();
    }

    @Ignore("b/313615637")
    @Test
    public void testNotFinishedIfAdmin() {
        mUserManager.setIsAdminUser(true);
        mPicker.onAttach(mContext);
        verify(mActivity, never()).finish();
    }

    @Test
    public void testDisabledReasonNullIfPackagesOk() {
        UserPackage packageForFirstUser = mock(UserPackage.class);
        when(packageForFirstUser.isEnabledPackage()).thenReturn(true);
        when(packageForFirstUser.isInstalledPackage()).thenReturn(true);

        UserPackage packageForSecondUser = mock(UserPackage.class);
        when(packageForSecondUser.isEnabledPackage()).thenReturn(true);
        when(packageForSecondUser.isInstalledPackage()).thenReturn(true);

        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        when(wvusWrapper.getPackageInfosAllUsers(any(), eq(PACKAGE_NAME)))
                .thenReturn(Arrays.asList(packageForFirstUser, packageForSecondUser));

        assertThat(mPicker.getDisabledReason(wvusWrapper, mContext, PACKAGE_NAME)).isNull();
    }

    @Test
    public void testDisabledReasonForSingleUserDisabledPackage() {
        UserPackage packageForFirstUser = mock(UserPackage.class);
        when(packageForFirstUser.isEnabledPackage()).thenReturn(false);
        when(packageForFirstUser.isInstalledPackage()).thenReturn(true);
        when(packageForFirstUser.getUserInfo()).thenReturn(mFirstUser);

        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        when(wvusWrapper.getPackageInfosAllUsers(any(), eq(PACKAGE_NAME)))
                .thenReturn(Collections.singletonList(packageForFirstUser));

        final String expectedReason = String.format("(disabled for user %s)", mFirstUser.name);
        assertThat(mPicker.getDisabledReason(wvusWrapper, mContext, PACKAGE_NAME))
                .isEqualTo(expectedReason);
    }

    @Test
    public void testDisabledReasonForSingleUserUninstalledPackage() {
        UserPackage packageForFirstUser = mock(UserPackage.class);
        when(packageForFirstUser.isEnabledPackage()).thenReturn(true);
        when(packageForFirstUser.isInstalledPackage()).thenReturn(false);
        when(packageForFirstUser.getUserInfo()).thenReturn(mFirstUser);

        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        when(wvusWrapper.getPackageInfosAllUsers(any(), eq(PACKAGE_NAME)))
                .thenReturn(Collections.singletonList(packageForFirstUser));

        final String expectedReason = String.format("(uninstalled for user %s)", mFirstUser.name);
        assertThat(mPicker.getDisabledReason(wvusWrapper, mContext, PACKAGE_NAME))
                .isEqualTo(expectedReason);
    }

    @Test
    public void testDisabledReasonSeveralUsers() {
        UserPackage packageForFirstUser = mock(UserPackage.class);
        when(packageForFirstUser.isEnabledPackage()).thenReturn(false);
        when(packageForFirstUser.isInstalledPackage()).thenReturn(true);
        when(packageForFirstUser.getUserInfo()).thenReturn(mFirstUser);

        UserPackage packageForSecondUser = mock(UserPackage.class);
        when(packageForSecondUser.isEnabledPackage()).thenReturn(true);
        when(packageForSecondUser.isInstalledPackage()).thenReturn(false);
        when(packageForSecondUser.getUserInfo()).thenReturn(mSecondUser);

        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        when(wvusWrapper.getPackageInfosAllUsers(any(), eq(PACKAGE_NAME)))
                .thenReturn(Arrays.asList(packageForFirstUser, packageForSecondUser));

        final String expectedReason = String.format("(disabled for user %s)", mFirstUser.name);
        assertThat(mPicker.getDisabledReason(wvusWrapper, mContext, PACKAGE_NAME))
                .isEqualTo(expectedReason);
    }

    /**
     * Ensure we only proclaim a package as uninstalled for a certain user if it's both uninstalled
     * and disabled.
     */
    @Test
    public void testDisabledReasonUninstalledAndDisabled() {
        UserPackage packageForFirstUser = mock(UserPackage.class);
        when(packageForFirstUser.isEnabledPackage()).thenReturn(false);
        when(packageForFirstUser.isInstalledPackage()).thenReturn(false);
        when(packageForFirstUser.getUserInfo()).thenReturn(mFirstUser);

        UserPackage packageForSecondUser = mock(UserPackage.class);
        when(packageForSecondUser.isEnabledPackage()).thenReturn(true);
        when(packageForSecondUser.isInstalledPackage()).thenReturn(true);
        when(packageForSecondUser.getUserInfo()).thenReturn(mSecondUser);

        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        when(wvusWrapper.getPackageInfosAllUsers(any(), eq(PACKAGE_NAME)))
                .thenReturn(Arrays.asList(packageForFirstUser, packageForSecondUser));

        final String expectedReason = String.format("(uninstalled for user %s)", mFirstUser.name);
        assertThat(mPicker.getDisabledReason(wvusWrapper, mContext, PACKAGE_NAME))
                .isEqualTo(expectedReason);
    }

    /**
     * Ensure that the version name of a WebView package is displayed after its name in the
     * preference title.
     */
    @Test
    public void testWebViewVersionAddedAfterLabel() {
        final DefaultAppInfo webviewAppInfo = mPicker.createDefaultAppInfo(mContext,
                mContext.getPackageManager(),
                createApplicationInfo(PACKAGE_NAME), "" /* disabledReason */);

        final SelectorWithWidgetPreference mockPreference =
                mock(SelectorWithWidgetPreference.class);
        mPicker.bindPreference(mockPreference, PACKAGE_NAME, webviewAppInfo, null);
        mPicker.bindPreferenceExtra(
                mockPreference, PACKAGE_NAME, webviewAppInfo, null, null);

        verify(mockPreference).setTitle(eq(PACKAGE_NAME + " " + PACKAGE_VERSION));
        verify(mockPreference).setTitle(any());
    }

    private static ApplicationInfo createApplicationInfo(String packageName) {
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = packageName;
        return ai;
    }

    private void useWebViewSettingIntent() {
        Intent intent = new Intent(ACTION_WEBVIEW_SETTINGS);
        when(mActivity.getIntent()).thenReturn(intent);
    }

    private void testSuccessfulClickChangesProvider() {
        when(mWvusWrapper.getValidWebViewApplicationInfos(any()))
                .thenReturn(Collections.singletonList(createApplicationInfo(PACKAGE_NAME)));
        when(mWvusWrapper.setWebViewProvider(eq(PACKAGE_NAME))).thenReturn(true);

        SelectorWithWidgetPreference defaultPackagePref = mock(SelectorWithWidgetPreference.class);
        when(defaultPackagePref.getKey()).thenReturn(PACKAGE_NAME);
        mPicker.onRadioButtonClicked(defaultPackagePref);

        verify(mWvusWrapper, times(1)).setWebViewProvider(eq(PACKAGE_NAME));
        verify(mPicker, times(1)).updateCheckedState(PACKAGE_NAME);
        verify(mWvusWrapper, never()).showInvalidChoiceToast(any());
    }

    private void testFailingClickUpdatesSetting() {
        when(mWvusWrapper.getValidWebViewApplicationInfos(any()))
                .thenReturn(Collections.singletonList(createApplicationInfo(PACKAGE_NAME)));
        when(mWvusWrapper.setWebViewProvider(eq(PACKAGE_NAME))).thenReturn(false);

        SelectorWithWidgetPreference defaultPackagePref = mock(SelectorWithWidgetPreference.class);
        when(defaultPackagePref.getKey()).thenReturn(PACKAGE_NAME);
        mPicker.onRadioButtonClicked(defaultPackagePref);

        verify(mWvusWrapper, times(1)).setWebViewProvider(eq(PACKAGE_NAME));
        // Ensure we update the list of packages when we click a non-valid package - the list must
        // have changed, otherwise this click wouldn't fail.
        verify(mPicker, times(1)).updateCandidates();
        verify(mWvusWrapper, times(1)).showInvalidChoiceToast(any());
    }
}
