package com.android.settings.deviceinfo;

import com.android.internal.app.IMediaContainerService;

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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Measure the memory for various systems.
 *
 * TODO: This class should ideally have less knowledge about what the context
 * it's measuring is. In the future, reduce the amount of stuff it needs to
 * know about by just keeping an array of measurement types of the following
 * properties:
 *
 *   Filesystem stats (using StatFs)
 *   Directory measurements (using DefaultContainerService.measureDir)
 *   Applicaiton measurements (using PackageManager)
 *
 * Then the calling application would just specify the type and an argument.
 * This class would keep track of it while the calling application would
 * decide on how to use it.
 */
public class MemoryMeasurement {
    private static final String TAG = "MemoryMeasurement";

    public static final String TOTAL_SIZE = "total_size";

    public static final String AVAIL_SIZE = "avail_size";

    public static final String APPS_USED = "apps_used";

    public static final String MEDIA_USED = "media_used";

    private static final String DEFAULT_CONTAINER_PACKAGE = "com.android.defcontainer";

    private static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(
            DEFAULT_CONTAINER_PACKAGE, "com.android.defcontainer.DefaultContainerService");

    private final MeasurementHandler mHandler;

    private static volatile MemoryMeasurement sInstance;

    private volatile WeakReference<MeasurementReceiver> mReceiver;

    // Internal memory fields
    private long mInternalTotalSize;
    private long mInternalAvailSize;
    private long mInternalMediaSize;
    private long mInternalAppsSize;

    // External memory fields
    private long mExternalTotalSize;

    private long mExternalAvailSize;

    private MemoryMeasurement(Context context) {
        // Start the thread that will measure the disk usage.
        final HandlerThread t = new HandlerThread("MemoryMeasurement");
        t.start();
        mHandler = new MeasurementHandler(context, t.getLooper());
    }

