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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;

import androidx.test.runner.AndroidJUnit4;

import com.android.settings.Settings;
import com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminAdd;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


@RunWith(AndroidJUnit4.class)
public class ActionDisabledLearnMoreButtonLauncherImplTest {

    private static final int ENFORCED_ADMIN_USER_ID = 123;
    private static final ComponentName ADMIN_COMPONENT =
            new ComponentName("some.package.name", "some.package.name.SomeClass");
    private static final String URL = "https://testexample.com";
    private static final Uri URI = Uri.parse(URL);

    @Mock
    private Activity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void showAdminPolicies_noComponent_works() {
        final EnforcedAdmin enforcedAdmin = createEnforcedAdmin(/* component= */ null);

        ActionDisabledLearnMoreButtonLauncherImpl.SHOW_ADMIN_POLICIES
                .accept(mActivity, enforcedAdmin);

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mActivity).startActivity(captor.capture());
        assertThat(captor.getValue().getComponent().getClassName())
                .isEqualTo(Settings.DeviceAdminSettingsActivity.class.getName());
    }

    @Test
    public void showAdminPolicies_withComponent_works() {
        final EnforcedAdmin enforcedAdmin = createEnforcedAdmin(ADMIN_COMPONENT);

        ActionDisabledLearnMoreButtonLauncherImpl.SHOW_ADMIN_POLICIES
                .accept(mActivity, enforcedAdmin);

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mActivity).startActivityAsUser(
                captor.capture(),
                eq(UserHandle.of(ENFORCED_ADMIN_USER_ID)));
        assertDeviceAdminAddIntent(captor.getValue());
    }

    @Test
    public void launchHelpPage_works() {
        ActionDisabledLearnMoreButtonLauncherImpl.LAUNCH_HELP_PAGE.accept(mActivity, URL);

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mActivity).startActivityAsUser(captor.capture(), eq(UserHandle.SYSTEM));
        assertActionViewIntent(captor.getValue());
    }

    private EnforcedAdmin createEnforcedAdmin(ComponentName component) {
        return new RestrictedLockUtils.EnforcedAdmin(
                component, UserHandle.of(ENFORCED_ADMIN_USER_ID));
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
