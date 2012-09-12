/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.Bundle;
import android.os.Environment;
import android.os.Environment.UserEnvironment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.util.Pair;

import com.android.internal.app.IMediaContainerService;
import com.android.internal.util.Preconditions;
import com.google.android.collect.Maps;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Measure the memory for various systems.
 *
 * TODO: This class should ideally have less knowledge about what the context
 * it's measuring is. In the future, reduce the amount of stuff it needs to
 * know about by just keeping an array of measurement types of the following
 * properties:
 *
 *   Filesystem stats (using DefaultContainerService)
 *   Directory measurements (using DefaultContainerService.measureDir)
 *   Application measurements (using PackageManager)
 *
 * Then the calling application would just specify the type and an argument.
 * This class would keep track of it while the calling application would
 * decide on how to use it.
 */
public class StorageMeasurement {
    private static final String TAG = "StorageMeasurement";

    private static final boolean LOCAL_LOGV = true;
    static final boolean LOGV = LOCAL_LOGV && Log.isLoggable(TAG, Log.VERBOSE);

    public static final String TOTAL_SIZE = "total_size";

    public static final String AVAIL_SIZE = "avail_size";

    public static final String APPS_USED = "apps_used";

    public static final String DOWNLOADS_SIZE = "downloads_size";

    public static final String MISC_SIZE = "misc_size";

    public static final String MEDIA_SIZES = "media_sizes";

    private static final String DEFAULT_CONTAINER_PACKAGE = "com.android.defcontainer";

    public static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(
            DEFAULT_CONTAINER_PACKAGE, "com.android.defcontainer.DefaultContainerService");

    private final MeasurementHandler mHandler;

    private static HashMap<Pair<StorageVolume, UserHandle>, StorageMeasurement>
            sInstances = Maps.newHashMap();

    private volatile WeakReference<MeasurementReceiver> mReceiver;

    private long mTotalSize;
    private long mAvailSize;
    private long mAppsSize;
    private long mDownloadsSize;
    private long mMiscSize;
    private long[] mMediaSizes = new long[StorageVolumePreferenceCategory.sMediaCategories.length];

    private final StorageVolume mStorageVolume;
    private final UserHandle mUser;
    private final UserEnvironment mUserEnv;
    private final boolean mIsPrimary;
    private final boolean mIsInternal;

    private boolean mIncludeAppCodeSize = true;

    List<FileInfo> mFileInfoForMisc;

    public interface MeasurementReceiver {
        public void updateApproximate(StorageMeasurement meas, Bundle bundle);
        public void updateExact(StorageMeasurement meas, Bundle bundle);
    }

    private StorageMeasurement(Context context, StorageVolume volume, UserHandle user) {
        mStorageVolume = volume;
        mUser = Preconditions.checkNotNull(user);
        mUserEnv = new UserEnvironment(mUser.getIdentifier());
        mIsInternal = volume == null;
        mIsPrimary = volume != null ? volume.isPrimary() : false;

        // Start the thread that will measure the disk usage.
        final HandlerThread handlerThread = new HandlerThread("MemoryMeasurement");
        handlerThread.start();
        mHandler = new MeasurementHandler(context, handlerThread.getLooper());
    }

    public void setIncludeAppCodeSize(boolean include) {
        mIncludeAppCodeSize = include;
    }

    /**
     * Get the singleton of the StorageMeasurement class. The application
     * context is used to avoid leaking activities.
     * @param storageVolume The {@link StorageVolume} that will be measured
     * @param isPrimary true when this storage volume is the primary volume
     */
    public static StorageMeasurement getInstance(
            Context context, StorageVolume storageVolume, UserHandle user) {
        final Pair<StorageVolume, UserHandle> key = new Pair<StorageVolume, UserHandle>(
                storageVolume, user);
        synchronized (sInstances) {
            StorageMeasurement value = sInstances.get(key);
            if (value == null) {
                value = new StorageMeasurement(
                        context.getApplicationContext(), storageVolume, user);
                sInstances.put(key, value);
            }
            return value;
        }
    }

    public UserHandle getUser() {
        return mUser;
    }

    public void setReceiver(MeasurementReceiver receiver) {
        if (mReceiver == null || mReceiver.get() == null) {
            mReceiver = new WeakReference<MeasurementReceiver>(receiver);
        }
    }

