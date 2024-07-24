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

package com.android.settings.backup;


import android.app.backup.BackupManager;
import android.app.backup.IBackupManager;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Settings.PrivacySettingsActivity;

import java.net.URISyntaxException;

/**
 * Helper class for {@link UserBackupSettingsActivity} that interacts with {@link IBackupManager}.
 */
public class BackupSettingsHelper {
    private static final String TAG = "BackupSettingsHelper";

    private IBackupManager mBackupManager = IBackupManager.Stub.asInterface(
            ServiceManager.getService(Context.BACKUP_SERVICE));

    private Context mContext;

    public BackupSettingsHelper(Context context) {
        mContext = context;
    }

    /**
     * Returns an intent to launch backup settings from backup transport if the intent was provided
     * by the transport. Otherwise returns the intent to launch the default backup settings screen.
     *
     * @return Intent for launching backup settings
     */
    public Intent getIntentForBackupSettings() {
        Intent intent;
        if (isIntentProvidedByTransport()) {
            intent = getIntentForBackupSettingsFromTransport();
        } else {
            Log.e(TAG, "Backup transport has not provided an intent"
                    + " or the component for the intent is not found!");
            intent = getIntentForDefaultBackupSettings();
        }
        return intent;
    }

    /**
     * Returns a label for the settings item that will point to the backup settings provided by
     * the transport. If no label was provided by transport, returns the default string.
     *
     * @return Label for the backup settings item.
     */
    public CharSequence getLabelForBackupSettings() {
        CharSequence label = getLabelFromBackupTransport();
        if (TextUtils.isEmpty(label)) {
            label = mContext.getString(R.string.privacy_settings_title);
        }
        return label;
    }

    /**
     * Returns a summary string for the settings item that will point to the backup settings
     * provided by the transport. If no summary was provided by transport, returns the default
     * string.
     *
     * @return Summary for the backup settings item.
     */
    public String getSummaryForBackupSettings() {
        String summary = getSummaryFromBackupTransport();
        if (summary == null) {
            summary = mContext.getString(R.string.backup_configure_account_default_summary);
        }
        return summary;
    }


    /**
     * Checks if the manufacturer provided an intent to launch its backup settings screen
     * in the config file.
     */
    public boolean isBackupProvidedByManufacturer() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Checking if intent provided by manufacturer");
        }
        String intentString =
                mContext.getResources().getString(R.string.config_backup_settings_intent);

        return intentString != null && !intentString.isEmpty();
    }

    /**
     * Returns the label for the backup settings item provided by the manufacturer.
     */
    public String getLabelProvidedByManufacturer() {
        return mContext.getResources().getString(R.string.config_backup_settings_label);
    }

    /**
     * Returns the intent to the backup settings screen provided by the manufacturer.
     */
    public Intent getIntentProvidedByManufacturer() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Getting a backup settings intent provided by manufacturer");
        }
        String intentString =
                mContext.getResources().getString(R.string.config_backup_settings_intent);
        if (intentString != null && !intentString.isEmpty()) {
            try {
                return Intent.parseUri(intentString, 0);
            } catch (URISyntaxException e) {
                Log.e(TAG, "Invalid intent provided by the manufacturer.", e);
            }
        }
        return null;
    }

    /**
     * Gets the intent from Backup transport and adds the extra depending on whether the user has
     * rights to see backup settings.
     *
     * @return Intent to launch Backup settings provided by the Backup transport.
     */
    @VisibleForTesting
    Intent getIntentForBackupSettingsFromTransport() {
        Intent intent = getIntentFromBackupTransport();
        if (intent != null) {
            intent.putExtra(BackupManager.EXTRA_BACKUP_SERVICES_AVAILABLE, isBackupServiceActive());
        }
        return intent;
    }

    private Intent getIntentForDefaultBackupSettings() {
        return new Intent(mContext, PrivacySettingsActivity.class);
    }

    /**
     * Checks if the transport provided the intent to launch the backup settings and if that
     * intent resolves to an activity.
     */
    @VisibleForTesting
    boolean isIntentProvidedByTransport() {
        Intent intent = getIntentFromBackupTransport();
        return intent != null && intent.resolveActivity(mContext.getPackageManager()) != null;
    }

    /**
     * Gets an intent to launch the backup settings from the current transport using
     * {@link com.android.internal.backup.IBackupTransport#dataManagementIntent()} API.
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
    public boolean isBackupServiceActive() {
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

    @VisibleForTesting
    CharSequence getLabelFromBackupTransport() {
        try {
            CharSequence label =
                    mBackupManager.getDataManagementLabelForUser(
                            UserHandle.myUserId(), mBackupManager.getCurrentTransport());
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Received the backup settings label from backup transport: " + label);
            }
            return label;
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting data management label", e);
        }
        return null;
    }

    @VisibleForTesting
    String getSummaryFromBackupTransport() {
        try {
            String summary =
                    mBackupManager.getDestinationString(mBackupManager.getCurrentTransport());
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG,
                        "Received the backup settings summary from backup transport: " + summary);
            }
            return summary;
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting data management summary", e);
        }
        return null;
    }
}
