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
import android.system.helpers.SettingsHelper;
import android.system.helpers.SettingsHelper.SettingsType;
import android.test.InstrumentationTestCase;

import androidx.test.filters.MediumTest;
import androidx.test.filters.Suppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Ignore;

import java.util.Map;

@Ignore
public class SoundSettingsTest extends InstrumentationTestCase {
    private static final String PAGE = Settings.ACTION_SOUND_SETTINGS;
    private static final int TIMEOUT = 2000;

    private UiDevice mDevice;
    private ContentResolver mResolver;
    private SettingsHelper mHelper;


    private final Map<String, String> ringtoneSounds = Map.of(
            "angler", "Dione",
            "bullhead", "Dione",
            "marlin", "Spaceship",
            "sailfish", "Spaceship",
            "walleye", "Copycat",
            "taimen", "Copycat");

    private final Map<String, String> ringtoneCodes = Map.of(
            "angler", "38",
            "bullhead", "38",
            "marlin", "37",
            "sailfish", "37",
            "walleye", "26",
            "taimen", "26");

    private final Map<String, String> alarmSounds = Map.of(
            "angler", "Awaken",
            "bullhead", "Awaken",
            "marlin", "Bounce",
            "sailfish", "Bounce",
            "walleye", "Cuckoo clock",
            "taimen", "Cuckoo clock");

    private final Map<String, String> alarmCodes = Map.of(
            "angler", "6",
            "bullhead", "6",
            "marlin", "49",
            "sailfish", "49",
            "walleye", "15",
            "taimen", "15");

    private final Map<String, String> notificationSounds = Map.of(
            "angler", "Ceres",
            "bullhead", "Ceres",
            "marlin", "Trill",
            "sailfish", "Trill",
            "walleye", "Pipes",
            "taimen", "Pipes");


    private final Map<String, String> notificationCodes = Map.of(
            "angler", "26",
            "bullhead", "26",
            "marlin", "57",
            "sailfish", "57",
            "walleye", "69",
            "taimen", "69");

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
