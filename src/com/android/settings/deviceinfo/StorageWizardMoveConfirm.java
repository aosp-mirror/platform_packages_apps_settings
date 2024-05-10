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
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.android.internal.util.Preconditions;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.password.ChooseLockSettingsHelper;

public class StorageWizardMoveConfirm extends StorageWizardBase {
    private static final String TAG = "StorageWizardMoveConfirm";

    private static final int REQUEST_CREDENTIAL = 100;

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

        // Check that target volume is candidate
        Preconditions.checkState(
                getPackageManager().getPackageCandidateVolumes(mApp).contains(mVolume));

        final String appName = getPackageManager().getApplicationLabel(mApp).toString();
        final String volumeName = mStorage.getBestVolumeDescription(mVolume);

        setIcon(R.drawable.ic_swap_horiz);
        setHeaderText(R.string.storage_wizard_move_confirm_title, appName);
        setBodyText(R.string.storage_wizard_move_confirm_body, appName, volumeName);

        setNextButtonText(R.string.move_app);
        setBackButtonVisibility(View.INVISIBLE);
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
                            .setForceVerifyPath(true)
                            .setAllowAnyUserId(true)
                            .show();
                    return;
                }
            }
        }

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
