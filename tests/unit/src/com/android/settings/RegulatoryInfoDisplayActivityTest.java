/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RegulatoryInfoDisplayActivityTest {
    private static final String TAG = "RegulatoryInfoTest";

    private Instrumentation mInstrumentation;
    private Intent mRegulatoryInfoIntent;
    private UiAutomation mUiAutomation;

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mUiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        mRegulatoryInfoIntent = new Intent("android.settings.SHOW_REGULATORY_INFO")
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage(mInstrumentation.getTargetContext().getPackageName());
    }

    @Test
    public void resolveRegulatoryInfoIntent_intentShouldMatchConfig() {
        // Load intent from PackageManager and load config from Settings app
        final Context context = mInstrumentation.getTargetContext();

        final boolean hasRegulatoryInfo = context.getResources()
                .getBoolean(R.bool.config_show_regulatory_info);
        final ResolveInfo resolveInfo = mInstrumentation.getTargetContext().getPackageManager()
                .resolveActivity(mRegulatoryInfoIntent, 0 /* flags */);

        // Check config and intent both enable or both disabled.
        if (hasRegulatoryInfo && resolveInfo == null) {
            fail("Config enables regulatory info but there is no handling intent");
            return;
        }
        if (!hasRegulatoryInfo && resolveInfo != null) {
            fail("Config disables regulatory info but there is at least one handling intent");
            return;
        }
    }

    @Test
    public void launchRegulatoryInfo_shouldNotCrash() {
        final Context context = mInstrumentation.getTargetContext();
        final boolean hasRegulatoryInfo = context.getResources()
                .getBoolean(R.bool.config_show_regulatory_info);

        if (!hasRegulatoryInfo) {
            return;
        }
        // Launch intent
        mInstrumentation.startActivitySync(mRegulatoryInfoIntent);

        onView(withId(R.id.regulatoryInfo))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
    }

    @Test
    public void launchRegulatoryInfo_withInfoImage_shouldDisplay() throws IOException {
        // TODO: Remove "setenforce 0" when selinux rules is updated to give read permission for
        // regulatory info.
        mUiAutomation.executeShellCommand("setenforce 0");

        final boolean tempFileCreated = ensureRegulatoryInfoImageExists();
        try {
            final Context context = mInstrumentation.getTargetContext();
            final boolean hasRegulatoryInfo = context.getResources()
                    .getBoolean(R.bool.config_show_regulatory_info);

            if (!hasRegulatoryInfo) {
                return;
            }
            // Launch intent
            mInstrumentation.startActivitySync(mRegulatoryInfoIntent);

            onView(withId(R.id.regulatoryInfo))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
        } finally {
            if (tempFileCreated) {
                final String filename =
                        RegulatoryInfoDisplayActivity.getRegulatoryInfoImageFileName();
                new File(filename).delete();
                Log.d(TAG, "Deleting temp file " + filename);
            }
        }
    }

    /**
     * Ensures regulatory label image exists on disk.
     *
     * @return true if a test image is created.
     */
    private boolean ensureRegulatoryInfoImageExists() throws IOException {
        final String filename = RegulatoryInfoDisplayActivity.getRegulatoryInfoImageFileName();
        if (new File(filename).exists()) {
            return false;
        }
        Log.d(TAG, "Creating temp file " + filename);
        final Bitmap bitmap = Bitmap.createBitmap(400 /* width */, 400 /* height */,
                Bitmap.Config.ARGB_8888);
        final FileOutputStream out = new FileOutputStream(filename);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100 /* quality */, out);
        out.close();
        return true;
    }


}
