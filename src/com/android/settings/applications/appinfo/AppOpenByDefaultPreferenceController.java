/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.ServiceManager;
import android.os.UserHandle;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppLaunchSettings;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;

public class AppOpenByDefaultPreferenceController extends AppInfoPreferenceControllerBase {

    private IUsbManager mUsbManager;
    private PackageManager mPackageManager;
    private String mPackageName;

    public AppOpenByDefaultPreferenceController(Context context, String key) {
        super(context, key);
        mUsbManager = IUsbManager.Stub.asInterface(ServiceManager.getService(Context.USB_SERVICE));
        mPackageManager = context.getPackageManager();
    }

    /** Set a package name for this controller. */
    public AppOpenByDefaultPreferenceController setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final ApplicationsState.AppEntry appEntry = mParent.getAppEntry();
        if (appEntry == null || appEntry.info == null) {
            mPreference.setEnabled(false);
        } else if ((appEntry.info.flags & ApplicationInfo.FLAG_INSTALLED) == 0
                || !appEntry.info.enabled) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    public void updateState(Preference preference) {
        final PackageInfo packageInfo = mParent.getPackageInfo();
        if (packageInfo != null && !AppUtils.isInstant(packageInfo.applicationInfo)
                && !AppUtils.isBrowserApp(mContext, packageInfo.packageName,
                UserHandle.myUserId())) {
            preference.setVisible(true);
            preference.setSummary(AppUtils.getLaunchByDefaultSummary(mParent.getAppEntry(),
                    mUsbManager, mPackageManager, mContext));
        } else {
            preference.setVisible(false);
        }
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppLaunchSettings.class;
    }
}
