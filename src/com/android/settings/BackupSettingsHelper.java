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

package com.android.settings;


import android.app.backup.BackupManager;
import android.app.backup.BackupTransport;
import android.app.backup.IBackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Helper class for {@link BackupSettingsActivity} that interacts with {@link IBackupManager}.
 */
public class BackupSettingsHelper {
    private static final String TAG = "BackupSettingsHelper";

    private IBackupManager mBackupManager = IBackupManager.Stub.asInterface(
            ServiceManager.getService(Context.BACKUP_SERVICE));


    /**
     * Gets the intent from Backup transport and adds the extra depending on whether the user has
     * rights to see backup settings.
     *
     * @return Intent to launch Backup settings provided by the Backup transport.
     */
    public Intent getIntentForBackupSettings() {
        Intent intent = getIntentFromBackupTransport();
        if (intent != null) {
            intent.putExtra(BackupManager.EXTRA_BACKUP_SERVICES_AVAILABLE, isBackupServiceActive());
        }
        return intent;
    }


    /**
     * Checks if the transport provided the intent to launch the backup settings and if that
     * intent resolves to an activity.
     */
    public boolean isIntentProvidedByTransport(PackageManager packageManager) {
        Intent intent = getIntentFromBackupTransport();
        return intent != null && intent.resolveActivity(packageManager) != null;
    }

    /**
     * Gets an intent to launch the backup settings from the current transport using
     * {@link BackupTransport#dataManagementIntent()} API.
     *
     * @return intent provided by transport or null if no intent was provided.
     */
    private Intent getIntentFromBackupTransport() {
        try {
            Intent intent =
                    mBackupManager.getDataManagementIntent(mBackupManager.getCurrentTransport());
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                if (intent != null) {
                    Log.d(TAG, "Parsed intent from backup transport: " + intent.toString());
                } else {
                    Log.d(TAG, "Received a null intent from backup transport");
                }
            }
            return intent;
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting data management intent", e);
        }
        return null;
    }

    /** Checks if backup service is enabled for this user. */
    private boolean isBackupServiceActive() {
        boolean backupOkay;
        try {
            backupOkay = mBackupManager.isBackupServiceActive(UserHandle.myUserId());
        } catch (Exception e) {
            // things go wrong talking to the backup system => ignore and
            // pass the default 'false' as the "backup is a thing?" state.
            backupOkay = false;
        }
        return backupOkay;
    }
}
