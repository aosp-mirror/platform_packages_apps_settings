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
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.deviceinfo.StorageSettings.UnmountTask;
import com.android.settings.search.actionbar.SearchMenuController;

public class PrivateVolumeUnmount extends InstrumentedFragment {
    private VolumeInfo mVolume;
    private DiskInfo mDisk;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVICEINFO_STORAGE;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setHasOptionsMenu(true);
        SearchMenuController.init(this /* host */);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final StorageManager storage = getActivity().getSystemService(StorageManager.class);
        final String volumeId = getArguments().getString(VolumeInfo.EXTRA_VOLUME_ID);
        mVolume = storage.findVolumeById(volumeId);
        mDisk = storage.findDiskById(mVolume.getDiskId());

        final View view = inflater.inflate(R.layout.storage_internal_unmount, container, false);
        final TextView body = (TextView) view.findViewById(R.id.body);
        final Button confirm = (Button) view.findViewById(R.id.confirm);

        body.setText(TextUtils.expandTemplate(getText(R.string.storage_internal_unmount_details),
                mDisk.getDescription()));
        confirm.setOnClickListener(mConfirmListener);

        return view;
    }

    private final OnClickListener mConfirmListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            new UnmountTask(getActivity(), mVolume).execute();
            getActivity().finish();
        }
    };
}
