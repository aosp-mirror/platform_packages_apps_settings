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
import android.text.format.DateUtils;
import android.text.format.Formatter;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

public class StorageWizardMigrateConfirm extends StorageWizardBase {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_wizard_generic);

        Preconditions.checkNotNull(mDisk);

        final String time = DateUtils.formatDuration(0).toString();
        final String size = Formatter.formatFileSize(this, 0);

        setHeaderText(R.string.storage_wizard_migrate_confirm_title, mDisk.getDescription());
        setBodyText(R.string.storage_wizard_migrate_confirm_body, time, size);
        setSecondaryBodyText(R.string.storage_wizard_migrate_details, mDisk.getDescription());

        getNextButton().setText(R.string.storage_wizard_migrate_confirm_next);
    }

    @Override
    public void onNavigateNext() {
        final Intent intent = new Intent(this, StorageWizardMigrateProgress.class);
        intent.putExtra(EXTRA_DISK_ID, mDisk.id);
        startActivity(intent);
        finishAffinity();
    }
}
