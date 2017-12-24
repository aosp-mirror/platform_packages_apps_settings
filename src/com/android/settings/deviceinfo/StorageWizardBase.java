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

import android.annotation.LayoutRes;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;
import com.android.setupwizardlib.GlifLayout;

import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

public abstract class StorageWizardBase extends Activity {
    protected StorageManager mStorage;

    protected VolumeInfo mVolume;
    protected DiskInfo mDisk;

    private Button mNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStorage = getSystemService(StorageManager.class);

        final String volumeId = getIntent().getStringExtra(VolumeInfo.EXTRA_VOLUME_ID);
        if (!TextUtils.isEmpty(volumeId)) {
            mVolume = mStorage.findVolumeById(volumeId);
        }

        final String diskId = getIntent().getStringExtra(DiskInfo.EXTRA_DISK_ID);
        if (!TextUtils.isEmpty(diskId)) {
            mDisk = mStorage.findDiskById(diskId);
        } else if (mVolume != null) {
            mDisk = mVolume.getDisk();
        }

        if (mDisk != null) {
            mStorage.registerListener(mStorageListener);
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        mNext = (Button) findViewById(R.id.storage_next_button);
        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavigateNext();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mStorage.unregisterListener(mStorageListener);
        super.onDestroy();
    }

    protected Button getNextButton() {
        return mNext;
    }

    protected GlifLayout getGlifLayout() {
        return (GlifLayout) findViewById(R.id.setup_wizard_layout);
    }

    protected ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.storage_wizard_progress);
    }

    protected void setCurrentProgress(int progress) {
        getProgressBar().setProgress(progress);
        ((TextView) findViewById(R.id.storage_wizard_progress_summary)).setText(
                NumberFormat.getPercentInstance().format((double) progress / 100));
    }

    protected void setHeaderText(int resId, String... args) {
        final CharSequence headerText = TextUtils.expandTemplate(getText(resId), args);
        getGlifLayout().setHeaderText(headerText);
        setTitle(headerText);
    }

    protected void setBodyText(int resId, String... args) {
        ((TextView) findViewById(R.id.storage_wizard_body)).setText(
                TextUtils.expandTemplate(getText(resId), args));
    }

    protected void setSecondaryBodyText(int resId, String... args) {
        final TextView secondBody = ((TextView) findViewById(R.id.storage_wizard_second_body));
        secondBody.setText(TextUtils.expandTemplate(getText(resId), args));
        secondBody.setVisibility(View.VISIBLE);
    }

    protected static final int ILLUSTRATION_SETUP = 0;
    protected static final int ILLUSTRATION_INTERNAL = 1;
    protected static final int ILLUSTRATION_PORTABLE = 2;

    protected void setIllustrationType(int type) {
        // TODO: map type to updated icons once provided by UX
        TypedArray array = obtainStyledAttributes(new int[] {android.R.attr.colorAccent});
        Drawable icon = getDrawable(com.android.internal.R.drawable.ic_sd_card_48dp).mutate();
        icon.setTint(array.getColor(0, 0));
        array.recycle();
        getGlifLayout().setIcon(icon);
    }

    protected void setKeepScreenOn(boolean keepScreenOn) {
        getGlifLayout().setKeepScreenOn(keepScreenOn);
    }

    public void onNavigateNext() {
        throw new UnsupportedOperationException();
    }

    protected VolumeInfo findFirstVolume(int type) {
        return findFirstVolume(type, 1);
    }

    protected VolumeInfo findFirstVolume(int type, int attempts) {
        while (true) {
            final List<VolumeInfo> vols = mStorage.getVolumes();
            for (VolumeInfo vol : vols) {
                if (Objects.equals(mDisk.getId(), vol.getDiskId()) && (vol.getType() == type)
                        && (vol.getState() == VolumeInfo.STATE_MOUNTED)) {
                    return vol;
                }
            }

            if (--attempts > 0) {
                Log.w(TAG, "Missing mounted volume of type " + type + " hosted by disk "
                        + mDisk.getId() + "; trying again");
                SystemClock.sleep(250);
            } else {
                return null;
            }
        }
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            // We know mDisk != null.
            if (mDisk.id.equals(disk.id)) {
                finish();
            }
        }
    };
}
