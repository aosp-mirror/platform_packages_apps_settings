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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.settings.R;

public class StorageWizardFormatProgress extends StorageWizardBase {
    private static final String TAG_SLOW_WARNING = "slow_warning";

    private boolean mFormatPrivate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_progress);

        mFormatPrivate = getIntent().getBooleanExtra(
                StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);
        setIllustrationInternal(mFormatPrivate);

        setHeaderText(R.string.storage_wizard_format_progress_title, mDisk.getDescription());
        setBodyText(R.string.storage_wizard_format_progress_body, mDisk.getDescription());

        setCurrentProgress(20);

        getNextButton().setVisibility(View.GONE);

        new PartitionTask().execute();
    }

    public class PartitionTask extends AsyncTask<Void, Integer, Exception> {
        private volatile long mInternalBench;
        private volatile long mPrivateBench;

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                if (mFormatPrivate) {
                    mStorage.partitionPrivate(mDisk.getId());
                    publishProgress(40);

                    mInternalBench = mStorage.benchmark(null);
                    publishProgress(60);

                    final VolumeInfo privateVol = findFirstVolume(VolumeInfo.TYPE_PRIVATE);
                    mPrivateBench = mStorage.benchmark(privateVol.id);

                } else {
                    mStorage.partitionPublic(mDisk.getId());
                }
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            setCurrentProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Exception e) {
            final Context context = StorageWizardFormatProgress.this;
            if (e != null) {
                Log.e(TAG, "Failed to partition", e);
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                finishAffinity();
                return;
            }

            final float pct = (float) mInternalBench / (float) mPrivateBench;
            Log.d(TAG, "New volume is " + pct + "x the speed of internal");

            // TODO: refine this warning threshold
            if (mPrivateBench > 2000000000) {
                final SlowWarningFragment dialog = new SlowWarningFragment();
                dialog.show(getFragmentManager(), TAG_SLOW_WARNING);
            } else {
                onFormatFinished();
            }
        }
    }

    public class SlowWarningFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);

            final String descrip = mDisk.getDescription();
            final String genericDescip = getGenericDescription(mDisk);
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

    private String getGenericDescription(DiskInfo disk) {
        // TODO: move this directly to DiskInfo
        if (disk.isSd()) {
            return getString(com.android.internal.R.string.storage_sd_card);
        } else if (disk.isUsb()) {
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
}
