/**
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

package com.android.settings.applications;

import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.service.autofill.AutoFillService;
import android.service.autofill.AutoFillServiceInfo;
import android.util.AttributeSet;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.AppListPreferenceWithSettings;

import java.util.ArrayList;
import java.util.List;

public class DefaultAutoFillPreference extends AppListPreferenceWithSettings {
    private static final String TAG = "DefaultAutoFill";

    private static final String SETTING = Settings.Secure.AUTO_FILL_SERVICE;

    public DefaultAutoFillPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setSavesState(false);
        setShowItemNone(true);

        refreshData();
    }

    @Override
    protected CharSequence getConfirmationMessage(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        int index = findIndexOfValue(value);
        CharSequence[] entries = getEntries();
        if (index < 0 || index >= entries.length) {
            return null;
        }

        CharSequence entry = entries[index];
        return getContext().getString(R.string.autofill_confirmation_message, entry);
    }

    @Override
    protected boolean persistString(String value) {
        Settings.Secure.putString(getContext().getContentResolver(), SETTING, value);
        refreshData();
        return true;
    }

    private void refreshData() {
        ComponentName selectedComponent = getSelectedComponentName();
        List<AutoFillServiceInfo> infos = getInfos();

        AutoFillServiceInfo selectedInfo = null;
        int numberOfComponents = infos.size();
        ComponentName[] components = new ComponentName[numberOfComponents];
        for (int i = 0; i < numberOfComponents; ++i) {
            AutoFillServiceInfo info = infos.get(i);
            ServiceInfo serviceInfo = info.getServiceInfo();
            ComponentName component =
                    new ComponentName(serviceInfo.packageName, serviceInfo.name);
            components[i] = component;

            if (component.equals(selectedComponent)) {
                selectedInfo = info;
            }
        }

        ComponentName selectedComponentSettings = null;
        if (selectedInfo != null) {
            String settingsActivity = selectedInfo.getSettingsActivity();
            selectedComponentSettings = settingsActivity != null
                    ? new ComponentName(selectedComponent.getPackageName(), settingsActivity)
                    : null;
        } else { // selected component not found
            Log.w(TAG, "Selected AutoFillService not found " + selectedComponent);
            selectedComponent = null;
            selectedComponentSettings = null;
        }

        setComponentNames(components, selectedComponent);
        setSettingsComponent(selectedComponentSettings);
        setSummary(getEntry());
    }

    @Nullable
    private ComponentName getSelectedComponentName() {
        String componentString =
                Settings.Secure.getString(getContext().getContentResolver(), SETTING);
        if (componentString == null) {
            return null;
        }

        return ComponentName.unflattenFromString(componentString);
    }

    private List<AutoFillServiceInfo> getInfos() {
        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(
                new Intent(AutoFillService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);
        List<AutoFillServiceInfo> infos = new ArrayList<>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            AutoFillServiceInfo info = new AutoFillServiceInfo(pm, serviceInfo);
            infos.add(info);
        }
        return infos;
    }
}
