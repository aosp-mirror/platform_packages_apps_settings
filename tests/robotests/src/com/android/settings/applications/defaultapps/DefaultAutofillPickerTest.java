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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.testutils.shadow.ShadowSecureSettings;
import com.android.settingslib.applications.DefaultAppInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowProcess;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSecureSettings.class,
        ShadowFragment.class,
})
public class DefaultAutofillPickerTest {

    private static final String MAIN_APP_KEY = "main.foo.bar/foo.bar.Baz";
    private static final String MANAGED_APP_KEY = "managed.foo.bar/foo.bar.Baz";
    private static final int MANAGED_PROFILE_UID = 10;
    private static final int MAIN_PROFILE_UID = 0;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FragmentActivity mActivity;
    @Mock
    private UserManager mUserManager;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;

    private DefaultAutofillPicker mPicker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();

        Resources res = application.getResources();

        when(mActivity.getApplicationContext()).thenReturn(mActivity);
        when(mActivity.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(mAppOpsManager);
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mActivity.getTheme()).thenReturn(res.newTheme());
        when(mActivity.getResources()).thenReturn(res);

        mPicker = spy(new DefaultAutofillPicker());

        doReturn(application).when(mPicker).getContext();
        doReturn(mActivity).when(mPicker).getActivity();
        doReturn(res).when(mPicker).getResources();
        doReturn(mScreen).when(mPicker).getPreferenceScreen();

        doNothing().when(mPicker).onCreatePreferences(any(), any());
        doNothing().when(mPicker).updateCandidates();

        ReflectionHelpers.setField(mPicker, "mPm", mPackageManager);
    }

    @Test
    public void setAndGetDefaultAppKey_shouldUpdateDefaultAutoFill() {
        mPicker.onAttach((Context) mActivity);

        ReflectionHelpers.setField(
                mPicker, "mUserId", MAIN_PROFILE_UID * UserHandle.PER_USER_RANGE);
        assertThat(mPicker.setDefaultKey(MAIN_APP_KEY)).isTrue();
        ReflectionHelpers.setField(
                mPicker, "mUserId", MANAGED_PROFILE_UID * UserHandle.PER_USER_RANGE);
        assertThat(mPicker.setDefaultKey(MANAGED_APP_KEY)).isTrue();

        ReflectionHelpers.setField(
                mPicker, "mUserId", MAIN_PROFILE_UID * UserHandle.PER_USER_RANGE);
        assertThat(mPicker.getDefaultKey()).isEqualTo(MAIN_APP_KEY);
        ReflectionHelpers.setField(
                mPicker, "mUserId", MANAGED_PROFILE_UID * UserHandle.PER_USER_RANGE);
        assertThat(mPicker.getDefaultKey()).isEqualTo(MANAGED_APP_KEY);
    }

    @Test
    public void getConfirmationMessage_shouldNotBeNull() {
        mPicker.onAttach((Context) mActivity);

        final DefaultAppInfo info = mock(DefaultAppInfo.class);
        when(info.loadLabel()).thenReturn("test_app_name");
        assertThat(mPicker.getConfirmationMessage(info)).isNotNull();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void mUserId_shouldDeriveUidFromManagedCaller() {
        setupUserManager();
        setupCaller();
        ShadowProcess.setUid(MANAGED_PROFILE_UID * UserHandle.PER_USER_RANGE);

        mPicker.onAttach((Context) mActivity);
        mPicker.onCreate(null);

        assertUserId(MANAGED_PROFILE_UID);
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void mUserId_shouldDeriveUidFromMainCaller() {
        setupUserManager();
        setupCaller();
        ShadowProcess.setUid(MAIN_PROFILE_UID * UserHandle.PER_USER_RANGE);

        mPicker.onAttach((Context) mActivity);
        mPicker.onCreate(null);

        assertUserId(MAIN_PROFILE_UID);
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void mUserId_shouldDeriveUidFromManagedClick() {
        setupUserManager();
        setupClick(/* forWork= */ true);
        ShadowProcess.setUid(MAIN_PROFILE_UID * UserHandle.PER_USER_RANGE);

        mPicker.onAttach((Context) mActivity);
        mPicker.onCreate(null);

        assertUserId(MANAGED_PROFILE_UID);
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void mUserId_shouldDeriveUidFromMainClick() {
        setupUserManager();
        setupClick(/* forWork= */ false);
        ShadowProcess.setUid(MAIN_PROFILE_UID * UserHandle.PER_USER_RANGE);

        mPicker.onAttach((Context) mActivity);
        mPicker.onCreate(null);

        assertUserId(MAIN_PROFILE_UID);
    }

    private void setupUserManager() {
        UserHandle mainUserHandle = new UserHandle(MAIN_PROFILE_UID);
        UserHandle managedUserHandle = new UserHandle(MANAGED_PROFILE_UID);
        UserInfo managedUserInfo = new UserInfo(
                MANAGED_PROFILE_UID, "managed", UserInfo.FLAG_MANAGED_PROFILE);
        when(mUserManager.getUserProfiles())
                .thenReturn(Arrays.asList(mainUserHandle, managedUserHandle));
        when(mUserManager.getUserInfo(MANAGED_PROFILE_UID))
                .thenReturn(managedUserInfo);
        when(mUserManager.getProcessUserId()).thenReturn(MAIN_PROFILE_UID);
    }

    private void setupCaller() {
        Intent intent = new Intent();
        intent.putExtra("package_name", "any package name");
        when(mActivity.getIntent()).thenReturn(intent);
    }

    private void setupClick(boolean forWork) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("for_work", forWork);
        doReturn(bundle).when(mPicker).getArguments();
    }

    private void assertUserId(int userId) {
        assertThat((Integer) ReflectionHelpers.getField(mPicker, "mUserId")).isEqualTo(userId);
    }
}
