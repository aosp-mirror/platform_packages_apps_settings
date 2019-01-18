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

package com.android.settings.applications.assist;

import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionServiceInfo;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.internal.app.AssistUtils;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPreferenceController;
import com.android.settingslib.applications.DefaultAppInfo;

import java.util.List;

public class DefaultAssistPreferenceController extends DefaultAppPreferenceController {

    private final AssistUtils mAssistUtils;
    private final boolean mShowSetting;
    private final String mPrefKey;
    private final Intent mIntent;

    public DefaultAssistPreferenceController(Context context, String prefKey,
            boolean showSetting) {
        super(context);
        mPrefKey = prefKey;
        mShowSetting = showSetting;
        mAssistUtils = new AssistUtils(context);

        final String packageName = mPackageManager.getPermissionControllerPackageName();
        if (packageName != null) {
            mIntent = new Intent(Intent.ACTION_MANAGE_DEFAULT_APP)
                    .setPackage(packageName)
                    .putExtra(Intent.EXTRA_ROLE_NAME, RoleManager.ROLE_ASSISTANT);
        } else {
            mIntent = null;
        }
    }

    @Override
    protected Intent getSettingIntent(DefaultAppInfo info) {
        if (!mShowSetting) {
            return null;
        }
        final ComponentName cn = mAssistUtils.getAssistComponentForUser(mUserId);
        if (cn == null) {
            return null;
        }
        final Intent probe = new Intent(VoiceInteractionService.SERVICE_INTERFACE)
                .setPackage(cn.getPackageName());

        final List<ResolveInfo> services = mPackageManager.queryIntentServices(probe,
                PackageManager.GET_META_DATA);
        if (services == null || services.isEmpty()) {
            return null;
        }
        final String activity = getAssistSettingsActivity(cn, services.get(0), mPackageManager);
        if (activity == null) {
            return null;
        }
        return new Intent(Intent.ACTION_MAIN)
                .setComponent(new ComponentName(cn.getPackageName(), activity));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), "default_assist")) {
            if (mIntent != null) {
                mContext.startActivity(mIntent);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_assist_and_voice_input);
    }

    @Override
    public String getPreferenceKey() {
        return mPrefKey;
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        final ComponentName cn = mAssistUtils.getAssistComponentForUser(mUserId);
        if (cn == null) {
            return null;
        }
        return new DefaultAppInfo(mContext, mPackageManager, mUserId, cn);
    }

    @VisibleForTesting
    String getAssistSettingsActivity(ComponentName cn, ResolveInfo resolveInfo, PackageManager pm) {
        final VoiceInteractionServiceInfo voiceInfo =
            new VoiceInteractionServiceInfo(pm, resolveInfo.serviceInfo);
        if (!voiceInfo.getSupportsAssist()) {
            return null;
        }
        return voiceInfo.getSettingsActivity();
    }
}
