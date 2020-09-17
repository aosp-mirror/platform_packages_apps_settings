/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.bugreporthandler.BugReportHandlerUtil;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * PreferenceController for BugReportHandler
 */
public class BugReportHandlerPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_BUG_REPORT_HANDLER = "bug_report_handler";

    private final UserManager mUserManager;
    private final BugReportHandlerUtil mBugReportHandlerUtil;

    public BugReportHandlerPreferenceController(Context context) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mBugReportHandlerUtil = new BugReportHandlerUtil();
    }

    @Override
    public boolean isAvailable() {
        return !mUserManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)
                && mBugReportHandlerUtil.isBugReportHandlerEnabled(mContext);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BUG_REPORT_HANDLER;
    }

    @Override
    public void updateState(Preference preference) {
        final CharSequence currentBugReportHandlerAppLabel = getCurrentBugReportHandlerAppLabel();
        if (!TextUtils.isEmpty(currentBugReportHandlerAppLabel)) {
            mPreference.setSummary(currentBugReportHandlerAppLabel);
        } else {
            mPreference.setSummary(R.string.app_list_preference_none);
        }
    }

    @VisibleForTesting
    CharSequence getCurrentBugReportHandlerAppLabel() {
        final String handlerApp = mBugReportHandlerUtil.getCurrentBugReportHandlerAppAndUser(
                mContext).first;
        if (BugReportHandlerUtil.SHELL_APP_PACKAGE.equals(handlerApp)) {
            return mContext.getString(com.android.internal.R.string.android_system_label);
        }
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = mContext.getPackageManager().getApplicationInfo(handlerApp,
                    PackageManager.MATCH_ANY_USER);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        return applicationInfo.loadLabel(mContext.getPackageManager());
    }
}
