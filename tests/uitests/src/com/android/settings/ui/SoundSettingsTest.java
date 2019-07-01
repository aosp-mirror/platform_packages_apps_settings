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

package com.android.settings.ui;

import android.content.ContentResolver;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.system.helpers.SettingsHelper;
import android.system.helpers.SettingsHelper.SettingsType;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;

import java.util.HashMap;

public class SoundSettingsTest extends InstrumentationTestCase {
    private static final String PAGE = Settings.ACTION_SOUND_SETTINGS;
    private static final int TIMEOUT = 2000;

    private UiDevice mDevice;
    private ContentResolver mResolver;
    private SettingsHelper mHelper;


    private HashMap ringtoneSounds = new HashMap<String, String>() {{
        put("angler","Dione");
        put("bullhead","Dione");
        put("marlin","Spaceship");
        put("sailfish","Spaceship");
        put("walleye","Copycat");
        put("taimen","Copycat");
    }};

    private HashMap ringtoneCodes = new HashMap<String, String>() {{
        put("angler","38");
        put("bullhead","38");
        put("marlin","37");
        put("sailfish","37");
        put("walleye","26");
        put("taimen","26");
    }};

    private HashMap alarmSounds = new HashMap<String, String>() {{
        put("angler","Awaken");
        put("bullhead","Awaken");
        put("marlin","Bounce");
        put("sailfish","Bounce");
        put("walleye","Cuckoo clock");
        put("taimen","Cuckoo clock");
    }};

    private HashMap alarmCodes = new HashMap<String, String>() {{
        put("angler","6");
        put("bullhead","6");
        put("marlin","49");
        put("sailfish","49");
        put("walleye","15");
        put("taimen","15");
    }};

    private HashMap notificationSounds = new HashMap<String, String>() {{
        put("angler","Ceres");
        put("bullhead","Ceres");
        put("marlin","Trill");
        put("sailfish","Trill");
        put("walleye","Pipes");
        put("taimen","Pipes");
    }};


