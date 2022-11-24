/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.view;

import android.content.Context;
import android.content.Intent;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.biometrics2.utils.LockScreenUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FingerprintEnrollmentActivityTest {

    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String ACTIVITY_CLASS_NAME =
            "com.android.settings.biometrics2.ui.view.FingerprintEnrollmentActivity";
    private static final String EXTRA_FROM_SETTINGS_SUMMARY = "from_settings_summary";
    private static final String EXTRA_PAGE_TRANSITION_TYPE = "page_transition_type";
    private static final String EXTRA_KEY_CHALLENGE_TOKEN = "hw_auth_token";

    private UiDevice mDevice;
    private byte[] mToken = new byte[]{};
    private Context mContext;

    private static final int IDLE_TIMEOUT = 10000;

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mContext = InstrumentationRegistry.getContext();
        mDevice.pressHome();
    }

    @Test
    public void lunchWithoutCredential() {
        launchFingerprintEnrollActivity(true);
        Assert.assertNotNull(mDevice.wait(Until.hasObject(
                By.text("Choose your backup screen lock method")), IDLE_TIMEOUT));
    }

    @Test
    public void lunchWithCredential() {
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, "1234", true);
        launchFingerprintEnrollActivity(true);
        Assert.assertNotNull(mDevice.wait(Until.hasObject(
                By.text("More")), IDLE_TIMEOUT));

        //click more btn twice and the introduction should stay in the last page
        UiObject2 moreBtn = mDevice.findObject(By.text("More"));
        moreBtn.click();
        Assert.assertNotNull(mDevice.wait(Until.hasObject(
                By.text("More")), IDLE_TIMEOUT));
        moreBtn = mDevice.findObject(By.text("More"));
        moreBtn.click();

        Assert.assertNotNull(mDevice.wait(Until.hasObject(
                By.text("I agree")), IDLE_TIMEOUT));
        Assert.assertNotNull(mDevice.wait(Until.hasObject(
                By.text("No thanks")), IDLE_TIMEOUT));

        LockScreenUtil.resetLockscreen("1234");
    }

    @Test
    public void launchCheckPin() {
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, "1234", true);
        launchFingerprintEnrollActivity(false);
        Assert.assertNotNull(mDevice.wait(Until.hasObject(
                By.text("Enter your device PIN to continue")), IDLE_TIMEOUT));
        LockScreenUtil.resetLockscreen("1234");
    }

    @After
    public void tearDown() throws Exception {
        LockScreenUtil.resetLockscreen("1234");
        mDevice.pressHome();
    }

    private void launchFingerprintEnrollActivity(boolean hasToken) {
        Intent intent = new Intent();
        intent.setClassName(SETTINGS_PACKAGE_NAME, ACTIVITY_CLASS_NAME);
        intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, true);
        intent.putExtra(EXTRA_PAGE_TRANSITION_TYPE, 1);
        intent.putExtra(Intent.EXTRA_USER_ID, mContext.getUserId());
        if (hasToken) {
            intent.putExtra(EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

}
