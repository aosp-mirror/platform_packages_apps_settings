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
package com.android.settings.gesture;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings.Secure;
import android.support.test.filters.MediumTest;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.UiScrollable;
import android.test.InstrumentationTestCase;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.android.settings.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.Test;

/**
 * Test for Gesture preferences.
 */
@MediumTest
public class GestureSettingsTest extends InstrumentationTestCase {

    private static final String TAG = "GestureSettingsTest";

    private UiDevice mDevice;
    private Context mTargetContext;
    private String mTargetPackage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mTargetContext = getInstrumentation().getTargetContext();
        mTargetPackage = mTargetContext.getPackageName();
    }

    @Test
    public void testAmbientDisplaySwitchPreference() throws Exception {
        launchSettings();
        UiObject dozeSwitch = getDozeSwitch();
        assertNotNull(dozeSwitch);
        assertTrue(dozeSwitch.exists());
        assertToggleStateMatchesSettingValue(dozeSwitch, Secure.DOZE_ENABLED, 1, 1);
    }

    @Test
    public void testGestureSettingsExists() throws Exception {
        launchMoves();
        UiObject titleObj = mDevice.findObject(new UiSelector().text(
                mTargetContext.getResources().getString(R.string.gesture_preference_title)));
        assertNotNull(titleObj);
        assertTrue(titleObj.exists());
    }

    @Test
    public void testCameraDoubleTapToggle() throws Exception {
        assertSwitchToggle(mTargetContext.getResources().getString(
                R.string.double_tap_power_for_camera_title),
                Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 0, 0);
    }

    @Test
    public void testCameraDoubleTwistToggle() throws Exception {
        assertSwitchToggle(mTargetContext.getResources().getString(
                R.string.double_twist_for_camera_mode_title),
                Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 1, 1);
    }

    @Test
    public void testFingerprintSwipeToggle() throws Exception {
        assertSwitchToggle(mTargetContext.getResources().getString(
                R.string.fingerprint_swipe_for_notifications_title),
                Secure.SYSTEM_NAVIGATION_KEYS_ENABLED, 0, 1);
    }

    @Test
    public void testDozeDoubleTapToggle() throws Exception {
        assertSwitchToggle(mTargetContext.getResources().getString(
                R.string.ambient_display_title),
                Secure.DOZE_PULSE_ON_DOUBLE_TAP, 1, 1);
    }

    @Test
    public void testDozePickupToggle() throws Exception {
        assertSwitchToggle(mTargetContext.getResources().getString(
                R.string.ambient_display_pickup_title),
                Secure.DOZE_PULSE_ON_PICK_UP, 1, 1);
    }

    private void launchSettings() {
        Intent settingsIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(mTargetPackage)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getInstrumentation().getContext().startActivity(settingsIntent);
    }

    private void launchMoves() throws Exception  {
        launchSettings();
        UiScrollable settings = new UiScrollable(
                new UiSelector().packageName(mTargetPackage).scrollable(true));
        String titleMoves =
                mTargetContext.getResources().getString(R.string.gesture_preference_title);
        settings.scrollTextIntoView(titleMoves);
        mDevice.findObject(new UiSelector().text(titleMoves)).click();
    }

    private void navigateToMovesSetting(String title) throws Exception {
        launchMoves();
        UiScrollable moves = new UiScrollable(
                new UiSelector().packageName(mTargetPackage).scrollable(true));
        moves.scrollTextIntoView(title);
    }

    private UiScrollable navigateToAmbientDisplay() throws Exception {
        UiScrollable settings = new UiScrollable(
                new UiSelector().packageName(mTargetPackage).scrollable(true));
        String titleDisplay =
                mTargetContext.getResources().getString(R.string.display_settings_title);
        settings.scrollTextIntoView(titleDisplay);
        mDevice.findObject(new UiSelector().text(titleDisplay)).click();
        settings.scrollTextIntoView(mTargetContext.getResources().getString(R.string.doze_title));
        return settings;
    }

    private UiObject getGestureSwitch(String title) throws Exception {
        UiObject titleView = mDevice.findObject(new UiSelector().text(title));
        UiObject gestureSwitch =
            titleView.getFromParent(new UiSelector().className(Switch.class.getName()));
        assertNotNull(gestureSwitch);
        assertTrue(gestureSwitch.exists());
        return gestureSwitch;
    }

    private UiObject getDozeSwitch() throws Exception {
        UiScrollable settings = navigateToAmbientDisplay();
        UiObject dozeSwitch = null;
        UiSelector relativeLayoutSelector =
                new UiSelector().className(RelativeLayout.class.getName());
        String titleDoze = mTargetContext.getResources().getString(R.string.doze_title);
        for (int i = 0; i <= settings.getChildCount(relativeLayoutSelector); i++) {
            UiObject relativeLayout = settings.getChild(relativeLayoutSelector.instance(i));
            if (relativeLayout.getChildCount() != 2) {
                continue;
            }
            UiObject obj1 = relativeLayout.getChild(new UiSelector().index(0));
            if (obj1.getText() == titleDoze) {
                return relativeLayout.getFromParent(
                        new UiSelector().className(Switch.class.getName()));
            }
        }
        return null;
    }

    private void assertSwitchToggle(String title, String key, int defaultVal, int enabledVal)
            throws Exception {
        navigateToMovesSetting(title);
        assertToggleStateMatchesSettingValue(getGestureSwitch(title), key, defaultVal, enabledVal);
    }

    private void assertToggleStateMatchesSettingValue (
            UiObject testSwitch, String key, int defaultVal, int enabledVal) throws Exception {
        // check initial state
        int currentValue = Secure.getInt(mTargetContext.getContentResolver(), key, defaultVal);
        boolean enabled = currentValue == enabledVal;
        assertEquals(enabled, testSwitch.isChecked());
        // toggle the preference
        testSwitch.click();
        assertEquals(!enabled, testSwitch.isChecked());
        int newValue = currentValue == 1 ? 0 : 1;
        assertEquals(newValue, Secure.getInt(mTargetContext.getContentResolver(), key, defaultVal));
    }

}
