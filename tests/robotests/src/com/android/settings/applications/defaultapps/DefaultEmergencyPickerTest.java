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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
public class DefaultEmergencyPickerTest {
    private static final String TAG = DefaultEmergencyPickerTest.class.getSimpleName();
    private static final String TEST_APP_KEY = "test_app";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private RoleManager mRoleManager;

    private DefaultEmergencyPicker mPicker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.ROLE_SERVICE, mRoleManager);
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mPicker = spy(new DefaultEmergencyPicker());
        mPicker.onAttach(mActivity);

        ReflectionHelpers.setField(mPicker, "mPm", mPackageManager);
        when(mPicker.getContext()).thenReturn(RuntimeEnvironment.application);
    }

    @Test
    public void setDefaultAppKey_shouldUpdateDefault() {
        mPicker.setDefaultKey(TEST_APP_KEY);
        verify(mRoleManager).addRoleHolderAsUser(
            eq(RoleManager.ROLE_EMERGENCY),
            eq(TEST_APP_KEY),
            eq(0),
            any(UserHandle.class),
            any(Executor.class),
            any(Consumer.class));
    }

    @Test
    public void getDefaultAppKey_shouldReturnDefault() {
      when(mRoleManager.getRoleHolders(RoleManager.ROLE_EMERGENCY))
              .thenReturn(Arrays.asList(TEST_APP_KEY));
      assertThat(mPicker.getDefaultKey()).isEqualTo(TEST_APP_KEY);
    }
}
