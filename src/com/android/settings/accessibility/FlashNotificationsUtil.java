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

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class FlashNotificationsUtil {
    static final String LOG_TAG = "FlashNotificationsUtil";
    static final String ACTION_FLASH_NOTIFICATION_START_PREVIEW =
            "com.android.internal.intent.action.FLASH_NOTIFICATION_START_PREVIEW";
    static final String ACTION_FLASH_NOTIFICATION_STOP_PREVIEW =
            "com.android.internal.intent.action.FLASH_NOTIFICATION_STOP_PREVIEW";
    static final String EXTRA_FLASH_NOTIFICATION_PREVIEW_COLOR =
            "com.android.internal.intent.extra.FLASH_NOTIFICATION_PREVIEW_COLOR";
    static final String EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE =
            "com.android.internal.intent.extra.FLASH_NOTIFICATION_PREVIEW_TYPE";

    static final int TYPE_SHORT_PREVIEW = 0;
    static final int TYPE_LONG_PREVIEW = 1;

    static final int DEFAULT_SCREEN_FLASH_COLOR = ScreenFlashNotificationColor.YELLOW.mColorInt;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            FlashNotificationsUtil.State.OFF,
            FlashNotificationsUtil.State.CAMERA,
            FlashNotificationsUtil.State.SCREEN,
            FlashNotificationsUtil.State.CAMERA_SCREEN,
    })
    @interface State {
        int OFF = 0;
        int CAMERA = 1;
        int SCREEN = 2;
        int CAMERA_SCREEN = 3;
    }

    static boolean isTorchAvailable(@NonNull Context context) {
        // TODO This is duplicated logic of FlashNotificationsController.getCameraId.
        final CameraManager cameraManager = context.getSystemService(CameraManager.class);
        if (cameraManager == null) return false;

        try {
            final String[] ids = cameraManager.getCameraIdList();

            for (String id : ids) {
                final CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);

                final Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (flashAvailable == null) continue;

                final Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing == null) continue;

                if (flashAvailable && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return true;
                }
            }
        } catch (CameraAccessException ignored) {
            Log.w(LOG_TAG, "Failed to get valid camera for camera flash notification.");
        }
        return false;
    }

    static ScreenFlashNotificationColor getScreenColor(@ColorInt int colorInt)
            throws ScreenColorNotFoundException {

        colorInt |= ScreenFlashNotificationColor.ALPHA_MASK;
        for (ScreenFlashNotificationColor color : ScreenFlashNotificationColor.values()) {
            if (colorInt == color.mOpaqueColorInt) {
                return color;
            }
        }

        throw new ScreenColorNotFoundException();
    }

    @NonNull
    static String getColorDescriptionText(@NonNull Context context, @ColorInt int color) {
        try {
            return context.getString(getScreenColor(color).mStringRes);
        } catch (ScreenColorNotFoundException e) {
            return "";
        }
    }

    @State
    static int getFlashNotificationsState(Context context) {
        if (context == null) {
            return State.OFF;
        }

        final boolean isTorchAvailable = FlashNotificationsUtil.isTorchAvailable(context);
        final boolean isCameraFlashEnabled = Settings.System.getInt(context.getContentResolver(),
                Settings.System.CAMERA_FLASH_NOTIFICATION, State.OFF) != State.OFF;
        final boolean isScreenFlashEnabled = Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_FLASH_NOTIFICATION, State.OFF) != State.OFF;

        return ((isTorchAvailable && isCameraFlashEnabled) ? State.CAMERA : State.OFF)
                | (isScreenFlashEnabled ? State.SCREEN : State.OFF);
    }

    static class ScreenColorNotFoundException extends Exception {
    }
}
