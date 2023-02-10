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

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller for flash notifications.
 */
public class FlashNotificationsPreferenceController extends BasePreferenceController {

    public FlashNotificationsPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final int res;

        switch (FlashNotificationsUtil.getFlashNotificationsState(mContext)) {
            case FlashNotificationsUtil.State.CAMERA:
                res = R.string.flash_notifications_summary_on_camera;
                break;
            case FlashNotificationsUtil.State.SCREEN:
                res = R.string.flash_notifications_summary_on_screen;
                break;
            case FlashNotificationsUtil.State.CAMERA_SCREEN:
                res = R.string.flash_notifications_summary_on_camera_and_screen;
                break;
            case FlashNotificationsUtil.State.OFF:
            default:
                res = R.string.flash_notifications_summary_off;
                break;
        }

        return mContext.getString(res);
    }
}
