/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static android.content.pm.PackageManager.GET_META_DATA;
import static android.content.pm.PackageManager.GET_RESOLVED_FILTER;
import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.pm.UserInfo;
import android.os.Flags;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.Settings.CreateShortcutActivity;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.settings.homepage.DeepLinkHomepageActivity;
import com.android.settings.search.SearchStateReceiver;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Listens to {@link Intent.ACTION_PRE_BOOT_COMPLETED} and {@link Intent.ACTION_USER_INITIALIZED}
 * performs setup steps for a managed profile (disables the launcher icon of the Settings app,
 * adds cross-profile intent filters for the appropriate Settings activities), disables the
 * webview setting for non-admin users, updates the intent flags for any existing shortcuts and
 * enables DeepLinkHomepageActivity for large screen devices.
 */
public class SettingsInitialize extends BroadcastReceiver {
    private static final String TAG = "Settings";
    private static final String PRIMARY_PROFILE_SETTING =
            "com.android.settings.PRIMARY_PROFILE_CONTROLLED";
    private static final String WEBVIEW_IMPLEMENTATION_ACTIVITY = ".WebViewImplementation";

    @Override
    public void onReceive(Context context, Intent broadcast) {
        final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        UserInfo userInfo = um.getUserInfo(UserHandle.myUserId());
        final PackageManager pm = context.getPackageManager();
        managedProfileSetup(context, pm, broadcast, userInfo);
        cloneProfileSetup(context, pm, userInfo);
        privateProfileSetup(context, pm, userInfo);
        webviewSettingSetup(context, pm, userInfo);
        ThreadUtils.postOnBackgroundThread(() -> refreshExistingShortcuts(context));
        enableTwoPaneDeepLinkActivityIfNecessary(pm, context);
    }

    private void managedProfileSetup(Context context, final PackageManager pm, Intent broadcast,
            UserInfo userInfo) {
        if (userInfo == null || !userInfo.isManagedProfile()) {
            return;
        }
        Log.i(TAG, "Received broadcast: " + broadcast.getAction()
                + ". Setting up intent forwarding for managed profile.");
        // Clear any previous intent forwarding we set up
        pm.clearCrossProfileIntentFilters(userInfo.id);

        // Set up intent forwarding for implicit intents
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setPackage(context.getPackageName());

        // Resolves activities for the managed profile (which we're running as)
        List<ResolveInfo> resolvedIntents = pm.queryIntentActivities(intent,
                GET_ACTIVITIES | GET_META_DATA | GET_RESOLVED_FILTER | MATCH_DISABLED_COMPONENTS);
        final int count = resolvedIntents.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = resolvedIntents.get(i);
            if (info.filter != null && info.activityInfo != null
                    && info.activityInfo.metaData != null) {
                boolean shouldForward = info.activityInfo.metaData.getBoolean(
                        PRIMARY_PROFILE_SETTING);
                if (shouldForward) {
                    pm.addCrossProfileIntentFilter(info.filter, userInfo.id,
                            userInfo.profileGroupId, PackageManager.SKIP_CURRENT_PROFILE);
                }
            }
        }

        disableComponentsToHideSettings(context, pm);
    }

    private void cloneProfileSetup(Context context, PackageManager pm, UserInfo userInfo) {
        if (userInfo == null || !userInfo.isCloneProfile()) {
            return;
        }

        disableComponentsToHideSettings(context, pm);
    }

    private void privateProfileSetup(Context context, PackageManager pm, UserInfo userInfo) {
        if (Flags.allowPrivateProfile()) {
            if (userInfo == null || !userInfo.isPrivateProfile()) {
                return;
            }

            disableComponentsToHideSettings(context, pm);
        }
    }

    private void disableComponentsToHideSettings(Context context, PackageManager pm) {
        // Disable settings app launcher icon
        disableComponent(pm, new ComponentName(context, Settings.class));

        //Disable Shortcut picker
        disableComponent(pm, new ComponentName(context, CreateShortcutActivity.class));
    }

    private void disableComponent(PackageManager pm, ComponentName componentName) {
        pm.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    // Disable WebView Setting if the current user is not an admin
    private void webviewSettingSetup(Context context, PackageManager pm, UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        ComponentName settingsComponentName =
                new ComponentName(SETTINGS_PACKAGE_NAME,
                        SETTINGS_PACKAGE_NAME + WEBVIEW_IMPLEMENTATION_ACTIVITY);
        pm.setComponentEnabledSetting(settingsComponentName,
                userInfo.isAdmin() ?
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    // Refresh settings shortcuts to have correct intent flags
    @VisibleForTesting
    void refreshExistingShortcuts(Context context) {
        final ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        final List<ShortcutInfo> pinnedShortcuts = shortcutManager.getPinnedShortcuts();
        final List<ShortcutInfo> updates = new ArrayList<>();
        for (ShortcutInfo info : pinnedShortcuts) {
            if (info.isImmutable()) {
                continue;
            }
            final Intent shortcutIntent = info.getIntent();
            shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            final ShortcutInfo updatedInfo = new ShortcutInfo.Builder(context, info.getId())
                    .setIntent(shortcutIntent)
                    .build();
            updates.add(updatedInfo);
        }
        shortcutManager.updateShortcuts(updates);
    }

    private void enableTwoPaneDeepLinkActivityIfNecessary(PackageManager pm, Context context) {
        final ComponentName deepLinkHome = new ComponentName(context,
                DeepLinkHomepageActivity.class);
        final ComponentName searchStateReceiver = new ComponentName(context,
                SearchStateReceiver.class);
        final int enableState = ActivityEmbeddingUtils.isSettingsSplitEnabled(context)
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(deepLinkHome, enableState, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(searchStateReceiver, enableState,
                PackageManager.DONT_KILL_APP);
    }
}
