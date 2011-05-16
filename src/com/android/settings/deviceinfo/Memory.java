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

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.deviceinfo.Constants.MediaDirectory;
import com.android.settings.deviceinfo.MemoryMeasurement.MeasurementReceiver;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class Memory extends SettingsPreferenceFragment implements OnCancelListener,
        MeasurementReceiver {
    private static final String TAG = "MemorySettings";

    private static final String MEMORY_SD_SIZE = "memory_sd_size";

    private static final String MEMORY_SD_AVAIL = "memory_sd_avail";

    private static final String MEMORY_SD_MOUNT_TOGGLE = "memory_sd_mount_toggle";

    private static final String MEMORY_SD_FORMAT = "memory_sd_format";

    private static final String MEMORY_SD_GROUP = "memory_sd";

    private static final String MEMORY_INTERNAL_SIZE = "memory_internal_size";

    private static final String MEMORY_INTERNAL_AVAIL = "memory_internal_avail";

    private static final String MEMORY_INTERNAL_APPS = "memory_internal_apps";

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
    private Preference mInternalAppsUsage;
    private final Preference[] mMediaPreferences = new Preference[Constants.NUM_MEDIA_DIRS_TRACKED];
    private UsageBarPreference mInternalUsageChart;

    // Internal storage chart colors
    private int mInternalAppsColor;
    private int mInternalAvailColor;
    private int mInternalUsedColor;

    boolean mSdMountToggleAdded = true;
    
    // Access using getMountService()
    private IMountService mMountService = null;

    private StorageManager mStorageManager = null;

    // Updates the memory usage bar graph.
    private static final int MSG_UI_UPDATE_INTERNAL_APPROXIMATE = 1;

    // Updates the memory usage bar graph.
    private static final int MSG_UI_UPDATE_INTERNAL_EXACT = 2;

    // Updates the memory usage stats for external.
    private static final int MSG_UI_UPDATE_EXTERNAL_APPROXIMATE = 3;

    private Handler mUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UI_UPDATE_INTERNAL_APPROXIMATE: {
                    Bundle bundle = msg.getData();
                    final long totalSize = bundle.getLong(MemoryMeasurement.TOTAL_SIZE);
                    final long availSize = bundle.getLong(MemoryMeasurement.AVAIL_SIZE);
                    updateUiApproximate(totalSize, availSize);
                    break;
                }
                case MSG_UI_UPDATE_INTERNAL_EXACT: {
                    Bundle bundle = msg.getData();
                    final long totalSize = bundle.getLong(MemoryMeasurement.TOTAL_SIZE);
                    final long availSize = bundle.getLong(MemoryMeasurement.AVAIL_SIZE);
                    final long appsUsed = bundle.getLong(MemoryMeasurement.APPS_USED);
                    final long[] mediaSizes = new long[Constants.NUM_MEDIA_DIRS_TRACKED];
                    for (int i = 0; i < Constants.NUM_MEDIA_DIRS_TRACKED; i++) {
                        mediaSizes[i] = bundle.getLong(Constants.mMediaDirs.get(i).mKey);
                    }
                    updateUiExact(totalSize, availSize, appsUsed, mediaSizes);
                    break;
                }
                case MSG_UI_UPDATE_EXTERNAL_APPROXIMATE: {
                    Bundle bundle = msg.getData();
                    final long totalSize = bundle.getLong(MemoryMeasurement.TOTAL_SIZE);
                    final long availSize = bundle.getLong(MemoryMeasurement.AVAIL_SIZE);
                    updateExternalStorage(totalSize, availSize);
                    break;
                }
            }
        }
    };

    private MemoryMeasurement mMeasurement;

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
            getPreferenceScreen().removePreference(mSdMountPreferenceGroup);
        }

        mInternalSize = findPreference(MEMORY_INTERNAL_SIZE);
        mInternalAppsColor = mRes.getColor(R.color.memory_apps_usage);
        mInternalUsedColor = android.graphics.Color.GRAY;
        mInternalAvailColor = mRes.getColor(R.color.memory_avail);
        final int buttonWidth = (int) mRes.getDimension(R.dimen.device_memory_usage_button_width);
        final int buttonHeight = (int) mRes.getDimension(R.dimen.device_memory_usage_button_height);

        // total available space
        mInternalAvail = findPreference(MEMORY_INTERNAL_AVAIL);
        mInternalAvail.setIcon(createRectShape(buttonHeight, buttonWidth, mInternalAvailColor));

        // used by apps
        mInternalAppsUsage = findPreference(MEMORY_INTERNAL_APPS);
        mInternalAppsUsage.setIcon(createRectShape(buttonHeight, buttonWidth, mInternalAppsColor));

        // space used by individual major directories on /sdcard
        for (int i = 0; i < Constants.NUM_MEDIA_DIRS_TRACKED; i++) {
            // nothing to be displayed for certain entries in Constants.mMediaDirs
            final MediaDirectory mediaDirectory = Constants.mMediaDirs.get(i);
            final String preferenceName = mediaDirectory.mPreferenceName;
            if (preferenceName == null) {
                continue;
            }
            mMediaPreferences[i] = findPreference(preferenceName);
            final int color = mRes.getColor(mediaDirectory.mColor);
            mMediaPreferences[i].setIcon(createRectShape(buttonHeight, buttonWidth, color));
        }
        mInternalUsageChart = (UsageBarPreference) findPreference(MEMORY_INTERNAL_CHART);

        mMeasurement = MemoryMeasurement.getInstance(getActivity());
        mMeasurement.setReceiver(this);
    }

    private ShapeDrawable createRectShape(int height, int width, int color) {
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.setIntrinsicHeight(height);
        shape.setIntrinsicWidth(width);
        shape.getPaint().setColor(color);
        return shape;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMeasurement.setReceiver(this);
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        getActivity().registerReceiver(mReceiver, intentFilter);

        mMeasurement.invalidate();
        if (!Environment.isExternalStorageEmulated()) {
            mMeasurement.measureExternal();
        }
        mMeasurement.measureInternal();
    }

    StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Log.i(TAG, "Received storage state changed notification that " +
                    path + " changed state from " + oldState +
                    " to " + newState);
            if (!Environment.isExternalStorageEmulated()) {
                mMeasurement.measureExternal();
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
        mMeasurement.cleanUp();
    }

    @Override
    public void onDestroy() {
        if (mStorageManager != null && mStorageListener != null) {
            mStorageManager.unregisterListener(mStorageListener);
        }
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
        } else if (preference == mInternalAppsUsage) {
            Intent intent = new Intent(Intent.ACTION_MANAGE_PACKAGE_STORAGE);
            intent.setClass(getActivity(),
                    com.android.settings.Settings.ManageApplicationsActivity.class);
            startActivity(intent);
            return true;
        } else if (preference == mMediaPreferences[Constants.DOWNLOADS_INDEX]) {
            Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                    .putExtra(DownloadManager.INTENT_EXTRAS_SORT_BY_SIZE, true);
            startActivity(intent);
            return true;
        } else if (preference == mMediaPreferences[Constants.MUSIC_INDEX]) {
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("audio/mp3");
            startActivity(intent);
            return true;
        } else if (preference == mMediaPreferences[Constants.PIC_VIDEO_INDEX]) {
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("image/jpeg");
            startActivity(intent);
            return true;
        } else if (preference == mMediaPreferences[Constants.MEDIA_MISC_INDEX]) {
            Context context = getActivity().getApplicationContext();
            if (MemoryMeasurement.getInstance(context).isSizeOfMiscCategoryNonZero()) {
                startActivity(new Intent(context, MiscFilesHandler.class));
            }
            return true;
        }

        return false;
    }
     
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mMeasurement.invalidate();

            if (!Environment.isExternalStorageEmulated()) {
                mMeasurement.measureExternal();
            }
            mMeasurement.measureInternal();
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
                    .create();
        case DLG_ERROR_UNMOUNT:
                return new AlertDialog.Builder(getActivity())
            .setTitle(R.string.dlg_error_unmount_title)
            .setNeutralButton(R.string.dlg_ok, null)
            .setMessage(R.string.dlg_error_unmount_text)
            .create();
        }
        return null;
    }

    @Override
    protected void showDialog(int id) {
        super.showDialog(id);

        switch (id) {
        case DLG_CONFIRM_UNMOUNT:
        case DLG_ERROR_UNMOUNT:
            setOnCancelListener(this);
            break;
        }
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

    private void updateUiExact(long totalSize, long availSize, long appsSize, long[] mediaSizes) {
        // There are other things that can take up storage, but we didn't measure it.
        // add that unaccounted-for-usage to Apps Usage
        long appsPlusRemaining = totalSize - availSize;
        for (int i = 0; i < Constants.NUM_MEDIA_DIRS_TRACKED; i++) {
            if (Constants.mMediaDirs.get(i).mPreferenceName == null) {
                continue;
            }
            appsPlusRemaining -= mediaSizes[i];
        }

        mInternalSize.setSummary(formatSize(totalSize));
        mInternalAvail.setSummary(formatSize(availSize));
        mInternalAppsUsage.setSummary(formatSize(appsPlusRemaining));

        mInternalUsageChart.clear();
        mInternalUsageChart.addEntry(appsPlusRemaining / (float) totalSize, mInternalAppsColor);

        for (int i = 0; i < Constants.NUM_MEDIA_DIRS_TRACKED; i++) {
            if (Constants.mMediaDirs.get(i).mPreferenceName == null) {
                continue;
            }
            this.mMediaPreferences[i].setSummary(formatSize(mediaSizes[i]));
            // don't add entry to color chart for media usage and for zero-sized dirs
            if (mediaSizes[i] > 0) {
                final int color = mRes.getColor(Constants.mMediaDirs.get(i).mColor);
                mInternalUsageChart.addEntry(mediaSizes[i] / (float) totalSize, color);
            }
        }
        mInternalUsageChart.addEntry(availSize / (float) totalSize, mInternalAvailColor);
        mInternalUsageChart.commit();
    }

    private void updateUiApproximate(long totalSize, long availSize) {
        mInternalSize.setSummary(formatSize(totalSize));
        mInternalAvail.setSummary(formatSize(availSize));

        final long usedSize = totalSize - availSize;

        mInternalUsageChart.clear();
        mInternalUsageChart.addEntry(usedSize / (float) totalSize, mInternalUsedColor);
        mInternalUsageChart.commit();
    }

    private void updateExternalStorage(long totalSize, long availSize) {
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
                mSdSize.setSummary(formatSize(totalSize));
                mSdAvail.setSummary(formatSize(availSize) + readOnly);

                mSdMountToggle.setEnabled(true);
                mSdMountToggle.setTitle(mRes.getString(R.string.sd_eject));
                mSdMountToggle.setSummary(mRes.getString(R.string.sd_eject_summary));

            } catch (IllegalArgumentException e) {
                // this can occur if the SD card is removed, but we haven't
                // received the ACTION_MEDIA_REMOVED Intent yet.
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

            if (status.equals(Environment.MEDIA_UNMOUNTED) || status.equals(Environment.MEDIA_NOFS)
                    || status.equals(Environment.MEDIA_UNMOUNTABLE)) {
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

    private String formatSize(long size) {
        return Formatter.formatFileSize(getActivity(), size);
    }

    public void onCancel(DialogInterface dialog) {
        // TODO: Is this really required?
        // finish();
    }

    @Override
    public void updateApproximateExternal(Bundle bundle) {
        final Message message = mUpdateHandler.obtainMessage(MSG_UI_UPDATE_EXTERNAL_APPROXIMATE);
        message.setData(bundle);
        mUpdateHandler.sendMessage(message);
    }

    @Override
    public void updateApproximateInternal(Bundle bundle) {
        final Message message = mUpdateHandler.obtainMessage(MSG_UI_UPDATE_INTERNAL_APPROXIMATE);
        message.setData(bundle);
        mUpdateHandler.sendMessage(message);
    }

    @Override
    public void updateExactInternal(Bundle bundle) {
        final Message message = mUpdateHandler.obtainMessage(MSG_UI_UPDATE_INTERNAL_EXACT);
        message.setData(bundle);
        mUpdateHandler.sendMessage(message);
    }
}
