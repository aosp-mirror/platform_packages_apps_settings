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

import android.content.Intent;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;

import com.android.settings.R;

public class StorageWizardMigrate extends StorageWizardBase {
    private MigrateEstimateTask mEstimate;

    private RadioButton mRadioNow;
    private RadioButton mRadioLater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_migrate);

        setIllustrationInternal(true);
        setHeaderText(R.string.storage_wizard_migrate_title, mDisk.getDescription());
        setBodyText(R.string.memory_calculating_size);

        mRadioNow = (RadioButton) findViewById(R.id.storage_wizard_migrate_now);
        mRadioLater = (RadioButton) findViewById(R.id.storage_wizard_migrate_later);

        mRadioNow.setOnCheckedChangeListener(mRadioListener);
        mRadioLater.setOnCheckedChangeListener(mRadioListener);

        getNextButton().setEnabled(false);

        mEstimate = new MigrateEstimateTask(this) {
            @Override
            public void onPostExecute(String size, String time) {
                setBodyText(R.string.storage_wizard_migrate_body,
                        mDisk.getDescription(), time, size);
            }
        };

        mEstimate.copyFrom(getIntent());
        mEstimate.execute();
    }

    private final OnCheckedChangeListener mRadioListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (buttonView == mRadioNow) {
                    mRadioLater.setChecked(false);
                } else if (buttonView == mRadioLater) {
                    mRadioNow.setChecked(false);
                }
                getNextButton().setEnabled(true);
            }
        }
    };

    @Override
    public void onNavigateNext() {
        if (mRadioNow.isChecked()) {
            final Intent intent = new Intent(this, StorageWizardMigrateConfirm.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            mEstimate.copyTo(intent);
            startActivity(intent);
        } else if (mRadioLater.isChecked()) {
            final Intent intent = new Intent(this, StorageWizardReady.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            startActivity(intent);
        }
    }
}
