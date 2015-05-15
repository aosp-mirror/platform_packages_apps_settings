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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

public class StorageWizardFormatProgress extends StorageWizardBase {
    private boolean mFormatPrivate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_wizard_progress);

        Preconditions.checkNotNull(mDisk);

        mFormatPrivate = getIntent().getBooleanExtra(
                StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);

        setHeaderText(R.string.storage_wizard_format_progress_title, mDisk.getDescription());
        setBodyText(R.string.storage_wizard_format_progress_body, mDisk.getDescription());

        setCurrentProgress(20);

        getNextButton().setVisibility(View.GONE);

        new PartitionTask().execute();
    }

    public class PartitionTask extends AsyncTask<Void, Integer, Exception> {
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                if (mFormatPrivate) {
                    mStorage.partitionPrivate(mDisk.getId());
                    publishProgress(40);

                    final long internalBench = mStorage.benchmark(null);
                    publishProgress(60);

                    final VolumeInfo privateVol = findFirstVolume(VolumeInfo.TYPE_PRIVATE);
                    final long privateBench = mStorage.benchmark(privateVol.id);

                    // TODO: plumb through to user when below threshold
                    final float pct = (float) internalBench / (float) privateBench;
                    Log.d(TAG, "New volume is " + pct + "x the speed of internal");
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
            if (e == null) {
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
                    final Intent intent = new Intent(context, StorageWizardMigrate.class);
                    intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                    startActivity(intent);
                } else {
                    final Intent intent = new Intent(context, StorageWizardReady.class);
                    intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                    startActivity(intent);
                }
                finishAffinity();

            } else {
                Log.e(TAG, "Failed to partition", e);
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                finishAffinity();
            }
        }
    }
}
