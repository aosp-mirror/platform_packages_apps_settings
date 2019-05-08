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

import static android.os.storage.VolumeInfo.TYPE_PRIVATE;

import static com.android.settings.deviceinfo.StorageSettings.TAG;

import android.content.Intent;
import android.content.pm.IPackageMoveObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IVoldTaskListener;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.settings.R;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class StorageWizardFormatProgress extends StorageWizardBase {
    private static final String PROP_DEBUG_STORAGE_SLOW = "sys.debug.storage_slow";

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

        mFormatPrivate = getIntent().getBooleanExtra(EXTRA_FORMAT_PRIVATE, false);

        setHeaderText(R.string.storage_wizard_format_progress_title, getDiskShortDescription());
        setBodyText(R.string.storage_wizard_format_progress_body, getDiskDescription());
        setBackButtonVisibility(View.INVISIBLE);
        setNextButtonVisibility(View.INVISIBLE);
        mTask = (PartitionTask) getLastCustomNonConfigurationInstance();
        if (mTask == null) {
            mTask = new PartitionTask();
            mTask.setActivity(this);
            mTask.execute();
        } else {
            mTask.setActivity(this);
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mTask;
    }

    public static class PartitionTask extends AsyncTask<Void, Integer, Exception> {
        public StorageWizardFormatProgress mActivity;

        private volatile int mProgress = 20;

        private volatile long mPrivateBench;

        @Override
        protected Exception doInBackground(Void... params) {
            final StorageWizardFormatProgress activity = mActivity;
            final StorageManager storage = mActivity.mStorage;
            try {
                if (activity.mFormatPrivate) {
                    storage.partitionPrivate(activity.mDisk.getId());
                    publishProgress(40);

                    final VolumeInfo privateVol = activity.findFirstVolume(TYPE_PRIVATE, 25);
                    final CompletableFuture<PersistableBundle> result = new CompletableFuture<>();
                    storage.benchmark(privateVol.getId(), new IVoldTaskListener.Stub() {
                        @Override
                        public void onStatus(int status, PersistableBundle extras) {
                            // Map benchmark 0-100% progress onto 40-80%
                            publishProgress(40 + ((status * 40) / 100));
                        }

                        @Override
                        public void onFinished(int status, PersistableBundle extras) {
                            result.complete(extras);
                        }
                    });
                    mPrivateBench = result.get(60, TimeUnit.SECONDS).getLong("run", Long.MAX_VALUE);

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
                // When the adoptable storage feature originally launched, we
                // benchmarked both internal storage and the newly adopted
                // storage and we warned if the adopted device was less than
                // 0.25x the speed of internal. (The goal was to help set user
                // expectations and encourage use of devices comparable to
                // internal storage performance.)

                // However, since then, internal storage has started moving from
                // eMMC to UFS, which can significantly outperform adopted
                // devices, causing the speed warning to always trigger. To
                // mitigate this, we've switched to using a static threshold.

                // The static threshold was derived by running the benchmark on
                // a wide selection of SD cards from several vendors; here are
                // some 50th percentile results from 20+ runs of each card:

                // 8GB C4 40MB/s+: 3282ms
                // 16GB C10 40MB/s+: 1881ms
                // 32GB C10 40MB/s+: 2897ms
                // 32GB U3 80MB/s+: 1595ms
                // 32GB C10 80MB/s+: 1680ms
                // 128GB U1 80MB/s+: 1532ms

                // Thus a 2000ms static threshold strikes a reasonable balance
                // to help us identify slower cards. Users can still proceed
                // with these slower cards; we're just showing a warning.

                // The above analysis was done using the "r1572:w1001:s285"
                // benchmark, and it should be redone any time the benchmark
                // changes.

                Log.d(TAG, "New volume took " + mPrivateBench + "ms to run benchmark");
                if (mPrivateBench > 2000
                        || SystemProperties.getBoolean(PROP_DEBUG_STORAGE_SLOW, false)) {
                    mActivity.onFormatFinishedSlow();
                } else {
                    mActivity.onFormatFinished();
                }
            } else {
                mActivity.onFormatFinished();
            }
        }
    }

    public void onFormatFinished() {
        final Intent intent = new Intent(this, StorageWizardFormatSlow.class);
        intent.putExtra(EXTRA_FORMAT_SLOW, false);
        startActivity(intent);
        finishAffinity();
    }

    public void onFormatFinishedSlow() {
        final Intent intent = new Intent(this, StorageWizardFormatSlow.class);
        intent.putExtra(EXTRA_FORMAT_SLOW, true);
        startActivity(intent);
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
