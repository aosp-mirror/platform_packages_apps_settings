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
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;

class DeviceAdminListItem implements Comparable<DeviceAdminListItem> {

    private static final String TAG = "DeviceAdminListItem";

    private final UserHandle mUserHandle;
    private final String mKey;
    private final DeviceAdminInfo mInfo;
    private final CharSequence mName;
    private final Drawable mIcon;
    private final DevicePolicyManager mDPM;
    private CharSequence mDescription;

    public DeviceAdminListItem(Context context, DeviceAdminInfo info) {
        mInfo = info;
        mUserHandle = new UserHandle(getUserIdFromDeviceAdminInfo(mInfo));
        mKey = mUserHandle.getIdentifier() + "@" + mInfo.getComponent().flattenToString();
        mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final PackageManager pm = context.getPackageManager();
        mName = mInfo.loadLabel(pm);
        try {
            mDescription = mInfo.loadDescription(pm);
        } catch (Resources.NotFoundException exception) {
            Log.w(TAG, "Setting description to null because can't find resource: " + mKey);
        }
        mIcon = pm.getUserBadgedIcon(mInfo.loadIcon(pm), mUserHandle);
    }

    @Override
    public int compareTo(DeviceAdminListItem other) {
        return this.mName.toString().compareTo(other.mName.toString());
    }

    public String getKey() {
        return mKey;
    }

    public CharSequence getName() {
        return mName;
    }

    public CharSequence getDescription() {
        return mDescription;
    }

    public boolean isActive() {
        return mDPM.isAdminActiveAsUser(mInfo.getComponent(), getUserIdFromDeviceAdminInfo(mInfo));
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public boolean isEnabled() {
        return !mDPM.isRemovingAdmin(mInfo.getComponent(), getUserIdFromDeviceAdminInfo(mInfo));
    }

    public UserHandle getUser() {
        return new UserHandle(getUserIdFromDeviceAdminInfo(mInfo));
    }

    public Intent getLaunchIntent(Context context) {
        return new Intent(context, DeviceAdminAdd.class)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mInfo.getComponent());
    }

    /**
     * Extracts the user id from a device admin info object.
     *
     * @param adminInfo the device administrator info.
     * @return identifier of the user associated with the device admin.
     */
    private static int getUserIdFromDeviceAdminInfo(DeviceAdminInfo adminInfo) {
        return UserHandle.getUserId(adminInfo.getActivityInfo().applicationInfo.uid);
    }
}
