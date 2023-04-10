/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.regionalpreferences;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.LocaleList;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

public class NumberingSystemControllerTest {
    private Context mApplicationContext;
    private NumberingSystemController mController;

    @Before
    public void setUp() throws Exception {
        mApplicationContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void getAvailabilityStatus_noLocale_unavailable() {
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US,zh-Hant-TW"));
        mController = new NumberingSystemController(mApplicationContext, "key");

        int result = mController.getAvailabilityStatus();

        assertEquals(CONDITIONALLY_UNAVAILABLE, result);
    }

    @Test
    public void getAvailabilityStatus_hasLocaleWithNumberingSystems_available() {
        // ar-JO has different numbering system.
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US,zh-Hant-TW,ar-JO"));
        mController = new NumberingSystemController(mApplicationContext, "key");

        int result = mController.getAvailabilityStatus();

        assertEquals(AVAILABLE, result);
    }
}