    public void measure() {
        if (!mHandler.hasMessages(MeasurementHandler.MSG_MEASURE)) {
            mHandler.sendEmptyMessage(MeasurementHandler.MSG_MEASURE);
        }
    }

    public void cleanUp() {
        mReceiver = null;
        mHandler.removeMessages(MeasurementHandler.MSG_MEASURE);
        mHandler.sendEmptyMessage(MeasurementHandler.MSG_DISCONNECT);
    }

    public void invalidate() {
        mHandler.sendEmptyMessage(MeasurementHandler.MSG_INVALIDATE);
    }

    private void sendInternalApproximateUpdate() {
        MeasurementReceiver receiver = (mReceiver != null) ? mReceiver.get() : null;
        if (receiver == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putLong(TOTAL_SIZE, mTotalSize);
        bundle.putLong(AVAIL_SIZE, mAvailSize);

        receiver.updateApproximate(this, bundle);
    }

    private void sendExactUpdate() {
        MeasurementReceiver receiver = (mReceiver != null) ? mReceiver.get() : null;
        if (receiver == null) {
            if (LOGV) {
                Log.i(TAG, "measurements dropped because receiver is null! wasted effort");
            }
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putLong(TOTAL_SIZE, mTotalSize);
        bundle.putLong(AVAIL_SIZE, mAvailSize);
        bundle.putLong(APPS_USED, mAppsSize);
        bundle.putLong(DOWNLOADS_SIZE, mDownloadsSize);
        bundle.putLong(MISC_SIZE, mMiscSize);
        bundle.putLongArray(MEDIA_SIZES, mMediaSizes);

        receiver.updateExact(this, bundle);
    }

    private class MeasurementHandler extends Handler {
        public static final int MSG_MEASURE = 1;

        public static final int MSG_CONNECTED = 2;

        public static final int MSG_DISCONNECT = 3;

        public static final int MSG_COMPLETED = 4;

        public static final int MSG_INVALIDATE = 5;

        private Object mLock = new Object();

        private IMediaContainerService mDefaultContainer;

        private volatile boolean mBound = false;

        private volatile boolean mMeasured = false;

        private StatsObserver mStatsObserver;

        private final WeakReference<Context> mContext;

        final private ServiceConnection mDefContainerConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                final IMediaContainerService imcs = IMediaContainerService.Stub
                .asInterface(service);
                mDefaultContainer = imcs;
                mBound = true;
                sendMessage(obtainMessage(MSG_CONNECTED, imcs));
            }

            public void onServiceDisconnected(ComponentName name) {
                mBound = false;
                removeMessages(MSG_CONNECTED);
            }
        };

        public MeasurementHandler(Context context, Looper looper) {
            super(looper);
            mContext = new WeakReference<Context>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_MEASURE: {
                    if (mMeasured) {
                        sendExactUpdate();
                        break;
                    }

                    final Context context = (mContext != null) ? mContext.get() : null;
                    if (context == null) {
                        return;
                    }

                    synchronized (mLock) {
                        if (mBound) {
                            removeMessages(MSG_DISCONNECT);
                            sendMessage(obtainMessage(MSG_CONNECTED, mDefaultContainer));
                        } else {
                            Intent service = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
                            context.bindService(service, mDefContainerConn,
                                    Context.BIND_AUTO_CREATE, mUser.getIdentifier());
                        }
                    }
                    break;
                }
                case MSG_CONNECTED: {
                    IMediaContainerService imcs = (IMediaContainerService) msg.obj;
                    measureApproximateStorage(imcs);
                    measureExactStorage(imcs);
                    break;
                }
                case MSG_DISCONNECT: {
                    synchronized (mLock) {
                        if (mBound) {
                            final Context context = (mContext != null) ? mContext.get() : null;
                            if (context == null) {
                                return;
                            }

                            mBound = false;
                            context.unbindService(mDefContainerConn);
                        }
                    }
                    break;
                }
                case MSG_COMPLETED: {
                    mMeasured = true;
                    sendExactUpdate();
                    break;
                }
                case MSG_INVALIDATE: {
                    mMeasured = false;
                    break;
                }
            }
        }

