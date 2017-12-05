/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.network;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class MobilePlanPreferenceControllerTest {

    @Test
    public void testNoProvisionStringFormattedCorrectly() {
        final String operator = "test_operator";
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        assertThat(context.getString(R.string.mobile_no_provisioning_url, operator, operator))
                .contains(operator);
    }
}
