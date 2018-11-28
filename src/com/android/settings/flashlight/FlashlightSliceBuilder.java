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

package com.android.settings.flashlight;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static androidx.slice.builders.ListBuilder.ICON_IMAGE;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SliceBroadcastReceiver;


/**
 * Utility class to build a Flashlight Slice, and handle all associated actions.
 */
public class FlashlightSliceBuilder {

    private static final String TAG = "FlashlightSliceBuilder";

    /**
     * Action notifying a change on the Flashlight Slice.
     */
    public static final String ACTION_FLASHLIGHT_SLICE_CHANGED =
            "com.android.settings.flashlight.action.FLASHLIGHT_SLICE_CHANGED";

    /**
     * Action broadcasting a change on whether flashlight is on or off.
     */
    public static final String ACTION_FLASHLIGHT_CHANGED =
            "com.android.settings.flashlight.action.FLASHLIGHT_CHANGED";

    public static final IntentFilter INTENT_FILTER = new IntentFilter(ACTION_FLASHLIGHT_CHANGED);

    private FlashlightSliceBuilder() {
    }

    public static Slice getSlice(Context context) {
        if (!isFlashlightAvailable(context)) {
            return null;
        }
        final PendingIntent toggleAction = getBroadcastIntent(context);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(context);
        final IconCompat icon =
                IconCompat.createWithResource(context, R.drawable.ic_signal_flashlight);
        return new ListBuilder(context, CustomSliceRegistry.FLASHLIGHT_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new RowBuilder()
                        .setTitle(context.getText(R.string.power_flashlight))
                        .setTitleItem(icon, ICON_IMAGE)
                        .setPrimaryAction(
                                new SliceAction(toggleAction, null, isFlashlightEnabled(context))))
                .build();
    }

    /**
     * Update the current flashlight status to the boolean value keyed by
     * {@link android.app.slice.Slice#EXTRA_TOGGLE_STATE} on {@param intent}.
     */
    public static void handleUriChange(Context context, Intent intent) {
        try {
            final String cameraId = getCameraId(context);
            if (cameraId != null) {
                final boolean state = intent.getBooleanExtra(
                        EXTRA_TOGGLE_STATE, isFlashlightEnabled(context));
                final CameraManager cameraManager = context.getSystemService(CameraManager.class);
                cameraManager.setTorchMode(cameraId, state);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera couldn't set torch mode.", e);
        }
        context.getContentResolver().notifyChange(CustomSliceRegistry.FLASHLIGHT_SLICE_URI, null);
    }

    private static String getCameraId(Context context) throws CameraAccessException {
        final CameraManager cameraManager = context.getSystemService(CameraManager.class);
        final String[] ids = cameraManager.getCameraIdList();
        for (String id : ids) {
            CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
            Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
            if (flashAvailable != null && flashAvailable
                    && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return null;
    }

    private static PendingIntent getBroadcastIntent(Context context) {
        final Intent intent = new Intent(ACTION_FLASHLIGHT_SLICE_CHANGED);
        intent.setClass(context, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static boolean isFlashlightAvailable(Context context) {
        return Settings.Secure.getInt(
                context.getContentResolver(), Secure.FLASHLIGHT_AVAILABLE, 0) == 1;
    }

    private static boolean isFlashlightEnabled(Context context) {
        return Settings.Secure.getInt(
                context.getContentResolver(), Secure.FLASHLIGHT_ENABLED, 0) == 1;
    }
}