    private HashMap notificationCodes = new HashMap<String, String>() {{
        put("angler","26");
        put("bullhead","26");
        put("marlin","57");
        put("sailfish","57");
        put("walleye","69");
        put("taimen","69");
    }};

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mDevice.setOrientationNatural();
        mResolver = getInstrumentation().getContext().getContentResolver();
        mHelper = new SettingsHelper();
    }

    @Override
    public void tearDown() throws Exception {
        mDevice.pressBack();
        mDevice.pressHome();
        mDevice.waitForIdle();
        mDevice.unfreezeRotation();
        super.tearDown();
    }

    @MediumTest
    public void testCallVibrate() throws Exception {
        assertTrue(mHelper.verifyToggleSetting(SettingsType.SYSTEM, PAGE,
                "Also vibrate for calls", Settings.System.VIBRATE_WHEN_RINGING));
        assertTrue(mHelper.verifyToggleSetting(SettingsType.SYSTEM, PAGE,
                "Also vibrate for calls", Settings.System.VIBRATE_WHEN_RINGING));
    }

    @MediumTest
    public void testOtherSoundsDialPadTones() throws Exception {
        loadOtherSoundsPage();
        assertTrue("Dial pad tones not toggled", mHelper.verifyToggleSetting(
                SettingsType.SYSTEM, PAGE, "Dial pad tones",
                Settings.System.DTMF_TONE_WHEN_DIALING));
    }

    @MediumTest
    public void testOtherSoundsScreenLocking() throws Exception {
        loadOtherSoundsPage();
        assertTrue("Screen locking sounds not toggled",
                    mHelper.verifyToggleSetting(SettingsType.SYSTEM, PAGE,
                    "Screen locking sounds", Settings.System.LOCKSCREEN_SOUNDS_ENABLED));
    }

    @MediumTest
    public void testOtherSoundsCharging() throws Exception {
        loadOtherSoundsPage();
        assertTrue("Charging sounds not toggled",
                    mHelper.verifyToggleSetting(SettingsType.GLOBAL, PAGE,
                    "Charging sounds", Settings.Global.CHARGING_SOUNDS_ENABLED));
    }

    @MediumTest
    public void testOtherSoundsTouch() throws Exception {
        loadOtherSoundsPage();
        assertTrue("Touch sounds not toggled",
                    mHelper.verifyToggleSetting(SettingsType.SYSTEM, PAGE,
                    "Touch sounds", Settings.System.SOUND_EFFECTS_ENABLED));
    }

    @MediumTest
    public void testOtherSoundsVibrateOnTap() throws Exception {
        loadOtherSoundsPage();
        assertTrue("Vibrate on tap not toggled",
                    mHelper.verifyToggleSetting(SettingsType.SYSTEM, PAGE,
                    "Vibrate on tap", Settings.System.HAPTIC_FEEDBACK_ENABLED));
    }

    private void loadOtherSoundsPage() throws Exception {
        launchSoundSettings();
        mHelper.scrollVert(false);
        Thread.sleep(1000);
    }

    private void launchSoundSettings() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        mHelper.scrollVert(false);
        clickMore();
        Thread.sleep(1000);
        mHelper.scrollVert(true);
        Thread.sleep(1000);
    }

    /*
     * Rather than verifying every ringtone, verify the ones least likely to change
     * (None and Hangouts) and an arbitrary one from the ringtone pool.
     */
    @MediumTest
    public void testPhoneRingtoneNone() throws Exception {
        launchSoundSettings();
        mHelper.clickSetting("Phone ringtone");
        verifyRingtone(new RingtoneSetting("None", "null"),
                Settings.System.RINGTONE);
    }

    @MediumTest
    @Suppress
    public void testPhoneRingtoneHangouts() throws Exception {
        launchSoundSettings();
        mHelper.clickSetting("Phone ringtone");
        verifyRingtone(new RingtoneSetting("Hangouts Call", "31"), Settings.System.RINGTONE);
    }

    @MediumTest
    public void testPhoneRingtone() throws Exception {
        launchSoundSettings();
        mHelper.clickSetting("Phone ringtone");
        String ringtone = ringtoneSounds.get(mDevice.getProductName()).toString();
        String ringtoneSettingValue = ringtoneCodes.get(mDevice.getProductName()).toString();
        verifyRingtone(new RingtoneSetting(ringtone, ringtoneSettingValue),
                Settings.System.RINGTONE);
    }

    @MediumTest
    public void testNotificationRingtoneNone() throws Exception {
        launchSoundSettings();
        mHelper.clickSetting("Default notification sound");
        verifyRingtone(new RingtoneSetting("None", "null"),
                Settings.System.NOTIFICATION_SOUND);
    }

    @MediumTest
    @Suppress
    public void testNotificationRingtoneHangouts() throws Exception {
        launchSoundSettings();
        mHelper.clickSetting("Default notification sound");
        verifyRingtone(new RingtoneSetting("Hangouts Message", "30"),
                Settings.System.NOTIFICATION_SOUND);
    }

    @MediumTest
    public void testNotificationRingtone() throws Exception {
        launchSoundSettings();
        mHelper.clickSetting("Default notification sound");
        String notificationRingtone = notificationSounds.get(mDevice.getProductName()).toString();
        String notificationSettingValue = notificationCodes.get(mDevice.getProductName()).toString();
        verifyRingtone(new RingtoneSetting(notificationRingtone, notificationSettingValue),
                Settings.System.NOTIFICATION_SOUND);
    }

    @MediumTest
    public void testAlarmRingtoneNone() throws Exception {
        launchSoundSettings();
        mHelper.clickSetting("Default alarm sound");
        verifyRingtone(new RingtoneSetting("None", "null"),
                Settings.System.ALARM_ALERT);
    }

    @MediumTest
    public void testAlarmRingtone() throws Exception {
        launchSoundSettings();
        String alarmRingtone = alarmSounds.get(mDevice.getProductName()).toString();
        String alarmSettingValue = alarmCodes.get(mDevice.getProductName()).toString();
        mHelper.clickSetting("Default alarm sound");
        verifyRingtone(new RingtoneSetting(alarmRingtone, alarmSettingValue),
                Settings.System.ALARM_ALERT);
    }

    /*
     * This method verifies that setting a custom ringtone changes the
     * ringtone code setting on the system. Each ringtone sound corresponds
     * to an arbitrary code. To see which ringtone code this is on your device, run
     * adb shell settings get system ringtone
     * The number you see at the end of the file path is the one you need.
     * To see alarms and notifications ringtone codes, run the following:
     * adb shell settings get system alarm_alert
     * adb shell settings get system notification_sound
     * @param r Ringtone setting - the name of the ringtone as displayed on device
     * @param settingName - the code of the ringtone as explained above
     * @param dir - the direction in which to scroll
     */
    private void verifyRingtone(RingtoneSetting r, String settingName) throws Exception {
        findRingtoneInList(r.getName()).click();
        if (mDevice.getProductName().equals("walleye") || mDevice.getProductName().equals("taimen")) {
            mDevice.wait(Until.findObject(By.text("SAVE")), TIMEOUT).click();
        }
        else {
            mDevice.wait(Until.findObject(By.text("OK")), TIMEOUT).click();
        }
        SystemClock.sleep(1000);
        if (r.getVal().equals("null")) {
            assertEquals(null,
                    Settings.System.getString(mResolver, settingName));
        } else if (r.getName().contains("Hangouts")) {
            assertEquals("content://media/external/audio/media/" + r.getVal(),
                    Settings.System.getString(mResolver, settingName));
        } else {
            assertEquals("content://media/internal/audio/media/" + r.getVal(),
                    Settings.System.getString(mResolver, settingName));
        }
    }

    private enum ScrollDir {
        UP,
        DOWN,
        NOSCROLL
    }

    class RingtoneSetting {
        private final String mName;
        private final String mMediaVal;
        public RingtoneSetting(String name, String fname) {
            mName = name;
            mMediaVal = fname;
        }
        public String getName() {
            return mName;
        }
        public String getVal() {
            return mMediaVal;
        }
    }

    private void clickMore() throws InterruptedException {
        UiObject2 more = mDevice.wait(Until.findObject(By.text("Advanced")), TIMEOUT);
        if (more != null) {
            more.click();
            Thread.sleep(TIMEOUT);
        }
    }

    private UiObject2 findRingtoneInList(String ringtone) throws Exception {
        mHelper.scrollVert(false);
        SystemClock.sleep(1000);
        UiObject2 ringToneObject = mDevice.wait(Until.findObject(By.text(ringtone)), TIMEOUT);
        int count = 0;
        while (ringToneObject == null && count < 5) {
            mHelper.scrollVert(true);
            SystemClock.sleep(1000);
            ringToneObject = mDevice.wait(Until.findObject(By.text(ringtone)), TIMEOUT);
            count++;
        }
        return ringToneObject;
    }
}
