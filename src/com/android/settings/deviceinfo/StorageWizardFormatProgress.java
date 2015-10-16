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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageMoveObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.settings.R;

import java.util.Objects;

public class StorageWizardFormatProgress extends StorageWizardBase {
    private static final String TAG_SLOW_WARNING = "slow_warning";

    private boolean mFormatPrivate;

    private PartitionTask mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_progress);
        setKeepScreenOn(true);

        mFormatPrivate = getIntent().getBooleanExtra(
                StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);
        setIllustrationInternal(mFormatPrivate);

        setHeaderText(R.string.storage_wizard_format_progress_title, mDisk.getDescription());
        setBodyText(R.string.storage_wizard_format_progress_body, mDisk.getDescription());

        getNextButton().setVisibility(View.GONE);

        mTask = (PartitionTask) getLastNonConfigurationInstance();
        if (mTask == null) {
            mTask = new PartitionTask();
            mTask.setActivity(this);
            mTask.execute();
        } else {
            mTask.setActivity(this);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mTask;
    }

    public static class PartitionTask extends AsyncTask<Void, Integer, Exception> {
        public StorageWizardFormatProgress mActivity;

        private volatile int mProgress = 20;

        private volatile long mInternalBench;
        private volatile long mPrivateBench;

        @Override
        protected Exception doInBackground(Void... params) {
            final StorageWizardFormatProgress activity = mActivity;
            final StorageManager storage = mActivity.mStorage;
            try {
                if (activity.mFormatPrivate) {
                    storage.partitionPrivate(activity.mDisk.getId());
                    publishProgress(40);

                    mInternalBench = storage.benchmark(null);
                    publishProgress(60);

                    final VolumeInfo privateVol = activity.findFirstVolume(VolumeInfo.TYPE_PRIVATE);
                    mPrivateBench = storage.benchmark(privateVol.getId());

                    // If we just adopted the device that had been providing
                    // physical storage, then automatically move storage to the
                    // new emulated volume.
                    if (activity.mDisk.isDefaultPrimary()
                            && Objects.equals(storage.getPrimaryStorageUuid(),
                                    StorageManager.UUID_PRIMARY_PHYSICAL)) {
                        Log.d(TAG, "Just formatted primary physical; silently moving "
                                + "storage to new emulated volume");
                        storage.setPrimaryStorageUuid(privateVol.getFsUuid(), new SilentObserver());
                    }

                } else {
                    storage.partitionPublic(activity.mDisk.getId());
                }
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgress = progress[0];
            mActivity.setCurrentProgress(mProgress);
        }

        public void setActivity(StorageWizardFormatProgress activity) {
            mActivity = activity;
            mActivity.setCurrentProgress(mProgress);
        }

        @Override
        protected void onPostExecute(Exception e) {
            final StorageWizardFormatProgress activity = mActivity;
            if (activity.isDestroyed()) {
                return;
            }

            if (e != null) {
                Log.e(TAG, "Failed to partition", e);
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
                activity.finishAffinity();
                return;
            }

            if (activity.mFormatPrivate) {
                final float pct = (float) mInternalBench / (float) mPrivateBench;
                Log.d(TAG, "New volume is " + pct + "x the speed of internal");

                // To help set user expectations around device performance, we
                // warn if the adopted media is 0.25x the speed of internal
                // storage or slower.
                if (Float.isNaN(pct) || pct < 0.25) {
                    final SlowWarningFragment dialog = new SlowWarningFragment();
                    dialog.showAllowingStateLoss(activity.getFragmentManager(), TAG_SLOW_WARNING);
                } else {
                    activity.onFormatFinished();
                }
            } else {
                activity.onFormatFinished();
            }
        }
    }

    public static class SlowWarningFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);

            final StorageWizardFormatProgress target =
                    (StorageWizardFormatProgress) getActivity();
            final String descrip = target.getDiskDescription();
            final String genericDescip = target.getGenericDiskDescription();
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_wizard_slow_body),
                    descrip, genericDescip));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final StorageWizardFormatProgress target =
                            (StorageWizardFormatProgress) getActivity();
                    target.onFormatFinished();
                }
            });

            return builder.create();
        }
    }

    private String getDiskDescription() {
        return mDisk.getDescription();
    }

    private String getGenericDiskDescription() {
        // TODO: move this directly to DiskInfo
        if (mDisk.isSd()) {
            return getString(com.android.internal.R.string.storage_sd_card);
        } else if (mDisk.isUsb()) {
            return getString(com.android.internal.R.string.storage_usb_drive);
        } else {
            return null;
        }
    }

    private void onFormatFinished() {
        final String forgetUuid = getIntent().getStringExtra(
                StorageWizardFormatConfirm.EXTRA_FORGET_UUID);
        if (!TextUtils.isEmpty(forgetUuid)) {
            mStorage.forgetVolume(forgetUuid);
        }

        final boolean offerMigrate;
        if (mFormatPrivate) {
            // Offer to migrate only if storage is currently internal
            final VolumeInfo privateVol = getPackageManager()
                    .getPrimaryStorageCurrentVolume();
            offerMigrate = (privateVol != null
                    && VolumeInfo.ID_PRIVATE_INTERNAL.equals(privateVol.getId()));
        } else {
            offerMigrate = false;
        }

        if (offerMigrate) {
            final Intent intent = new Intent(this, StorageWizardMigrate.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            startActivity(intent);
        } else {
            final Intent intent = new Intent(this, StorageWizardReady.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            startActivity(intent);
        }
        finishAffinity();
    }

    private static class SilentObserver extends IPackageMoveObserver.Stub {
        @Override
        public void onCreated(int moveId, Bundle extras) {
            // Ignored
        }

        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            // Ignored
        }
    }
}