        /**
         * Request measurement of each package.
         *
         * @param pm PackageManager instance to query
         */
        public void requestQueuedMeasurementsLocked(PackageManager pm) {
            final String[] appsList = mStatsObserver.getAppsList();
            final int N = appsList.length;
            for (int i = 0; i < N; i++) {
                pm.getPackageSizeInfo(appsList[i], mStatsObserver);
            }
        }

        private class StatsObserver extends IPackageStatsObserver.Stub {
            private long mAppsSizeForThisStatsObserver = 0;
            private final List<String> mAppsList = new ArrayList<String>();

            public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
                if (!mStatsObserver.equals(this)) {
                    // this callback's class object is no longer in use. ignore this callback.
                    return;
                }

                if (succeeded) {
                    if (mIsInternal) {
                        if (mIncludeAppCodeSize) {
                            mAppsSizeForThisStatsObserver += stats.codeSize;
                        }
                        mAppsSizeForThisStatsObserver += stats.dataSize;
                    } else if (!Environment.isExternalStorageEmulated()) {
                        mAppsSizeForThisStatsObserver += stats.externalObbSize +
                                stats.externalCodeSize + stats.externalDataSize +
                                stats.externalCacheSize + stats.externalMediaSize;
                    } else {
                        if (mIncludeAppCodeSize) {
                            mAppsSizeForThisStatsObserver += stats.codeSize;
                        }
                        mAppsSizeForThisStatsObserver += stats.dataSize +
                                stats.externalCodeSize + stats.externalDataSize +
                                stats.externalCacheSize + stats.externalMediaSize +
                                stats.externalObbSize;
                    }
                }

                synchronized (mAppsList) {
                    mAppsList.remove(stats.packageName);
                    if (mAppsList.size() > 0) return;
                }

                mAppsSize = mAppsSizeForThisStatsObserver;
                onInternalMeasurementComplete();
            }

            public void queuePackageMeasurementLocked(String packageName) {
                synchronized (mAppsList) {
                    mAppsList.add(packageName);
                }
            }