    /**
     * Get the singleton of the MemoryMeasurement class. The application
     * context is used to avoid leaking activities.
     */
    public static MemoryMeasurement getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MemoryMeasurement.class) {
                if (sInstance == null) {
                    sInstance = new MemoryMeasurement(context.getApplicationContext());
                }
            }
        }

        return sInstance;
    }

    public void setReceiver(MeasurementReceiver receiver) {
        mReceiver = new WeakReference<MeasurementReceiver>(receiver);
    }

    public void measureExternal() {
        if (!mHandler.hasMessages(MeasurementHandler.MSG_MEASURE_EXTERNAL)) {
            mHandler.sendEmptyMessage(MeasurementHandler.MSG_MEASURE_EXTERNAL);
        }
    }

    public void measureInternal() {
        if (!mHandler.hasMessages(MeasurementHandler.MSG_MEASURE_INTERNAL)) {
            mHandler.sendEmptyMessage(MeasurementHandler.MSG_MEASURE_INTERNAL);
        }
    }

    public void cleanUp() {
        mReceiver = null;
        mHandler.cleanUp();
    }

    private void sendInternalApproximateUpdate() {
        MeasurementReceiver receiver = (mReceiver != null) ? mReceiver.get() : null;
        if (receiver == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putLong(TOTAL_SIZE, mInternalTotalSize);
        bundle.putLong(AVAIL_SIZE, mInternalAvailSize);

        receiver.updateApproximateInternal(bundle);
    }

    private void sendInternalExactUpdate() {
        MeasurementReceiver receiver = (mReceiver != null) ? mReceiver.get() : null;
        if (receiver == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putLong(TOTAL_SIZE, mInternalTotalSize);
        bundle.putLong(AVAIL_SIZE, mInternalAvailSize);
        bundle.putLong(APPS_USED, mInternalAppsSize);
        bundle.putLong(MEDIA_USED, mInternalMediaSize);

        receiver.updateExactInternal(bundle);
    }

    private void sendExternalApproximateUpdate() {
        MeasurementReceiver receiver = (mReceiver != null) ? mReceiver.get() : null;
        if (receiver == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putLong(TOTAL_SIZE, mExternalTotalSize);
        bundle.putLong(AVAIL_SIZE, mExternalAvailSize);

        receiver.updateApproximateExternal(bundle);
    }

    public interface MeasurementReceiver {
        public void updateApproximateInternal(Bundle bundle);

        public void updateExactInternal(Bundle bundle);

        public void updateApproximateExternal(Bundle bundle);
    }

    private class MeasurementHandler extends Handler {
        public static final int MSG_MEASURE_INTERNAL = 1;

        public static final int MSG_MEASURE_EXTERNAL = 2;

        public static final int MSG_CONNECTED = 3;

        public static final int MSG_DISCONNECT = 4;

        public static final int MSG_COMPLETED = 5;

        public static final int MSG_INVALIDATE = 6;

        private List<String> mPendingApps = new ArrayList<String>();

        private Object mLock = new Object();

        private IMediaContainerService mDefaultContainer;

        private volatile boolean mBound = false;

        private volatile boolean mMeasured = false;

        private long mAppsSize = 0;

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
                case MSG_MEASURE_EXTERNAL: {
                    if (mMeasured) {
                        sendExternalApproximateUpdate();
                        break;
                    }

                    measureApproximateExternalStorage();
                    break;
                }
                case MSG_MEASURE_INTERNAL: {
                    if (mMeasured) {
                        sendInternalExactUpdate();
                        break;
                    }

                    final Context context = (mContext != null) ? mContext.get() : null;
                    if (context == null) {
                        return;
                    }

                    measureApproximateInternalStorage();

                    synchronized (mLock) {
                        if (mBound) {
                            removeMessages(MSG_DISCONNECT);
                            sendMessage(obtainMessage(MSG_CONNECTED, mDefaultContainer));
                        } else {
                            Intent service = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
                            context.bindService(service, mDefContainerConn,
                                    Context.BIND_AUTO_CREATE);
                        }
                    }
                    break;
                }
                case MSG_CONNECTED: {
                    IMediaContainerService imcs = (IMediaContainerService) msg.obj;
                    measureExactInternalStorage(imcs);
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
                }
                case MSG_COMPLETED: {
                    mMeasured = true;
                    sendInternalExactUpdate();
                    break;
                }
                case MSG_INVALIDATE: {
                    mMeasured = false;
                    break;
                }
            }
        }

        public void cleanUp() {
            removeMessages(MSG_MEASURE_INTERNAL);
            removeMessages(MSG_MEASURE_EXTERNAL);

            sendEmptyMessage(MSG_DISCONNECT);
        }

        public void queuePackageMeasurementLocked(String packageName) {
            mPendingApps.add(packageName);
        }

        /**
         * Request measurement of each package.
         *
         * @param pm PackageManager instance to query
         */
        public void requestQueuedMeasurementsLocked(PackageManager pm) {
            final int N = mPendingApps.size();
            for (int i = 0; i < N; i++) {
                pm.getPackageSizeInfo(mPendingApps.get(i), mStatsObserver);
            }
        }

        final IPackageStatsObserver.Stub mStatsObserver = new IPackageStatsObserver.Stub() {
            public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
                if (succeeded) {
                    mAppsSize += stats.codeSize + stats.dataSize;
                }

                synchronized (mPendingApps) {
                    mPendingApps.remove(stats.packageName);

                    if (mPendingApps.size() == 0) {
                        mInternalAppsSize = mAppsSize;

                        onInternalMeasurementComplete();
                    }
                }
            }
        };

        private void onInternalMeasurementComplete() {
            sendEmptyMessage(MSG_COMPLETED);
        }

        private void measureApproximateInternalStorage() {
            final File dataPath = Environment.getDataDirectory();
            final StatFs stat = new StatFs(dataPath.getPath());
            final long blockSize = stat.getBlockSize();
            final long totalBlocks = stat.getBlockCount();
            final long availableBlocks = stat.getAvailableBlocks();

            final long totalSize = totalBlocks * blockSize;
            final long availSize = availableBlocks * blockSize;

            mInternalTotalSize = totalSize;
            mInternalAvailSize = availSize;

            sendInternalApproximateUpdate();
        }

        private void measureExactInternalStorage(IMediaContainerService imcs) {
            Context context = mContext != null ? mContext.get() : null;
            if (context == null) {
                return;
            }

            // We have to get installd to measure the package sizes.
            PackageManager pm = context.getPackageManager();
            if (pm == null) {
                return;
            }

            long mediaSize;
            try {
                // TODO get these directories from somewhere
                mediaSize = imcs.calculateDirectorySize("/data/media");
            } catch (Exception e) {
                Log.i(TAG, "Could not read memory from default container service");
                return;
            }

            mInternalMediaSize = mediaSize;

            final List<ApplicationInfo> apps = pm
                    .getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES
                            | PackageManager.GET_DISABLED_COMPONENTS);
            if (apps != null) {
                synchronized (mPendingApps) {
                    for (int i = 0; i < apps.size(); i++) {
                        final ApplicationInfo info = apps.get(i);
                        queuePackageMeasurementLocked(info.packageName);
                    }

                    requestQueuedMeasurementsLocked(pm);
                }
            }

            // Sending of the message back to the MeasurementReceiver is
            // completed in the PackageObserver
        }

        public void measureApproximateExternalStorage() {
            File path = Environment.getExternalStorageDirectory();

            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            long availableBlocks = stat.getAvailableBlocks();

            mExternalTotalSize = totalBlocks * blockSize;
            mExternalAvailSize = availableBlocks * blockSize;

            sendExternalApproximateUpdate();
        }
    }

    public void invalidate() {
        mHandler.sendEmptyMessage(MeasurementHandler.MSG_INVALIDATE);
    }
}
