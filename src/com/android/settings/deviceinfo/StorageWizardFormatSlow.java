/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.view.View;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

public class StorageWizardFormatSlow extends StorageWizardBase {
    private boolean mFormatPrivate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_generic);

        mFormatPrivate = getIntent().getBooleanExtra(EXTRA_FORMAT_PRIVATE, false);

        setHeaderText(R.string.storage_wizard_slow_v2_title, getDiskShortDescription());
        setBodyText(R.string.storage_wizard_slow_v2_body, getDiskDescription(),
                getDiskShortDescription(), getDiskShortDescription(),
                getDiskShortDescription());

        setBackButtonText(R.string.storage_wizard_slow_v2_start_over);
        setNextButtonText(R.string.storage_wizard_slow_v2_continue);

        // If benchmark wasn't actually slow, skip this warning
        if (!getIntent().getBooleanExtra(EXTRA_FORMAT_SLOW, false)) {
            onNavigateNext(null);
        }
    }

    @Override
    public void onNavigateBack(View view) {
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(this,
                SettingsEnums.ACTION_STORAGE_BENCHMARK_SLOW_ABORT);

        final Intent intent = new Intent(this, StorageWizardInit.class);
        startActivity(intent);
        finishAffinity();
    }

    @Override
    public void onNavigateNext(View view) {
        if (view != null) {
            // User made an explicit choice to continue when slow
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(this,
                    SettingsEnums.ACTION_STORAGE_BENCHMARK_SLOW_CONTINUE);
        } else {
            // User made an implicit choice to continue when fast
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(this,
                    SettingsEnums.ACTION_STORAGE_BENCHMARK_FAST_CONTINUE);
        }

        final String forgetUuid = getIntent().getStringExtra(EXTRA_FORMAT_FORGET_UUID);
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
            final Intent intent = new Intent(this, StorageWizardMigrateConfirm.class);
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
