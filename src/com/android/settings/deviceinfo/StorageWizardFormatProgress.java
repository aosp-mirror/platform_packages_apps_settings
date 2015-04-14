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
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

public class StorageWizardFormatProgress extends StorageWizardBase {
    public static final String EXTRA_FORMAT_PUBLIC = "format_private";

    private boolean mFormatPublic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_wizard_progress);

        Preconditions.checkNotNull(mDisk);

        mFormatPublic = getIntent().getBooleanExtra(EXTRA_FORMAT_PUBLIC, false);

        setHeaderText(R.string.storage_wizard_format_progress_title, mDisk.getDescription());
        setBodyText(R.string.storage_wizard_format_progress_body, mDisk.getDescription());

        setCurrentProgress(20);

        getNextButton().setVisibility(View.GONE);

        new PartitionTask().execute();
    }

    public class PartitionTask extends AsyncTask<Void, Void, Exception> {
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                if (mFormatPublic) {
                    mStorage.partitionPublic(mDisk.id);
                } else {
                    mStorage.partitionPrivate(mDisk.id);
                }
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            final Context context = StorageWizardFormatProgress.this;
            if (e == null) {
                if (!mFormatPublic) {
                    final Intent intent = new Intent(context, StorageWizardMigrate.class);
                    intent.putExtra(EXTRA_DISK_ID, mDisk.id);
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
