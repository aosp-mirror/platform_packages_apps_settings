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
 * limitations under the License
 */

package com.android.settings.backup;

import android.app.backup.IBackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class PrivacySettingsUtils {
    private static final String TAG = "PrivacySettingsUtils";
    private static final String GSETTINGS_PROVIDER = "com.google.settings";

    static final String BACKUP_DATA = "backup_data";
    static final String AUTO_RESTORE = "auto_restore";
    static final String CONFIGURE_ACCOUNT = "configure_account";
    static final String BACKUP_INACTIVE = "backup_inactive";

    // Don't allow any access if this is not an admin user.
    // TODO: backup/restore currently only works with owner user b/22760572
    static boolean isAdminUser(final Context context) {
        return UserManager.get(context).isAdminUser();
    }

    /**
     * Send a {@param key} to check its preference will display in PrivacySettings or not.
     */
    static boolean isInvisibleKey(final Context context, final String key) {
        final Set<String> keysToRemove = getInvisibleKey(context);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG,
                    "keysToRemove size=" + keysToRemove.size() + " keysToRemove=" + keysToRemove);
        }
        if (keysToRemove.contains(key)) {
            return true;
        }
        return false;
    }

    private static Set<String> getInvisibleKey(final Context context) {
        final IBackupManager backupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));
        boolean isServiceActive = false;
        try {
            isServiceActive = backupManager.isBackupServiceActive(UserHandle.myUserId());
        } catch (RemoteException e) {
            Log.w(TAG, "Failed querying backup manager service activity status. " +
                    "Assuming it is inactive.");
        }
        boolean vendorSpecific = context.getPackageManager().
                resolveContentProvider(GSETTINGS_PROVIDER, 0) == null;
        final Set<String> inVisibleKeys = new TreeSet<>();
        if (vendorSpecific || isServiceActive) {
            inVisibleKeys.add(BACKUP_INACTIVE);
        }
        if (vendorSpecific || !isServiceActive) {
            inVisibleKeys.add(BACKUP_DATA);
            inVisibleKeys.add(AUTO_RESTORE);
            inVisibleKeys.add(CONFIGURE_ACCOUNT);
        }
        return inVisibleKeys;
    }

    public static void updatePrivacyBuffer(final Context context, PrivacySettingsConfigData data) {
        final IBackupManager backupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));

        try {
            data.setBackupEnabled(backupManager.isBackupEnabled());
            String transport = backupManager.getCurrentTransport();
            data.setConfigIntent(validatedActivityIntent(context,
                    backupManager.getConfigurationIntent(transport), "config"));
            data.setConfigSummary(backupManager.getDestinationString(transport));
            data.setManageIntent(validatedActivityIntent(context,
                    backupManager.getDataManagementIntent(transport), "management"));
            data.setManageLabel(
                    backupManager.getDataManagementLabelForUser(UserHandle.myUserId(), transport));
            data.setBackupGray(false);
        } catch (RemoteException e) {
            // leave it 'false' and disable the UI; there's no backup manager
            //  mBackup.setEnabled(false);
            data.setBackupGray(true);
        }
    }

    private static Intent validatedActivityIntent(final Context context, Intent intent,
            String logLabel) {
        if (intent != null) {
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
            if (resolved == null || resolved.isEmpty()) {
                intent = null;
                Log.e(TAG, "Backup " + logLabel + " intent " + intent
                        + " fails to resolve; ignoring");
            }
        }
        return intent;
    }
}