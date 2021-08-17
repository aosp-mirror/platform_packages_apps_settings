/*
 * Copyright (C) 2021 The Android Open Source Project
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
 *
 */

package com.android.settings.fuelgauge;

import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/** An implementation to backup and restore battery configurations. */
public final class BatteryBackupHelper implements BackupHelper {
    /** An inditifier for {@link BackupHelper}. */
    public static final String TAG = "BatteryBackupHelper";

    private final Context mContext;

    public BatteryBackupHelper(Context context) {
        mContext = context;
    }

    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) {
        Log.d(TAG, "performBackup()");
    }

    @Override
    public void restoreEntity(BackupDataInputStream data) {
        Log.d(TAG, "restoreEntity()");
    }

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState) {
    }
}
