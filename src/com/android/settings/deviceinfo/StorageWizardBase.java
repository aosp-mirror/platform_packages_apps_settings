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

import static android.os.storage.DiskInfo.EXTRA_DISK_ID;
import static android.os.storage.VolumeInfo.EXTRA_VOLUME_ID;

import android.annotation.LayoutRes;
import android.annotation.NonNull;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.template.HeaderMixin;
import com.google.android.setupdesign.util.ThemeHelper;
import com.google.android.setupdesign.util.ThemeResolver;

import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

public abstract class StorageWizardBase extends FragmentActivity {

    private static final String TAG = "StorageWizardBase";

    protected static final String EXTRA_FORMAT_FORGET_UUID = "format_forget_uuid";
    protected static final String EXTRA_FORMAT_PRIVATE = "format_private";
    protected static final String EXTRA_FORMAT_SLOW = "format_slow";
    protected static final String EXTRA_MIGRATE_SKIP = "migrate_skip";

    protected StorageManager mStorage;

    protected VolumeInfo mVolume;
    protected DiskInfo mDisk;

    private FooterBarMixin mFooterBarMixin;
    private FooterButton mBack;
    private FooterButton mNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean isDayNightThemeSupportedBySuW = ThemeHelper.isSetupWizardDayNightEnabled(this);
        int sudTheme =
            new ThemeResolver.Builder(ThemeResolver.getDefault())
                .setDefaultTheme(ThemeHelper.getSuwDefaultTheme(this))
                .setUseDayNight(true)
                .build()
                .resolve("", !isDayNightThemeSupportedBySuW);

