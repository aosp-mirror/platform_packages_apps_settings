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

import static android.security.advancedprotection.AdvancedProtectionManager.ADVANCED_PROTECTION_SYSTEM_ENTITY;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.UnknownAuthority;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.security.advancedprotection.AdvancedProtectionManager;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class ActionDisabledByAdminDialog extends Activity
        implements DialogInterface.OnDismissListener {

    private ActionDisabledByAdminDialogHelper mDialogHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final RestrictedLockUtils.EnforcedAdmin enforcedAdmin =
                getAdminDetailsFromIntent(getIntent());
        final String restriction = getRestrictionFromIntent(getIntent());
        mDialogHelper = new ActionDisabledByAdminDialogHelper(this, restriction);
        mDialogHelper.prepareDialogBuilder(restriction, enforcedAdmin)
                .setOnDismissListener(this)
                .show();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final EnforcedAdmin admin = getAdminDetailsFromIntent(intent);
        final String restriction = getRestrictionFromIntent(intent);
        mDialogHelper.updateDialog(restriction, admin);
    }

    @androidx.annotation.VisibleForTesting
    EnforcedAdmin getAdminDetailsFromIntent(Intent intent) {
        final EnforcedAdmin enforcedAdmin = new EnforcedAdmin(null, UserHandle.of(
                UserHandle.myUserId()));
        if (intent == null) {
            return enforcedAdmin;
        }
        enforcedAdmin.component = intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName.class);
        int userId = intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());

        Bundle adminDetails = null;
        if (enforcedAdmin.component == null) {
            DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
            final String restriction = getRestrictionFromIntent(intent);
            if (android.security.Flags.aapmApi() && dpm != null && restriction != null) {
                // TODO(b/381025131): Move advanced protection logic to DevicePolicyManager or
                //  elsewhere.
                launchAdvancedProtectionDialogOrTryToSetAdminComponent(dpm, userId, restriction,
                        enforcedAdmin);
            } else {
                adminDetails = dpm.getEnforcingAdminAndUserDetails(userId, restriction);
                if (adminDetails != null) {
                    enforcedAdmin.component = adminDetails.getParcelable(
                            DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName.class);
                }
            }
        }

        if (intent.hasExtra(Intent.EXTRA_USER)) {
            enforcedAdmin.user = intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle.class);
        } else {
            if (adminDetails != null) {
                userId = adminDetails.getInt(Intent.EXTRA_USER_ID, UserHandle.myUserId());
            }
            if (userId == UserHandle.USER_NULL) {
                enforcedAdmin.user = null;
            } else {
                enforcedAdmin.user = UserHandle.of(userId);
            }
        }
        return enforcedAdmin;
    }

    private void launchAdvancedProtectionDialogOrTryToSetAdminComponent(DevicePolicyManager dpm,
            int userId, String restriction, EnforcedAdmin enforcedAdmin) {
        EnforcingAdmin enforcingAdmin = dpm.getEnforcingAdmin(userId, restriction);
        if (enforcingAdmin == null) {
            return;
        }
        if (enforcingAdmin.getAuthority() instanceof UnknownAuthority authority
                && ADVANCED_PROTECTION_SYSTEM_ENTITY.equals(authority.getName())) {
            AdvancedProtectionManager apm = getSystemService(AdvancedProtectionManager.class);
            if (apm == null) {
                return;
            }
            Intent apmSupportIntent = apm.createSupportIntentForPolicyIdentifierOrRestriction(
                    restriction, /* type */ null);
            startActivityAsUser(apmSupportIntent, UserHandle.of(userId));
            finish();
        } else {
            enforcedAdmin.component = enforcingAdmin.getComponentName();
        }
    }

    @androidx.annotation.VisibleForTesting
    String getRestrictionFromIntent(Intent intent) {
        if (intent == null) return null;
        return intent.getStringExtra(DevicePolicyManager.EXTRA_RESTRICTION);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
