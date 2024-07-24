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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.dashboard.profileselector.ProfileSelectDialog;
import com.android.settings.utils.ContentCaptureUtils;

import java.util.ArrayList;
import java.util.List;

public final class EnableContentCaptureWithServiceSettingsPreferenceController
        extends TogglePreferenceController {

    private static final String TAG = "ContentCaptureController";

    public EnableContentCaptureWithServiceSettingsPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
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
    }

    @Override
    public int getAvailabilityStatus() {
        boolean available = ContentCaptureUtils.isFeatureAvailable()
                && ContentCaptureUtils.getServiceSettingsComponentName() != null;
        return available ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_privacy;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        show(preference);
        return true;
    }

    private void show(Preference preference) {
        final UserManager userManager = UserManager.get(mContext);
        final List<UserInfo> userInfos = userManager.getUsers();
        final ArrayList<UserHandle> userHandles = new ArrayList<>(userInfos.size());
        for (UserInfo info : userInfos) {
            userHandles.add(info.getUserHandle());
        }
        final Intent intent = preference.getIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (userHandles.size() == 1) {
            mContext.startActivityAsUser(intent, userHandles.get(0));
            return;
        }
        ProfileSelectDialog.createDialog(mContext, userHandles, (int position) -> {
            // Show menu on top level items.
            mContext.startActivityAsUser(intent, userHandles.get(position));
        }).show();
    }
}
