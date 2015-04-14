/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.SmsApplication.SmsApplicationData;
import com.android.settings.AppListPreference;

import java.util.Collection;
import java.util.Objects;

public class DefaultSmsPreference extends AppListPreference {

    public DefaultSmsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        loadSmsApps();
    }

    private void loadSmsApps() {
        Collection<SmsApplicationData> smsApplications =
                SmsApplication.getApplicationCollection(getContext());

        int count = smsApplications.size();
        String[] packageNames = new String[count];
        int i = 0;
        for (SmsApplicationData smsApplicationData : smsApplications) {
            packageNames[i++] = smsApplicationData.mPackageName;
        }
        setPackageNames(packageNames, getDefaultPackage());
    }

    private String getDefaultPackage() {
        ComponentName appName = SmsApplication.getDefaultSmsApplication(getContext(), true);
        if (appName != null) {
            return appName.getPackageName();
        }
        return null;
    }

    @Override
    protected boolean persistString(String value) {
        if (!TextUtils.isEmpty(value) && !Objects.equals(value, getDefaultPackage())) {
            SmsApplication.setDefaultApplication(value, getContext());
        }
        setSummary(getEntry());
        return true;
    }

    public static boolean isAvailable(Context context) {
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.isSmsCapable();
    }

}
