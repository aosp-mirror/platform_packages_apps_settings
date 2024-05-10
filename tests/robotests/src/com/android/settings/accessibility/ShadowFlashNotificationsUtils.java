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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.FlashNotificationsUtil.State;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(FlashNotificationsUtil.class)
public class ShadowFlashNotificationsUtils {

    private static boolean sIsTorchAvailable;
    private static int sState;
    private static String sColorDescriptionText = "";

    public static void setIsTorchAvailable(boolean isTorchAvailable) {
        sIsTorchAvailable = isTorchAvailable;
    }

    @Implementation
    protected static boolean isTorchAvailable(Context context) {
        return sIsTorchAvailable;
    }

    public static void setFlashNotificationsState(@State int state) {
        sState = state;
    }

    @State
    @Implementation
    protected static int getFlashNotificationsState(Context context) {
        return sState;
    }

    public static void setColorDescriptionText(@NonNull String text) {
        sColorDescriptionText = text;
    }

    @Implementation
    @NonNull
    protected static String getColorDescriptionText(@NonNull Context context, @ColorInt int color) {
        return sColorDescriptionText;
    }

    @Resetter
    public static void reset() {
        sIsTorchAvailable = false;
        sState = 0;
        sColorDescriptionText = "";
    }
}
