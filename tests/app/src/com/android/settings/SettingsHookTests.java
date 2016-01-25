/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.settings.Settings;
import com.android.settings.tests.Manufacturer;
import com.android.settings.tests.Operator;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.test.ActivityInstrumentationTestCase2;

import java.util.List;

/**
 * Tests for the Settings operator/manufacturer hook.
 *
 * Running all tests:
 *
 *   make SettingsTests
 *   adb push SettingsTests.apk /system/app/SettingsTests.apk
 *   adb shell am instrument \
 *    -w com.android.settings.tests/android.test.InstrumentationTestRunner
 */
public class SettingsHookTests extends ActivityInstrumentationTestCase2<Settings> {

    private static final String PACKAGE_NAME = "com.android.settings.tests";

    private static final String KEY_SETTINGS_ROOT = "parent";
    private static final String KEY_SETTINGS_OPERATOR = "operator_settings";
    private static final String KEY_SETTINGS_MANUFACTURER = "manufacturer_settings";

    private static final String INTENT_OPERATOR_HOOK = "com.android.settings.OPERATOR_APPLICATION_SETTING";
    private static final String INTENT_MANUFACTURER_HOOK = "com.android.settings.MANUFACTURER_APPLICATION_SETTING";

    private Settings mSettings;

    public SettingsHookTests() {
        super("com.android.settings", Settings.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSettings = getActivity();
    }

    /**
     * Test that the operator/manufacturer settings hook test application is
     * available and that it's installed in the device's system image.
     */
    public void testSettingsHookTestAppAvailable() throws Exception {
        Context context = mSettings.getApplicationContext();
        PackageManager pm = context.getPackageManager();
        ApplicationInfo applicationInfo = pm.getApplicationInfo(PACKAGE_NAME, 0);
        assertTrue((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    /**
     * Test that the operator test activity has registered an intent-filter for
     * an action named 'android.settings.OPERATOR_APPLICATION_SETTING'.
     */
    public void testOperatorIntentFilter() {
        boolean result = false;
        Context context = mSettings.getApplicationContext();
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(INTENT_OPERATOR_HOOK);
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : list) {
            if (resolveInfo.activityInfo.packageName.equals(PACKAGE_NAME)) {
                result = true;
            }
        }
        assertTrue("Intent-filter not found", result);
    }

    /**
     * Test that the manufacturer test activity has registered an intent-filter
     * for an action named 'android.settings.MANUFACTURER_APPLICATION_SETTING'.
     */
    public void testManufacturerIntentFilter() {
        boolean result = false;
        Context context = mSettings.getApplicationContext();
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(INTENT_MANUFACTURER_HOOK);
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : list) {
            if (resolveInfo.activityInfo.packageName.equals(PACKAGE_NAME)) {
                result = true;
            }
        }
        assertTrue("Intent-filter not found", result);
    }

    /**
     * Test that the operator preference is available in the Settings
     * application.
     */
    public void testOperatorPreferenceAvailable() {
// TODO: fix this test case to work with fragments
//        PreferenceGroup root = (PreferenceGroup)mSettings.findPreference(KEY_SETTINGS_ROOT);
//        Preference operatorPreference = root.findPreference(KEY_SETTINGS_OPERATOR);
//        assertNotNull(operatorPreference);
    }

    /**
     * Test that the manufacturer preference is available in the Settings
     * application.
     */
    public void testManufacturerPreferenceAvailable() {
// TODO: fix this test case to work with fragments
//        PreferenceGroup root = (PreferenceGroup)mSettings.findPreference(KEY_SETTINGS_ROOT);
//        Preference manufacturerHook = root.findPreference(KEY_SETTINGS_MANUFACTURER);
//        assertNotNull(manufacturerHook);
    }

}
