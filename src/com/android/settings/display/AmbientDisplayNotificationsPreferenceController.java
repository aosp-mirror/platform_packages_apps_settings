/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.provider.Settings.Secure.DOZE_ENABLED;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_AMBIENT_DISPLAY;

public class AmbientDisplayNotificationsPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin,
        Preference.OnPreferenceChangeListener {

    private final int ON = 1;
    private final int OFF = 0;

    @VisibleForTesting
    static final String KEY_AMBIENT_DISPLAY_NOTIFICATIONS = "ambient_display_notification";
    private static final int MY_USER = UserHandle.myUserId();

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final AmbientDisplayConfiguration mConfig;

    public AmbientDisplayNotificationsPreferenceController(Context context,
            AmbientDisplayConfiguration config, MetricsFeatureProvider metricsFeatureProvider) {
        super(context);
        mMetricsFeatureProvider = metricsFeatureProvider;
        mConfig = config;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AMBIENT_DISPLAY_NOTIFICATIONS;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_AMBIENT_DISPLAY_NOTIFICATIONS.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, ACTION_AMBIENT_DISPLAY);
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(mConfig.pulseOnNotificationEnabled(MY_USER));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(), DOZE_ENABLED, value ? ON : OFF);
        return true;
    }

    @Override
    public boolean isAvailable() {
        return mConfig.pulseOnNotificationAvailable();
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSubsettingIntent(mContext,
                AmbientDisplaySettings.class.getName(), KEY_AMBIENT_DISPLAY_NOTIFICATIONS,
                mContext.getString(R.string.ambient_display_screen_title));

        return new InlineSwitchPayload(Settings.Secure.DOZE_ENABLED,
                ResultPayload.SettingsSource.SECURE, ON /* onValue */, intent, isAvailable(),
                ON /* defaultValue */);
    }
}
