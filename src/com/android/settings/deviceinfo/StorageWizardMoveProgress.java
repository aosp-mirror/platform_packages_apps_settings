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
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

import java.util.concurrent.CountDownLatch;

public class StorageWizardMoveProgress extends StorageWizardBase {
    private String mPackageName;
    private ApplicationInfo mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_wizard_progress);

        try {
            mPackageName = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
            mApp = getPackageManager().getApplicationInfo(mPackageName, 0);
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        Preconditions.checkNotNull(mVolume);
        Preconditions.checkNotNull(mApp);

        final String appName = getPackageManager().getApplicationLabel(mApp).toString();
        final String volumeName = mStorage.getBestVolumeDescription(mVolume);

        setHeaderText(R.string.storage_wizard_move_progress_title, appName);
        setBodyText(R.string.storage_wizard_move_progress_body, volumeName, appName);

        setCurrentProgress(20);

        getNextButton().setVisibility(View.GONE);

        new MoveTask().execute();
    }

    private CharSequence moveStatusToMessage(int returnCode) {
        switch (returnCode) {
            case PackageManager.MOVE_FAILED_INSUFFICIENT_STORAGE:
                return getString(R.string.insufficient_storage);
            case PackageManager.MOVE_FAILED_DOESNT_EXIST:
                return getString(R.string.does_not_exist);
            case PackageManager.MOVE_FAILED_FORWARD_LOCKED:
                return getString(R.string.app_forward_locked);
            case PackageManager.MOVE_FAILED_INVALID_LOCATION:
                return getString(R.string.invalid_location);
            case PackageManager.MOVE_FAILED_SYSTEM_PACKAGE:
                return getString(R.string.system_package);
            case PackageManager.MOVE_FAILED_INTERNAL_ERROR:
            default:
                return getString(R.string.insufficient_storage);
        }
    }

    private class LocalPackageMoveObserver extends IPackageMoveObserver.Stub {
        public int returnCode;
        public CountDownLatch finished = new CountDownLatch(1);

        @Override
        public void packageMoved(String packageName, int returnCode) throws RemoteException {
            this.returnCode = returnCode;
            this.finished.countDown();
        }
    }

    public class MoveTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                final LocalPackageMoveObserver observer = new LocalPackageMoveObserver();

                if (mApp.isExternalAsec()) {
                    getPackageManager().movePackage(mPackageName, observer,
                            PackageManager.MOVE_INTERNAL);
                } else if (mVolume.getType() == VolumeInfo.TYPE_PUBLIC) {
                    getPackageManager().movePackage(mPackageName, observer,
                            PackageManager.MOVE_EXTERNAL_MEDIA);
                } else {
                    getPackageManager().movePackageAndData(mPackageName, mVolume.fsUuid, observer);
                }

                observer.finished.await();
                return observer.returnCode;
            } catch (Exception e) {
                Log.e(TAG, "Failed to move", e);
                return PackageManager.MOVE_FAILED_INTERNAL_ERROR;
            }
        }

        @Override
        protected void onPostExecute(Integer returnCode) {
            final Context context = StorageWizardMoveProgress.this;
            if (returnCode == PackageManager.MOVE_SUCCEEDED) {
                finishAffinity();

            } else {
                Log.w(TAG, "Move failed with status " + returnCode);
                Toast.makeText(context, moveStatusToMessage(returnCode), Toast.LENGTH_LONG).show();
                finishAffinity();
            }
        }
    }
}
