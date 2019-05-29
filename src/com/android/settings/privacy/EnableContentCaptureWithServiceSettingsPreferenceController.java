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

package com.android.settings.privacy;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.dashboard.profileselector.UserAdapter;
import com.android.settings.utils.ContentCaptureUtils;

import java.util.ArrayList;
import java.util.List;

public final class EnableContentCaptureWithServiceSettingsPreferenceController
        extends TogglePreferenceController {

    private static final String TAG = "ContentCaptureController";

    private final UserManager mUserManager;

    public EnableContentCaptureWithServiceSettingsPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);

        mUserManager = UserManager.get(context);
    }

    @Override
    public boolean isChecked() {
        return ContentCaptureUtils.isEnabledForUser(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        ContentCaptureUtils.setEnabledForUser(mContext, isChecked);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        ComponentName componentName = ContentCaptureUtils.getServiceSettingsComponentName();
        if (componentName != null) {
            preference.setIntent(new Intent(Intent.ACTION_MAIN).setComponent(componentName));
        } else {
            // Should not happen - preference should be disabled by controller
            Log.w(TAG, "No component name for custom service settings");
            preference.setSelectable(false);
        }

        preference.setOnPreferenceClickListener((pref) -> {
            ProfileSelectDialog.show(mContext, pref);
            return true;
        });
    }

    @Override
    public int getAvailabilityStatus() {
        boolean available = ContentCaptureUtils.isFeatureAvailable()
                && ContentCaptureUtils.getServiceSettingsComponentName() != null;
        return available ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private static final class ProfileSelectDialog {
        public static void show(Context context, Preference pref) {
            final UserManager userManager = UserManager.get(context);
            final List<UserInfo> userInfos = userManager.getUsers();
            final ArrayList<UserHandle> userHandles = new ArrayList<>(userInfos.size());
            for (UserInfo info: userInfos) {
                userHandles.add(info.getUserHandle());
            }
            if (userHandles.size() == 1) {
                final Intent intent = pref.getIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivityAsUser(intent, userHandles.get(0));
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                UserAdapter adapter = UserAdapter.createUserAdapter(userManager, context,
                        userHandles);
                builder.setTitle(com.android.settingslib.R.string.choose_profile)
                        .setAdapter(adapter, (DialogInterface dialog, int which) -> {
                            final UserHandle user = userHandles.get(which);
                            // Show menu on top level items.
                            final Intent intent = pref.getIntent()
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivityAsUser(intent, user);
                        })
                        .show();
            }
        }
    }

}
