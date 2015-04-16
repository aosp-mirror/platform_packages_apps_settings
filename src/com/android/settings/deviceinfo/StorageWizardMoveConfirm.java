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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.storage.VolumeInfo;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

public class StorageWizardMoveConfirm extends StorageWizardBase {
    private String mPackageName;
    private ApplicationInfo mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_wizard_generic);

        try {
            mPackageName = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
            mApp = getPackageManager().getApplicationInfo(mPackageName, 0);
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        Preconditions.checkNotNull(mVolume);
        Preconditions.checkNotNull(mApp);

        // Sanity check that target volume is candidate
        Preconditions.checkState(
                getPackageManager().getApplicationCandidateVolumes(mApp).contains(mVolume));

        final String appName = getPackageManager().getApplicationLabel(mApp).toString();
        final String volumeName = mStorage.getBestVolumeDescription(mVolume);

        setHeaderText(R.string.storage_wizard_move_confirm_title, appName);
        setBodyText(R.string.storage_wizard_move_confirm_body, appName, volumeName);

        getNextButton().setText(R.string.move_app);
    }

    @Override
    public void onNavigateNext() {
        final Intent intent = new Intent(this, StorageWizardMoveProgress.class);
        intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mVolume.getId());
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mPackageName);
        startActivity(intent);
        finishAffinity();
    }
}
