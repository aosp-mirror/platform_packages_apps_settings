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

import static org.junit.Assert.assertEquals;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.UserHandle;

import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ActionDisabledByAdminDialogTest {

    private ActionDisabledByAdminDialog mDialog;

    @Before
    public void setUp() {
        mDialog = new ActionDisabledByAdminDialog();
    }

    @Test
    public void testGetAdminDetailsFromIntent() {
        final int userId = 123;
        final ComponentName component = new ComponentName("com.some.package", ".SomeClass");
        final EnforcedAdmin expectedAdmin = new EnforcedAdmin(component, UserHandle.of(userId));

        final Intent intent = new Intent();
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component);
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
}
