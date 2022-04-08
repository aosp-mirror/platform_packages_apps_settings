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

import static android.content.pm.PackageManager.EXTRA_MOVE_ID;

import static com.android.settings.deviceinfo.StorageSettings.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.MoveCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.DiskInfo;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.settings.R;

public class StorageWizardMigrateProgress extends StorageWizardBase {
    private static final String ACTION_FINISH_WIZARD = "com.android.systemui.action.FINISH_WIZARD";

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

        setIcon(R.drawable.ic_swap_horiz);
        setHeaderText(R.string.storage_wizard_migrate_progress_v2_title);
        setAuxChecklist();
        setBackButtonVisibility(View.INVISIBLE);
        setNextButtonVisibility(View.INVISIBLE);
        // Register for updates and push through current status
        getPackageManager().registerMoveCallback(mCallback, new Handler());
        mCallback.onStatusChanged(mMoveId, getPackageManager().getMoveStatus(mMoveId), -1);
    }

    private final MoveCallback mCallback = new MoveCallback() {
        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (mMoveId != moveId) return;

            final Context context = StorageWizardMigrateProgress.this;
            if (PackageManager.isMoveStatusFinished(status)) {
                Log.d(TAG, "Finished with status " + status);
                if (status == PackageManager.MOVE_SUCCEEDED) {
                    if (mDisk != null) {
                        // Kinda lame, but tear down that shiny finished
                        // notification, since user is still in wizard flow
                        final Intent finishIntent = new Intent(ACTION_FINISH_WIZARD);
                        finishIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                        sendBroadcast(finishIntent);

                        if (!StorageWizardMigrateProgress.this.isFinishing()) {
                            final Intent intent = new Intent(context, StorageWizardReady.class);
                            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                            startActivity(intent);
                        }
                    }
                } else {
                    Toast.makeText(context, getString(R.string.insufficient_storage),
                            Toast.LENGTH_LONG).show();
                }
                finishAffinity();

            } else {
                setCurrentProgress(status);
            }
        }
    };
}
