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

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

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
        mDialogHelper = new ActionDisabledByAdminDialogHelper(this);
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
        final EnforcedAdmin admin = new EnforcedAdmin(null, UserHandle.of(UserHandle.myUserId()));
        if (intent == null) {
            return admin;
        }
        admin.component = intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN);

        if (intent.hasExtra(Intent.EXTRA_USER)) {
            admin.user = intent.getParcelableExtra(Intent.EXTRA_USER);
        } else {
            int userId = intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
            if (userId == UserHandle.USER_NULL) {
                admin.user = null;
            } else {
                admin.user = UserHandle.of(userId);
            }
        }
        return admin;
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
