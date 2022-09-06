/*
 * Copyright (C) 2020 The Calyx Institute
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

package com.android.settings.backup.transport;

import android.app.backup.IBackupManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import androidx.annotation.Nullable;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Helper class for {@link TransportActivity} that interacts with {@link IBackupManager}.
 */
class TransportHelper {
    private static final String TAG = "TransportHelper";

    private final IBackupManager mBackupManager = IBackupManager.Stub.asInterface(
        ServiceManager.getService(Context.BACKUP_SERVICE));

    private Context mContext;

    TransportHelper(Context context) {
        mContext = context;
    }

    List<Transport> getTransports() {
        String[] backupTransports = getBackupTransports();
        if (backupTransports == null) return Collections.emptyList();
        ArrayList<Transport> transports = new ArrayList<>(backupTransports.length);
        String[] ignoredTransports = mContext.getResources().getStringArray(
                R.array.config_ignored_backup_transports);
        for (String name : getBackupTransports()) {
            boolean ignored = false;
            for (String ignoredTransport : ignoredTransports) {
                if (name.equals(ignoredTransport)) ignored = true;
            }
            if (ignored) continue;
            CharSequence label = getLabelFromBackupTransport(name);
            if (label == null || label.length() == 0) label = name;
            Transport transport = new Transport(name, label, getSummaryFromBackupTransport(name));
            transports.add(transport);
        }
        return transports;
    }

    void selectTransport(String name) {
        try {
            mBackupManager.selectBackupTransport(name);
        } catch (RemoteException e) {
            Log.e(TAG, "Error selecting transport: " + name, e);
        }
    }

    @Nullable
    private String[] getBackupTransports() {
        try {
            String[] transports = mBackupManager.listAllTransports();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Received all backup transports: " + Arrays.toString(transports));
            }
            return transports;
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting all backup transports", e);
        }
        return null;
    }

    private CharSequence getLabelFromBackupTransport(String transport) {
        try {
            CharSequence label = mBackupManager.getDataManagementLabelForUser(UserHandle.myUserId(), transport);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Received the backup settings label from " + transport + ": " + label);
            }
            return label;
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting data management label for " + transport, e);
        }
        return null;
    }

    private String getSummaryFromBackupTransport(String transport) {
        try {
            String summary = mBackupManager.getDestinationString(transport);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Received the backup settings summary from " + transport + ": " + summary);
            }
            return summary;
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting data management summary", e);
        }
        return null;
    }
}
