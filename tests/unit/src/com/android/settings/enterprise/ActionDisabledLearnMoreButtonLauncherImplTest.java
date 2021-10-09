/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.enterprise;

import static android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN;

import static com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminAdd.EXTRA_CALLED_FROM_SUPPORT_DIALOG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.appcompat.app.AlertDialog;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.Settings;
import com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminAdd;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class ActionDisabledLearnMoreButtonLauncherImplTest {

    private static final int ENFORCED_ADMIN_USER_ID = 123;
    private static final UserHandle ENFORCED_ADMIN_USER = UserHandle.of(ENFORCED_ADMIN_USER_ID);

    private static final int CONTEXT_USER_ID = -ENFORCED_ADMIN_USER_ID;
    private static final UserHandle CONTEXT_USER = UserHandle.of(CONTEXT_USER_ID);

    private static final ComponentName ADMIN_COMPONENT =
            new ComponentName("some.package.name", "some.package.name.SomeClass");
    private static final String URL = "https://testexample.com";
    private static final Uri URI = Uri.parse(URL);

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private Activity mActivity;

    @Captor
    private ArgumentCaptor<Intent> mIntentCaptor;

    @Mock
    private AlertDialog.Builder mBuilder;

    private ActionDisabledLearnMoreButtonLauncherImpl mImpl;

    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        // Can't mock getSystemService(Class) directly because it's final
        when(mActivity.getSystemServiceName(UserManager.class)).thenReturn(Context.USER_SERVICE);
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        when(mActivity.getUserId()).thenReturn(CONTEXT_USER_ID);
        when(mUserManager.getProcessUserId()).thenReturn(CONTEXT_USER_ID);

        mImpl = new ActionDisabledLearnMoreButtonLauncherImpl(mActivity, mBuilder);
    }

    @Test
    public void launchShowAdminSettings_works() {
        mImpl.launchShowAdminSettings(mActivity);

        verify(mActivity).startActivity(mIntentCaptor.capture());
        assertDeviceAdminSettingsActivity(mIntentCaptor.getValue());
    }

    @Test
    public void launchShowAdminPolicies_works() {
        mImpl.launchShowAdminPolicies(mActivity, ENFORCED_ADMIN_USER, ADMIN_COMPONENT);

        verify(mActivity).startActivityAsUser(mIntentCaptor.capture(), eq(ENFORCED_ADMIN_USER));
        assertDeviceAdminAddIntent(mIntentCaptor.getValue());
    }

    @Test
    public void showHelpPage_works() {
        mImpl.showHelpPage(mActivity, URL, CONTEXT_USER);

        verify(mActivity).startActivityAsUser(mIntentCaptor.capture(), eq(CONTEXT_USER));
        assertActionViewIntent(mIntentCaptor.getValue());
    }

    private void assertDeviceAdminSettingsActivity(Intent intent) {
        assertThat(intent.getComponent().getClassName())
                .isEqualTo(Settings.DeviceAdminSettingsActivity.class.getName());
    }

    private void assertDeviceAdminAddIntent(Intent intent) {
        assertThat(intent.getComponent().getClassName())
                .isEqualTo(DeviceAdminAdd.class.getName());
        assertThat((ComponentName) intent.getParcelableExtra(EXTRA_DEVICE_ADMIN))
                .isEqualTo(ADMIN_COMPONENT);
        assertThat(intent.getBooleanExtra(
                EXTRA_CALLED_FROM_SUPPORT_DIALOG,
                /* defaultValue= */ false))
                .isTrue();
    }

    private void assertActionViewIntent(Intent intent) {
        assertThat(intent.getAction())
                .isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData())
                .isEqualTo(URI);
    }
}
