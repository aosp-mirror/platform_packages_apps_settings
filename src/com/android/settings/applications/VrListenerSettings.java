/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.ComponentName;
import android.provider.Settings;
import android.service.vr.VrListenerService;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.ManagedServiceSettings;

public class VrListenerSettings extends ManagedServiceSettings {
    private static final String TAG = VrListenerSettings.class.getSimpleName();
    private static final Config CONFIG = getVrListenerConfig();

    private static final Config getVrListenerConfig() {
        final Config c = new Config();
        c.tag = TAG;
        c.setting = Settings.Secure.ENABLED_VR_LISTENERS;
        c.intentAction = VrListenerService.SERVICE_INTERFACE;
        c.permission = android.Manifest.permission.BIND_VR_LISTENER_SERVICE;
        c.noun = "vr listener";
        c.warningDialogTitle = R.string.vr_listener_security_warning_title;
        c.warningDialogSummary = R.string.vr_listener_security_warning_summary;
        c.emptyText = R.string.no_vr_listeners;
        return c;
    }

    @Override
    protected Config getConfig() {
        return CONFIG;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.VR_MANAGE_LISTENERS;
    }

    @Override
    protected boolean setEnabled(ComponentName service, String title, boolean enable) {
        logSpecialPermissionChange(enable, service.getPackageName());
        return super.setEnabled(service, title, enable);
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean enable, String packageName) {
        int logCategory = enable ? MetricsEvent.APP_SPECIAL_PERMISSION_VRHELPER_ALLOW
                : MetricsEvent.APP_SPECIAL_PERMISSION_VRHELPER_DENY;
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(),
                logCategory, packageName);
    }
}
