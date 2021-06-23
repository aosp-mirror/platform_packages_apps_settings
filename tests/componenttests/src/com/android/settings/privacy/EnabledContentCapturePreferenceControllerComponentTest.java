/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.privacy;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.testutils.AdbUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class EnabledContentCapturePreferenceControllerComponentTest {
    private Instrumentation mInstrumentation;
    private static final String TAG =
            EnabledContentCapturePreferenceControllerComponentTest.class.getSimpleName();

    @Before
    public void setUp() {
        if (null == mInstrumentation) {
            mInstrumentation = InstrumentationRegistry.getInstrumentation();
        }
    }

    @Test
    public void test_uncheck_content_capture() throws Exception {
        content_capture_checkbox_test_helper(false);
    }

    @Test
    public void test_check_content_capture() throws Exception {
        content_capture_checkbox_test_helper(true);
    }

    private void content_capture_checkbox_test_helper(boolean check) throws Exception {
        EnableContentCapturePreferenceController enableContentCapturePreferenceController =
                new EnableContentCapturePreferenceController(
                        ApplicationProvider.getApplicationContext(),
                        "Test_key");
        enableContentCapturePreferenceController.setChecked(check);

        //Check through adb command
        assertThat(AdbUtils.checkStringInAdbCommandOutput(TAG, "dumpsys content_capture",
                "Users disabled by Settings: ", check ? "{}" : "{0=true}", 1000)).isTrue();
    }
}