            public String[] getAppsList() {
                synchronized (mAppsList) {
                    return mAppsList.toArray(new String[mAppsList.size()]);
                }
            }
        }

        private void onInternalMeasurementComplete() {
            sendEmptyMessage(MSG_COMPLETED);
        }

        private void measureApproximateStorage(IMediaContainerService imcs) {
            final String path = mStorageVolume != null ? mStorageVolume.getPath()
                    : Environment.getDataDirectory().getPath();
            try {
                final long[] stats = imcs.getFileSystemStats(path);
                mTotalSize = stats[0];
                mAvailSize = stats[1];
            } catch (Exception e) {
                Log.w(TAG, "Problem in container service", e);
            }

            sendInternalApproximateUpdate();
        }

        private void measureExactStorage(IMediaContainerService imcs) {
            Context context = mContext != null ? mContext.get() : null;
            if (context == null) {
                return;
            }

            // Media
            for (int i = 0; i < StorageVolumePreferenceCategory.sMediaCategories.length; i++) {
                if (mIsPrimary) {
                    String[] dirs = StorageVolumePreferenceCategory.sMediaCategories[i].mDirPaths;
                    final int length = dirs.length;
                    mMediaSizes[i] = 0;
                    for (int d = 0; d < length; d++) {
                        final String path = dirs[d];
                        mMediaSizes[i] += getDirectorySize(imcs, path);
                    }
                } else {
                    // TODO Compute sizes using the MediaStore
                    mMediaSizes[i] = 0;
                }
            }

            /* Compute sizes using the media provider
            // Media sizes are measured by the MediaStore. Query database.
            ContentResolver contentResolver = context.getContentResolver();
            // TODO "external" as a static String from MediaStore?
            Uri audioUri = MediaStore.Files.getContentUri("external");
            final String[] projection =
                new String[] { "sum(" + MediaStore.Files.FileColumns.SIZE + ")" };
            final String selection =
                MediaStore.Files.FileColumns.STORAGE_ID + "=" +
                Integer.toString(mStorageVolume.getStorageId()) + " AND " +
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";

            for (int i = 0; i < StorageVolumePreferenceCategory.sMediaCategories.length; i++) {
                mMediaSizes[i] = 0;
                int mediaType = StorageVolumePreferenceCategory.sMediaCategories[i].mediaType;
                Cursor c = null;
                try {
                    c = contentResolver.query(audioUri, projection, selection,
                            new String[] { Integer.toString(mediaType) } , null);

                    if (c != null && c.moveToNext()) {
                        long size = c.getLong(0);
                        mMediaSizes[i] = size;
                    }
                } finally {
                    if (c != null) c.close();
                }
            }
             */

            // Downloads (primary volume only)
            if (mIsPrimary) {
                final String downloadsPath = mUserEnv.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                mDownloadsSize = getDirectorySize(imcs, downloadsPath);
            } else {
                mDownloadsSize = 0;
            }

            // Misc
            mMiscSize = 0;
            if (mIsPrimary) {
                measureSizesOfMisc(imcs);
            }

            // Apps
            // We have to get installd to measure the package sizes.
            PackageManager pm = context.getPackageManager();
            if (pm == null) {
                return;
            }
            final List<ApplicationInfo> apps;
            if (mIsPrimary || mIsInternal) {
                apps = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES |
                        PackageManager.GET_DISABLED_COMPONENTS);
            } else {
                // TODO also measure apps installed on the SD card
                apps = Collections.emptyList();
            }

            if (apps != null && apps.size() > 0) {
                // initiate measurement of all package sizes. need new StatsObserver object.
                mStatsObserver = new StatsObserver();
                synchronized (mStatsObserver.mAppsList) {
                    for (int i = 0; i < apps.size(); i++) {
                        final ApplicationInfo info = apps.get(i);
                        mStatsObserver.queuePackageMeasurementLocked(info.packageName);
                    }
                }

                requestQueuedMeasurementsLocked(pm);
                // Sending of the message back to the MeasurementReceiver is
                // completed in the PackageObserver
            } else {
                onInternalMeasurementComplete();
            }
        }
    }

    private long getDirectorySize(IMediaContainerService imcs, String dir) {
        try {
            return imcs.calculateDirectorySize(dir);
        } catch (Exception e) {
            Log.w(TAG, "Could not read memory from default container service for " + dir, e);
            return 0;
        }
    }

    long getMiscSize() {
        return mMiscSize;
    }

    private void measureSizesOfMisc(IMediaContainerService imcs) {
        File top = new File(mStorageVolume.getPath());
        mFileInfoForMisc = new ArrayList<FileInfo>();
        File[] files = top.listFiles();
        if (files == null) return;
        final int len = files.length;
        // Get sizes of all top level nodes except the ones already computed...
        long counter = 0;
        for (int i = 0; i < len; i++) {
            String path = files[i].getAbsolutePath();
            if (StorageVolumePreferenceCategory.sPathsExcludedForMisc.contains(path)) {
                continue;
            }
            if (files[i].isFile()) {
                final long fileSize = files[i].length();
                mFileInfoForMisc.add(new FileInfo(path, fileSize, counter++));
                mMiscSize += fileSize;
            } else if (files[i].isDirectory()) {
                final long dirSize = getDirectorySize(imcs, path);
                mFileInfoForMisc.add(new FileInfo(path, dirSize, counter++));
                mMiscSize += dirSize;
            } else {
                // Non directory, non file: not listed
            }
        }
        // sort the list of FileInfo objects collected above in descending order of their sizes
        Collections.sort(mFileInfoForMisc);
    }

    static class FileInfo implements Comparable<FileInfo> {
        final String mFileName;
        final long mSize;
        final long mId;

        FileInfo(String fileName, long size, long id) {
            mFileName = fileName;
            mSize = size;
            mId = id;
        }

        @Override
        public int compareTo(FileInfo that) {
            if (this == that || mSize == that.mSize) return 0;
            else return (mSize < that.mSize) ? 1 : -1; // for descending sort
        }

        @Override
        public String toString() {
            return mFileName  + " : " + mSize + ", id:" + mId;
        }
    }

    /**
     * TODO remove this method, only used because external SD Card needs a special treatment.
     */
    boolean isExternalSDCard() {
        return !mIsPrimary && !mIsInternal;
    }
}
