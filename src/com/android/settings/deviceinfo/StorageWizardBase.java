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

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;
import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.view.NavigationBar;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

import java.text.NumberFormat;

public abstract class StorageWizardBase extends Activity implements NavigationBarListener {
    protected StorageManager mStorage;

    protected VolumeInfo mVolume;
    protected DiskInfo mDisk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStorage = getSystemService(StorageManager.class);

        final String volumeId = getIntent().getStringExtra(VolumeInfo.EXTRA_VOLUME_ID);
        if (!TextUtils.isEmpty(volumeId)) {
            mVolume = mStorage.findVolumeById(volumeId);
        }

        final String diskId = getIntent().getStringExtra(DiskInfo.EXTRA_DISK_ID);
        if (!TextUtils.isEmpty(diskId)) {
            mDisk = mStorage.findDiskById(diskId);
        } else {
            mDisk = mStorage.findDiskById(mVolume.diskId);
        }

        setTheme(R.style.SuwThemeMaterial_Light);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);

        getNavigationBar().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        getWindow().setStatusBarColor(Color.TRANSPARENT);

        getNavigationBar().setNavigationBarListener(this);
        getBackButton().setVisibility(View.GONE);
    }

    protected NavigationBar getNavigationBar() {
        return (NavigationBar) findViewById(R.id.suw_layout_navigation_bar);
    }

    protected Button getBackButton() {
        return getNavigationBar().getBackButton();
    }

    protected Button getNextButton() {
        return getNavigationBar().getNextButton();
    }

    protected SetupWizardLayout getSetupWizardLayout() {
        return (SetupWizardLayout) findViewById(R.id.setup_wizard_layout);
    }

    protected ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.storage_wizard_progress);
    }

    protected void setCurrentProgress(int progress) {
        getProgressBar().setProgress(progress);
        ((TextView) findViewById(R.id.storage_wizard_progress_summary)).setText(
                NumberFormat.getPercentInstance().format((double) progress / 100));
    }

    protected void setHeaderText(int resId, String... args) {
        getSetupWizardLayout().setHeaderText(TextUtils.expandTemplate(getText(resId), args));
    }

    protected void setBodyText(int resId, String... args) {
        ((TextView) findViewById(R.id.storage_wizard_body)).setText(
                TextUtils.expandTemplate(getText(resId), args));
    }

    protected void setSecondaryBodyText(int resId, String... args) {
        final TextView secondBody = ((TextView) findViewById(R.id.storage_wizard_second_body));
        secondBody.setText(TextUtils.expandTemplate(getText(resId), args));
        secondBody.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNavigateBack() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onNavigateNext() {
        throw new UnsupportedOperationException();
    }
}
