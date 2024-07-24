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

package com.android.settings.biometrics2.utils;


import static java.lang.String.format;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.Assert;

import java.io.IOException;
import java.util.function.Supplier;

public class LockScreenUtil {

    private static final String TAG = LockScreenUtil.class.getSimpleName();

    private static final int SLEEP_MS = 100;
    private static final int WAIT_TIME_MS = 10000;

    private static final String SET_PATTERN_COMMAND = "locksettings set-pattern";
    private static final String SET_PASSWORD_COMMAND = "locksettings set-password";
    private static final String SET_PIN_COMMAND = "locksettings set-pin";

    private static final String RESET_LOCKSCREEN_SHELL_COMMAND = "locksettings clear --old";

    /**
     * Different way to set the Lockscreen for Android device. Currently we only support PIN,
     * PATTERN and PASSWORD
     *
     * @param lockscreenType it enum with list of supported lockscreen type
     * @param lockscreenCode code[PIN or PATTERN or PASSWORD] which needs to be set.
     * @param expectedResult expected result after setting the lockscreen because for lock type
     *                       Swipe and None Keygaurd#isKeyguardSecure remain unlocked i.e. false
     */
    public static void setLockscreen(LockscreenType lockscreenType, String lockscreenCode,
            boolean expectedResult) {
        Log.d(TAG, format("Setting Lockscreen [%s(%s)]", lockscreenType, lockscreenCode));
        switch (lockscreenType) {
            case PIN:
                executeShellCommand(format("%s %s", SET_PIN_COMMAND, lockscreenCode));
                break;
            case PASSWORD:
                executeShellCommand(format("%s %s", SET_PASSWORD_COMMAND, lockscreenCode));
                break;
            case PATTERN:
                executeShellCommand(format("%s %s", SET_PATTERN_COMMAND, lockscreenCode));
                break;
            default:
                throw new AssertionError("Non-supported Lockscreen Type: " + lockscreenType);
        }
        assertKeyguardSecure(expectedResult);
    }

    /**
     * Resets the give lockscreen.
     *
     * @param lockscreenCode old code which is currently set.
     */
    public static void resetLockscreen(String lockscreenCode) {
        Log.d(TAG, String.format("Re-Setting Lockscreen %s", lockscreenCode));
        executeShellCommand(
                format("%s %s", RESET_LOCKSCREEN_SHELL_COMMAND, lockscreenCode));
        assertKeyguardSecure(/* expectedSecure= */ false);
    }


    /**
     * This method help you execute you shell command.
     * Example: adb shell pm list packages -f
     * Here you just need to provide executeShellCommand("pm list packages -f")
     *
     * @param command command need to executed.
     */
    private static void executeShellCommand(String command) {
        Log.d(TAG, format("Executing Shell Command: %s", command));
        try {
            getUiDevice().executeShellCommand(command);
        } catch (IOException e) {
            Log.d(TAG, format("IOException Occurred: %s", e));
        }
    }

    /**
     * Enum for different types of Lockscreen, PIN, PATTERN and PASSWORD.
     */
    public enum LockscreenType {
        PIN,
        PASSWORD,
        PATTERN
    }

    private static UiDevice getUiDevice() {
        return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    private static void assertKeyguardSecure(boolean expectedSecure) {
        waitForCondition(
                () -> String.format("Assert that keyguard %s secure, but failed.",
                        expectedSecure ? "is" : "isn't"),
                () -> getKeyguardManager().isKeyguardSecure() == expectedSecure);
    }

    /**
     * Waits for a condition and fails if it doesn't become true within 10 sec.
     *
     * @param message   Supplier of the error message.
     * @param condition Condition.
     */
    private static void waitForCondition(
            Supplier<String> message, Condition condition) {
        waitForCondition(message, condition, WAIT_TIME_MS);
    }

    /**
     * Waits for a condition and fails if it doesn't become true within specified time period.
     *
     * @param message   Supplier of the error message.
     * @param condition Condition.
     * @param timeoutMs Timeout.
     */
    private static void waitForCondition(
            Supplier<String> message, Condition condition, long timeoutMs) {
        final long startTime = SystemClock.uptimeMillis();
        while (SystemClock.uptimeMillis() < startTime + timeoutMs) {
            try {
                if (condition.isTrue()) {
                    return;
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            SystemClock.sleep(SLEEP_MS);
        }

        // Check once more before failing.
        try {
            if (condition.isTrue()) {
                return;
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        Assert.fail(message.get());
    }

    /**
     * To get an instance of class that can be used to lock and unlock the keygaurd.
     *
     * @return an instance of class that can be used to lock and unlock the screen.
     */
    private static KeyguardManager getKeyguardManager() {
        return (KeyguardManager) InstrumentationRegistry.getContext().getSystemService(
                Context.KEYGUARD_SERVICE);
    }

    /** Supplier of a boolean that can throw an exception. */
    private interface Condition {
        boolean isTrue() throws Throwable;
    }
}
