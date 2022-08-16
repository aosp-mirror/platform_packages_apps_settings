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

package com.android.settings.testutils;

import android.os.ParcelFileDescriptor;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class AdbUtils {
    public static String getCallerClassName() {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            if (!ste.getClassName().equals(new Object() {
            }.getClass().getEnclosingClass().getName()) && ste.getClassName().indexOf(
                    "java.lang.Thread") != 0) {
                return ste.getClassName();
            }
        }
        return null;
    }

    public static boolean checkStringInAdbCommandOutput(String logTag, String command,
            String prefix, String target, int timeoutInMillis) throws Exception {
        long start = System.nanoTime();

        //Sometimes the change do no reflect in adn output immediately, so need a wait and poll here
        while (System.nanoTime() - start < (timeoutInMillis * 1000000)) {
            String result = shell(command);
            if (result.contains(prefix == null ? "" : prefix)
                    && result.contains(target == null ? "" : target)) {
                return true;
            } else {
                Thread.sleep(100);
            }
        }

        return false;
    }

    public static String shell(String shellCommand) {
        String returnValue = "";
        try (ParcelFileDescriptor.AutoCloseInputStream in =
                     new ParcelFileDescriptor.AutoCloseInputStream(
                             InstrumentationRegistry.getInstrumentation()
                                     .getUiAutomation()
                                     .executeShellCommand(shellCommand))) {
            try (BufferedReader br =
                         new BufferedReader(
                                 new InputStreamReader(in, StandardCharsets.UTF_8))) {
                returnValue = br.lines().collect(Collectors.joining());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnValue;
    }
}
