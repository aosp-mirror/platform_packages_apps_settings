/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.print;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintJob;
import android.print.PrintManager;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.Settings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class PrintJobSettingsActivityTest {
    private static final String EXTRA_PRINT_JOB_ID = "EXTRA_PRINT_JOB_ID";
    private static final String LOG_TAG = PrintJobSettingsActivityTest.class.getSimpleName();

    // Any activity is fine
    @Rule
    public final ActivityTestRule<Settings.PrintSettingsActivity> mActivityRule =
            new ActivityTestRule<>(Settings.PrintSettingsActivity.class, true);

    public static void runShellCommand(@NonNull String cmd) throws IOException {
        ParcelFileDescriptor stdOut =
                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                        cmd);

        try (FileInputStream fis = new ParcelFileDescriptor.AutoCloseInputStream(stdOut)) {
            byte[] buf = new byte[512];
            while (fis.read(buf) != -1) {
                // keep reading
            }
        }
    }

    @Before
    public void requirePrintFeature() {
        assumeTrue(InstrumentationRegistry.getTargetContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_PRINTING));
    }

    @Before
    public void wakeUpScreen() throws Exception {
        runShellCommand("input keyevent KEYCODE_WAKEUP");
    }

    @Test
    @LargeTest
    public void viewPrintJobSettings() throws Exception {
        UUID uuid = UUID.randomUUID();
        Object isWriteCalled = new Object();

        // Create adapter that is good enough to start a print preview
        PrintDocumentAdapter adapter = new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                    CancellationSignal cancellationSignal,
                    LayoutResultCallback callback, Bundle extras) {
                callback.onLayoutFinished(new PrintDocumentInfo.Builder(uuid.toString()).build(),
                        true);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                    CancellationSignal cancellationSignal,
                    WriteResultCallback callback) {
                synchronized (isWriteCalled) {
                    isWriteCalled.notify();
                }
                callback.onWriteFailed(null);
            }
        };

        Activity activity =  mActivityRule.getActivity();
        PrintManager pm = mActivityRule.getActivity().getSystemService(PrintManager.class);

        // Start printing
        PrintJob printJob = pm.print(uuid.toString(), adapter, null);

        // Wait until print preview is up
        synchronized (isWriteCalled) {
            isWriteCalled.wait();
        }

        // Start print job settings
        Intent intent = new Intent(android.provider.Settings.ACTION_PRINT_SETTINGS);
        intent.putExtra(EXTRA_PRINT_JOB_ID, printJob.getId().flattenToString());
        intent.setData(Uri.fromParts("printjob", printJob.getId().flattenToString(), null));
        activity.startActivity(intent);

        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiObject2 printPrefTitle = uiDevice.wait(Until.findObject(By.text("Configuring "
                + uuid.toString())), 5000);
        assertNotNull(printPrefTitle);

        Log.i(LOG_TAG, "Found " + printPrefTitle.getText());
    }
}
