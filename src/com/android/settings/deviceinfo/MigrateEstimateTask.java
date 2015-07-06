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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.telecom.Log;
import android.text.format.DateUtils;
import android.text.format.Formatter;

import com.android.internal.app.IMediaContainerService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class MigrateEstimateTask extends AsyncTask<Void, Void, Long> implements
        ServiceConnection {
    private static final String EXTRA_SIZE_BYTES = "size_bytes";

    private static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(
            "com.android.defcontainer", "com.android.defcontainer.DefaultContainerService");

    /**
     * Assume roughly a Class 10 card.
     */
    private static final long SPEED_ESTIMATE_BPS = 10 * TrafficStats.MB_IN_BYTES;

    private final Context mContext;
    private final StorageManager mStorage;

    private final CountDownLatch mConnected = new CountDownLatch(1);
    private IMediaContainerService mService;

    private long mSizeBytes = -1;
    private long mTimeMillis = -1;

    public MigrateEstimateTask(Context context) {
        mContext = context;
        mStorage = context.getSystemService(StorageManager.class);
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

        final VolumeInfo privateVol = mContext.getPackageManager().getPrimaryStorageCurrentVolume();
        final VolumeInfo emulatedVol = mStorage.findEmulatedForPrivate(privateVol);

        if (emulatedVol == null) {
            Log.w(TAG, "Failed to find current primary storage");
            return -1L;
        }

        final String path = emulatedVol.getPath().getAbsolutePath();
        Log.d(TAG, "Estimating for current path " + path);

        final Intent intent = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
        mContext.bindServiceAsUser(intent, this, Context.BIND_AUTO_CREATE, UserHandle.OWNER);

        try {
            if (mConnected.await(15, TimeUnit.SECONDS)) {
                return mService.calculateDirectorySize(path);
            }
        } catch (InterruptedException | RemoteException e) {
            Log.w(TAG, "Failed to measure " + path);
        } finally {
            mContext.unbindService(this);
        }

        return -1L;
    }

    @Override
    protected void onPostExecute(Long result) {
        mSizeBytes = result;
        mTimeMillis = (mSizeBytes * DateUtils.SECOND_IN_MILLIS) / SPEED_ESTIMATE_BPS;
        mTimeMillis = Math.max(mTimeMillis, DateUtils.SECOND_IN_MILLIS);

        final String size = Formatter.formatFileSize(mContext, mSizeBytes);
        final String time = DateUtils.formatDuration(mTimeMillis).toString();
        onPostExecute(size, time);
    }

    public abstract void onPostExecute(String size, String time);

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IMediaContainerService.Stub.asInterface(service);
        mConnected.countDown();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Ignored; we leave service in place for the background thread to
        // run into DeadObjectException
    }
}
