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
 * limitations under the License
 */
package com.android.settings.system;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.preference.Preference;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FactoryResetPreferenceControllerTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String FACTORY_RESET_KEY = "factory_reset";
    private static final String FACTORY_RESET_APP_PACKAGE = "com.frw_app";

    @Mock private ActivityResultLauncher<Intent> mFactoryResetLauncher;
    @Mock private Preference mPreference;
    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;
    @Mock private UserManager mUserManager;
    private ResolveInfo mFactoryResetAppResolveInfo;
    private PackageInfo mFactoryResetAppPackageInfo;

    private FactoryResetPreferenceController mController;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = new FactoryResetPreferenceController(mContext, FACTORY_RESET_KEY);
        mFactoryResetAppResolveInfo = new ResolveInfo();
        mFactoryResetAppResolveInfo.activityInfo = new ActivityInfo();
        mFactoryResetAppResolveInfo.activityInfo.packageName = FACTORY_RESET_APP_PACKAGE;
        mFactoryResetAppPackageInfo = new PackageInfo();
        mFactoryResetAppPackageInfo.requestedPermissions =
                new String[] {Manifest.permission.PREPARE_FACTORY_RESET};
        mFactoryResetAppPackageInfo.requestedPermissionsFlags = new int[] {
                PackageInfo.REQUESTED_PERMISSION_GRANTED
        };
        when(mPackageManager.resolveActivity(any(), anyInt()))
                .thenReturn(mFactoryResetAppResolveInfo);
        when(mPackageManager.getPackageInfo(anyString(), anyInt()))
                .thenReturn(mFactoryResetAppPackageInfo);
        when(mPreference.getKey()).thenReturn(FACTORY_RESET_KEY);
        mController.mFactoryResetPreparationLauncher = mFactoryResetLauncher;

    }

    @After
    public void tearDown() {
        Mockito.reset(mUserManager, mPackageManager);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 0);
    }

    @Ignore("b/314930928")
    @Test
    public void isAvailable_systemUser() {
        when(mUserManager.isAdminUser()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_nonSystemUser() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        when(mUserManager.isDemoUser()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_demoUser() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        // Place the device in demo mode.
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 1);

        // Indicate the user is a demo user.
        when(mUserManager.getUserProfiles())
                .thenReturn(ImmutableList.of(new UserHandle(UserHandle.myUserId())));
        when(mUserManager.getUserInfo(eq(UserHandle.myUserId())))
                .thenReturn(new UserInfo(UserHandle.myUserId(), "test", UserInfo.FLAG_DEMO));

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(FACTORY_RESET_KEY);
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.factory_reset.Flags.FLAG_ENABLE_FACTORY_RESET_WIZARD)
    public void handlePreference_factoryResetWizardEnabled() {
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();
        verify(mFactoryResetLauncher).launch(intentArgumentCaptor.capture());
        assertThat(intentArgumentCaptor.getValue()).isNotNull();
        assertThat(intentArgumentCaptor.getValue().getAction())
                .isEqualTo(FactoryResetPreferenceController.ACTION_PREPARE_FACTORY_RESET);
    }
}
