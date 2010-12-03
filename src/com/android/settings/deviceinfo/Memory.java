/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.internal.app.IMediaContainerService;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.res.Resources;
import android.hardware.Usb;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.storage.IMountService;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Memory extends SettingsPreferenceFragment implements OnCancelListener {
    private static final String TAG = "Memory";
    private static final boolean localLOGV = false;

    private static final String MEMORY_SD_SIZE = "memory_sd_size";

    private static final String MEMORY_SD_AVAIL = "memory_sd_avail";

    private static final String MEMORY_SD_MOUNT_TOGGLE = "memory_sd_mount_toggle";

    private static final String MEMORY_SD_FORMAT = "memory_sd_format";

    private static final String MEMORY_SD_GROUP = "memory_sd";

    private static final String PTP_MODE_TOGGLE = "ptp_mode_toggle";

    private static final String MEMORY_INTERNAL_SIZE = "memory_internal_size";

    private static final String MEMORY_INTERNAL_AVAIL = "memory_internal_avail";

    private static final String MEMORY_INTERNAL_APPS = "memory_internal_apps";

    private static final String MEMORY_INTERNAL_MEDIA = "memory_internal_media";

    private static final String MEMORY_INTERNAL_CHART = "memory_internal_chart";

    private static final int DLG_CONFIRM_UNMOUNT = 1;
    private static final int DLG_ERROR_UNMOUNT = 2;

    private Resources mRes;

    // External storage preferences
    private Preference mSdSize;
    private Preference mSdAvail;
    private Preference mSdMountToggle;
    private Preference mSdFormat;
    private PreferenceGroup mSdMountPreferenceGroup;

    // Internal storage preferences
    private Preference mInternalSize;
    private Preference mInternalAvail;
    private Preference mInternalMediaUsage;
    private Preference mInternalAppsUsage;
    private UsageBarPreference mInternalUsageChart;

    // Internal storage chart colors
    private int mInternalMediaColor;
    private int mInternalAppsColor;
    private int mInternalUsedColor;

    // Internal memory fields
    private long mInternalTotalSize;
    private long mInternalUsedSize;
    private long mInternalMediaSize;
    private long mInternalAppsSize;
    private boolean mMeasured = false;

    boolean mSdMountToggleAdded = true;

    private CheckBoxPreference mPtpModeToggle;
    
    // Access using getMountService()
    private IMountService mMountService = null;

    private StorageManager mStorageManager = null;

    // Updates the memory usage bar graph.
    private static final int MSG_UI_UPDATE_APPROXIMATE = 1;

    // Updates the memory usage bar graph.
    private static final int MSG_UI_UPDATE_EXACT = 2;

    private Handler mUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UI_UPDATE_APPROXIMATE:
                    updateUiApproximate();
                    break;
                case MSG_UI_UPDATE_EXACT:
                    updateUiExact();
                    mMeasured = true;
                    break;
            }
        }
    };

    private static final String DEFAULT_CONTAINER_PACKAGE = "com.android.defcontainer";

    private static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(
            DEFAULT_CONTAINER_PACKAGE, "com.android.defcontainer.DefaultContainerService");

    class MemoryMeasurementHandler extends Handler {
        public static final int MSG_MEASURE_ALL = 1;

        public static final int MSG_CONNECTED = 2;

        public static final int MSG_DISCONNECTED = 3;

        private List<String> mPendingApps = new ArrayList<String>();

        private volatile boolean mBound = false;

        private long mAppsSize = 0;

        final private ServiceConnection mDefContainerConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                mBound = true;
                IMediaContainerService imcs = IMediaContainerService.Stub.asInterface(service);
                mMeasurementHandler.sendMessage(mMeasurementHandler.obtainMessage(
                        MemoryMeasurementHandler.MSG_CONNECTED, imcs));
            }

            public void onServiceDisconnected(ComponentName name) {
                mBound = false;
            }
        };

        MemoryMeasurementHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_MEASURE_ALL: {
                    updateExternalStorage();
                    updateApproximateInternalStorage();

                    Intent service = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
                    getActivity().bindService(service, mDefContainerConn, Context.BIND_AUTO_CREATE);

                    mUpdateHandler.sendEmptyMessage(MSG_UI_UPDATE_APPROXIMATE);
                    break;
                }
                case MSG_CONNECTED: {
                    IMediaContainerService imcs = (IMediaContainerService) msg.obj;
                    updateExactInternalStorage(imcs);
                }
            }
        }

        public void cleanUp() {
            if (mBound) {
                getActivity().unbindService(mDefContainerConn);
            }
        }

        public void queuePackageMeasurementLocked(String packageName) {
            mPendingApps.add(packageName);
        }

        public void requestQueuedMeasurementsLocked() {
            final Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            final PackageManager pm = activity.getPackageManager();
            if (pm == null) {
                return;
            }

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

                        mUpdateHandler.sendEmptyMessage(MSG_UI_UPDATE_EXACT);
                    }
                }
            }
        };

        private void updateApproximateInternalStorage() {
            final File dataPath = Environment.getDataDirectory();
            final StatFs stat = new StatFs(dataPath.getPath());
            final long blockSize = stat.getBlockSize();
            final long totalBlocks = stat.getBlockCount();
            final long availableBlocks = stat.getAvailableBlocks();

            final long totalSize = totalBlocks * blockSize;
            final long availSize = availableBlocks * blockSize;
            mInternalSize.setSummary(formatSize(totalSize));
            mInternalAvail.setSummary(formatSize(availSize));

            mInternalTotalSize = totalSize;
            mInternalUsedSize = totalSize - availSize;
        }

        private void updateExactInternalStorage(IMediaContainerService imcs) {
            long mediaSize;
            try {
                // TODO get these directories from somewhere
                mediaSize = imcs.calculateDirectorySize("/data/media");
            } catch (Exception e) {
                Log.i(TAG, "Could not read memory from default container service");
                return;
            }

            mInternalMediaSize = mediaSize;

            // We have to get installd to measure the package sizes.
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm
                    .getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES
                            | PackageManager.GET_DISABLED_COMPONENTS);
            if (apps != null) {
                synchronized (mPendingApps) {
                    for (int i = 0; i < apps.size(); i++) {
                        final ApplicationInfo info = apps.get(i);
                        queuePackageMeasurementLocked(info.packageName);
                    }

                    requestQueuedMeasurementsLocked();
                }
            }
        }

        private void updateExternalStorage() {
            if (Environment.isExternalStorageEmulated()) {
                return;
            }

            String status = Environment.getExternalStorageState();
            String readOnly = "";
            if (status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                status = Environment.MEDIA_MOUNTED;
                readOnly = mRes.getString(R.string.read_only);
            }

            if (status.equals(Environment.MEDIA_MOUNTED)) {
                if (!Environment.isExternalStorageRemovable()) {
                    // This device has built-in storage that is not removable.
                    // There is no reason for the user to unmount it.
                    if (mSdMountToggleAdded) {
                        mSdMountPreferenceGroup.removePreference(mSdMountToggle);
                        mSdMountToggleAdded = false;
                    }
                }
                try {
                    File path = Environment.getExternalStorageDirectory();
                    StatFs stat = new StatFs(path.getPath());
                    long blockSize = stat.getBlockSize();
                    long totalBlocks = stat.getBlockCount();
                    long availableBlocks = stat.getAvailableBlocks();

                    mSdSize.setSummary(formatSize(totalBlocks * blockSize));
                    mSdAvail.setSummary(formatSize(availableBlocks * blockSize) + readOnly);

                    mSdMountToggle.setEnabled(true);
                    mSdMountToggle.setTitle(mRes.getString(R.string.sd_eject));
                    mSdMountToggle.setSummary(mRes.getString(R.string.sd_eject_summary));

                } catch (IllegalArgumentException e) {
                    // this can occur if the SD card is removed, but we haven't
                    // received the
                    // ACTION_MEDIA_REMOVED Intent yet.
                    status = Environment.MEDIA_REMOVED;
                }
            } else {
                mSdSize.setSummary(mRes.getString(R.string.sd_unavailable));
                mSdAvail.setSummary(mRes.getString(R.string.sd_unavailable));

                if (!Environment.isExternalStorageRemovable()) {
                    if (status.equals(Environment.MEDIA_UNMOUNTED)) {
                        if (!mSdMountToggleAdded) {
                            mSdMountPreferenceGroup.addPreference(mSdMountToggle);
                            mSdMountToggleAdded = true;
                        }
                    }
                }

                if (status.equals(Environment.MEDIA_UNMOUNTED) ||
                    status.equals(Environment.MEDIA_NOFS) ||
                    status.equals(Environment.MEDIA_UNMOUNTABLE) ) {
                    mSdMountToggle.setEnabled(true);
                    mSdMountToggle.setTitle(mRes.getString(R.string.sd_mount));
                    mSdMountToggle.setSummary(mRes.getString(R.string.sd_mount_summary));
                } else {
                    mSdMountToggle.setEnabled(false);
                    mSdMountToggle.setTitle(mRes.getString(R.string.sd_mount));
                    mSdMountToggle.setSummary(mRes.getString(R.string.sd_insert_summary));
                }
            }
        }
    }

    private MemoryMeasurementHandler mMeasurementHandler;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (mStorageManager == null) {
            mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            mStorageManager.registerListener(mStorageListener);
        }

        addPreferencesFromResource(R.xml.device_info_memory);

        mRes = getResources();
        mSdSize = findPreference(MEMORY_SD_SIZE);
        mSdAvail = findPreference(MEMORY_SD_AVAIL);
        mSdMountToggle = findPreference(MEMORY_SD_MOUNT_TOGGLE);
        mSdFormat = findPreference(MEMORY_SD_FORMAT);
        mSdMountPreferenceGroup = (PreferenceGroup)findPreference(MEMORY_SD_GROUP);

        if (Environment.isExternalStorageEmulated()) {
            mSdMountPreferenceGroup.removePreference(mSdSize);
            mSdMountPreferenceGroup.removePreference(mSdAvail);
            mSdMountPreferenceGroup.removePreference(mSdMountToggle);
        }

        mPtpModeToggle = (CheckBoxPreference)findPreference(PTP_MODE_TOGGLE);
        if (Usb.isFunctionSupported(Usb.USB_FUNCTION_MTP)) {
            mPtpModeToggle.setChecked(Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.USE_PTP_INTERFACE, 0) != 0);
        } else {
            // hide the PTP mode toggle checkbox if MTP is not supported
            getPreferenceScreen().removePreference(mPtpModeToggle);
        }

        mInternalSize = findPreference(MEMORY_INTERNAL_SIZE);
        mInternalAvail = findPreference(MEMORY_INTERNAL_AVAIL);
        mInternalMediaUsage = findPreference(MEMORY_INTERNAL_MEDIA);
        mInternalAppsUsage = findPreference(MEMORY_INTERNAL_APPS);

        mInternalMediaColor = mRes.getColor(R.color.memory_media_usage);
        mInternalAppsColor = mRes.getColor(R.color.memory_apps_usage);
        mInternalUsedColor = mRes.getColor(R.color.memory_used);

        mInternalUsageChart = (UsageBarPreference) findPreference(MEMORY_INTERNAL_CHART);

        // Start the thread that will measure the disk usage.
        final HandlerThread t = new HandlerThread("MeasurementHandler");
        t.start();
        mMeasurementHandler = new MemoryMeasurementHandler(t.getLooper());
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        getActivity().registerReceiver(mReceiver, intentFilter);

        if (!mMeasured) {
            mMeasurementHandler.sendEmptyMessage(MemoryMeasurementHandler.MSG_MEASURE_ALL);
        }
    }

    StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Log.i(TAG, "Received storage state changed notification that " +
                    path + " changed state from " + oldState +
                    " to " + newState);
            mMeasurementHandler.sendEmptyMessage(MemoryMeasurementHandler.MSG_MEASURE_ALL);
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
        mMeasurementHandler.removeMessages(MemoryMeasurementHandler.MSG_MEASURE_ALL);
    }

    @Override
    public void onDestroy() {
        if (mStorageManager != null && mStorageListener != null) {
            mStorageManager.unregisterListener(mStorageListener);
        }
        mMeasurementHandler.cleanUp();
        super.onDestroy();
    }

    private synchronized IMountService getMountService() {
       if (mMountService == null) {
           IBinder service = ServiceManager.getService("mount");
           if (service != null) {
               mMountService = IMountService.Stub.asInterface(service);
           } else {
               Log.e(TAG, "Can't get mount service");
           }
       }
       return mMountService;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSdMountToggle) {
            String status = Environment.getExternalStorageState();
            if (status.equals(Environment.MEDIA_MOUNTED)) {
                unmount();
            } else {
                mount();
            }
            return true;
        } else if (preference == mSdFormat) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(getActivity(), com.android.settings.MediaFormat.class);
            startActivity(intent);
            return true;
        } else if (preference == mPtpModeToggle) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.USE_PTP_INTERFACE,
                    mPtpModeToggle.isChecked() ? 1 : 0);
            return true;
        }

        return false;
    }
     
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mMeasurementHandler.sendEmptyMessage(MemoryMeasurementHandler.MSG_MEASURE_ALL);
        }
    };

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DLG_CONFIRM_UNMOUNT:
                return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dlg_confirm_unmount_title)
                    .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            doUnmount(true);
                        }})
                    .setNegativeButton(R.string.cancel, null)
                    .setMessage(R.string.dlg_confirm_unmount_text)
                    .setOnCancelListener(this)
                    .create();
        case DLG_ERROR_UNMOUNT:
                return new AlertDialog.Builder(getActivity())
            .setTitle(R.string.dlg_error_unmount_title)
            .setNeutralButton(R.string.dlg_ok, null)
            .setMessage(R.string.dlg_error_unmount_text)
            .setOnCancelListener(this)
            .create();
        }
        return null;
    }

    private void doUnmount(boolean force) {
        // Present a toast here
        Toast.makeText(getActivity(), R.string.unmount_inform_text, Toast.LENGTH_SHORT).show();
        IMountService mountService = getMountService();
        String extStoragePath = Environment.getExternalStorageDirectory().toString();
        try {
            mSdMountToggle.setEnabled(false);
            mSdMountToggle.setTitle(mRes.getString(R.string.sd_ejecting_title));
            mSdMountToggle.setSummary(mRes.getString(R.string.sd_ejecting_summary));
            mountService.unmountVolume(extStoragePath, force);
        } catch (RemoteException e) {
            // Informative dialog to user that
            // unmount failed.
            showDialogInner(DLG_ERROR_UNMOUNT);
        }
    }

    private void showDialogInner(int id) {
        removeDialog(id);
        showDialog(id);
    }

    private boolean hasAppsAccessingStorage() throws RemoteException {
        String extStoragePath = Environment.getExternalStorageDirectory().toString();
        IMountService mountService = getMountService();
        int stUsers[] = mountService.getStorageUsers(extStoragePath);
        if (stUsers != null && stUsers.length > 0) {
            return true;
        }
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ApplicationInfo> list = am.getRunningExternalApplications();
        if (list != null && list.size() > 0) {
            return true;
        }
        return false;
    }

    private void unmount() {
        // Check if external media is in use.
        try {
           if (hasAppsAccessingStorage()) {
               if (localLOGV) Log.i(TAG, "Do have storage users accessing media");
               // Present dialog to user
               showDialogInner(DLG_CONFIRM_UNMOUNT);
           } else {
               doUnmount(true);
           }
        } catch (RemoteException e) {
            // Very unlikely. But present an error dialog anyway
            Log.e(TAG, "Is MountService running?");
            showDialogInner(DLG_ERROR_UNMOUNT);
        }
    }

    private void mount() {
        IMountService mountService = getMountService();
        try {
            if (mountService != null) {
                mountService.mountVolume(Environment.getExternalStorageDirectory().toString());
            } else {
                Log.e(TAG, "Mount service is null, can't mount");
            }
        } catch (RemoteException ex) {
        }
    }

    private void updateUiExact() {
        final float totalSize = mInternalTotalSize;

        final long mediaSize = mInternalMediaSize;
        final long appsSize = mInternalAppsSize;

        mInternalUsageChart.clear();
        mInternalUsageChart.addEntry(mediaSize / totalSize, mInternalMediaColor);
        mInternalUsageChart.addEntry(appsSize / totalSize, mInternalAppsColor);

        // There are other things that can take up storage, but we didn't
        // measure it.
        final long remaining = mInternalUsedSize - (mediaSize + appsSize);
        if (remaining > 0) {
            mInternalUsageChart.addEntry(remaining / totalSize, mInternalUsedColor);
        }
        mInternalUsageChart.commit();

        mInternalMediaUsage.setSummary(formatSize(mediaSize));
        mInternalAppsUsage.setSummary(formatSize(appsSize));
    }

    private void updateUiApproximate() {
        mInternalUsageChart.clear();
        mInternalUsageChart.addEntry(mInternalUsedSize / (float) mInternalTotalSize, getResources()
                .getColor(R.color.memory_used));
        mInternalUsageChart.commit();
    }

    private String formatSize(long size) {
        return Formatter.formatFileSize(getActivity(), size);
    }

    public void onCancel(DialogInterface dialog) {
        // TODO: Is this really required?
        // finish();
    }
}
