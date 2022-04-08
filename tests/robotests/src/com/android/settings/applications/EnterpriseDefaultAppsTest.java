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
 * limitations under the License
 */

package com.android.settings.applications;

import static org.junit.Assert.assertTrue;

import com.android.settings.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class EnterpriseDefaultAppsTest {
    @Test
    public void testNumberOfIntentsCorrelateWithUI() {
        final int concatenation_templates[] =
                new int[]{0 /* no need for single app name */,
                        R.string.app_names_concatenation_template_2,
                        R.string.app_names_concatenation_template_3};
        for (EnterpriseDefaultApps app : EnterpriseDefaultApps.values()) {
            assertTrue("Number of intents should be limited by number of apps the UI can show",
                    app.getIntents().length <= concatenation_templates.length);
        }
    }
}
