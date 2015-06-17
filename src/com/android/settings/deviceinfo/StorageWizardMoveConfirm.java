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

import static android.content.Intent.EXTRA_PACKAGE_NAME;
import static android.content.Intent.EXTRA_TITLE;
import static android.content.pm.PackageManager.EXTRA_MOVE_ID;
import static android.os.storage.VolumeInfo.EXTRA_VOLUME_ID;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

public class StorageWizardMoveConfirm extends StorageWizardBase {
    private String mPackageName;
    private ApplicationInfo mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mVolume == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_generic);

        try {
            mPackageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
            mApp = getPackageManager().getApplicationInfo(mPackageName, 0);
        } catch (NameNotFoundException e) {
            finish();
            return;
        }

        // Sanity check that target volume is candidate
        Preconditions.checkState(
                getPackageManager().getPackageCandidateVolumes(mApp).contains(mVolume));

        final String appName = getPackageManager().getApplicationLabel(mApp).toString();
        final String volumeName = mStorage.getBestVolumeDescription(mVolume);

        setIllustrationInternal(true);
        setHeaderText(R.string.storage_wizard_move_confirm_title, appName);
        setBodyText(R.string.storage_wizard_move_confirm_body, appName, volumeName);

        getNextButton().setText(R.string.move_app);
    }

    @Override
    public void onNavigateNext() {
        // Kick off move before we transition
        final String appName = getPackageManager().getApplicationLabel(mApp).toString();
        final int moveId = getPackageManager().movePackage(mPackageName, mVolume);

        final Intent intent = new Intent(this, StorageWizardMoveProgress.class);
        intent.putExtra(EXTRA_MOVE_ID, moveId);
        intent.putExtra(EXTRA_TITLE, appName);
        intent.putExtra(EXTRA_VOLUME_ID, mVolume.getId());
        startActivity(intent);
        finishAffinity();
    }
}
