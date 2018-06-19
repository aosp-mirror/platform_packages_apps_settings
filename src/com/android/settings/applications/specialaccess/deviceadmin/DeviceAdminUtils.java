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

package com.android.settings.applications.specialaccess.deviceadmin;

import android.app.admin.DeviceAdminInfo;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.UserHandle;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class DeviceAdminUtils {

    private static final String TAG = "DeviceAdminUtils";

    /**
     * Creates a device admin info object for the resolved intent that points to the component of
     * the device admin.
     *
     * @param ai ActivityInfo for the admin component.
     * @return new {@link DeviceAdminInfo} object or null if there was an error.
     */
    public static DeviceAdminInfo createDeviceAdminInfo(Context context, ActivityInfo ai) {
        try {
            return new DeviceAdminInfo(context, ai);
        } catch (XmlPullParserException | IOException e) {
            Log.w(TAG, "Skipping " + ai, e);
        }
        return null;
    }

    /**
     * Extracts the user id from a device admin info object.
     *
     * @param adminInfo the device administrator info.
     * @return identifier of the user associated with the device admin.
     */
    public static int getUserIdFromDeviceAdminInfo(DeviceAdminInfo adminInfo) {
        return UserHandle.getUserId(adminInfo.getActivityInfo().applicationInfo.uid);
    }
}
