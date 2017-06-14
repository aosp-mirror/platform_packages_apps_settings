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

package com.android.settings.core.codeinspection;

import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.InstrumentableFragmentCodeInspector;
import com.android.settings.search.SearchIndexProviderCodeInspector;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * Test suite that scans all class in app package, and perform different types of code inspection
 * for conformance.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        assetDir = "/tests/robotests/assets")
public class CodeInspectionTest {

    private List<Class<?>> mClasses;

    @Before
    public void setUp() throws Exception {
        mClasses = new ClassScanner().getClassesForPackage(CodeInspector.PACKAGE_NAME);
    }

    @Test
    public void runCodeInspections() {
        new InstrumentableFragmentCodeInspector(mClasses).run();
        new SearchIndexProviderCodeInspector(mClasses).run();
    }
}
