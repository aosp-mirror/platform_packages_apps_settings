/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.storage.VolumeRecord;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.deviceinfo.PrivateVolumeForget;

/** Storage utilities */
public class StorageUtils {

    /** Launches the fragment to forget a specified missing volume record. */
    public static void launchForgetMissingVolumeRecordFragment(Context context,
            StorageEntry storageEntry) {
        if (storageEntry == null || !storageEntry.isVolumeRecordMissed()) {
            return;
        }

        final Bundle args = new Bundle();
        args.putString(VolumeRecord.EXTRA_FS_UUID, storageEntry.getFsUuid());
        new SubSettingLauncher(context)
                .setDestination(PrivateVolumeForget.class.getCanonicalName())
                .setTitleRes(R.string.storage_menu_forget)
                .setSourceMetricsCategory(SettingsEnums.SETTINGS_STORAGE_CATEGORY)
                .setArguments(args)
                .launch();
    }
}
