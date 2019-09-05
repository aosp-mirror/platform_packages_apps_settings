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

import static android.content.Intent.EXTRA_TITLE;
import static android.content.pm.PackageManager.EXTRA_MOVE_ID;

import static com.android.settings.deviceinfo.StorageSettings.TAG;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.MoveCallback;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.settings.R;

public class StorageWizardMoveProgress extends StorageWizardBase {
    private int mMoveId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mVolume == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_progress);

        mMoveId = getIntent().getIntExtra(EXTRA_MOVE_ID, -1);
        final String appName = getIntent().getStringExtra(EXTRA_TITLE);
        final String volumeName = mStorage.getBestVolumeDescription(mVolume);

        setIcon(R.drawable.ic_swap_horiz);
        setHeaderText(R.string.storage_wizard_move_progress_title, appName);
        setBodyText(R.string.storage_wizard_move_progress_body, volumeName, appName);
        setBackButtonVisibility(View.INVISIBLE);
        setNextButtonVisibility(View.INVISIBLE);
        // Register for updates and push through current status
        getPackageManager().registerMoveCallback(mCallback, new Handler());
        mCallback.onStatusChanged(mMoveId, getPackageManager().getMoveStatus(mMoveId), -1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getPackageManager().unregisterMoveCallback(mCallback);
    }

    private final MoveCallback mCallback = new MoveCallback() {
        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (mMoveId != moveId) return;

            if (PackageManager.isMoveStatusFinished(status)) {
                Log.d(TAG, "Finished with status " + status);
                if (status != PackageManager.MOVE_SUCCEEDED) {
                    Toast.makeText(StorageWizardMoveProgress.this, moveStatusToMessage(status),
                            Toast.LENGTH_LONG).show();
                }
                finishAffinity();

            } else {
                setCurrentProgress(status);
            }
        }
    };

    private CharSequence moveStatusToMessage(int returnCode) {
        switch (returnCode) {
            case PackageManager.MOVE_FAILED_INSUFFICIENT_STORAGE:
                return getString(R.string.insufficient_storage);
            case PackageManager.MOVE_FAILED_DEVICE_ADMIN:
                return getString(R.string.move_error_device_admin);
            case PackageManager.MOVE_FAILED_DOESNT_EXIST:
                return getString(R.string.does_not_exist);
            case PackageManager.MOVE_FAILED_INVALID_LOCATION:
                return getString(R.string.invalid_location);
            case PackageManager.MOVE_FAILED_SYSTEM_PACKAGE:
                return getString(R.string.system_package);
            case PackageManager.MOVE_FAILED_INTERNAL_ERROR:
            default:
                return getString(R.string.insufficient_storage);
        }
    }
}
