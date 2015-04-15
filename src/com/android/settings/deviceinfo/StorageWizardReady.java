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

import android.os.Bundle;
import android.os.storage.VolumeInfo;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

import java.util.List;
import java.util.Objects;

public class StorageWizardReady extends StorageWizardBase {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_wizard_generic);

        Preconditions.checkNotNull(mDisk);

        setHeaderText(R.string.storage_wizard_ready_title, mDisk.getDescription());

        // TODO: handle mixed partition cases instead of just guessing based on
        // first volume type we encounter
        final List<VolumeInfo> vols = mStorage.getVolumes();
        for (VolumeInfo vol : vols) {
            if (!Objects.equals(mDisk.getId(), vol.getDiskId())) continue;

            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                setBodyText(R.string.storage_wizard_ready_external_body,
                        mDisk.getDescription());
                break;
            } else if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                setBodyText(R.string.storage_wizard_ready_internal_body,
                        mDisk.getDescription());
                break;
            }
        }

        getNextButton().setText(R.string.done);
    }

    @Override
    public void onNavigateNext() {
        finishAffinity();
    }
}
