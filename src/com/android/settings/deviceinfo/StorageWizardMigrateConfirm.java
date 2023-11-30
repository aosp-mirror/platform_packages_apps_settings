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

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;

import java.util.Objects;

public class StorageWizardMigrateConfirm extends StorageWizardBase {
    private static final String TAG = "StorageWizardMigrateConfirm";

    private static final int REQUEST_CREDENTIAL = 100;

    private MigrateEstimateTask mEstimate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_wizard_generic);

        // When called with just disk, find the first private volume
        if (mVolume == null) {
            mVolume = findFirstVolume(VolumeInfo.TYPE_PRIVATE);
        }

        final VolumeInfo sourceVol = getPackageManager().getPrimaryStorageCurrentVolume();
        if (sourceVol == null || mVolume == null) {
            Log.d(TAG, "Missing either source or target volume");
            finish();
            return;
        }

        setIcon(R.drawable.ic_swap_horiz);
        setHeaderText(R.string.storage_wizard_migrate_v2_title, getDiskShortDescription());
        setBodyText(R.string.memory_calculating_size);
        setAuxChecklist();

        mEstimate = new MigrateEstimateTask(this) {
            @Override
            public void onPostExecute(String size, String time) {
                setBodyText(R.string.storage_wizard_migrate_v2_body,
                        getDiskDescription(), size, time);
            }
        };

        mEstimate.copyFrom(getIntent());
        mEstimate.execute();

        setBackButtonText(R.string.storage_wizard_migrate_v2_later);
        setNextButtonText(R.string.storage_wizard_migrate_v2_now);
    }

    @Override
    public void onNavigateBack(View view) {
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(this,
                SettingsEnums.ACTION_STORAGE_MIGRATE_LATER);

        if (mDisk != null) {
            final Intent intent = new Intent(this, StorageWizardReady.class);
            intent.putExtra(EXTRA_MIGRATE_SKIP, true);
            startActivity(intent);
        } else {
            finishAffinity();
        }
    }

    @Override
    public void onNavigateNext(View view) {
        // Ensure that all users are unlocked so that we can move their data
        final LockPatternUtils lpu = new LockPatternUtils(this);
        if (StorageManager.isFileEncrypted()) {
            for (UserInfo user : getSystemService(UserManager.class).getUsers()) {
                if (StorageManager.isCeStorageUnlocked(user.id)) {
                    continue;
                }
                if (!lpu.isSecure(user.id)) {
                    Log.d(TAG, "Unsecured user " + user.id + " is currently locked; attempting "
                            + "automatic unlock");
                    lpu.unlockUserKeyIfUnsecured(user.id);
                } else {
                    Log.d(TAG, "Secured user " + user.id + " is currently locked; requesting "
                            + "manual unlock");
                    final CharSequence description = TextUtils.expandTemplate(
                            getText(R.string.storage_wizard_move_unlock), user.name);
                    final ChooseLockSettingsHelper.Builder builder =
                            new ChooseLockSettingsHelper.Builder(this);
                    builder.setRequestCode(REQUEST_CREDENTIAL)
                            .setDescription(description)
                            .setUserId(user.id)
                            .setAllowAnyUserId(true)
                            .setForceVerifyPath(true)
                            .show();
                    return;
                }
            }
        }

        // We only expect exceptions from StorageManagerService#setPrimaryStorageUuid
        int moveId;
        try {
            moveId = getPackageManager().movePrimaryStorage(mVolume);
        } catch (IllegalArgumentException e) {
            StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);

            if (Objects.equals(mVolume.getFsUuid(), sm.getPrimaryStorageVolume().getUuid())) {
                final Intent intent = new Intent(this, StorageWizardReady.class);
                intent.putExtra(DiskInfo.EXTRA_DISK_ID,
                        getIntent().getStringExtra(DiskInfo.EXTRA_DISK_ID));
                startActivity(intent);
                finishAffinity();

                return;
            } else {
                throw e;
            }
        } catch (IllegalStateException e) {
            Toast.makeText(this, getString(R.string.another_migration_already_in_progress),
                    Toast.LENGTH_LONG).show();
            finishAffinity();

            return;
        }

        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(this,
                SettingsEnums.ACTION_STORAGE_MIGRATE_NOW);

        final Intent intent = new Intent(this, StorageWizardMigrateProgress.class);
        intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mVolume.getId());
        intent.putExtra(PackageManager.EXTRA_MOVE_ID, moveId);
        startActivity(intent);
        finishAffinity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CREDENTIAL) {
            if (resultCode == RESULT_OK) {
                // Credentials confirmed, so storage should be unlocked; let's
                // go look for the next locked user.
                onNavigateNext(null);
            } else {
                // User wasn't able to confirm credentials, so we're okay
                // landing back at the wizard page again, where they read
                // instructions again and tap "Next" to try again.
                Log.w(TAG, "Failed to confirm credentials");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
