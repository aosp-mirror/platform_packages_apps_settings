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

package com.android.settings.notification;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.service.notification.ConditionProviderService;

import com.android.settings.R;

public class ConditionProviderSettings extends ManagedServiceSettings {
    private static final String TAG = ConditionProviderSettings.class.getSimpleName();
    private static final Config CONFIG = getConditionProviderConfig();

    private static Config getConditionProviderConfig() {
        final Config c = new Config();
        c.tag = TAG;
        c.setting = Settings.Secure.ENABLED_CONDITION_PROVIDERS;
        c.intentAction = ConditionProviderService.SERVICE_INTERFACE;
        c.permission = android.Manifest.permission.BIND_CONDITION_PROVIDER_SERVICE;
        c.noun = "condition provider";
        c.warningDialogTitle = R.string.condition_provider_security_warning_title;
        c.warningDialogSummary = R.string.condition_provider_security_warning_summary;
        c.emptyText = R.string.no_condition_providers;
        return c;
    }

    @Override
    protected Config getConfig() {
        return CONFIG;
    }

    public static int getProviderCount(PackageManager pm) {
        return getServicesCount(CONFIG, pm);
    }

    public static int getEnabledProviderCount(Context context) {
        return getEnabledServicesCount(CONFIG, context);
    }
}
