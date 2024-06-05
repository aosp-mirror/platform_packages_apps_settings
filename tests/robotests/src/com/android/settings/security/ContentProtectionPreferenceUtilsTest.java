/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.security;

import static android.view.contentprotection.flags.Flags.FLAG_MANAGE_DEVICE_POLICY_ENABLED;

import static com.android.internal.R.string.config_defaultContentProtectionService;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.DeviceConfig;
import android.view.contentcapture.ContentCaptureManager;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class ContentProtectionPreferenceUtilsTest {

    private static final String PACKAGE_NAME = "com.test.package";

    private static final ComponentName COMPONENT_NAME =
            new ComponentName(PACKAGE_NAME, "TestClass");

    private static final UserHandle USER_HANDLE = UserHandle.of(111);

    private static final int PROCESS_USER_ID = 222;

    private final String mConfigDefaultContentProtectionService = COMPONENT_NAME.flattenToString();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private Context mMockContext;

    @Mock private Context mMockUserContext;

    @Mock private UserManager mMockUserManager;

    @Mock private DevicePolicyManager mMockDevicePolicyManager;

    @Mock private UserInfo mMockUserInfo;

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
    }

    @Test
    public void isAvailable_bothEnabled_true() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);
        when(mMockContext.getString(config_defaultContentProtectionService))
                .thenReturn(mConfigDefaultContentProtectionService);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isTrue();
    }

    @Test
    public void isAvailable_onlyUiEnabled_false() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }

    @Test
    public void isAvailable_onlyServiceEnabled_false() {
        when(mMockContext.getString(config_defaultContentProtectionService))
                .thenReturn(mConfigDefaultContentProtectionService);
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "false",
                /* makeDefault= */ false);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }

    @Test
    public void isAvailable_emptyComponentName_false() {
        when(mMockContext.getString(config_defaultContentProtectionService))
                .thenReturn("");
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }

    @Test
    public void isAvailable_blankComponentName_false() {
	when(mMockContext.getString(config_defaultContentProtectionService))
                .thenReturn("   ");
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }

    @Test
    public void isAvailable_invalidComponentName_false() {
        when(mMockContext.getString(config_defaultContentProtectionService))
                .thenReturn("invalid");

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }

    @Test
    public void isAvailable_bothDisabled_false() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "false",
                /* makeDefault= */ false);

        assertThat(ContentProtectionPreferenceUtils.isAvailable(mMockContext)).isFalse();
    }

    @Test
    public void getManagedProfile_noProfiles() {
        when(mMockContext.getSystemService(UserManager.class)).thenReturn(mMockUserManager);
        when(mMockUserManager.getUserProfiles()).thenReturn(List.of());

        UserHandle actual = ContentProtectionPreferenceUtils.getManagedProfile(mMockContext);

        assertThat(actual).isNull();
    }

    @Test
    public void getManagedProfile_notManaged() {
        when(mMockContext.getSystemService(UserManager.class)).thenReturn(mMockUserManager);
        when(mMockUserManager.getUserProfiles()).thenReturn(List.of(USER_HANDLE));
        when(mMockUserManager.getProcessUserId()).thenReturn(PROCESS_USER_ID);
        when(mMockUserManager.getUserInfo(USER_HANDLE.getIdentifier())).thenReturn(mMockUserInfo);

        UserHandle actual = ContentProtectionPreferenceUtils.getManagedProfile(mMockContext);

        assertThat(actual).isNull();
        verify(mMockUserInfo).isManagedProfile();
    }

    @Test
    public void getManagedProfile_managed() {
        when(mMockContext.getSystemService(UserManager.class)).thenReturn(mMockUserManager);
        when(mMockUserManager.getUserProfiles()).thenReturn(List.of(USER_HANDLE));
        when(mMockUserManager.getProcessUserId()).thenReturn(PROCESS_USER_ID);
        when(mMockUserManager.getUserInfo(USER_HANDLE.getIdentifier())).thenReturn(mMockUserInfo);
        when(mMockUserInfo.isManagedProfile()).thenReturn(true);

        UserHandle actual = ContentProtectionPreferenceUtils.getManagedProfile(mMockContext);

        assertThat(actual).isEqualTo(USER_HANDLE);
    }

    @Test
    public void getContentProtectionPolicy_flagDisabled_managedProfileNull() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);

        int actual =
                ContentProtectionPreferenceUtils.getContentProtectionPolicy(
                        mMockContext, /* managedProfile= */ null);

        assertThat(actual).isEqualTo(DevicePolicyManager.CONTENT_PROTECTION_DISABLED);
    }

    @Test
    public void getContentProtectionPolicy_flagDisabled_managedProfileNotNull() {
        mSetFlagsRule.disableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);

        int actual =
                ContentProtectionPreferenceUtils.getContentProtectionPolicy(
                        mMockContext, USER_HANDLE);

        assertThat(actual).isEqualTo(DevicePolicyManager.CONTENT_PROTECTION_DISABLED);
    }

    @Test
    public void getContentProtectionPolicy_flagEnabled_managedProfileNull() throws Exception {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        when(mMockContext.getSystemService(DevicePolicyManager.class))
                .thenReturn(mMockDevicePolicyManager);
        when(mMockDevicePolicyManager.getContentProtectionPolicy(/* admin= */ null))
                .thenReturn(DevicePolicyManager.CONTENT_PROTECTION_ENABLED);

        int actual =
                ContentProtectionPreferenceUtils.getContentProtectionPolicy(
                        mMockContext, /* managedProfile= */ null);

        assertThat(actual).isEqualTo(DevicePolicyManager.CONTENT_PROTECTION_ENABLED);
        verify(mMockContext, never()).createPackageContextAsUser(anyString(), anyInt(), any());
    }

    @Test
    public void getContentProtectionPolicy_flagEnabled_managedProfileNotNull() throws Exception {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        when(mMockContext.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mMockContext.createPackageContextAsUser(PACKAGE_NAME, /* flags= */ 0, USER_HANDLE))
                .thenReturn(mMockUserContext);
        when(mMockUserContext.getSystemService(DevicePolicyManager.class))
                .thenReturn(mMockDevicePolicyManager);
        when(mMockDevicePolicyManager.getContentProtectionPolicy(/* admin= */ null))
                .thenReturn(DevicePolicyManager.CONTENT_PROTECTION_ENABLED);

        int actual =
                ContentProtectionPreferenceUtils.getContentProtectionPolicy(
                        mMockContext, USER_HANDLE);

        assertThat(actual).isEqualTo(DevicePolicyManager.CONTENT_PROTECTION_ENABLED);
    }

    @Test
    public void getContentProtectionPolicy_flagEnabled_managedProfileNotNull_nameNotFound()
            throws Exception {
        mSetFlagsRule.enableFlags(FLAG_MANAGE_DEVICE_POLICY_ENABLED);
        when(mMockContext.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mMockContext.createPackageContextAsUser(PACKAGE_NAME, /* flags= */ 0, USER_HANDLE))
                .thenThrow(new PackageManager.NameNotFoundException());

        assertThrows(
                IllegalStateException.class,
                () ->
                        ContentProtectionPreferenceUtils.getContentProtectionPolicy(
                                mMockContext, USER_HANDLE));

        verify(mMockContext, never()).getSystemService(DevicePolicyManager.class);
    }
}
