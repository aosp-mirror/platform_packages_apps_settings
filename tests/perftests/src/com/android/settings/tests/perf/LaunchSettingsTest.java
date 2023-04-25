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
package com.android.settings.tests.perf;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static junit.framework.TestCase.fail;

import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(AndroidJUnit4.class)
public class LaunchSettingsTest {
    private static class Page {
        String action;
        String displayName;
        String title;

        Page(String action, String displayName, String title) {
            this.action = action;
            this.displayName = displayName;
            this.title = title;
        }
    }

    private static final String SCREEN_TIME_OUT = "7200000";
    private static final String DEFAULT_SCREEN_TIMEOUT = "15000";
    private static final int TIME_OUT = 5000;
    private static final int TEST_TIME = 10;
    private static final Pattern PATTERN = Pattern.compile("TotalTime:\\s[0-9]*");
    private static final Page[] PAGES;
    private static final String TAG = "SettingsPerfTests";

    static {
        PAGES = new Page[]{
                new Page("android.settings.SETTINGS", "Search settings", "Settings"),
                new Page("android.settings.WIFI_SETTINGS", "Use Wi‑Fi", "Wi-Fi"),
                new Page("android.settings.BLUETOOTH_SETTINGS", "Connected devices", "BlueTooth"),
                new Page("android.settings.APPLICATION_SETTINGS", "App info", "Application"),
                new Page("android.intent.action.POWER_USAGE_SUMMARY", "Battery", "Battery"),
                new Page("android.settings.INTERNAL_STORAGE_SETTINGS", "Storage", "Storage")
        };
    }

    private Bundle mBundle;
    private UiDevice mDevice;
    private Instrumentation mInstrumentation;
    private Map<String, ArrayList<Integer>> mResult;
    private String mDefaultScreenTimeout;
    private String mDefaultAirplaneModeStatus;

    @Before
    public void setUp() throws Exception {
        mBundle = new Bundle();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mResult = new LinkedHashMap<>();
        mDefaultScreenTimeout = mDevice.executeShellCommand(
                "settings get system screen_off_timeout");
        mDefaultAirplaneModeStatus = getAirplaneModeStatus();
        setScreenTimeOut(SCREEN_TIME_OUT);
        setAirplaneMode();
        mDevice.pressHome();
        mDevice.waitForIdle(TIME_OUT);

        for (Page page : PAGES) {
            mResult.put(page.title, new ArrayList<Integer>());
        }
    }

    @After
    public void tearDown() throws Exception {
        putResultToBundle();
        mInstrumentation.sendStatus(0, mBundle);
        resetScreenTimeout();
        resetAirplaneMode();
        closeApp();
    }

    @Test
    public void settingsPerformanceTest() throws Exception {
        for (int i = 0; i < TEST_TIME; i++) {
            for (Page page : PAGES) {
                executePreformanceTest(page.action, page.displayName, page.title);
            }
        }
    }

    private void executePreformanceTest(String action, String displayName, String title)
            throws Exception {
        closeApp();
        mDevice.waitForIdle(TIME_OUT);
        final String mString = mDevice.executeShellCommand("am start -W -a" + action);
        mDevice.wait(Until.findObject(By.text(displayName)), TIME_OUT);
        handleLaunchResult(title, mString);
    }

    private void handleLaunchResult(String title, String shellCommandResult) {
        Matcher mMatcher = PATTERN.matcher(shellCommandResult);
        if (mMatcher.find()) {
            mResult.get(title).add(Integer.valueOf(mMatcher.group().split("\\s")[1]));
        } else {
            fail(String.format("Not found %s.\n %s", title, shellCommandResult));
        }
    }

    private void closeApp() throws Exception {
        mDevice.executeShellCommand("am force-stop com.android.settings");
        Thread.sleep(1000);
    }

    private void putResultToBundle() {
        for (String string : mResult.keySet()) {
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "max"),
                    getMax(string));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "min"),
                    getMin(string));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "avg"),
                    getAvg(string));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "25 Percentile"),
                    getPercentile(string, 25));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "50 Percentile"),
                    getPercentile(string, 50));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "75 Percentile"),
                    getPercentile(string, 75));
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "all_results"),
                    mResult.get(string).toString());
            mBundle.putString(String.format("LaunchSettingsTest_%s_%s", string, "results_count"),
                    String.valueOf(mResult.get(string).size()));
        }
    }

    private String getMax(String page) {
        if (mResult.get(page).size() == TEST_TIME) {
            return String.format("%s", Collections.max(mResult.get(page)));
        }
        Log.e(TAG, String.format("Fail to get max of %s.", page));
        return "0";

    }

    private String getMin(String page) {
        if (mResult.get(page).size() == TEST_TIME) {
            return String.format("%s", Collections.min(mResult.get(page)));
        }
        Log.e(TAG, String.format("Fail to get min of %s.", page));
        return "0";
    }

    private String getAvg(String page) {
        if (mResult.get(page).size() == TEST_TIME) {
            return String.valueOf((int) mResult.get(page).stream().mapToInt(
                    i -> i).average().orElse(0));
        }
        Log.e(TAG, String.format("Fail to get avg of %s.", page));
        return "0";
    }

    private void setScreenTimeOut(String timeout) throws Exception {
        mDevice.executeShellCommand("settings put system screen_off_timeout " + timeout);
    }

    private void resetScreenTimeout() throws Exception {
        String timeout = DEFAULT_SCREEN_TIMEOUT;
        if (!mDefaultScreenTimeout.isEmpty()) {
            timeout = mDefaultScreenTimeout;
        }
        setScreenTimeOut(timeout);
    }

    private void setAirplaneMode() throws Exception {
        if (mDefaultAirplaneModeStatus.equals("0\n")) {
            clickAirplaneMode();
        }
    }

    private void resetAirplaneMode() throws Exception {
        if (!getAirplaneModeStatus().equals(mDefaultAirplaneModeStatus)) {
            clickAirplaneMode();
        }
    }

    private void clickAirplaneMode() throws Exception {
        mDevice.executeShellCommand("am start -W -a android.settings.AIRPLANE_MODE_SETTINGS");
        mDevice.waitForIdle(TIME_OUT);
        mDevice.findObject(By.textContains("Airplane")).click();
        mDevice.waitForIdle(TIME_OUT);
    }

    private String getAirplaneModeStatus() throws Exception {
        return mDevice.executeShellCommand("settings get global airplane_mode_on");
    }

    private String getPercentile(String page, double position) {
        Collections.sort(mResult.get(page));
        if (mResult.get(page).size() == TEST_TIME) {
            return String.valueOf(
                    mResult.get(page).get((int) (Math.ceil(TEST_TIME * position / 100)) - 1));
        }
        Log.e(TAG, String.format("Fail to get percentile of %s.", page));
        return "0";
    }
}