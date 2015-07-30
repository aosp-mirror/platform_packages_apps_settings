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

import android.annotation.LayoutRes;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;
import com.android.setupwizardlib.SetupWizardLayout;

import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

public abstract class StorageWizardBase extends Activity {
    protected StorageManager mStorage;

    protected VolumeInfo mVolume;
    protected DiskInfo mDisk;

    private View mCustomNav;
    private Button mCustomNext;

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
        } else if (mVolume != null) {
            mDisk = mVolume.getDisk();
        }

        setTheme(R.style.SetupWizardStorageStyle);

        if (mDisk != null) {
            mStorage.registerListener(mStorageListener);
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        // Our wizard is a unique flower, so it has custom buttons
        final ViewGroup navParent = (ViewGroup) findViewById(R.id.suw_layout_navigation_bar)
                .getParent();
        mCustomNav = getLayoutInflater().inflate(R.layout.storage_wizard_navigation,
                navParent, false);

        mCustomNext = (Button) mCustomNav.findViewById(R.id.suw_navbar_next);
        mCustomNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavigateNext();
            }
        });

        // Swap our custom navigation bar into place
        for (int i = 0; i < navParent.getChildCount(); i++) {
            if (navParent.getChildAt(i).getId() == R.id.suw_layout_navigation_bar) {
                navParent.removeViewAt(i);
                navParent.addView(mCustomNav, i);
                break;
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        window.setStatusBarColor(Color.TRANSPARENT);

        mCustomNav.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        final View scrollView = findViewById(R.id.suw_bottom_scroll_view);
        scrollView.setVerticalFadingEdgeEnabled(true);
        scrollView.setFadingEdgeLength(scrollView.getVerticalFadingEdgeLength() * 2);

        // Our header assets already have padding baked in
        final View title = findViewById(R.id.suw_layout_title);
        title.setPadding(title.getPaddingLeft(), 0, title.getPaddingRight(),
                title.getPaddingBottom());
    }

    @Override
    protected void onDestroy() {
        mStorage.unregisterListener(mStorageListener);
        super.onDestroy();
    }

    protected Button getNextButton() {
        return mCustomNext;
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
        final CharSequence headerText = TextUtils.expandTemplate(getText(resId), args);
        getSetupWizardLayout().setHeaderText(headerText);
        setTitle(headerText);
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

    protected void setIllustrationInternal(boolean internal) {
        if (internal) {
            getSetupWizardLayout().setIllustration(R.drawable.bg_internal_storage_header,
                    R.drawable.bg_header_horizontal_tile);
        } else {
            getSetupWizardLayout().setIllustration(R.drawable.bg_portable_storage_header,
                    R.drawable.bg_header_horizontal_tile);
        }
    }

    protected void setKeepScreenOn(boolean keepScreenOn) {
        getSetupWizardLayout().setKeepScreenOn(keepScreenOn);
    }

    public void onNavigateNext() {
        throw new UnsupportedOperationException();
    }

    protected VolumeInfo findFirstVolume(int type) {
        final List<VolumeInfo> vols = mStorage.getVolumes();
        for (VolumeInfo vol : vols) {
            if (Objects.equals(mDisk.getId(), vol.getDiskId()) && (vol.getType() == type)) {
                return vol;
            }
        }
        return null;
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            // We know mDisk != null.
            if (mDisk.id.equals(disk.id)) {
                finish();
            }
        }
    };
}
