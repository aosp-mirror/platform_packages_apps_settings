/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Environment;
import android.os.IMountService;
import android.os.ServiceManager;
import android.os.StatFs;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;


public class SdCardSettings extends Activity
{
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.sdcard_settings_screen);

        mMountService = IMountService.Stub.asInterface(ServiceManager.getService("mount"));

        mRemovedLayout = findViewById(R.id.removed);
        mMountedLayout = findViewById(R.id.mounted);
        mUnmountedLayout = findViewById(R.id.unmounted);
        mScanningLayout = findViewById(R.id.scanning);
        mSharedLayout = findViewById(R.id.shared);
        mBadRemovalLayout = findViewById(R.id.bad_removal);
        mReadOnlyStatus = findViewById(R.id.read_only);

        mMassStorage = (CheckBox)findViewById(R.id.mass_storage);
        mMassStorage.setOnClickListener(mMassStorageListener);

        Button unmountButton = (Button)findViewById(R.id.sdcard_unmount);
        unmountButton.setOnClickListener(mUnmountButtonHandler);

        Button formatButton = (Button)findViewById(R.id.sdcard_format);
        formatButton.setOnClickListener(mFormatButtonHandler);

        mTotalSize = (TextView)findViewById(R.id.total);
        mUsedSize = (TextView)findViewById(R.id.used);
        mAvailableSize = (TextView)findViewById(R.id.available);

        // install an intent filter to receive SD card related events.
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SHARED);
        intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
        intentFilter.addAction(Intent.ACTION_MEDIA_NOFS);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    private void setLayout(View layout) {
        mRemovedLayout.setVisibility(layout == mRemovedLayout ? View.VISIBLE : View.GONE);
        mMountedLayout.setVisibility(layout == mMountedLayout ? View.VISIBLE : View.GONE);
        mUnmountedLayout.setVisibility(layout == mUnmountedLayout ? View.VISIBLE : View.GONE);
        mScanningLayout.setVisibility(layout == mScanningLayout ? View.VISIBLE : View.GONE);
        mSharedLayout.setVisibility(layout == mSharedLayout ? View.VISIBLE : View.GONE);
        mBadRemovalLayout.setVisibility(layout == mBadRemovalLayout ? View.VISIBLE : View.GONE);
    }

    private void update() {
        try {
            mMassStorage.setChecked(mMountService.getMassStorageEnabled());
        } catch (RemoteException ex) {
        }

        String scanVolume = null; // this no longer exists: SystemProperties.get(MediaScanner.CURRENT_VOLUME_PROPERTY, "");
        boolean scanning = "external".equals(scanVolume);

        if (scanning) {
            setLayout(mScanningLayout);
        } else {
            String status = Environment.getExternalStorageState();
            boolean readOnly = false;

            if (status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                status = Environment.MEDIA_MOUNTED;
                readOnly = true;
            }

            if (status.equals(Environment.MEDIA_MOUNTED)) {
                try {
                    File path = Environment.getExternalStorageDirectory();
                    StatFs stat = new StatFs(path.getPath());
                    long blockSize = stat.getBlockSize();
                    long totalBlocks = stat.getBlockCount();
                    long availableBlocks = stat.getAvailableBlocks();

                    mTotalSize.setText(formatSize(totalBlocks * blockSize));
                    mUsedSize.setText(formatSize((totalBlocks - availableBlocks) * blockSize));
                    mAvailableSize.setText(formatSize(availableBlocks * blockSize));
                } catch (IllegalArgumentException e) {
                    // this can occur if the SD card is removed, but we haven't received the
                    // ACTION_MEDIA_REMOVED Intent yet.
                    status = Environment.MEDIA_REMOVED;
                }

                mReadOnlyStatus.setVisibility(readOnly ? View.VISIBLE : View.GONE);
                setLayout(mMountedLayout);
            } else if (status.equals(Environment.MEDIA_UNMOUNTED)) {
                setLayout(mUnmountedLayout);
            } else if (status.equals(Environment.MEDIA_REMOVED)) {
                setLayout(mRemovedLayout);
            } else if (status.equals(Environment.MEDIA_SHARED)) {
                setLayout(mSharedLayout);
            } else if (status.equals(Environment.MEDIA_BAD_REMOVAL)) {
                setLayout(mBadRemovalLayout);
            }
        }
    }

    private String formatSize(long size) {
        String suffix = null;

        // add K or M suffix if size is greater than 1K or 1M
        if (size >= 1024) {
            suffix = "K";
            size /= 1024;
            if (size >= 1024) {
                suffix = "M";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null)
            resultBuffer.append(suffix);
        return resultBuffer.toString();
    }

    OnClickListener mMassStorageListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                mMountService.setMassStorageEnabled(mMassStorage.isChecked());
            } catch (RemoteException ex) {
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    OnClickListener mUnmountButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            try {
                mMountService.unmountMedia(Environment.getExternalStorageDirectory().toString());
            } catch (RemoteException ex) {
            }
        }
    };

    OnClickListener mFormatButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            try {
                mMountService.formatMedia(Environment.getExternalStorageDirectory().toString());
            } catch (RemoteException ex) {
            }
        }
    };


    private int         mStatus;
    private IMountService   mMountService;

    private CheckBox    mMassStorage;

    private TextView    mTotalSize;
    private TextView    mUsedSize;
    private TextView    mAvailableSize;

    private View        mRemovedLayout;
    private View        mMountedLayout;
    private View        mUnmountedLayout;
    private View        mScanningLayout;
    private View        mSharedLayout;
    private View        mBadRemovalLayout;
    private View        mReadOnlyStatus;
}
