/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.SystemUpdateManager;
import android.os.UpdateEngine;
import android.os.UpdateEngineStable;
import android.os.UpdateEngineStableCallback;
import android.provider.Settings;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Controller for 16K pages developer option */
public class Enable16kPagesPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener,
                PreferenceControllerMixin,
                Enable16kbPagesDialogHost {

    private static final String TAG = "Enable16kPages";
    private static final String REBOOT_REASON = "toggle16k";
    private static final String ENABLE_16K_PAGES = "enable_16k_pages";

    @VisibleForTesting
    static final String DEV_OPTION_PROPERTY = "ro.product.build.16k_page.enabled";

    private static final int ENABLE_4K_PAGE_SIZE = 0;
    private static final int ENABLE_16K_PAGE_SIZE = 1;

    private static final String OTA_16K_PATH = "/system/boot_otas/boot_ota_16k.zip";
    private static final String OTA_4K_PATH = "/system/boot_otas/boot_ota_4k.zip";
    private static final String PAYLOAD_BINARY_FILE_NAME = "payload.bin";
    private static final String PAYLOAD_PROPERTIES_FILE_NAME = "payload_properties.txt";
    private static final int OFFSET_TO_FILE_NAME = 30;
    public static final String EXPERIMENTAL_UPDATE_TITLE = "Android 16K Kernel Experimental Update";

    private @Nullable DevelopmentSettingsDashboardFragment mFragment = null;
    private boolean mEnable16k;

    private final ListeningExecutorService mExecutorService =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    private AlertDialog mProgressDialog;

    public Enable16kPagesPreferenceController(
            @NonNull Context context, @Nullable DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return SystemProperties.getBoolean(DEV_OPTION_PROPERTY, false);
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_16K_PAGES;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mEnable16k = (Boolean) newValue;
        Enable16kPagesWarningDialog.show(mFragment, this, mEnable16k);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int optionValue =
                Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.ENABLE_16K_PAGES,
                        ENABLE_4K_PAGE_SIZE /* default */);

        ((SwitchPreference) mPreference).setChecked(optionValue == ENABLE_16K_PAGE_SIZE);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        // TODO(295035851) : Revert kernel when dev option turned off
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.ENABLE_16K_PAGES,
                ENABLE_4K_PAGE_SIZE);
        ((SwitchPreference) mPreference).setChecked(false);
    }

    /** Called when user confirms reboot dialog */
    @Override
    public void on16kPagesDialogConfirmed() {
        // Show progress bar
        mProgressDialog = makeProgressDialog();
        mProgressDialog.show();

        // Apply update in background
        ListenableFuture future = mExecutorService.submit(() -> installUpdate());
        Futures.addCallback(
                future,
                new FutureCallback<>() {

                    @Override
                    public void onSuccess(@NonNull Object result) {
                        // This means UpdateEngineStable is working on applying update in
                        // background.
                        // Result of that operation will be provided by separate callback.
                        Log.i(TAG, "applyPayload call to UpdateEngineStable succeeded.");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        hideProgressDialog();
                        Log.e(TAG, "Failed to call applyPayload of UpdateEngineStable!");
                        displayToast(mContext.getString(R.string.toast_16k_update_failed_text));
                    }
                },
                ContextCompat.getMainExecutor(mContext));
    }

    /** Called when user dismisses to reboot dialog */
    @Override
    public void on16kPagesDialogDismissed() {}

    private void installUpdate() {
        // Check if there is any pending system update
        SystemUpdateManager manager = mContext.getSystemService(SystemUpdateManager.class);
        Bundle data = manager.retrieveSystemUpdateInfo();
        int status = data.getInt(SystemUpdateManager.KEY_STATUS);
        if (status != SystemUpdateManager.STATUS_UNKNOWN
                && status != SystemUpdateManager.STATUS_IDLE) {
            throw new RuntimeException("System has pending update!");
        }

        // Publish system update info
        PersistableBundle info = createUpdateInfo(SystemUpdateManager.STATUS_IN_PROGRESS);
        manager.updateSystemUpdateInfo(info);

        String updateFilePath = mEnable16k ? OTA_16K_PATH : OTA_4K_PATH;
        try {
            File updateFile = new File(updateFilePath);
            applyUpdateFile(updateFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    void applyUpdateFile(@NonNull File updateFile) throws IOException, FileNotFoundException {
        boolean payloadFound = false;
        boolean propertiesFound = false;
        long payloadOffset = 0;
        long payloadSize = 0;

        List<String> properties = new ArrayList<>();
        try (ZipFile zip = new ZipFile(updateFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            long offset = 0;
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String fileName = zipEntry.getName();
                long extraSize = zipEntry.getExtra() == null ? 0 : zipEntry.getExtra().length;
                offset += OFFSET_TO_FILE_NAME + fileName.length() + extraSize;

                if (zipEntry.isDirectory()) {
                    continue;
                }

                long length = zipEntry.getCompressedSize();
                if (PAYLOAD_BINARY_FILE_NAME.equals(fileName)) {
                    if (zipEntry.getMethod() != ZipEntry.STORED) {
                        throw new IOException("Unknown compression method.");
                    }
                    payloadFound = true;
                    payloadOffset = offset;
                    payloadSize = length;
                } else if (PAYLOAD_PROPERTIES_FILE_NAME.equals(fileName)) {
                    propertiesFound = true;
                    InputStream inputStream = zip.getInputStream(zipEntry);
                    if (inputStream != null) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                        String line;
                        while ((line = br.readLine()) != null) {
                            properties.add(line);
                        }
                    }
                }
                offset += length;
            }
        }

        if (!payloadFound) {
            throw new FileNotFoundException(
                    "Failed to find payload in zip: " + updateFile.getAbsolutePath());
        }

        if (!propertiesFound) {
            throw new FileNotFoundException(
                    "Failed to find payload properties in zip: " + updateFile.getAbsolutePath());
        }

        if (payloadSize == 0) {
            throw new IOException("Found empty payload in zip: " + updateFile.getAbsolutePath());
        }

        applyPayload(updateFile, payloadOffset, payloadSize, properties);
    }

    private void hideProgressDialog() {
        // Hide progress bar
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    @VisibleForTesting
    void applyPayload(
            @NonNull File updateFile,
            long payloadOffset,
            long payloadSize,
            @NonNull List<String> properties)
            throws FileNotFoundException {
        String[] header = properties.stream().toArray(String[]::new);
        UpdateEngineStable updateEngineStable = new UpdateEngineStable();
        try {
            ParcelFileDescriptor pfd =
                    ParcelFileDescriptor.open(updateFile, ParcelFileDescriptor.MODE_READ_ONLY);
            updateEngineStable.bind(
                    new OtaUpdateCallback(updateEngineStable),
                    new Handler(mContext.getMainLooper()));
            updateEngineStable.applyPayloadFd(pfd, payloadOffset, payloadSize, header);
        } finally {
            Log.e(TAG, "Failure while applying an update using update engine");
        }
    }

    private void displayToast(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    private class OtaUpdateCallback extends UpdateEngineStableCallback {
        UpdateEngineStable mUpdateEngineStable;

        OtaUpdateCallback(@NonNull UpdateEngineStable engine) {
            mUpdateEngineStable = engine;
        }

        @Override
        public void onStatusUpdate(int status, float percent) {}

        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            Log.i(TAG, "Callback from update engine stable received. unbinding..");
            // unbind the callback from update engine
            mUpdateEngineStable.unbind();

            // Hide progress bar
            hideProgressDialog();

            if (errorCode == UpdateEngine.ErrorCodeConstants.SUCCESS) {
                Log.i(TAG, "applyPayload successful");

                // Save changed preference
                Settings.Global.putInt(
                        mContext.getContentResolver(),
                        Settings.Global.ENABLE_16K_PAGES,
                        mEnable16k ? ENABLE_16K_PAGE_SIZE : ENABLE_4K_PAGE_SIZE);

                // Publish system update info
                SystemUpdateManager manager = mContext.getSystemService(SystemUpdateManager.class);
                PersistableBundle info =
                        createUpdateInfo(SystemUpdateManager.STATUS_WAITING_REBOOT);
                manager.updateSystemUpdateInfo(info);

                // Restart device to complete update
                PowerManager pm = mContext.getSystemService(PowerManager.class);
                pm.reboot(REBOOT_REASON);
            } else {
                Log.e(TAG, "applyPayload failed, error code: " + errorCode);
                displayToast(mContext.getString(R.string.toast_16k_update_failed_text));
            }
        }
    }

    private AlertDialog makeProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mFragment.getActivity());
        builder.setTitle(R.string.progress_16k_ota_title);

        final ProgressBar progressBar = new ProgressBar(mFragment.getActivity());
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBar.setLayoutParams(params);
        builder.setView(progressBar);
        builder.setCancelable(false);
        return builder.create();
    }

    private PersistableBundle createUpdateInfo(int status) {
        PersistableBundle infoBundle = new PersistableBundle();
        infoBundle.putInt(SystemUpdateManager.KEY_STATUS, status);
        infoBundle.putBoolean(SystemUpdateManager.KEY_IS_SECURITY_UPDATE, false);
        infoBundle.putString(SystemUpdateManager.KEY_TITLE, EXPERIMENTAL_UPDATE_TITLE);
        return infoBundle;
    }
}
