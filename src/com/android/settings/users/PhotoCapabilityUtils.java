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

package com.android.settings.users;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.MediaStore;

class PhotoCapabilityUtils {

    /**
     * Check if the current user can perform any activity for
     * android.media.action.IMAGE_CAPTURE action.
     */
    static boolean canTakePhoto(Context context) {
        return context.getPackageManager().queryIntentActivities(
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    /**
     * Check if the current user can perform any activity for
     * android.intent.action.GET_CONTENT action for images.
     */
    static boolean canChoosePhoto(Context context) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        return context.getPackageManager().queryIntentActivities(intent, 0).size() > 0;
    }

    /**
     * Check if the current user can perform any activity for
     * com.android.camera.action.CROP action for images.
     */
    static boolean canCropPhoto(Context context) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
        return context.getPackageManager().queryIntentActivities(intent, 0).size() > 0;
    }

}
