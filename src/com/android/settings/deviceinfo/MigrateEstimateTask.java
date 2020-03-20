/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import static com.android.settings.deviceinfo.StorageSettings.TAG;

import android.app.usage.ExternalStorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.text.format.DateUtils;
import android.text.format.Formatter;

import java.io.IOException;
import java.util.UUID;

public abstract class MigrateEstimateTask extends AsyncTask<Void, Void, Long> {
    private static final String EXTRA_SIZE_BYTES = "size_bytes";

    /**
     * Assume roughly a Class 10 card.
     */
    private static final long SPEED_ESTIMATE_BPS = 10 * TrafficStats.MB_IN_BYTES;

    private final Context mContext;

    private long mSizeBytes = -1;

    public MigrateEstimateTask(Context context) {
        mContext = context;
    }

    public void copyFrom(Intent intent) {
        mSizeBytes = intent.getLongExtra(EXTRA_SIZE_BYTES, -1);
    }

    public void copyTo(Intent intent) {
        intent.putExtra(EXTRA_SIZE_BYTES, mSizeBytes);
    }

    @Override
    protected Long doInBackground(Void... params) {
        if (mSizeBytes != -1) {
            return mSizeBytes;
        }

        final UserManager user = mContext.getSystemService(UserManager.class);
        final StorageManager storage = mContext.getSystemService(StorageManager.class);
        final StorageStatsManager stats = mContext.getSystemService(StorageStatsManager.class);

        final VolumeInfo privateVol = mContext.getPackageManager().getPrimaryStorageCurrentVolume();
        final VolumeInfo emulatedVol = storage.findEmulatedForPrivate(privateVol);

        if (emulatedVol == null) {
            Log.w(TAG, "Failed to find current primary storage");
            return -1L;
        }

        try {
            final UUID emulatedUuid = storage.getUuidForPath(emulatedVol.getPath());
            Log.d(TAG, "Measuring size of " + emulatedUuid);

            long size = 0;
            for (UserInfo u : user.getUsers()) {
                final ExternalStorageStats s = stats.queryExternalStatsForUser(emulatedUuid,
                        UserHandle.of(u.id));
                size += s.getTotalBytes();
                if (u.id == UserHandle.USER_SYSTEM) {
                    size += s.getObbBytes();
                }
            }
            return size;
        } catch (IOException e) {
            Log.w(TAG, "Failed to measure", e);
            return -1L;
        }
    }

    @Override
    protected void onPostExecute(Long result) {
        mSizeBytes = result;
        long timeMillis = (mSizeBytes * DateUtils.SECOND_IN_MILLIS) / SPEED_ESTIMATE_BPS;
        timeMillis = Math.max(timeMillis, DateUtils.SECOND_IN_MILLIS);

        final String size = Formatter.formatFileSize(mContext, mSizeBytes);
        final String time = DateUtils.formatDuration(timeMillis).toString();
        onPostExecute(size, time);
    }

    public abstract void onPostExecute(String size, String time);
}
