/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.security.advancedprotection.AdvancedProtectionManager.ACTION_SHOW_ADVANCED_PROTECTION_SUPPORT_DIALOG;
import static android.security.advancedprotection.AdvancedProtectionManager.ADVANCED_PROTECTION_SYSTEM_ENTITY;
import static android.security.advancedprotection.AdvancedProtectionManager.EXTRA_SUPPORT_DIALOG_FEATURE;
import static android.security.advancedprotection.AdvancedProtectionManager.EXTRA_SUPPORT_DIALOG_TYPE;
import static android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES;
import static android.security.advancedprotection.AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_UNKNOWN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.Authority;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.UnknownAuthority;
import android.content.ComponentName;
import android.content.Intent;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ActionDisabledByAdminDialogTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private ActionDisabledByAdminDialog mDialog;
    private final ComponentName mAdminComponent = new ComponentName("admin", "adminclass");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mDialog = spy(new ActionDisabledByAdminDialog());
        doReturn(mDevicePolicyManager).when(mDialog).getSystemService(DevicePolicyManager.class);
    }

    @Test
    public void testGetAdminDetailsFromIntent() {
        final int userId = 123;
        final EnforcedAdmin expectedAdmin = new EnforcedAdmin(mAdminComponent, UserHandle.of(
                userId));

        final Intent intent = new Intent();
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminComponent);
        intent.putExtra(Intent.EXTRA_USER_ID, userId);
        assertEquals(expectedAdmin, mDialog.getAdminDetailsFromIntent(intent));
    }

    @Test
    public void testGetAdminDetailsFromNullIntent() {
        final int userId = UserHandle.myUserId();
        final EnforcedAdmin expectedAdmin = new EnforcedAdmin(null, UserHandle.of(userId));

        assertEquals(expectedAdmin, mDialog.getAdminDetailsFromIntent(null));
    }

    @Test
    public void testGetRestrictionFromIntent() {
        final String restriction = "someRestriction";
        final Intent intent = new Intent();

        intent.putExtra(DevicePolicyManager.EXTRA_RESTRICTION, restriction);
        assertEquals(restriction, mDialog.getRestrictionFromIntent(intent));
    }

    @Test
    public void testGetRestrictionFromNullIntent() {
        assertEquals(null, mDialog.getRestrictionFromIntent(null));
    }

    @RequiresFlagsEnabled(android.security.Flags.FLAG_AAPM_API)
    @Test
    public void testGetAdminDetailsFromIntent_nullComponent_advancedProtection_launchesNewDialog() {
        final int userId = UserHandle.myUserId();
        final Authority advancedProtectionAuthority = new UnknownAuthority(
                ADVANCED_PROTECTION_SYSTEM_ENTITY);
        final EnforcingAdmin advancedProtectionEnforcingAdmin = new EnforcingAdmin("test.pkg",
                advancedProtectionAuthority, UserHandle.of(userId), mAdminComponent);
        final String userRestriction = UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY;

        final Intent dialogIntent = new Intent();
        dialogIntent.putExtra(Intent.EXTRA_USER_ID, userId);
        dialogIntent.putExtra(DevicePolicyManager.EXTRA_RESTRICTION, userRestriction);

        when(mDevicePolicyManager.getEnforcingAdmin(userId, userRestriction))
                .thenReturn(advancedProtectionEnforcingAdmin);
        doNothing().when(mDialog).startActivityAsUser(any(), eq(UserHandle.of(userId)));

        mDialog.getAdminDetailsFromIntent(dialogIntent);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mDialog).startActivityAsUser(intentCaptor.capture(), eq(UserHandle.of(userId)));
        assertTrue(mDialog.isFinishing());

        Intent launchedIntent = intentCaptor.getValue();
        assertEquals("Intent action is incorrect", ACTION_SHOW_ADVANCED_PROTECTION_SUPPORT_DIALOG,
                launchedIntent.getAction());
        assertEquals("Feature ID extra is incorrect", FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES,
                launchedIntent.getIntExtra(EXTRA_SUPPORT_DIALOG_FEATURE, -1));
        assertEquals("Type is incorrect", SUPPORT_DIALOG_TYPE_UNKNOWN,
                launchedIntent.getIntExtra(EXTRA_SUPPORT_DIALOG_TYPE, -1));
        assertEquals(FLAG_ACTIVITY_NEW_TASK, launchedIntent.getFlags());
    }

    @RequiresFlagsEnabled(android.security.Flags.FLAG_AAPM_API)
    @Test
    public void testGetAdminDetailsFromIntent_nullComponent_notAdvancedProtection_retrievesAdmin() {
        final int userId = UserHandle.myUserId();
        final EnforcingAdmin nonAdvancedProtectionEnforcingAdmin = new EnforcingAdmin("test.pkg",
                UnknownAuthority.UNKNOWN_AUTHORITY, UserHandle.of(userId), mAdminComponent);
        final String userRestriction = UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY;

        final Intent dialogIntent = new Intent();
        dialogIntent.putExtra(Intent.EXTRA_USER_ID, userId);
        dialogIntent.putExtra(DevicePolicyManager.EXTRA_RESTRICTION, userRestriction);

        when(mDevicePolicyManager.getEnforcingAdmin(userId, userRestriction))
                .thenReturn(nonAdvancedProtectionEnforcingAdmin);

        EnforcedAdmin admin = mDialog.getAdminDetailsFromIntent(dialogIntent);
        assertEquals(mAdminComponent, admin.component);
    }
}
