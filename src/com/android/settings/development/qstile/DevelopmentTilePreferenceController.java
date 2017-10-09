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

package com.android.settings.development.qstile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.service.quicksettings.TileService;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

public class DevelopmentTilePreferenceController extends AbstractPreferenceController {

    private final OnChangeHandler mOnChangeHandler;
    private final PackageManager mPackageManager;

    public DevelopmentTilePreferenceController(Context context) {
        super(context);
        mOnChangeHandler = new OnChangeHandler(context);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Context context = screen.getContext();
        Intent intent = new Intent(TileService.ACTION_QS_TILE)
                .setPackage(context.getPackageName());
        final List<ResolveInfo> resolveInfos = mPackageManager.queryIntentServices(intent,
                PackageManager.MATCH_DISABLED_COMPONENTS);
        for (ResolveInfo info : resolveInfos) {
            ServiceInfo sInfo = info.serviceInfo;
            final int enabledSetting = mPackageManager.getComponentEnabledSetting(
                    new ComponentName(sInfo.packageName, sInfo.name));
            boolean checked = enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    || ((enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
                    && sInfo.enabled);

            SwitchPreference preference = new SwitchPreference(context);
            preference.setTitle(sInfo.loadLabel(mPackageManager));
            preference.setIcon(sInfo.icon);
            preference.setKey(sInfo.name);
            preference.setChecked(checked);
            preference.setOnPreferenceChangeListener(mOnChangeHandler);
            screen.addPreference(preference);
        }
    }

    private static class OnChangeHandler implements Preference.OnPreferenceChangeListener {

        private final Context mContext;
        private final PackageManager mPackageManager;

        public OnChangeHandler(Context context) {
            mContext = context;
            mPackageManager = context.getPackageManager();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ComponentName cn = new ComponentName(
                    mContext.getPackageName(), preference.getKey());
            mPackageManager.setComponentEnabledSetting(cn, (Boolean) newValue
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            return true;
        }
    }
}