        this.setTheme(sudTheme);
        ThemeHelper.trySetDynamicColor(this);
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));

        mStorage = getSystemService(StorageManager.class);

        final String volumeId = getIntent().getStringExtra(EXTRA_VOLUME_ID);
        if (!TextUtils.isEmpty(volumeId)) {
            mVolume = mStorage.findVolumeById(volumeId);
        }

        final String diskId = getIntent().getStringExtra(EXTRA_DISK_ID);
        if (!TextUtils.isEmpty(diskId)) {
            mDisk = mStorage.findDiskById(diskId);
        } else if (mVolume != null) {
            mDisk = mVolume.getDisk();
        }

        if (mDisk != null) {
            mStorage.registerListener(mStorageListener);
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        mFooterBarMixin = getGlifLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
            new FooterButton.Builder(this)
                .setText(R.string.wizard_back)
                .setListener(this::onNavigateBack)
                .setButtonType(FooterButton.ButtonType.OTHER)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                .build()
        );
        mFooterBarMixin.setPrimaryButton(
            new FooterButton.Builder(this)
                .setText(R.string.wizard_next)
                .setListener(this::onNavigateNext)
                .setButtonType(FooterButton.ButtonType.NEXT)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                .build()
        );
        mBack = mFooterBarMixin.getSecondaryButton();
        mNext = mFooterBarMixin.getPrimaryButton();

        setIcon(com.android.internal.R.drawable.ic_sd_card_48dp);
    }

    @Override
    protected void onDestroy() {
        mStorage.unregisterListener(mStorageListener);
        super.onDestroy();
    }

    @Override
    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        theme.applyStyle(R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, resid, first);
    }

    protected FooterButton getBackButton() {
        return mBack;
    }

    protected FooterButton getNextButton() {
        return mNext;
    }

    protected GlifLayout getGlifLayout() {
        return requireViewById(R.id.setup_wizard_layout);
    }

    protected ProgressBar getProgressBar() {
        return requireViewById(R.id.storage_wizard_progress);
    }

    protected void setCurrentProgress(int progress) {
        getProgressBar().setProgress(progress);
        ((TextView) requireViewById(R.id.storage_wizard_progress_summary)).setText(
            NumberFormat.getPercentInstance().format((double) progress / 100));
    }

    protected void setHeaderText(int resId, CharSequence... args) {
        final CharSequence headerText = TextUtils.expandTemplate(getText(resId), args);
        final GlifLayout layout = getGlifLayout();
        layout.setHeaderText(headerText);
        layout.getMixin(HeaderMixin.class).setAutoTextSizeEnabled(false);
        setTitle(headerText);
    }

    protected void setBodyText(int resId, CharSequence... args) {
        final TextView body = requireViewById(R.id.storage_wizard_body);
        body.setText(TextUtils.expandTemplate(getText(resId), args));
        body.setVisibility(View.VISIBLE);
    }

    protected void setAuxChecklist() {
        final FrameLayout aux = requireViewById(R.id.storage_wizard_aux);
        aux.addView(LayoutInflater.from(aux.getContext())
            .inflate(R.layout.storage_wizard_checklist, aux, false));
        aux.setVisibility(View.VISIBLE);

        // Customize string based on disk
        ((TextView) aux.requireViewById(R.id.storage_wizard_migrate_v2_checklist_media))
            .setText(TextUtils.expandTemplate(
                getText(R.string.storage_wizard_migrate_v2_checklist_media),
                getDiskShortDescription()));
    }

    protected void setBackButtonText(int resId, CharSequence... args) {
        mBack.setText(TextUtils.expandTemplate(getText(resId), args));
        mBack.setVisibility(View.VISIBLE);
    }

    protected void setNextButtonText(int resId, CharSequence... args) {
        mNext.setText(TextUtils.expandTemplate(getText(resId), args));
        mNext.setVisibility(View.VISIBLE);
    }

    protected void setBackButtonVisibility(int visible) {
        mBack.setVisibility(visible);
    }

    protected void setNextButtonVisibility(int visible) {
        mNext.setVisibility(visible);
    }

    protected void setIcon(int resId) {
        final GlifLayout layout = getGlifLayout();
        final Drawable icon = getDrawable(resId).mutate();
        layout.setIcon(icon);
    }

    protected void setKeepScreenOn(boolean keepScreenOn) {
        getGlifLayout().setKeepScreenOn(keepScreenOn);
    }

    public void onNavigateBack(View view) {
        throw new UnsupportedOperationException();
    }

    public void onNavigateNext(View view) {
        throw new UnsupportedOperationException();
    }

    private void copyStringExtra(Intent from, Intent to, String key) {
        if (from.hasExtra(key) && !to.hasExtra(key)) {
            to.putExtra(key, from.getStringExtra(key));
        }
    }

    private void copyBooleanExtra(Intent from, Intent to, String key) {
        if (from.hasExtra(key) && !to.hasExtra(key)) {
            to.putExtra(key, from.getBooleanExtra(key, false));
        }
    }

    @Override
    public void startActivity(Intent intent) {
        final Intent from = getIntent();
        final Intent to = intent;

        copyStringExtra(from, to, EXTRA_DISK_ID);
        copyStringExtra(from, to, EXTRA_VOLUME_ID);
        copyStringExtra(from, to, EXTRA_FORMAT_FORGET_UUID);
        copyBooleanExtra(from, to, EXTRA_FORMAT_PRIVATE);
        copyBooleanExtra(from, to, EXTRA_FORMAT_SLOW);
        copyBooleanExtra(from, to, EXTRA_MIGRATE_SKIP);

        super.startActivity(intent);
    }

    protected VolumeInfo findFirstVolume(int type) {
        return findFirstVolume(type, 1);
    }

    protected VolumeInfo findFirstVolume(int type, int attempts) {
        while (true) {
            final List<VolumeInfo> vols = mStorage.getVolumes();
            for (VolumeInfo vol : vols) {
                if (Objects.equals(mDisk.getId(), vol.getDiskId()) && (vol.getType() == type)
                    && (vol.getState() == VolumeInfo.STATE_MOUNTED)) {
                    return vol;
                }
            }

            if (--attempts > 0) {
                Log.w(TAG, "Missing mounted volume of type " + type + " hosted by disk "
                    + mDisk.getId() + "; trying again");
                SystemClock.sleep(250);
            } else {
                return null;
            }
        }
    }

    protected @NonNull
    CharSequence getDiskDescription() {
        if (mDisk != null) {
            return mDisk.getDescription();
        } else if (mVolume != null) {
            return mVolume.getDescription();
        } else {
            return getText(R.string.unknown);
        }
    }

    protected @NonNull
    CharSequence getDiskShortDescription() {
        if (mDisk != null) {
            return mDisk.getShortDescription();
        } else if (mVolume != null) {
            return mVolume.getDescription();
        } else {
            return getText(R.string.unknown);
        }
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