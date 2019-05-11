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

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

public class StorageWizardInit extends StorageWizardBase {
    private Button mInternal;

    private boolean mIsPermittedToAdopt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_init);

        mIsPermittedToAdopt = UserManager.get(this).isAdminUser()
                && !ActivityManager.isUserAMonkey();

        setHeaderText(R.string.storage_wizard_init_v2_title, getDiskShortDescription());

        mInternal = requireViewById(R.id.storage_wizard_init_internal);

        setBackButtonText(R.string.storage_wizard_init_v2_later);
        setNextButtonVisibility(View.INVISIBLE);
        if (!mDisk.isAdoptable()) {
            // If not adoptable, we only have one choice
            mInternal.setEnabled(false);
            onNavigateExternal(null);
        } else if (!mIsPermittedToAdopt) {
            // TODO: Show a message about why this is disabled for guest and
            // that only an admin user can adopt an sd card.
            mInternal.setEnabled(false);
        }
    }

    @Override
    public void onNavigateBack(View view) {
        finish();
    }

    public void onNavigateExternal(View view) {
        if (view != null) {
            // User made an explicit choice for external
            FeatureFactory.getFactory(this).getMetricsFeatureProvider().action(this,
                    SettingsEnums.ACTION_STORAGE_INIT_EXTERNAL);
        }

        if (mVolume != null && mVolume.getType() == VolumeInfo.TYPE_PUBLIC
                && mVolume.getState() != VolumeInfo.STATE_UNMOUNTABLE) {
            // Remember that user made decision
            mStorage.setVolumeInited(mVolume.getFsUuid(), true);

            final Intent intent = new Intent(this, StorageWizardReady.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            startActivity(intent);
            finish();

        } else {
            // Gotta format to get there
            StorageWizardFormatConfirm.showPublic(this, mDisk.getId());
        }
    }

    public void onNavigateInternal(View view) {
        if (view != null) {
            // User made an explicit choice for internal
            FeatureFactory.getFactory(this).getMetricsFeatureProvider().action(this,
                    SettingsEnums.ACTION_STORAGE_INIT_INTERNAL);
        }

        StorageWizardFormatConfirm.showPrivate(this, mDisk.getId());
    }
}
