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

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility methods related to BugReportHandler.
 */
public class BugReportHandlerUtil {
    private static final String TAG = "BugReportHandlerUtil";
    private static final String INTENT_BUGREPORT_REQUESTED =
            "com.android.internal.intent.action.BUGREPORT_REQUESTED";

    public static final String SHELL_APP_PACKAGE = "com.android.shell";

    public BugReportHandlerUtil() {
    }

    /**
     * Check is BugReportHandler enabled on the device.
     *
     * @param context Context
     * @return true if BugReportHandler is enabled, or false otherwise
     */
    public boolean isBugReportHandlerEnabled(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_bugReportHandlerEnabled);
    }

    /**
     * Fetch the package name currently used as bug report handler app and the user.
     *
     * @param context Context
     * @return a pair of two values, first one is the current bug report handler app, second is
     * the user.
     */
    public Pair<String, Integer> getCurrentBugReportHandlerAppAndUser(Context context) {

        String handlerApp = getCustomBugReportHandlerApp(context);
        int handlerUser = getCustomBugReportHandlerUser(context);

        boolean needToResetOutdatedSettings = false;
        if (!isBugreportWhitelistedApp(handlerApp)) {
            handlerApp = getDefaultBugReportHandlerApp(context);
            handlerUser = UserHandle.USER_SYSTEM;
        } else if (getBugReportHandlerAppReceivers(context, handlerApp, handlerUser).isEmpty()) {
            // It looks like the settings are outdated, need to reset outdated settings.
            //
            // i.e.
            // If user chooses which profile and which bugreport-whitelisted app in that
            // profile to handle a bugreport, then user remove the profile.
            // === RESULT ===
            // The chosen bugreport handler app is outdated because the profile is removed,
            // so need to reset outdated settings
            handlerApp = getDefaultBugReportHandlerApp(context);
            handlerUser = UserHandle.USER_SYSTEM;
            needToResetOutdatedSettings = true;
        }

        if (!isBugreportWhitelistedApp(handlerApp)
                || getBugReportHandlerAppReceivers(context, handlerApp, handlerUser).isEmpty()) {
            // It looks like current handler app may be too old and doesn't support to handle a
            // bugreport, so change to let shell to handle a bugreport and need to reset
            // settings.
            handlerApp = SHELL_APP_PACKAGE;
            handlerUser = UserHandle.USER_SYSTEM;
            needToResetOutdatedSettings = true;
        }

        if (needToResetOutdatedSettings) {
            setBugreportHandlerAppAndUser(context, handlerApp, handlerUser);
        }

        return Pair.create(handlerApp, handlerUser);
    }

    private String getCustomBugReportHandlerApp(Context context) {
        // Get the package of custom bugreport handler app
        return Settings.Global.getString(context.getContentResolver(),
                Settings.Global.CUSTOM_BUGREPORT_HANDLER_APP);
    }

    private int getCustomBugReportHandlerUser(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.CUSTOM_BUGREPORT_HANDLER_USER, UserHandle.USER_NULL);
    }

    private String getDefaultBugReportHandlerApp(Context context) {
        return context.getResources().getString(
                com.android.internal.R.string.config_defaultBugReportHandlerApp);
    }

    /**
     * Change current bug report handler app and user.
     *
     * @param context     Context
     * @param handlerApp  the package name of the handler app
     * @param handlerUser the id of the handler user
     * @return whether the change succeeded
     */
    public boolean setCurrentBugReportHandlerAppAndUser(Context context, String handlerApp,
            int handlerUser) {
        if (!isBugreportWhitelistedApp(handlerApp)) {
            return false;
        } else if (getBugReportHandlerAppReceivers(context, handlerApp, handlerUser).isEmpty()) {
            return false;
        }
        setBugreportHandlerAppAndUser(context, handlerApp, handlerUser);
        return true;
    }

    /**
     * Fetches ApplicationInfo objects and user ids for all currently valid BugReportHandler.
     * A BugReportHandler is considered valid if it can receive BUGREPORT_REQUESTED intent.
     *
     * @param context Context
     * @return pair objects for all currently valid BugReportHandler packages and user id
     */
    public List<Pair<ApplicationInfo, Integer>> getValidBugReportHandlerInfos(Context context) {
        final List<Pair<ApplicationInfo, Integer>> validBugReportHandlerApplicationInfos =
                new ArrayList<>();
        List<String> bugreportWhitelistedPackages;
        try {
            bugreportWhitelistedPackages =
                    ActivityManager.getService().getBugreportWhitelistedPackages();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get bugreportWhitelistedPackages:", e);
            return validBugReportHandlerApplicationInfos;
        }

        // Add "Shell with system user" as System default preference on top of screen
        if (bugreportWhitelistedPackages.contains(SHELL_APP_PACKAGE)
                && !getBugReportHandlerAppReceivers(context, SHELL_APP_PACKAGE,
                UserHandle.USER_SYSTEM).isEmpty()) {
            try {
                validBugReportHandlerApplicationInfos.add(
                        Pair.create(
                                context.getPackageManager().getApplicationInfo(SHELL_APP_PACKAGE,
                                        PackageManager.MATCH_ANY_USER), UserHandle.USER_SYSTEM)
                );
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        final UserManager userManager = context.getSystemService(UserManager.class);
        final List<UserInfo> profileList = userManager.getProfiles(UserHandle.getCallingUserId());
        // Only add non-Shell app as normal preference
        final List<String> nonShellPackageList = bugreportWhitelistedPackages.stream()
                .filter(pkg -> !SHELL_APP_PACKAGE.equals(pkg)).collect(Collectors.toList());
        Collections.sort(nonShellPackageList);
        for (String pkg : nonShellPackageList) {
            for (UserInfo profile : profileList) {
                final int userId = profile.getUserHandle().getIdentifier();
                if (getBugReportHandlerAppReceivers(context, pkg, userId).isEmpty()) {
                    continue;
                }
                try {
                    validBugReportHandlerApplicationInfos.add(
                            Pair.create(context.getPackageManager()
                                            .getApplicationInfo(pkg, PackageManager.MATCH_ANY_USER),
                                    userId));
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        return validBugReportHandlerApplicationInfos;
    }

    private boolean isBugreportWhitelistedApp(String app) {
        // Verify the app is bugreport-whitelisted
        if (TextUtils.isEmpty(app)) {
            return false;
        }
        try {
            return ActivityManager.getService().getBugreportWhitelistedPackages().contains(app);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get bugreportWhitelistedPackages:", e);
            return false;
        }
    }

    private List<ResolveInfo> getBugReportHandlerAppReceivers(Context context, String handlerApp,
            int handlerUser) {
        // Use the app package and the user id to retrieve the receiver that can handle a
        // broadcast of the intent.
        final Intent intent = new Intent(INTENT_BUGREPORT_REQUESTED);
        intent.setPackage(handlerApp);
        return context.getPackageManager()
                .queryBroadcastReceiversAsUser(intent, PackageManager.MATCH_SYSTEM_ONLY,
                        handlerUser);
    }

    private void setBugreportHandlerAppAndUser(Context context, String handlerApp,
            int handlerUser) {
        Settings.Global.putString(context.getContentResolver(),
                Settings.Global.CUSTOM_BUGREPORT_HANDLER_APP,
                handlerApp);
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.CUSTOM_BUGREPORT_HANDLER_USER, handlerUser);
    }

    /**
     * Show a toast to explain the chosen bug report handler can no longer be chosen.
     */
    public void showInvalidChoiceToast(Context context) {
        final Toast toast = Toast.makeText(context,
                R.string.select_invalid_bug_report_handler_toast_text, Toast.LENGTH_SHORT);
        toast.show();
    }
}
