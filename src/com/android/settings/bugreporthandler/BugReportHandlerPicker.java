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

package com.android.settings.bugreporthandler;

import static android.provider.Settings.ACTION_BUGREPORT_HANDLER_SETTINGS;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Picker for BugReportHandler.
 */
public class BugReportHandlerPicker extends DefaultAppPickerFragment {
    private static final String TAG = "BugReportHandlerPicker";

    private BugReportHandlerUtil mBugReportHandlerUtil;
    private FooterPreference mFooter;

    private static String getHandlerApp(String key) {
        int index = key.lastIndexOf('#');
        String handlerApp = key.substring(0, index);
        return handlerApp;
    }

    private static int getHandlerUser(String key) {
        int index = key.lastIndexOf('#');
        int handlerUser = 0;
        try {
            handlerUser = Integer.parseInt(key.substring(index + 1));
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Failed to get handlerUser");
        }
        return handlerUser;
    }

    @VisibleForTesting
    static String getKey(String handlerApp, int handlerUser) {
        return handlerApp + "#" + handlerUser;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context)) {
            getActivity().finish();
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bug_report_handler_settings;
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        if (mFooter == null) {
            mFooter = new FooterPreference(screen.getContext());
            mFooter.setIcon(R.drawable.ic_info_outline_24dp);
            mFooter.setSingleLineTitle(false);
            mFooter.setTitle(R.string.bug_report_handler_picker_footer_text);
            mFooter.setSelectable(false);
        }
        screen.addPreference(mFooter);
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final Context context = getContext();
        final List<Pair<ApplicationInfo, Integer>> validBugReportHandlerInfos =
                getBugReportHandlerUtil().getValidBugReportHandlerInfos(context);
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        for (Pair<ApplicationInfo, Integer> info : validBugReportHandlerInfos) {
            candidates.add(createDefaultAppInfo(context, mPm, info.second, info.first));
        }
        return candidates;
    }

    private BugReportHandlerUtil getBugReportHandlerUtil() {
        if (mBugReportHandlerUtil == null) {
            setBugReportHandlerUtil(createDefaultBugReportHandlerUtil());
        }
        return mBugReportHandlerUtil;
    }

    @VisibleForTesting
    void setBugReportHandlerUtil(BugReportHandlerUtil bugReportHandlerUtil) {
        mBugReportHandlerUtil = bugReportHandlerUtil;
    }

    @VisibleForTesting
    BugReportHandlerUtil createDefaultBugReportHandlerUtil() {
        return new BugReportHandlerUtil();
    }

    @Override
    protected String getDefaultKey() {
        final Pair<String, Integer> pair =
                getBugReportHandlerUtil().getCurrentBugReportHandlerAppAndUser(getContext());
        return getKey(pair.first, pair.second);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        return getBugReportHandlerUtil().setCurrentBugReportHandlerAppAndUser(getContext(),
                getHandlerApp(key),
                getHandlerUser(key));
    }

    @Override
    protected void onSelectionPerformed(boolean success) {
        super.onSelectionPerformed(success);
        if (success) {
            final Activity activity = getActivity();
            final Intent intent = activity == null ? null : activity.getIntent();
            if (intent != null && ACTION_BUGREPORT_HANDLER_SETTINGS.equals(intent.getAction())) {
                // If this was started through ACTION_BUGREPORT_HANDLER_SETTINGS then return once
                // we have chosen a new handler.
                getActivity().finish();
            }
        } else {
            getBugReportHandlerUtil().showInvalidChoiceToast(getContext());
            updateCandidates();
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_BUGREPORT_HANDLER;
    }


    @Override
    public void bindPreferenceExtra(RadioButtonPreference pref,
            String key, CandidateInfo info, String defaultKey, String systemDefaultKey) {
        super.bindPreferenceExtra(pref, key, info, defaultKey, systemDefaultKey);
        pref.setAppendixVisibility(View.GONE);
    }

    @VisibleForTesting
    DefaultAppInfo createDefaultAppInfo(Context context, PackageManager pm, int userId,
            PackageItemInfo packageItemInfo) {
        return new BugreportHandlerAppInfo(context, pm, userId, packageItemInfo,
                getDescription(packageItemInfo.packageName, userId));
    }

    private String getDescription(String handlerApp, int handlerUser) {
        final Context context = getContext();
        if (BugReportHandlerUtil.SHELL_APP_PACKAGE.equals(handlerApp)) {
            return context.getString(R.string.system_default_app_subtext);
        }
        if (mUserManager.getUserProfiles().size() < 2) {
            return "";
        }
        final UserInfo userInfo = mUserManager.getUserInfo(handlerUser);
        if (userInfo != null && userInfo.isManagedProfile()) {
            return context.getString(R.string.work_profile_app_subtext);
        }
        return context.getString(R.string.personal_profile_app_subtext);
    }

    private static class BugreportHandlerAppInfo extends DefaultAppInfo {
        private final Context mContext;

        BugreportHandlerAppInfo(Context context, PackageManager pm, int userId,
                PackageItemInfo packageItemInfo, String summary) {
            super(context, pm, userId, packageItemInfo, summary, true /* enabled */);
            mContext = context;
        }

        @Override
        public String getKey() {
            if (packageItemInfo != null) {
                return BugReportHandlerPicker.getKey(packageItemInfo.packageName, userId);
            } else {
                return null;
            }
        }

        @Override
        public CharSequence loadLabel() {
            if (mContext == null || packageItemInfo == null) {
                return null;
            }
            if (BugReportHandlerUtil.SHELL_APP_PACKAGE.equals(packageItemInfo.packageName)) {
                return mContext.getString(com.android.internal.R.string.android_system_label);
            }
            return super.loadLabel();
        }
    }
}
